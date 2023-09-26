# PAVUDI Model and Data

Here lies the PAVUDI model and dataset for the paper "PAVUDI: Patch-based Vulnerability Discovery
using Machine Learning" submitted to the ACSAC'23 conference.

PAVUDI is a causal graph isomorphism network trained on taint graphs to detect vulnerabilities inside patches.

Disclaimer: 
The data extraction takes some time, hence we publish the dataset here containing the FFmpeg and QEMU taint graphs.
The training and data preprocessing pipeline may also take a while.

# Usage

## Requirements
For model training and evaluation:
- Python@3.8
- Pytorch 1.13.1 (CPU only)
- Pytorch Geometric

For data extraction:
- Kotlin 1.15.10
- JVM >= 11

Time requirements:
- For data extraction multiple days depending on the repository size (tested with c4.4xlarge EC2)
- For model training (about 1 hour on g4dn EC2 or 8 hours on MacBook Pro 2019)

## Preparations

Download the dataset from https://drive.google.com/file/d/1gKJ3RCG97igLX0JSV8FMWLRJv4Wygzg4/view?usp=sharing and place it to the data folder.

## Run via Docker

To train and evaluate the model using a docker instance simply use:

```
docker build . -t pavudi
docker run pavudi
```

## Run without Docker

We used Python@3.8 on a MacBook Pro 2020 and Ubuntu g4dn EC2 instance.


- Install Python requirements
```
python3.8 -m venv env
source env/bin/activate
pip3 install torch==1.13.1  --index-url https://download.pytorch.org/whl/cpu
pip3 install -r requirements.txt
```

- Unzip the data

```
cd data
unzip dataset.zip
```

Running the preprocessing and training step via:

```
python3.8 main.py
```
effectively loads the data, preprocesses (and caches) it and finally trains and evaluates the model.


## Expected Output
The model is trained on the provided data and the balanced accuracy will be reported at the end and the trained model stored. The command line output could resemble something like this:
```
Loading wordvectors.
100%|██████████| 3037/3037 [2:52:18<00:00,  3.40s/it]
100%|██████████| 2558/2558 [4:06:36<00:00,  5.78s/it]
979it [01:18, 12.47it/s]
100%|██████████| 1679/1679 [00:30<00:00, 54.51it/s]
979it [01:15, 13.03it/s]
100%|██████████| 1679/1679 [00:31<00:00, 52.83it/s]
979it [01:16, 12.81it/s]
100%|██████████| 1679/1679 [00:29<00:00, 57.85it/s]
979it [01:15, 13.04it/s]
100%|██████████| 1679/1679 [00:28<00:00, 58.03it/s]
979it [01:17, 12.65it/s]
100%|██████████| 1679/1679 [00:28<00:00, 59.22it/s]
979it [01:21, 11.95it/s]
100%|██████████| 1679/1679 [00:31<00:00, 53.89it/s]
979it [01:18, 12.44it/s]
100%|██████████| 1679/1679 [00:30<00:00, 54.61it/s]
979it [01:16, 12.85it/s]
100%|██████████| 1679/1679 [00:31<00:00, 54.13it/s]
979it [01:17, 12.55it/s]
100%|██████████| 1679/1679 [00:31<00:00, 53.69it/s]
The Model is trained and saved under model.model, the stats are:
training performance Loss:  0.39560627199108195  Accuracy:  0.7788559754851889
test Accuracy:  0.821917808219178
```

# Configuration and Parameters

First of all in the beginning of main.py we define the trainset and testset. When the train and testset are the same (e.g. ffmpeg or qemu) we do a 70/30 split.
Otherwise we train on the full trainset and cross evaluate on the testset (e.g. libxml2, curl, openssl, tinyproxy) 

Next, we define all model training specific parameters:
```
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
```

It uses 
- 152 features: 100 W2V Embedding Size, 50 Abstract Syntrax Label Encoding and 2 boolean feature for bounds. Don't change These!
- 3 Graph Isomorphism Networks (GIN Layers) followed by the Causal GIN Layer as outlined in the paper.
- CGIN is the layer type and can only be replaced by "GIN" without the causal structure learning part.
- We use a sum-Pooling which can be replaced by "max" or "mean" to pool the graph nodes to a single feature space
- no layer normalization. However, "norm_type": "BatchNorm" and "norm_type": "GraphNorm" for batch and graph normalization would be possible as well.
- 128 hidden_channels
- Classifier uses an MLP (no other choice right now)
- dropout of 0.5
- 3 MLP layers


# Analyzing new Datasets

The taint graphs can be extracted from the parent project.
- Install the parent project as in https://github.com/SAP-samples/security-research-taintgraphs
Assuming you are in security-research-taintgraphs/PAVUDI
```
cd ../
./gradlew installDist
cd PAVUDI
```
- You need a text file e.g. newdataset.txt
- The content should be in the following format:
```
REPO URL
INITIAL COMMIT
Commit that fixed vulnerability #1
Commit that fixed vulnerability #2
...
Commit that fixed vulnerability #n
...
```
The commits should be ordered by time
- Set up the database, e.g.
```
docker pull memgraph/memgraph-platform
docker image tag memgraph/memgraph-platform memgraph
docker run --memory="15g" -it -p 7687:7687 -p 3000:3000 -e MEMGRAPH="--query-execution-timeout-sec=180000 --bolt-session-inactivity-timeout=1800000 --query-max-plans=100000 --log-level DEBUG --memory-limit=15000 --storage-wal-enabled=false --storage-snapshot-interval-sec=0" memgraph
```
- Extract the taint graphs:
```
mkdir data/newdataset
build/install/security-research-taintgraphs/bin/security-research-taintgraphs --gitFile data/qemu.txt --host localhost --port 7687 --protocol bolt --output data
``` 
- Then configure the dataset in main.py and replace ffmpeg by newdataset 
# Content
- .. : Taintgraph Extraction Tool
- data/ : Dataset (FFmpeg + QEMU)
- cache/ : Cache Folder for preprocessed data
- main.py : Training and Evaluation Script
- model.py : Model Structure
- causalmodel.py : CGIN Implementation
- requirements.txt : needed Python libs
- word2vec.py : Word2Vec model management


