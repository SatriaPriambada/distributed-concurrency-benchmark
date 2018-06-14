#!/bin/bash

startmin=1
maxmin=1
interval=1
if [ $# -lt 3  ] || [ $# -gt 5 ]; then
	echo "Usage: ./experimentModeRunner.sh <exploring_policy> <persistent_dir> <max_minus_path>"
  echo "       ./experimentModeRunner.sh <exploring_policy> <persistent_dir> <starting_minus_path> <max_minus_path>"
	echo "       ./experimentModeRunner.sh <exploring_policy> <persistent_dir> <starting_minus_path> <max_minus_path> <interval>"
	exit 1
elif [ $# -eq 5 ]; then
  startmin=$3
	maxmin=$4
	interval=$5
elif [ $# -eq 4 ]; then
	startmin=$3
  maxmin=$4
elif [ $# -eq 3 ]; then
  maxmin=$3
fi

exploringPolicy=$1
persistentDir=$2
. ./readconfig

function runWorkload {
	# policy setup
	if [ $exploringPolicy == "samc" ] || [ $exploringPolicy == "SAMC" ]; then
		policy="exploring_strategy=edu.uchicago.cs.ucare.dmck.cassandra.CassSAMC"
		sed -i "s:.*exploring_strategy=.*:$policy:g" $working_dir/target-sys.conf
	elif [ $exploringPolicy == "dpor" ] || [ $exploringPolicy == "DPOR" ]; then
		policy="exploring_strategy=edu.uchicago.cs.ucare.dmck.server.DporModelChecker"
		sed -i "s:.*exploring_strategy=.*:$policy:g" $working_dir/target-sys.conf
	elif [ $exploringPolicy == "random" ] || [ $exploringPolicy == "Random" ]; then
		policy="exploring_strategy=edu.uchicago.cs.ucare.dmck.server.RandomModelChecker"
		sed -i "s:.*exploring_strategy=.*:$policy:g" $working_dir/target-sys.conf
	elif [ $exploringPolicy == "dfs" ] || [ $exploringPolicy == "DFS" ]; then
		policy="exploring_strategy=edu.uchicago.cs.ucare.dmck.server.DfsTreeTravelModelChecker"
		sed -i "s:.*exploring_strategy=.*:$policy:g" $working_dir/target-sys.conf
	fi

	# run workload
	$working_dir/dmckRunner.sh

	# copy logs to persistent directory
	today=$( date +%Y%m%d )
	curPersistentDir=$persistentDir/experiment-logs/$today-$bug_name-$policy/minus$1
	mkdir -p $curPersistentDir

	cp $working_dir/*.conf $curPersistentDir
	cp -r $working_dir/record $curPersistentDir
	cp -r $working_dir/console $curPersistentDir
}


for i in `seq $startmin $interval $maxmin`; do
	step="${i}ev"

	# prepare target-sys.conf
	initialPath="initial_path=${dmck_dir}/cassandra-2.0.0/initialPaths/$bug_name-${step}"
	sed -i "s:.*initial_path=.*:$initialPath:g" $working_dir/target-sys.conf

	runWorkload $step

	# reset working directory
	$dmck_dir/cassandra-2.0.0/setup
done

# lastly, runWorkload without initial_path
sed -i '6d' $working_dir/target-sys.conf
runWorkload all
