# PAVUDI Model and Data

Here lies the PAVUDI model and dataset for the paper "PAVUDI: Patch-based Vulnerability Discovery
using Machine Learning" submitted to the ACSAC'23 conference.

PAVUDI is a causal graph isomorphism network trained on tained graphs to detect vulnerabilities inside patches.

Disclaimer: 
The data extraction takes some time, hence we publish the dataset here containing the FFmpeg and QEMU taintgraphs.
The training and data preprocessing pipeline may also take a while.


## Install

We used Python@3.8 on a MacBook Pro 2020 and Ubuntu g4dn EC2 instance.

- Install Python requirements
```
python3.8 -m venv env
source env/bin/activate
pip3 install torch==1.13.1
pip3 install -r requirements.txt
```

- Unzip the data

```
cd data
unzip dataset.zip
```

## Run the model

Running the preprocessing and training step via:
```
python3.8 main.py
```
effectively loads the data, preprocesses (and caches) it and finally trains and evaluates the model.


## Extracting the data

The taint graphs can be extracted from the parent project. We recommend using the Memgraph database.
Follow the instruction from the readme from the parent directory and use the respective ffmpeg.txt or qemu.txt from the data folder.

## Content
- .. : Taintgraph Extraction Tool
- data/ : Dataset (FFmpeg + QEMU)
- cache/ : Cache Folder for preprocessed data
- main.py : Training and Evaluation Script
- model.py : Model Structure
- causalmodel.py : CGIN Implementation
- requirements.txt : needed Python libs
- word2vec.py : Word2Vec model management

