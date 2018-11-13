# How to Use
1. Grab Cassandra version 2.0.0
```
  wget http://archive.apache.org/dist/cassandra/2.0.0/apache-cassandra-2.0.0-src.tar.gz

  tar -xzvf apache-cassandra-2.0.0-src.tar.gz -C /.../apache-cassandra-2.0.0-src
```

2. Apply the patches to Cassandra
```
  cp -rf /.../distributed-concurrency-benchmark/bugs/cassandra-6023/mc-patches/src/ /.../apache-cassandra-2.0.0-src/src/
```

3. Go to the Cassandra directory and compile the code
#### If you want to use java 8 please update antlr in the build.xml

```
  cd /.../apache-cassandra-2.0.0-src
  vi build.xml 
  or gedit build.xml 
```
#### Replace antlr-3.2.jar with antlr-3.5.2.jar and
```
<dependency groupId="org.antlr" artifactId="antlr" version="3.2"/>
```
to
```
<dependency groupId="org.antlr" artifactId="antlr" version="3.5.2"/> 
```
#### Build your Cassandra
```
  cd /.../apache-cassandra-2.0.0-src
  ant
```

4. Follow further instruction for each specific bug on
```
/.../distributed-concurrency-benchmark/MC/src/DMCK 
```
