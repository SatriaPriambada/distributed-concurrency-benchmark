#!/bin/bash

# Restrict parameter to use 3 inputs.
if [ $# != 3 ]; then
  echo "Usage: startWorkload.sh <node_uuid> <tablet_uuid> <test_id>"
  exit 1
fi

. ./readconfig

# Collect parameters.
node_uuid=$1
tablet_uuid=$2
test_id=$3

# Create and commit new file that enable interception in Kudu nodes.
filename="DMCK-enable-kudu-interception"
touch "$ipc_dir/new/$filename"
mv $ipc_dir/new/$filename $ipc_dir/ack/
echo "Enabled Interception in all Kudu nodes." >> $working_dir/log/$test_id/workload.log

sleep 1

# Create and commit new file that will trigger LE.
filename="workload-le"
echo "node_uuid $node_uuid" >> $ipc_dir/new/$filename
echo "tablet_uuid $tablet_uuid" >> $ipc_dir/new/$filename
mv $ipc_dir/new/$filename $ipc_dir/ack/
echo "Injected LE." >> $working_dir/log/$test_id/workload.log

# Create and commit new file that will trigger Delete Tablet.
#filename="workload-delete-tablet"
#echo "node_uuid $node_uuid" >> $ipc_dir/new/$filename
#echo "tablet_uuid $tablet_uuid" >> $ipc_dir/new/$filename
#mv $ipc_dir/new/$filename $ipc_dir/ack/
#echo "Injected Delete Tablet." >> $working_dir/log/$test_id/workload.log\

# Execute Delete Table with kudu-example code
$target_sys_dir/kudu-examples/java/java-sample/run_workload.sh delete_table X
echo "Injected Delete Table." >> $working_dir/log/$test_id/workload.log

