# How to Use
1. Grab Cassandra version 2.0.0
```
  wget http://archive.apache.org/dist/cassandra/2.0.0/apache-cassandra-2.0.0-src.tar.gz

  tar -xzvf apache-cassandra-2.0.0-src.tar.gz -C /.../apache-cassandra-2.0.0-src
```

2. Apply the patches to Cassandra
```
  cp -rf /.../distributed-concurrency-benchmark/bugs/cassandra-5925/mc-patches/src/ /.../apache-cassandra-2.0.0-src/src/
```

3. Go to the Cassandra directory and compile the code
```
  cd /.../apache-cassandra-2.0.0-src
  ant
```