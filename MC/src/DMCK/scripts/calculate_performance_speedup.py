#!/usr/bin/env python

import os
import sys

def find_string(line, keyword, param, to_grab):
    # If param[to_grab] is null or the to_grab == 'opt' 
    # then get the param[to_grab] from summary.
    if not to_grab in param or param[to_grab] is None or to_grab == 'opt':
        sp = line.find(keyword)
        end_phrase = "ms;"
        if keyword.find("time") < 0:
            end_phrase = ";"
        if sp >= 0:           
            ep = line.find(end_phrase)
            time = int(line[sp + len(keyword):ep])
            return time
    return -1

def calculate_performance(param):
    perf_fp = os.path.join(param['rec'], 'performance.log')
    content = ""
    dat_file = open(param['save_to'], 'a+')
    num_events = 0
    sum_roundtrip_time = 0
    init_time = 0
    exec_time = 0
    ver_time = 0
    eval_time = 0
    total_time = 0

    with open(perf_fp) as perf_file:
        for line in perf_file:
            # Get the number of events in the path.
            cur_ev_pointer = line.find(" : max-roundtrip-time=")
            if cur_ev_pointer >= 0:
                cur_ev = int(line[0:cur_ev_pointer])
                if cur_ev > num_events:
                    num_events = cur_ev
                # If do optimized calculation, collect the roundtrip time
                time = find_string(line, " : max-roundtrip-time=", param, 'opt')
                if time > -1:
                    sum_roundtrip_time += time
                continue

            # If init is null then get the init from summary.
            time = find_string(line, "total-initialization-time=", param, "init")
            if time > -1:
                init_time = time
                continue

            # If steady is null then get the steady from summary.
            time = find_string(line, "total-execution-path-time=", param, 'steady')
            if time > -1:
                exec_time = time
                continue

            # If ver is null then get the ver from summary.
            time = find_string(line, "total-verification-time=", param, 'ver')
            if time > -1:
                ver_time = time
                continue

            # If ver is null then get the ver from summary.
            time = find_string(line, "total-evaluation-time=", param, 'eval')
            if time > -1:
                eval_time = time
                continue

    if init_time == 0 and 'init' in param and param['init'] > 0:
        init_time = int(param['init'])

    if exec_time == 0 and 'steady' in param and param['steady'] > 0:
        if param['opt']:
            opt_ev_time = float(sum_roundtrip_time) / num_events
            exec_time = (num_events - param['hit'] * opt_ev_time) + (param['hit'] * param['steady'])
        else:
            exec_time = num_events * param['steady']

    if ver_time == 0 and 'ver' in param and param['ver'] > 0:
        ver_time = param['ver']

    total_time = init_time + exec_time + ver_time + eval_time

    content = str(init_time) + "  " + str(int(exec_time)) + "  " + str(ver_time) + "  " + str(eval_time) + "\n"
    dat_file.write(content)
    
    print "### RESULT ###"
    print "Num Events: " + str(num_events)
    print "Init Time: " + str(init_time)
    print "Exec Time: " + str("%.0f" % exec_time)
    print "Ver Time: " + str(ver_time)
    print "Eval Time: " + str(eval_time)
    print "Total Time: " + str("%.0f" % total_time)
                    
def print_help():
    print "  This script is used to calculate the longest DMCK path execution time."
    print "  Usage: ./.calculate_performance_speedup.py --rec <rec_X_dir_path>"
    print ""
    print "  --rec,         -r  : specify record X directory that contains the performance.log file."
    print "  --init_time,   -it : if specified, the init time will be based on this number " + \
        "(counted in ms). Otherwise, it will take the number from performance.log."
    print "  --steady_time, -st : if specified, the steady state time will be based on this " + \
        " number(counted in ms). Otherwise, it will take the number from performance.log."
    print "  --ver_time,    -vt : if specified, the verification time will be based on this " + \
        " number(counted in ms). Otherwise, it will take the number from performance.log."
    print "  --eval_time,   -et : if specified, the evaluation time will be based on this " + \
        " number(counted in ms). Otherwise, it will take the number from performance.log."
    print "  --save_to,     -s  : if specified, the evaluation time will be based on this " + \
        " number(counted in ms). Otherwise, it will take the number from performance.log."

def extract_parameters():
    param = {}
    arg = sys.argv
    param['save_to'] = '/tmp/path_execution_performance.dat'
    param['opt'] = False
    for i in xrange(len(arg)):
        if arg[i] == '-r' or arg[i] == '--rec':
            param['rec'] = arg[i+1]
            if not os.path.isdir(param['rec']):
                sys.exit("ERROR: " + param['rec'] + " is not a directory.")
        if arg[i] == '-it' or arg[i] == '--init_time':
            param['init'] = int(arg[i+1])
        if arg[i] == '-st' or arg[i] == '--steady_time':
            param['steady'] = int(arg[i+1])
        if arg[i] == '-vt' or arg[i] == '--ver_time':
            param['ver'] = int(arg[i+1])
        if arg[i] == '-et' or arg[i] == '--eval_time':
            param['eval'] = int(arg[i+1])
        if arg[i] == '--opt':
            param['opt'] = True
        if arg[i] == '-hc' or arg[i] == '--hit_cache':
            param['hit'] = int(arg[i+1])
        if arg[i] == '-s' or arg[i] == '--save_to':
            param['save_to'] = arg[i+1]
        if arg[i] == '-h' or arg[i] == '--help':
            print_help()
    if not 'rec' in param or param['rec'] is None:
        print_help()
        sys.exit("ERROR: Please follow the minimum instruction.")
    return param

def main():
    param = extract_parameters()
    calculate_performance(param)
    print "Result is saved to " + param['save_to']

if __name__ == "__main__":
    main()
