from model import get_classification_model
from word2vec import Word2VecBuilder
from dataloader import ASTEncoder, load
from torch_geometric.data import DataLoader
import glob
from tqdm import tqdm
import torch.nn.functional as F
import torch
import os
import pickle
import numpy as np
from torch.optim import Adam
from torch_geometric.data import DataLoader, DenseDataLoader as DenseLoader
from torch_geometric.data import Batch

# Which dataset? ffmpeg or qemu
dataset = "ffmpeg"

# Set up hyperparameters for the model as outlined in the paper
BASELINE_GIN_CLASSIFIER = {
    "type": "GraphClassifier",
    "name": "BASELINE_GIN",
    "features": 152,
    "classes": 1,
    "encoder": {
        "type": "GraphComposite",
        "pooling": {
            "type": "sum"
        },
        "encoder": {
            "num_layers": 3,
            "hidden_channels": 128,
            "layer_type": "CGIN",
            "norm_type": "None"
        }
    },
    "classifier": {
        "layer_type": "MLP",
        "dropout": 0.5,
        "num_layers": 3
    }
}

model = get_classification_model(BASELINE_GIN_CLASSIFIER).encoder.node_level_encoder

# Configure the word2vec embedding
w2v = Word2VecBuilder({
    "vector_size": 100,
    "window": 3,
    "training_files": [],
    "cache_dir": "data"
})
w2v.build_model()

# Configure the AST Label One-Hot Encoding 
ast = ASTEncoder({"cache_dir": "data"})
ast.build_astdict()

# Load the samples
graphs = []
def loadfile(g, label):
    name = g.split("/")[-1]
    if os.path.exists("cache/"+name):
        file = open("cache/"+name,'rb')
        object_file = pickle.load(file)
        return object_file
    else:
        graphobj = load(g, ast, w2v, label)
        with open("cache/"+name, 'wb') as fp:
            pickle.dump(graphobj, fp)
        return graphobj

for g in tqdm(glob.glob("data/{}/benign/*.cpg".format(dataset))):
    graphs.append(loadfile(g, 0))
    
for g in tqdm(glob.glob("data/{}/vuln/*.cpg".format(dataset))):
    graphs.append(loadfile(g, 1))

# Initialize the bound information: If lower/upper bound has no information == 0 otherwise 1
for data in graphs:    
    upper = [int(x!="\"EMPTY_STRING\"") for x in data.upperBound]
    lower = [int(x!="\"EMPTY_STRING\"") for x in data.lowerBound]
    data.x = torch.cat((data.x, torch.tensor(upper).view(-1,1),torch.tensor(lower).view(-1,1)), 1)

# 70/20 Split for the dataset
np.random.shuffle(graphs)
train_loader = DataLoader(graphs[:int(len(graphs)*70/100)], 4, shuffle=True)
test_loader = DataLoader(graphs[int(len(graphs)*70/100):], 1, shuffle=False)

def train(model, optimizer, loader, device):
    # Training Loop
    model.train()
    total_loss = 0
    total_loss_c = 0
    total_loss_o = 0
    total_loss_co = 0
    correct_o = 0
    
    for it, data in tqdm(enumerate(loader)):
        
        optimizer.zero_grad()
        data = data.to(device)

        one_hot_target = data.y.view(-1)
        c_logs, o_logs, co_logs = model(data)
        uniform_target = torch.ones_like(c_logs, dtype=torch.float).to(device) / 2
        
        c_loss = F.kl_div(c_logs, uniform_target, reduction='batchmean')
        o_loss = F.nll_loss(o_logs, one_hot_target)
        co_loss = F.nll_loss(co_logs, one_hot_target)
        loss = 0.1 * c_loss + 0.8 * o_loss + 0.1 * co_loss

        pred_o = o_logs.max(1)[1]
        correct_o += pred_o.eq(data.y.view(-1)).sum().item()
        loss.backward()
        total_loss += loss.item() * 4
        total_loss_c += c_loss.item() * 4
        total_loss_o += o_loss.item() * 4
        total_loss_co += co_loss.item() * 4
        optimizer.step()
    
    num = len(loader.dataset)
    total_loss = total_loss / num
    total_loss_c = total_loss_c / num
    total_loss_o = total_loss_o / num
    total_loss_co = total_loss_co / num
    correct_o = correct_o / num
    return total_loss, total_loss_c, total_loss_o, total_loss_co, correct_o

def eval_acc(model, loader, device):
    model.eval()
    correct = 0
    correct_c = 0
    correct_o = 0
    for data in tqdm(loader):
        data = data.to(device)
        with torch.no_grad():
            c_logs, o_logs, co_logs = model(data)
            pred = co_logs.max(1)[1]
            pred_c = c_logs.max(1)[1] 
            pred_o = o_logs.max(1)[1] 
        correct += pred.eq(data.y.view(-1)).sum().item()
        correct_c += pred_c.eq(data.y.view(-1)).sum().item()
        correct_o += pred_o.eq(data.y.view(-1)).sum().item()

    acc_co = correct / len(loader.dataset)
    acc_c = correct_c / len(loader.dataset)
    acc_o = correct_o / len(loader.dataset)
    return acc_co, acc_c, acc_o

# Train PAVUDI
optimizer = Adam(model.parameters(), lr=0.0001)
for epoch in range(1, 10):
    train_loss, loss_c, loss_o, loss_co, train_acc = train(model, optimizer, train_loader, "cpu")
    test_acc, test_acc_c, test_acc_o = eval_acc(model, test_loader, "cpu")
print("The Model is trained and saved under model.model, the stats are:")
print("training performance Loss: ", train_loss, " Accuracy: ", train_acc)
print("test Accuracy: ", test_acc)

torch.save(model, "model.model")
