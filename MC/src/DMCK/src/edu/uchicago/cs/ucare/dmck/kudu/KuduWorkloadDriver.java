package edu.uchicago.cs.ucare.dmck.kudu;

import java.util.ArrayList;
import java.util.HashMap;
import edu.uchicago.cs.ucare.dmck.server.WorkloadDriver;

public class KuduWorkloadDriver extends WorkloadDriver {

  public static HashMap<Integer, String> tabletServersUUID;
  public static ArrayList<String> tabletIds;

  public KuduWorkloadDriver(int numNode, String workingDir, String ipcDir, String samcDir,
      String targetSysDir) {
    super(numNode, workingDir, ipcDir, samcDir, targetSysDir);
    tabletServersUUID = new HashMap<Integer, String>();
    tabletIds = new ArrayList<String>();
  }

  @Override
  public void startWorkload() {
    try {
      LOG.debug("Start Kudu Workload");
      Runtime.getRuntime().exec(workingDir + "/startWorkload.sh " + testId);
      // KUDU-2118
      // Runtime.getRuntime().exec(workingDir + "/startWorkload.sh " + tabletServersUUID.get(3) + "
      // "
      // + tabletIds.get(0) + " " + testId);
    } catch (Exception e) {
      LOG.error("Error in executing startWorkload.sh");
    }
  }

  @Override
  public void stopWorkload() {
    // Nothing
  }

  public static void printTabletsUUID() {
    String result = "Tablets UUID:\n";
    int i = 0;
    for (String tabletId : tabletIds) {
      result += ++i + "=" + tabletId + "\n";
    }
    LOG.debug(result);
  }

  public static void printTabletServersUUID() {
    String result = "Tablet Servers UUID:\n";
    for (Integer i : tabletServersUUID.keySet()) {
      result += i + "=" + tabletServersUUID.get(i) + "\n";
    }
    LOG.debug(result);
  }

}
