#!/bin/bash

# Load target-sys.conf variables to this script
. ./readconfig

# Refresh necessary data states of Cassandra nodes.
for i in `seq 0 $(($num_node-1))`; do
  rm $working_dir/data/cass$i/data/test
  cp -r $working_dir/data-copy/cass$i/data/test $working_dir/data/cass$i/data
done

# Create reset notification file for each node.
for i in `seq 0 $(($num_node-1))`; do
  touch $working_dir/resetNodeStates-$i
done

