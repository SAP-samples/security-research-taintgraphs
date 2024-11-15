# Taintgraph Extraction

[![REUSE status](https://api.reuse.software/badge/github.com/SAP-samples/security-research-taintgraphs)](https://api.reuse.software/info/github.com/SAP-samples/security-research-taintgraphs)


This tool reads Java and C++ code, exports its
- Control Flow Graph
- Data Flow Graph
- Abstract Syntax tree

## Requirements

- Kotlin 1.5.10
- JVM >= 11

## Build

Build using Gradle

```
git clone https://github.com/SAP-samples/security-research-taintgraphs
cd security-research-taintgraphs
./gradlew installDist
```

## Recommended Database

We find that Memgraph has the most efficient performance. Comparing RedisGraph, Neo4J and Memgraph.
The running command for Memgraph is slightly optimized to trade transactions guarantees for speed.
```
docker pull memgraph/memgraph-platform
docker image tag memgraph/memgraph-platform memgraph
docker run --memory="15g" -it -p 7687:7687 -p 3000:3000 -e MEMGRAPH="--query-execution-timeout-sec=180000 --bolt-session-inactivity-timeout=1800000 --query-max-plans=100000 --log-level DEBUG --memory-limit=15000 --storage-wal-enabled=false --storage-snapshot-interval-sec=0" memgraph
```

## Modes

### Paths/Taintgraph extraction
Export paths between user-controlled sources and sinks with intersections between Gitdiff changes:

This mode needs a running cypherql capable database up and running, e.g.:
```
╰─ build/install/security-research-taintgraphs/bin/security-research-taintgraphs --gitFile data/libxml2_git.txt --host localhost --port 7687 --protocol bolt --output out
```
The above command will use a graph database accessible via Bolt. 
The extracted paths will be written to %out% directory with the pattern %out%/%commit%.cpg

The git text file should have following format, but mind it is best advise to sort the commits by time. The extraction tool only updates Deltas for efficiency. The initial commit should be close to the first vulnerable commit, regarding the committing date.


```
http://GITREPO/URL
FIRST INISTAL COMMIT
Commit that fixed vulnerability #1
Commit that fixed vulnerability #2
...
Commit that fixed vulnerability #n
```

### Load CPG to DB
This mode will load a path to a project into a graph database.
```
╰─ build/install/security-research-taintgraphs/bin/security-research-taintgraphs --file path/to/project --host localhost --port 7687 --protocol redis
```

### Simply export a CPG as Dot
```
╰─ build/install/security-research-taintgraphs/bin/security-research-taintgraphs --file path/to/project --output out
```

## Example output
(Old image)
```Java
public class Implementor1  {

    private final static int i = 6;

    public static void main( String... args ) {
        final int j;
        j = 14;
        System.out.println(j);
    }
}
```

![visualized dot](example.png "visualized dot example")


## Requirements

The application requires Java 11 or higher.

## Optional commands

```
--tmpPath <Path to a temporary folder used for Git>
```

## Contributing
If you wish to contribute code, offer fixes or improvements, please send a pull request. Due to legal reasons, contributors will be asked to accept a DCO when they create the first pull request to this project. This happens in an automated fashion during the submission process. SAP uses [the standard DCO text of the Linux Foundation](https://developercertificate.org/).

## License
Copyright (c) 2022 SAP SE or an SAP affiliate company. All rights reserved. This project is licensed under the Apache Software License, version 2.0 except as noted otherwise in the [LICENSE](LICENSES/Apache-2.0.txt) file.

### Uses

Thanks to Fraunhofer AISEC for:
- https://github.com/Fraunhofer-AISEC/cpg-vis-neo4j
- https://github.com/Fraunhofer-AISEC/cpg
