import glob
import os
import pickle

import numpy as np
import torch
import torch.nn.functional as F
from torch.optim import Adam
from torch_geometric.data import DataLoader
from tqdm import tqdm

from dataloader import ASTEncoder, load
from model import get_classification_model
from word2vec import Word2VecBuilder

trainset = "ffmpeg"  # ffmpeg or qemu
testset = "ffmpeg"  # ffmpeg qemu libxml2 tinyproxy curl openssl or libxml2

# Set up hyperparameters for the model as outlined in the paper
GIN_CLASSIFIER = {
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
LR = 0.0001
EPOCHS = 10

model = get_classification_model(GIN_CLASSIFIER).encoder.node_level_encoder

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
print("Loading samples, this may take a while but will be cached")
traingraphs = []
testgraphs = []


def loadfile(g, label):
    name = g.split("/")[-1]
    if os.path.exists("cache/" + name):
        file = open("cache/" + name, 'rb')
        object_file = pickle.load(file)
        return object_file
    else:
        graphobj = load(g, ast, w2v, label)
        with open("cache/" + name, 'wb') as fp:
            pickle.dump(graphobj, fp)
        return graphobj


print("Collecting dataset")

for g in tqdm(glob.glob("data/{}/benign/*.cpg".format(trainset))):
    traingraphs.append(loadfile(g, 0))

for g in tqdm(glob.glob("data/{}/vuln/*.cpg".format(trainset))):
    traingraphs.append(loadfile(g, 1))

if not trainset == testset:
    for g in tqdm(glob.glob("data/{}/benign/*.cpg".format(testset))):
        testgraphs.append(loadfile(g, 0))

    for g in tqdm(glob.glob("data/{}/vuln/*.cpg".format(testset))):
        testgraphs.append(loadfile(g, 1))
    train_loader = DataLoader(traingraphs, 4, shuffle=True)
    test_loader = DataLoader(testgraphs, 1, shuffle=False)
else:
    # 70/20 Split for the dataset
    np.random.shuffle(traingraphs)
    train_loader = DataLoader(traingraphs[:int(len(traingraphs) * 70 / 100)], 4, shuffle=True)
    test_loader = DataLoader(traingraphs[int(len(traingraphs) * 70 / 100):], 1, shuffle=False)

# Initialize the bound information: If lower/upper bound has no information == 0 otherwise 1
print("Adding bound information")
for data in traingraphs:
    upper = [int(x != "\"EMPTY_STRING\"") for x in data.upperBound]
    lower = [int(x != "\"EMPTY_STRING\"") for x in data.lowerBound]
    data.x = torch.cat((data.x, torch.tensor(upper).view(-1, 1), torch.tensor(lower).view(-1, 1)), 1)
for data in testgraphs:
    upper = [int(x != "\"EMPTY_STRING\"") for x in data.upperBound]
    lower = [int(x != "\"EMPTY_STRING\"") for x in data.lowerBound]
    data.x = torch.cat((data.x, torch.tensor(upper).view(-1, 1), torch.tensor(lower).view(-1, 1)), 1)


# 70/20 Split for the dataset

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
print("Start Training")
optimizer = Adam(model.parameters(), lr=LR)
for epoch in range(1, EPOCHS):
    train_loss, loss_c, loss_o, loss_co, train_acc = train(model, optimizer, train_loader, "cpu")
    test_acc, test_acc_c, test_acc_o = eval_acc(model, test_loader, "cpu")
print("The Model is trained and saved under model.model, the stats are:")
print("training performance Loss: ", train_loss, " Accuracy: ", train_acc)
print("test Accuracy: ", test_acc)

torch.save(model, "model.model")
