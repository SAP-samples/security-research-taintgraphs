import os
import torch

from torch import nn
import torch.nn.functional as F
from torch_geometric.nn import GINConv, Linear
from torch_geometric.nn import BatchNorm, GraphNorm
from torch_geometric.nn import global_mean_pool, global_max_pool, global_add_pool
from causalmodel import CausalGIN
class Struct:
        def __init__(self, **entries):
                    self.__dict__.update(entries)

class AbstractGNNEncoder(torch.nn.Module):
    def __init__(self, **params):
        super(AbstractGNNEncoder, self).__init__()
        self.params = params

    def save(self, path):
        if not os.path.isdir(path):
            os.mkdir(path)
        path = os.path.join(path, "statedict.pt")
        torch.save(self.state_dict(), path)
    
    def load(self, path):
        path = os.path.join(path, "statedict.pt")
        self.load_state_dict(torch.load(path, map_location=torch.device('cpu')))
    
    def encode(self, graph):
        return self(graph.x, graph.edge_index, graph.batch)
    
    def get_params(self):
        return self.params    

class CGINEncoder(AbstractGNNEncoder):
    def __init__(self, **params):
        if params.get("dropout") is None:
            params["dropout"] = 0.0
        super(CGINEncoder, self).__init__(**params)
        
        assert params["num_layers"] > 0

        self.layer = CausalGIN(params["features"], 2,
                               Struct(layers=params["num_layers"], fc_num=params["hidden_channels"], cat_or_add="cat", hidden=params["hidden_channels"]))  
    def forward(self, data, *args, **kwargs):
        
        return self.layer(data)


class GINEncoder(AbstractGNNEncoder):
    def __init__(self, **params):
        if params.get("dropout") is None:
            params["dropout"] = 0.0
        super(GINEncoder, self).__init__(**params)
        
        assert params["num_layers"] > 0
        assert params["layer_type"] == "GIN"

        self.layers = torch.nn.ModuleList([])
        self.norms = torch.nn.ModuleList([])

        for i in range(self.params["num_layers"]):
            if i == 0:
                self.layers.append(GINConv(
                    nn.Sequential(
                        nn.Linear(params["features"], params["hidden_channels"]),
                        nn.ReLU(),
                        nn.Linear(params["hidden_channels"], params["hidden_channels"]),
                        nn.ReLU(),
                        nn.BatchNorm1d(params["hidden_channels"]),
                ), train_eps=False))
            else:
                self.layers.append(GINConv(
                    nn.Sequential(
                        nn.Linear(params["hidden_channels"], params["hidden_channels"]),
                        nn.ReLU(),
                        nn.Linear(params["hidden_channels"], params["hidden_channels"]),
                        nn.ReLU(),
                        nn.BatchNorm1d(params["hidden_channels"]),
                ), train_eps=False))
            if params["norm_type"] == "None":
                self.norms.append(None)
            elif params["norm_type"] == "BatchNorm":
                self.norms.append(BatchNorm(params["hidden_channels"]))
            elif params["norm_type"] == "GraphNorm":
                self.norms.append(GraphNorm(params["hidden_channels"]))
            else:
                raise ValueError(f"Unknown norm_type {params['norm_type']}")
    
    def forward(self, x, edge_index, batch, *args, **kwargs):
        initial_x = x
        for i, (conv, norm) in enumerate(zip(self.layers, self.norms)):
            x_old = x
            x = conv(x, edge_index)
            if norm is not None:
                x = norm(x)
            # x = F.dropout(x, p=self.params["dropout"], training=self.training)
            if i == len(self.layers) - 1:
                x = x
            elif i == 0:
                x = F.relu(x)
            else:
                x = F.relu(x + x_old)
        
        return x


class MLPClassifier(torch.nn.Module):
    def __init__(self, params):
        super(MLPClassifier, self).__init__()
        if params.get("dropout") is None:
            params["dropout"] = 0.0
        self.params = params

        self.layers = torch.nn.ModuleList([])

        now_channels = params["features"]
        for i in range(params["num_layers"]):
            if i == params["num_layers"] - 1:
                self.layers.append(Linear(now_channels, params["classes"]))
            elif i == 0:
                self.layers.append(Linear(now_channels, now_channels))
            else:
                self.layers.append(Linear(now_channels, now_channels//2))
                now_channels = now_channels // 2
    
    def forward(self, x):
        for i, lin in enumerate(self.layers):
            x = lin(x)
            if i == len(self.layers) - 1:
                x = x
            else:
                x = F.relu(x)
                x = F.dropout(x, p=self.params["dropout"], training=self.training)
        if self.params["classes"] == 1:
            x = x.reshape(-1)
        return x
    
    def get_params(self):
        return self.params
    
    def classify(self, representation):
        return self(representation)

    def save(self, path):
        if not os.path.isdir(path):
            os.mkdir(path)
        path = os.path.join(path, "statedict.pt")
        torch.save(self.state_dict(), path)
    
    def load(self, path):
        path = os.path.join(path, "statedict.pt")
        self.load_state_dict(torch.load(path, map_location=torch.device('cpu')))


class GraphClassifier(torch.nn.Module):
    def __init__(self, encoder, classifier):
        super(GraphClassifier, self).__init__()
        self.encoder = encoder
        self.classifier = classifier
    
    def get_params(self):
        return {
            "type": "GraphClassifier",
            "encoder": self.encoder.get_params(),
            "classifier": self.classifier.get_params()
        }
    
    def classify(self, graph):
        representation = self.encoder.encode(graph)
        pred = self.classifier.classify(representation)

        return pred
    
    def save(self, path):
        if not os.path.isdir(path):
            os.mkdir(path)
        self.encoder.save(os.path.join(path, "encoder"))
        self.classifier.save(os.path.join(path, "classifier"))
    
    def load(self, path):
        self.encoder.load(os.path.join(path, "encoder"))
        self.classifier.load(os.path.join(path, "classifier"))


class CompositeGraphLevelEncoder(torch.nn.Module):
    def __init__(self, node_level_encoder, pooling):
        super(CompositeGraphLevelEncoder, self).__init__()
        self.node_level_encoder = node_level_encoder
        self.pooling = pooling
    
    def encode(self, graph):
        nodes_representation = self.node_level_encoder.encode(graph)
        return self.pooling.pool(nodes_representation, graph.batch)
    
    def save(self, path):
        if not os.path.isdir(path):
            os.mkdir(path)
        self.node_level_encoder.save(os.path.join(path, "encoder"))
        self.pooling.save(os.path.join(path, "pooling"))
    
    def load(self, path):
        self.node_level_encoder.load(os.path.join(path, "encoder"))
        self.pooling.load(os.path.join(path, "pooling"))
    
    def get_params(self):
        return {
            "type": "CompositeGraphLevel",
            "encoder": self.node_level_encoder.get_params(),
            "pooling": self.pooling.get_params()
        }


class AbstractParameterlessPooling(torch.nn.Module):
    def __init__(self, params):
        super(AbstractParameterlessPooling, self).__init__()
        self.params = params

    def save(self, path):
        pass
    
    def load(self, path):
        pass
    
    def loss_keys(self):
        return []
    
    def loss(self, representation, batch):
        return (self.pool(representation, batch), {})
    
    def get_params(self):
        return self.params


class MeanPooling(AbstractParameterlessPooling):
    def pool(self, representation, batch):
        return global_mean_pool(representation, batch)


class SumPooling(AbstractParameterlessPooling):
    def pool(self, representation, batch):
        return global_add_pool(representation, batch)


class MaxPooling(AbstractParameterlessPooling):
    def pool(self, representation, batch):
        return global_max_pool(representation, batch)


def get_classification_model(params):
    params["encoder"]["features"] = params["features"]
    encoder = get_encoder(params["encoder"])
    
    params["classifier"]["features"] = params["encoder"]["hidden_channels"]
    params["classifier"]["classes"] = params["classes"]
    classifier = get_classifier(params["classifier"])

    return GraphClassifier(encoder, classifier)


def get_encoder(params):
    if params.get("type") == "GraphComposite":
        pooling = get_pooling(params["pooling"])
        params["encoder"]["features"] = params["features"]
        params["hidden_channels"] = params["encoder"]["hidden_channels"]
        encoder = get_encoder(params["encoder"])

        return CompositeGraphLevelEncoder(encoder, pooling)
    if params["layer_type"] == "GIN":
        return GINEncoder(**params)
    if params["layer_type"] == "CGIN":
        return CGINEncoder(**params)
    raise ValueError(f"Could not construct encoder for {params}")


def get_pooling(params):
    if params.get("type") == "mean":
        return MeanPooling(params)
    if params.get("type") == "max":
        return MaxPooling(params)
    if params.get("type") == "sum":
        return SumPooling(params)
    
    raise ValueError(f"Could not construct pooling for {params}")


def get_classifier(params):
    if params.get("layer_type") == "MLP":
        return MLPClassifier(params)
    
    raise ValueError(f"Could not construct classifier for {params}")
