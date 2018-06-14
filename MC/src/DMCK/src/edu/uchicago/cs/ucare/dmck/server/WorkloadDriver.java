package edu.uchicago.cs.ucare.dmck.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class WorkloadDriver {

  protected final static Logger LOG = LoggerFactory.getLogger(WorkloadDriver.class);

  protected int numNode;
  protected String workingDir;
  protected String ipcDir;
  protected String samcDir;
  protected String targetSysDir;
  protected int testId;

  public SpecVerifier verifier;

  // Reset states only feature.
  protected boolean resetStatesOnlyMode;
  protected boolean nodesStarted;
  protected int refreshStateTimeout;

  public WorkloadDriver(int numNode, String workingDir, String ipcDir, String samcDir,
      String targetSysDir) {
    this.numNode = numNode;
    this.workingDir = workingDir;
    this.ipcDir = ipcDir;
    this.samcDir = samcDir;
    this.targetSysDir = targetSysDir;
    testId = 0;
    nodesStarted = false;
    refreshStateTimeout = 0;
  }

  public WorkloadDriver(int numNode, String workingDir, SpecVerifier verifier) {
    this.numNode = numNode;
    this.workingDir = workingDir;
    this.verifier = verifier;
  }

  public void setResetStatesOnlyMode(boolean useResetStatesOnlyMode) {
    resetStatesOnlyMode = useResetStatesOnlyMode;
  }

  public void setRefreshStateTimeout(int timeout) {
    refreshStateTimeout = timeout;
  }

  public boolean isResetStatesOnlyMode() {
    return resetStatesOnlyMode;
  }

  public int getRefreshStateTimeout() {
    return refreshStateTimeout;
  }

  public void startNode(int id) {
    try {
      Runtime.getRuntime().exec(workingDir + "/startNode.sh " + id + " " + testId);
      Thread.sleep(50);
      LOG.info("Start Node-" + id);
    } catch (Exception e) {
      LOG.error("Error in Starting Node " + id);
    }
  }

  public void stopNode(int id) {
    try {
      Runtime.getRuntime().exec(workingDir + "/killNode.sh " + id + " " + testId);
      Thread.sleep(20);
      LOG.info("Stop Node-" + id);
    } catch (Exception e) {
      LOG.error("Error in Killing Node " + id);
    }
  }

  public boolean startEnsemble() {
    // If running DMCK with Reset states only mode,
    // check whether the nodes are alive or not.
    if (resetStatesOnlyMode && nodesStarted) {
      return false;
    }

    LOG.debug("Start Ensemble");
    for (int i = 0; i < numNode; ++i) {
      try {
        startNode(i);
      } catch (Exception e) {
        LOG.error("Error in starting ensemble");
      }
    }

    // If running DMCK with Reset states only mode,
    // then after running all nodes, set the nodesStarted to true.
    if (resetStatesOnlyMode) {
      nodesStarted = true;
    }
    return true;
  }

  public void stopEnsemble() {
    LOG.debug("Stop Ensemble");
    for (int i = 0; i < numNode; i++) {
      try {
        stopNode(i);
      } catch (Exception e) {
        LOG.error("Error in stopping ensemble");
      }
    }

    // If running DMCK with Reset states only mode,
    // then on stopEnsemble(), set the nodesStarted to false.
    if (resetStatesOnlyMode) {
      nodesStarted = false;
    }
  }

  // Reset states only feature.
  // If DMCK is run in Reset states only mode,
  // then DMCK will call resetNodes() instead of stopEnsemble().
  public void resetNodes() {
    LOG.debug("Reset Nodes States Only by calling refrehStates.sh");
    try {
      Runtime.getRuntime().exec(workingDir + "/refreshStates.sh").waitFor();
    } catch (Exception e) {
      LOG.error("Error in executing refreshStates.sh script");
    }
  }

  public void clearIPCDir() {
    try {
      Runtime.getRuntime().exec(new String[] {"sh", "-c", "rm " + ipcDir + "/*/*"}).waitFor();
    } catch (Exception e) {
      LOG.error("Error in clear IPC Dir");
    }
  }

  public void resetTest(int testId) {
    clearIPCDir();
    this.testId = testId;
    try {
      Runtime.getRuntime().exec(workingDir + "/resettest " + this.testId).waitFor();
    } catch (Exception e) {
      LOG.error("Error in cexecuting resettest script");
    }
  }

  public abstract void startWorkload();

  public abstract void stopWorkload();

  public void setVerifier(SpecVerifier verifier) {
    this.verifier = verifier;
  }

}
