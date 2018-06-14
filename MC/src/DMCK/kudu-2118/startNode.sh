#!/bin/bash

# Restrict parameters.
if [ $# -ne 2 ]; then
  echo "Usage: ./startNode.sh <node_id> <test_id>"
  exit 1
fi

# Read target-sys.conf.
. ./readconfig

# Initialize with given parameters.
node_id=$1
test_id=$2

# Prepare classpath.
classpath=$working_dir:$target_sys_dir/build/debug/bin/
export PATH=${PATH}:$classpath

# How fast will a Leader declare that a follower is disconnected in seconds.
follower_failed_timeout=3000
consensus_rpc_timeout=300000
raft_heartbeat_timeout=5000

RPC_PORT=`expr 7150 + $node_id`
WEBSERVER_PORT=`expr 8150 + $node_id`

if [ $node_id -eq "0" ]; then
  echo "Start Master Node.."
  # Start Master node
  kudu-master --fs_data_dirs=$working_dir/data/node-$node_id \
    --fs_wal_dir=$working_dir/wal/node-$node_id \
    --log_dir=$working_dir/log/$test_id/node-$node_id \
    --follower_unavailable_considered_failed_sec=$follower_failed_timeout \
    --consensus_rpc_timeout_ms=$consensus_rpc_timeout \
    --raft_heartbeat_interval_ms=$raft_heartbeat_timeout \
    --logtostderr > $working_dir/log/$test_id/node-$node_id/output.log 2>&1 &
  echo $! > $working_dir/log/node-$node_id.pid
else
  # Start Tablet Server node
  echo "Start Tablet Server Node-$node_id.."
  kudu-tserver --fs_data_dirs=$working_dir/data/node-$node_id \
    --fs_wal_dir=$working_dir/wal/node-$node_id \
    --log_dir=$working_dir/log/$test_id/node-$node_id \
    --rpc_bind_addresses=127.0.0.1:$RPC_PORT --webserver_port=$WEBSERVER_PORT \
    --follower_unavailable_considered_failed_sec=$follower_failed_timeout \
    --consensus_rpc_timeout_ms=$consensus_rpc_timeout \
    --raft_heartbeat_interval_ms=$raft_heartbeat_timeout \
    --logtostderr > $working_dir/log/$test_id/node-$node_id/output.log 2>&1 &
  echo $! > $working_dir/log/node-$node_id.pid
fi
