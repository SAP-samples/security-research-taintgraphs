FROM python:3.8.10
COPY . /src/
RUN apt update && apt install zip unzip cmake software-properties-common -y
WORKDIR /src/data
RUN unzip dataset.zip
WORKDIR /src/
RUN python3 -m pip install --upgrade pip
RUN pip3 install torch==1.13.1 --index-url https://download.pytorch.org/whl/cpu
RUN pip3 install  ninja && pip3 install -r requirements.txt
ENTRYPOINT ["sh", "-c", "python3 main.py"]
