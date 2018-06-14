package edu.uchicago.cs.ucare.dmck.election;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.uchicago.cs.ucare.dmck.server.ModelCheckingServerAbstract;
import edu.uchicago.cs.ucare.dmck.server.SpecVerifier;
import edu.uchicago.cs.ucare.dmck.util.LocalState;
import edu.uchicago.cs.ucare.example.election.LeaderElectionMain;

public class LeaderElectionVerifier extends SpecVerifier {

  protected static final Logger LOG = LoggerFactory.getLogger(LeaderElectionVerifier.class);

  public LeaderElectionVerifier() {

  }

  public LeaderElectionVerifier(ModelCheckingServerAbstract modelCheckingServer) {
    this.dmck = modelCheckingServer;
  }

  @Override
  public boolean verify() {
    int onlineNode = 0;
    int[] supportTable = new int[dmck.isNodeOnline.length];
    for (boolean status : dmck.isNodeOnline) {
      if (status) {
        onlineNode++;
      }
    }
    for (int i = 0; i < 3; ++i) {
      int numLeader = 0;
      int numFollower = 0;
      int numLooking = 0;
      for (int j = 0; j < dmck.isNodeOnline.length; ++j) {
        if (dmck.isNodeOnline[j]) {
          LocalState localState = dmck.localStates[j];
          if ((int) localState.getValue("role") == LeaderElectionMain.LEADING) {
            numLeader++;
            supportTable[j] = j;
          } else if ((int) localState.getValue("role") == LeaderElectionMain.FOLLOWING) {
            numFollower++;
            supportTable[j] = (int) localState.getValue("leader");
          } else if ((int) localState.getValue("role") == LeaderElectionMain.LOOKING) {
            numLooking++;
            supportTable[j] = -1;
          }
        }
      }
      int quorum = dmck.numNode / 2 + 1;
      if (onlineNode < quorum) {
        if (numLeader == 0 && numFollower == 0 && numLooking == onlineNode) {
          return true;
        }
      } else {
        if (numLeader == 1 && numFollower == (onlineNode - 1)) {
          int leader = -1;
          for (int j = 0; j < dmck.isNodeOnline.length; ++j) {
            if (dmck.isNodeOnline[j]) {
              if (leader == -1) {
                leader = supportTable[j];
              } else if (supportTable[j] != leader) {
                return false;
              }
            }
          }
          return true;
        }
      }
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {

      }
    }
    return false;
  }

  @Override
  public String verificationDetail() {
    StringBuilder strBuilder = new StringBuilder();
    for (int i = 0; i < dmck.isNodeOnline.length; ++i) {
      if (dmck.isNodeOnline[i]) {
        LocalState localState = dmck.localStates[i];
        if ((int) localState.getValue("role") == LeaderElectionMain.LEADING) {
          strBuilder.append("node " + i + " is LEADING ; ");
        } else if ((int) localState.getValue("role") == LeaderElectionMain.FOLLOWING) {
          strBuilder
              .append("node " + i + " is FOLLOWING to " + localState.getValue("leader") + " ; ");
        } else if ((int) localState.getValue("role") == LeaderElectionMain.LOOKING) {
          strBuilder.append("node " + i + " is still LOOKING ; ");
        }
      } else {
        strBuilder.append("node " + i + " is down ; ");
      }
    }
    return strBuilder.toString();
  }

}
