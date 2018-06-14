#!/bin/bash

# Restrict script to be called with test_id parameter.
if [ $# != 1 ]; then
  echo "Usage: startWorkload-cqlsh.sh <test_id>" > workload_error.log
	exit 1
fi

# Provide target-sys.conf parameters.
. ./readconfig

# Setup parameter and classpath.
test_id=$1

id=1
for cql_file in $working_dir/query/$bug_name/*.cql; do
  $target_sys_dir/bin/cqlsh 127.0.0.$id --file="$cql_file" > $working_dir/console/$test_id/workload$id.log &
  echo $! > $working_dir/dmck-workload$id.pid
  sleep 0.3
  id=`expr $id + 1`
done

# Send a signal to Cassandra nodes to start executing these workloads.
touch "$working_dir/executeCas-0"
