import os
import glob
import gzip
import pickle

from tqdm import tqdm
from collections import defaultdict

import torch
import numpy as np
import networkx as nx
import torch_geometric
from sklearn.preprocessing import OneHotEncoder


class ASTEncoder(object):
    def __init__(self, params, overwrite_cache=False):
        self.params = params
        self.overwrite_cache = overwrite_cache
        self.cache_dir = params["cache_dir"]
        self.ast_dict = None

        if not os.path.exists(self.cache_dir):
            os.mkdir(self.cache_dir)
    
    def _get_cache_path(self):
        return os.path.join(self.cache_dir, "asttypes.pkl.gz")
    
    def _clean(self, s):
        if len(s) > 0 and s[0] == s[-1] and s[0] in ["'", '"']:
            return s[1:-1].strip()
        return s.strip()

    def build_astdict(self):
        asttypes = list(self.get_asttypes().keys())
        asttypes.append("UNKNOWN")
        self.astenc = OneHotEncoder(handle_unknown="ignore", sparse=False)
        self.astenc.fit(np.array(asttypes).reshape(-1, 1))
        self.ast_dict = {asttype: np.squeeze(self.astenc.transform(np.array(asttype).reshape(1, -1))) 
                         for asttype in asttypes}

    def get_asttypes(self):
        if os.path.isfile(self._get_cache_path()) and not self.overwrite_cache:
            with gzip.open(self._get_cache_path(), "r") as f:
                return pickle.load(f)
        asttypes = defaultdict(int)
        for directory, modern in self.params["training_files"]:
            cpg_files = list(glob.glob(os.path.join(directory, "**", "*.cpg")))
            assert len(cpg_files) > 0, "Must find cpg files for AST labels"

            print(f"Loading cpg files for {directory}")
            for path in tqdm(cpg_files):
                with open(path, "r", encoding='utf-8', errors='ignore') as f:
                    graph = read_dot(f, modern)

                    for node_id in graph:
                        node = graph.nodes[node_id]
                        if node.get("label") is None:
                            asttypes["UNKNOWN"] += 1
                            continue
                        if modern:
                            asttypes[self._clean(node["label"])] += 1
                        else:
                            information = ",".join(node["label"].split(",")[:-1])
                            paramsplit = information.split(",")
                            ast = paramsplit[0].strip()
                            asttypes[self._clean(ast)] += 1
        
        with gzip.open(self._get_cache_path(), "w") as f:
            pickle.dump(asttypes, f)

        return asttypes


def load(p, astencoder, w2v, label):
    with open(p, "r") as f:
        dot = read_dot(f, True)
        dot.graph["label"] = torch.tensor(label) # label unknown

        data = encode(dot, astencoder, w2v)

        xs = [data.astenc, data.codeenc]
        data.x = torch.cat(xs, dim=-1).float()

        return data


def encode(graph, astencoder, w2v):
    for node_id in graph:
        node = graph.nodes[node_id]
        node["ast"] = node["label"]
        node["lines"] = node["location"]
        node["code"] = node["enclosing"]
    
    for node in graph:
        asttype = astencoder._clean(graph.nodes[node]["ast"])
        if not asttype in astencoder.ast_dict:
            asttype = "UNKNOWN"
        graph.nodes[node]["astenc"] = astencoder.ast_dict[asttype]
        graph.nodes[node]["codeenc"] = w2v.get_embedding(graph.nodes[node]["code"])
    try:
        torch_graph = from_networkx_multi(graph)
        torch_graph.y = graph.graph["label"]
        return torch_graph
    except Exception as e:
        print(f"failed with {repr(e)}")


def read_dot(f, modern):
    if modern:
        return nx.Graph(nx.drawing.nx_pydot.read_dot(f))
    else:
        content = f.read()
        if content == "":
            raise ValueError(f"{path} is empty")
        for num, line in enumerate(content.split("\n")):
            if "digraph G {" in line:
                content = "\n".join(content.replace("yodigraph", "digraph").split("\n")[num:])
                break
        return _forgiving_dot_parser(content)


def _forgiving_dot_parser(content):
    graph = nx.Graph()
    lines = content.split("\n")
    for line in lines:
        if len(line) <= 0:
            continue
        if line[0] == "\"":
            name = line.split(" ")[0].replace("\"", "")
            label = "".join(line.split(" [label = \"(")[1])[:-3]
            graph.add_node(name, label=label)
        elif line[0] == " ":
            splitted = line.split(" -> ")
            lnode = splitted[0].replace("\"", "")
            rnode = splitted[1].replace("\"", "").split(" ")[0]
            graph.add_edge(lnode.strip(), rnode.strip())

    return graph

# from: https://github.wdf.sap.corp/GRAPHXAI/SARDDataset/blob/0ac5f8f28f1cd363240b712fdd28f8461f7958f4/src/lib/utils.py#L287
def from_networkx_multi(G):
    # original code: https://pytorch-geometric.readthedocs.io/en/latest/_modules/torch_geometric/utils/convert.html#from_networkx

    G = nx.convert_node_labels_to_integers(G)
    G = G.to_directed() if not nx.is_directed(G) else G
    edge_index = torch.LongTensor(list(G.edges)).t().contiguous()
    if isinstance(G, (nx.MultiDiGraph, nx.MultiGraph)):
        edge_index = edge_index[0:2, :]

    data = {}

    for i, (_, feat_dict) in enumerate(G.nodes(data=True)):
        for key, value in feat_dict.items():
            data[str(key)] = [value] if i == 0 else data[str(key)] + [value]

    for i, (_, _, feat_dict) in enumerate(G.edges(data=True)):
        for key, value in feat_dict.items():
            data[str(key)] = [value] if i == 0 else data[str(key)] + [value]

    for key, item in data.items():
        try:
            # print(key)
            # print(item)
            if type(item) is list and len(item) > 0 and type(item[0]) is np.ndarray:
                item = np.stack(item)
            data[key] = torch.tensor(item)
        except ValueError:
            pass

    data['edge_index'] = edge_index.view(2, -1)
    data = torch_geometric.data.Data.from_dict(data)
    data.num_nodes = G.number_of_nodes()

    return data
