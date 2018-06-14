#!/usr/bin/env python

from copy import deepcopy
import sys, os, re, time

param = {}

permutated = set()
initPaths = set()

# for symmetry
agsDB = []
ecDB = set()

class EventConsequence:
  absEvent = ""
  stateBefore = ""
  stateAfter  = ""

  def __init__(self, line):
    section = line.split(" >> ")
    self.absEvent = section[1]
    self.stateBefore = self.getCleanupStateString(section[0])
    self.stateAfter = self.getCleanupStateString(section[2])
    
  def getCleanupStateString(self,nodeStates):
    filteredStates = []
    states = nodeStates.split(",")
    for state in states:
      temp = state.replace("[","").replace("]","").strip()
      if not "null" in temp:
        filteredStates.append(temp)
    result = "["
    isFirst = True
    for state in filteredStates:
      if isFirst:
        isFirst = False
      else:
        result += ", "
      result += state
    result += "]"
    return result
    
  def getNextState(self, prevState, curEvent):
    if self.stateBefore == prevState and self.absEvent == curEvent:
      return self.stateAfter
    else:
      return None

  def toString(self):
    return "stateBefore=" + self.stateBefore + "\nabsEvent   =" + self.absEvent + "\nstateAfter =" + self.stateAfter

  def __eq__(self,other):
    return self.stateBefore == other.stateBefore and self.absEvent == other.absEvent and self.stateAfter == other.stateAfter

  def __hash__(self):
    return hash((self.stateBefore, self.absEvent, self.stateAfter))

class PermutateElement:
  initPathHash = 0
  concMsgs = []

  def __init__(self, initialPathHash, concurrentMessages):
    self.initPathHash = initialPathHash
    self.concMsgs = deepcopy(concurrentMessages)

  def isIdentical(self, otherPermutate):
    # if this initPathHash is different with other permutate initPathHash
    # then they are not identical
    if self.initPathHash != otherPermutate.initPathHash:
      return False

    similarEvents = []
    for thisEv in self.concMsgs:
      isSimilar = False
      for otherEv in otherPermutate.concMsgs:
        if thisEv.tid == otherEv.tid:
          similarEvents.append(thisEv.tid)
          isSimilar = True
          break
      if not isSimilar:
        return False

    return len(similarEvents) == len(self.concMsgs)

  def toString(self):
    msgs = ""
    isFirst = True
    for msg in self.concMsgs:
      if isFirst:
        isFirst = False
      else:
        msgs += ","
      msgs += str(msg.tid)
    return "initPathHash=" + str(self.initPathHash) + ", concMsgs=" + msgs


class Event:
  tid = 0
  clientRequest = -1
  vClock = []
  send = -1
  recv = -1
  verb = ""
  payload = ""
  usrval = ""
  key = ""
  ballot = ""
  prepResp = True

  # parse event to string at constructor
  def __init__(self, eventString):
    self.ev = eventString
    self.tid = int(re.search(r'tid=-(\d+)', eventString).group(1)[4:])
    self.clientRequest = re.search(r'clientRequest=(\d+)', eventString).group(1)
    self.send = re.search(r'sendNode=(\d+)', eventString).group(1)
    self.recv = re.search(r'recvNode=(\d+)', eventString).group(1)
    self.verb = re.search(r'verb=([a-zA-Z\_]+)', eventString).group(1)
    self.payload = "{" + re.search(r'payload=\{(.*?)\}', eventString).group(1) + "}"
    self.usrval = "{" + re.search(r'usrval=\{(.*?)\}', eventString).group(1) + "}"
    if not "RESPONSE" in self.verb:
      self.key = re.search(r'key=(\d+)', eventString).group(1)
      self.ballot = re.search(r'ballot=(.*?)\,', eventString).group(1)
    if self.verb == "PAXOS_PREPARE_RESPONSE":
      self.prepResp = re.search(r'response=([a-zA-Z]+)', eventString).group(1) == "true"
    tempVClock = eventString[eventString.find("} [[") + 4:-2].split("], [")
    h = len(tempVClock)
    w = len(tempVClock[0].split(', '))
    self.vClock = [[0 for x in range(w)] for y in range(h)]
    for node in xrange(len(tempVClock)):
      vClockNode = tempVClock[node].split(', ')
      for vc in xrange(len(vClockNode)):
        self.vClock[node][vc] = vClockNode[vc]

  def isConcurrent(self, otherEvent):
    # if both events are from different client request,
    # then both events are concurrent
    if self.clientRequest != otherEvent.clientRequest:
      return 0

    isBefore = False; isAfter = False; isSimultaneous = False
    for node in xrange(len(self.vClock)):
      for vc in xrange(len(self.vClock[node])):
        if self.vClock[node][vc] < otherEvent.vClock[node][vc]:
          isBefore = True
        elif self.vClock[node][vc] > otherEvent.vClock[node][vc]:
          isAfter = True
        if isBefore and isAfter:
          isSimultaneous = True
          break
      if isSimultaneous:
        break

    if isBefore and not isAfter:
      # this event happens before the other event
      return -1;
    elif isAfter and not isBefore:
      # the other event happens before this event
      return 1;
    else:
      # concurrent
      return 0;

  def printVClock(self):
    result = ""
    for vc in self.vClock:
      isFirst = True
      result += "["
      for c in vc:
        if isFirst:
          isFirst = False
        else:
          result += ","
        result += c
      result += "]"
    return result

  def getAbstractEvent(self):
    #return "abstract-event:payload=" + self.payload + ", verb=" + self.verb + ", usrval=" + self.usrval + ", clientRequest=" + self.clientRequest
    return "abstract-message: verb=" + self.verb + ", usrval=" + self.usrval + ", clientRequest=" + self.clientRequest


  def toString(self):
    result = "tid=" + str(self.tid) + ", send=" + self.send + ", recv=" + self.recv + ", verb=" + self.verb + ", payload=" + self.payload + ", usrval=" + self.usrval
    #result = "tid=" + str(self.tid) + ", send=" + self.send + ", recv=" + self.recv + ", verb=" + self.verb + ", usrval=" + self.usrval
    if not "RESPONSE" in self.verb:
      result += ", ballot=" + self.ballot + ", key=" + self.key
    if self.verb == "PAXOS_PREPARE_RESPONSE":
      result += ", response=" + str(self.prepResp)
    result += ", clientRequest=" + self.clientRequest + ", vectorClock=" + self.printVClock()
    return result

  def __eq__(self, other):
    #return self.tid == other.tid and self.payload == other.payload and self.usrval == other.usrval
    return self.tid == other.tid and self.usrval == other.usrval


  def __hash__(self):
    #return hash((self.tid, self.payload, self.usrval))
    return hash((self.tid, self.usrval))

# parallel functions

def getPair(path):
  order = {}
  for event in path:
    if not event.recv in order:
      order[event.recv] = []
    order[event.recv].append(event)
  return order

def getPairs(paths):
  pairs = set()
  for path in paths:
    order = getPair(path)
    for key in order:
      pairTuple = tuple(order[key])
      pairs.add(pairTuple)
  return pairs

def getCheckList(pairs):
  checklist = {}
  for pair in pairs:
    checklist[pair] = False
  return checklist

def getParallelPaths(paths, pairs):
  result = []
  checklist = getCheckList(pairs)
  for path in paths:
    orderOfEvents = getPair(path)
    hasNewPair = False
    for key, pair in orderOfEvents.iteritems():
      if not checklist[tuple(pair)]:
        checklist[tuple(pair)] = True
        hasNewPair = True
    if hasNewPair:
      result.append(path)
  return result

def findParallelism(paths, pairs):
  rawParallelPaths = getParallelPaths(paths, pairs)
  finalParallelPaths = getParallelPaths(reversed(rawParallelPaths), pairs)
  return finalParallelPaths

def reduceByParallelism(concPaths):
  pairs = getPairs(concPaths)
  moreThanOneEventNode = 0
  for pair in pairs:
    if len(pair) > 1:
      moreThanOneEventNode += 1
  if moreThanOneEventNode > 1:
    parallelPaths = findParallelism(concPaths, pairs)
    return parallelPaths
  else:
    return concPaths


# symmetry functions

def identicalAGS(ags1, ags2):
  similar = []
  for x, state1 in enumerate(ags1):
    hasSimilar = False
    for y, state2 in enumerate(ags2):
      if not y in similar and state1 == state2:
        similar.append(y)
        hasSimilar = True
        break
    if not hasSimilar:
      return False
  return len(similar) == len(ags1)

def existInAGSDB(ags):
  global agsDB
  for historicalAGS in agsDB:
    if identicalAGS(historicalAGS, ags):
      return True
  return False

def addAGSToAGSDB(ags):
  global agsDB
  if not existInAGSDB(ags):
    agsDB.append(ags)

def nextState(curState, curAbsEvent):
  global ecDB
  for eventConsequence in ecDB:
    nextState = eventConsequence.getNextState(curState, curAbsEvent)
    if nextState != None:
      return nextState
  return None

def propagateForward(curGS, path):
  for event in path:
    recv = int(event.recv)
    newState = nextState(curGS[recv], event.getAbstractEvent())
    if newState == None:
      print "Cannot predict future state of: "
      print "stateBefore=" + str(curGS[recv])
      print "absEvent   =" + str(event.getAbstractEvent())
      curGS = None
      break
    else:
      curGS[recv] = newState
  return curGS

def isSymmetry(curGS, path):
  global param
  initGS = deepcopy(curGS)
  if "symmetry" in param['mode'] and initGS != None:
    endGS = propagateForward(initGS, path)
    if endGS == None:
      return False
    elif existInAGSDB(endGS):
      return True
  return False


# evaluation functions

def isReducedReordering(e1, e2):
  global param
  # skip evaluation if they are the same events
  if e1 == e2:
    return False
  # DPOR evaluation
  if "DPOR" in param['mode'] or "SAMC" in param['mode'] or "AMC" in param['mode'] or "symmetry" in param['mode'] or "parallel" in param['mode']:
    if e1.recv != e2.recv:
      printf("[DPOR] Skip reordering: \n" + e2.toString() + " before\n" + e1.toString())
      return True
    # SAMC evaluation
    if "SAMC" in param['mode'] or "AMC" in param['mode'] or "symmetry" in param['mode'] or "parallel" in param['mode']:
      if "RESPONSE" in e1.verb and e1.verb == e2.verb and \
        e1.payload == e2.payload:
        printf("[SAMC] Skip reordering: \n" + e2.toString() + " before\n" + e1.toString())
        return True
      # DeepMC evaluation
      if "AMC" in param['mode']:
        # equal with policy-2 (always-dis)
        if e1.verb == "PAXOS_COMMIT_RESPONSE" or e2.verb == "PAXOS_COMMIT_RESPONSE":
          printf("[AMC2] Skip reordering: \n" + e2.toString() + " before\n" + e1.toString())
          return True
        # equal with policy-3 (always-dis)
        if not "RESPONSE" in e1.verb and e1.verb == e2.verb and \
          e1.key == e2.key and e1.ballot > e2.ballot:
          printf("[AMC3] Skip reordering: \n" + e2.toString() + " before\n" + e1.toString())
          return True
        # equal with policy-4 (always-dis)
        if e1.verb == "PAXOS_PREPARE_RESPONSE" and e2.verb == "PAXOS_PREPARE_RESPONSE" and \
          e1.key == e2.key:
          if e1.prepResp == False or e2.prepResp == False:
            printf("[AMC4] Skip reordering: \n" + e2.toString() + " before\n" + e1.toString())
            return True
        # equal with policy-7 (always-dis)
        if e1.verb == "PAXOS_PROPOSE_RESPONSE" and e2.verb == "PAXOS_PROPOSE_RESPONSE":
          printf("[AMC7] Skip reordering: \n" + e2.toString() + " before\n" + e1.toString())
          return True
        # equal with policy-5 (disjoint)
        if e1.verb == "PAXOS_PREPARE" and e2.verb == "PAXOS_PREPARE_RESPONSE":
          printf("[AMC5] Skip reordering: \n" + e2.toString() + " before\n" + e1.toString())
          return True
        if e1.verb == "PAXOS_PREPARE_RESPONSE" and e2.verb == "PAXOS_PREPARE":
          printf("[AMC5] Skip reordering: \n" + e2.toString() + " before\n" + e1.toString())
          return True
        if e1.verb == "PAXOS_PROPOSE" and e2.verb == "PAXOS_PROPOSE_RESPONSE":
          printf("[AMC5] Skip reordering: \n" + e2.toString() + " before\n" + e1.toString())
          return True
        if e1.verb == "PAXOS_PROPOSE_RESPONSE" and e2.verb == "PAXOS_PROPOSE":
          printf("[AMC5] Skip reordering: \n" + e2.toString() + " before\n" + e1.toString())
          return True
        if e1.verb == "PAXOS_COMMIT" and e2.verb == "PAXOS_PROPOSE_RESPONSE":
          printf("[AMC5] Skip reordering: \n" + e2.toString() + " before\n" + e1.toString())
          return True
        if e1.verb == "PAXOS_PROPOSE_RESPONSE" and e2.verb == "PAXOS_COMMIT":
          printf("[AMC5] Skip reordering: \n" + e2.toString() + " before\n" + e1.toString())
          return True
        # equal with policy-6 (disjoint)
        if e1.verb == "PAXOS_PREPARE_RESPONSE" and e2.verb == "PAXOS_PROPOSE_RESPONSE":
          printf("[AMC6] Skip reordering: \n" + e2.toString() + " before\n" + e1.toString())
          return True
        if e1.verb == "PAXOS_PROPOSE_RESPONSE" and e2.verb == "PAXOS_PREPARE_RESPONSE":
          printf("[AMC6] Skip reordering: \n" + e2.toString() + " before\n" + e1.toString())
          return True
          
  # in default, do reordering
  return False


def permute(result, curGS, array, left, right):
  global param
  if left == right:
    if isSymmetry(curGS, array):
      printf("[SYM] Skip reordering")
      return
    if "symmetry" in param['mode'] or "parallel" in param['mode']:
      orderOfEvents = array
      print "New Init Path=" + ",".join(map(str, pathToEventIds(orderOfEvents)))
    else:
      orderOfEvents = pathToEventIds(array)
    result.append(deepcopy(orderOfEvents))
  else:
    for i in xrange(left, right+1):
      if isReducedReordering(array[left], array[i]):
        continue
      # swap
      array[left], array[i] = array[i], array[left]
      permute(result, curGS, array, left+1, right)
      array[left], array[i] = array[i], array[left]


def permutateEvents(initGS, initPath, concMsgs):
  global param
  global initPaths
  n = len(concMsgs)
  reorderedEvents = []
  permuteTime = time.time()
  permute(reorderedEvents, initGS, concMsgs, 0, n-1)
  printf("Permute Algorithm Time = " + str(time.time() - permuteTime))
  checkTime = time.time()
  # for parallel
  if "parallel" in param['mode']:
    if len(concMsgs) > 3:
      reorderedEvents = reduceByParallelism(reorderedEvents)
  for reorder in reorderedEvents:
    if len(initPath) > 0:
      newPath = initPath + reorder
    else:
      newPath = reorder
    # save new path
    if "symmetry" in param['mode'] or "parallel" in param['mode']:
      newPathTuple = tuple(newPath)
      print "New Init Path=" + ",".join(map(str, pathToEventIds(newPathTuple)))
      initPaths.add(newPathTuple)
    else:
      # if not symmetry, save in hashcode
      newPathHash = eventIdsToHash(newPath)
      print "New Init Path=" + ",".join(map(str, newPath)) + " hashed as=" + str(newPathHash)
      initPaths.add(newPathHash)
  printf("Permute Existence Time = " + str(time.time() - checkTime))


# general functions

def eventIdsToHash(eventIds):
  return hash(tuple(eventIds))

def pathToEventIds(path):
  eventIds = []
  for event in path:
    eventIds.append(event.tid)
  return eventIds

def saveStatistic(dir, record):
  global initPaths

  logPath = os.path.join(dir, "generatedInitialPaths.dat")
  logFile = open(logPath, "a")
  logFile.write(record + "  " + str(len(initPaths)) + "\n")
  logFile.close()

def saveDB():
  global ecDB
  global agsDB
  global param
  
  ecDBPath = os.path.join(param['save_to'], "ecDB.txt")
  ecDBFile = open(ecDBPath, "w")
  content = ""
  for ec in ecDB:
    content += ec.toString() + "\n"
  ecDBFile.write(content)
  ecDBFile.close()  

  agsDBPath = os.path.join(param['save_to'], "agsDB.txt")
  agsDBFile = open(agsDBPath, "w")
  content = ""
  for ags in agsDB:
    content += str(ags) + "\n"
  agsDBFile.write(content)
  agsDBFile.close()  

def generateStatistics():
  global ecDB
  global agsDB
  global param
  global permutated
  global initPaths

  # get max records
  recordDir = os.path.join(param['dir'], "record")
  maxRecords = len(os.listdir(recordDir))
  ecLoadRecords = maxRecords
  if "max" in param and param['max'] < maxRecords:
    ecLoadRecords = param['max']

  if "symmetry" in param['mode']:
    # go through all of the record dirs
    initialLoad = time.time()
    for i in xrange(1, ecLoadRecords + 1):
      print "Load AGS and EC from path-" + str(i)
      eachRecord = os.path.join(recordDir, str(i))

      # collect AGS from executed path into AGSDB
      debugFile = os.path.join(eachRecord, "debug.log")
      with open(debugFile) as debug:
        isGlobalStatesLine = False
        states = []
        for line in debug:
          if line.startswith("Events in Queue:"):
            isGlobalStatesLine = False
            addAGSToAGSDB(states)
            states = []
          elif line.startswith("Global States:"):
            isGlobalStatesLine = True
          elif isGlobalStatesLine:
            state = "[" + re.search(r'\[(.*?)\]', line).group(1) + "]"
            states.append(state)

      # collect event consequence from executed path into ecDB
      eventConsequenceFile = os.path.join(eachRecord, "eventConsequences")
      with open(eventConsequenceFile) as ecList:
        for ec in ecList:
          newCandidate = EventConsequence(ec.strip())
          ecDB.add(newCandidate)
    print "Total agsDB=" + str(len(agsDB))
    print "Total ecDB=" + str(len(ecDB))
    print "Initial Load for AGS and EC=" + str(time.time()-initialLoad)

    saveDB()

  # go through all of the record dirs
  for i in xrange(1, maxRecords + 1):
    onePathTime = time.time()
    eachRecord = os.path.join(recordDir, str(i))

    # collect executed path from a single record
    events = []
    pathFile = os.path.join(eachRecord, "path")
    with open(pathFile) as path:
      for event in path:
        event = Event(event.strip())
        events.append(event)

    # evaluate last execution path to get concurrent messages
    # before each event execution
    print "----------------------------"
    print "Evaluate path=" + pathFile
    concMsgs = []
    while(len(events) > 0):
      lastEvent = events.pop()
      printf("At lastEvent=" + lastEvent.toString())
      # for symmetry or parallel: save initPath in events, otherwise ids are enough
      if "symmetry" in param['mode'] or "parallel" in param['mode']:
        initPath = events
        printf("Init Path=" + str(pathToEventIds(initPath)))
      else:
        initPath = pathToEventIds(events)
        printf("Init Path=" + str(initPath))
      if len(concMsgs) == 0:
        concMsgs.append(lastEvent)
      else:
        addLastEvent = False
        notConcMsgs = []
        # list all messages that are no longer concurrent
        for msg in concMsgs:
          if lastEvent.isConcurrent(msg) == -1:
            notConcMsgs.append(msg)
        # the real remove of all not concurrent messages anymore.
        for msg in notConcMsgs:
          concMsgs.remove(msg)

        # insert latest event to the top of the array,
        # just the same as it was in DMCK queue order.
        concMsgs.insert(0, lastEvent)

      # log concurrent messages
      if param['verbose']:
        print "Concurrent Messages:"
        for m in concMsgs:
          print "  " + m.toString()

      # propagate global state forward if it is possible
      initGS = []
      if "symmetry" in param['mode']:
        initGS = ["[]","[]","[]"]
        initGS = propagateForward(initGS, events)

      # only permutate new permutation element
      # for symmetry: initPath are in events
      if "symmetry" in param['mode'] or "parallel" in param['mode']:
        initPathHash = eventIdsToHash(pathToEventIds(initPath))
      else:
        initPathHash = eventIdsToHash(initPath)
      pe = PermutateElement(initPathHash, concMsgs)
      executePermutation = True
      for existP in permutated:
        if pe.isIdentical(existP):
          executePermutation = False
          break

      if executePermutation:
        permutated.add(pe)
        permuteTime = time.time()
        permutateEvents(initGS, initPath, concMsgs)
        printf("Total time for PERMUTATING " + str(len(concMsgs)) + " events=" + str(time.time() - permuteTime) + "s")
      else :
        printf("Has been permutated before!")

      printf("Total Initial Paths=" + str(len(initPaths)))
      printf("Total Permutation List=" + str(len(permutated)))
      printf("")

    # save total statistic numbers
    saveStatistic(param['save_to'], str(i))

    print pathFile + " in total requires=" + str(time.time() - onePathTime) + "s"

  print "Initial Paths to explore:"
  for i, path in enumerate(initPaths):
    if "symmetry" in param['mode'] or "parallel" in param["mode"]:
      print str(i+1) + ". " + ",".join(map(str, pathToEventIds(path)))
    else:
      print str(i+1) + ". " + str(path)

def printf(comment):
  global param
  if param['verbose']:
    print comment
  

def printHelp():
  print "  Usage: ./count_generated_initial_paths.py --dir /parent/dir/of/logs --save_to /save/file/location --mode <none/default: DPOR> <optional: --verbose>"
  print ""
  print "  --dir, -d        : used to specify the parent directory of all model checking logs that will be evaluated."
  print "  --save_to, -s    : used to direct the save location of all of the new buggy initial paths."
  print "  --verbose, -v    : to print out logs to screen."
  print "  --mode, -m       : activate mode of evaluation. options: none, DPOR, SAMC, AMC, symmetry, parallel"
  print "  --max            : define max records that will be used in loading eventConsequence which will affect the symmetry policy power."
  exit(0)

def extractParameters():
  global param
  arg = sys.argv
  param['verbose'] = False
  param['mode'] = ['paralelism', 'symmetry', 'AMC']
  for i in range(len(arg)):
    if arg[i] == '-d' or arg[i] == '--dir':
      param['dir'] = arg[i+1]
      if not os.path.isdir(param['dir']):
        print("ERROR: Configured directory must be a directory.\n")
        printHelp()
    if arg[i] == '-s' or arg[i] == '--save_to':
      param['save_to'] = arg[i+1]
      if not os.path.isdir(param['dir']):
        print("ERROR: Configured save_to must be a directory.\n")
        printHelp()
    if arg[i] == '-v' or arg[i] == '--verbose':
      param['verbose'] = True
    if arg[i] == '-m' or arg[i] == '--mode':
      param['mode'] = arg[i+1].split(",")
    if arg[i] == '--max':
      param['max'] = int(arg[i+1])
    if arg[i] == '-h' or arg[i] == '--help':
      printHelp()
      
  usage = "\nUsage: ./cound_generated_initial_paths.py --dir /parent/dir/of/logs --save_to /save/dir/location --mode <none/DPOR/SAMC/default: parallel,symmetry,AMC> <optional: --verbose>"
  if not "dir" in param or param["dir"] == "":
    exit("Directory must be specified." + usage)
  if not "save_to" in param or param["save_to"] == '':
    exit("Save_to must be specified." + usage)
  if not "parallel" in param['mode'] and not "symmetry" in param['mode'] and not "AMC" in param['mode']  and not "SAMC" in param['mode'] and not "DPOR" in param['mode'] and not "none" in param['mode']:
    exit("Mode " + param['mode'] + " is not defined." + usage)

def main():
  extractParameters()
  print "EVALUATION: " + str(param['mode'])
  generateStatistics()

if __name__ == "__main__":
  main()
