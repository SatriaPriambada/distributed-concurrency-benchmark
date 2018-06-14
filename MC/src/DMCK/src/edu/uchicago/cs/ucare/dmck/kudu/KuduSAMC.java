package edu.uchicago.cs.ucare.dmck.kudu;

import edu.uchicago.cs.ucare.dmck.event.Event;
import edu.uchicago.cs.ucare.dmck.server.EvaluationModelChecker;
import edu.uchicago.cs.ucare.dmck.server.FileWatcher;
import edu.uchicago.cs.ucare.dmck.server.WorkloadDriver;
import edu.uchicago.cs.ucare.dmck.transition.NodeCrashTransition;
import edu.uchicago.cs.ucare.dmck.transition.NodeOperationTransition;
import edu.uchicago.cs.ucare.dmck.transition.Transition;
import edu.uchicago.cs.ucare.dmck.util.LocalState;

public class KuduSAMC extends EvaluationModelChecker {

  public KuduSAMC(String dmckName, FileWatcher fileWatcher, int numNode, int numCrash,
      int numReboot, String cacheDir, WorkloadDriver workloadDriver, String ipcDir) {
    super(dmckName, fileWatcher, numNode, numCrash, numReboot, cacheDir, workloadDriver, ipcDir);
  }

  @Override
  public boolean isLMDependent(LocalState state, Event e1, Event e2) {
    // LMI-Discard: if both events are discard events, then they are independent.
    if (isDiscardMsg(state, e1) && isDiscardMsg(state, e2)) {
      recordPolicyEffectiveness("lmi_discard");
      return false;
    }
    // LMI-Extension for DeepMC: if one of the event is a discard event then
    // both events are independent to one another.
    if (reductionAlgorithms.contains("msg_always_dis")) {
      if (isDiscardMsg(state, e1) || isDiscardMsg(state, e2)) {
        recordPolicyEffectiveness("msg_always_dis");
        return false;
      }
    }
    // DeepMC - Disjoint Update: if two events are updating two different sets of states,
    // then both events are independent to one another.
    if (reductionAlgorithms.contains("msg_ww_disjoint")) {
      if (isDisjointMsgs(e1, e2)) {
        recordPolicyEffectiveness("msg_ww_disjoint");
        return false;
      }
    }
    return true;
  }

  public boolean isDiscardMsg(LocalState state, Event ev) {
    // Discard-1: if message event is a DELETE_TABLET_RESPOND message, then it is a discard message.
    if ((int) ev.getValue("type") == 4) {
      return true;
    }
    // Discard-2: if message event is a ADV_COMMIT_IDX_RESPOND message, then it is a discard
    // message.
    if ((int) ev.getValue("type") == 8) {
      return true;
    }
    // Discard-3: if message event is a REPLICATE_RESPOND message and
    // the receiver node's current term is greater or equal to the message term and
    // the receiver node's committed index is greater or equal to the message index,
    // then it is a discard message.
    String tabletId = ev.getValue("tabletId").toString();
    if ((int) ev.getValue("type") == 6
        && (int) state.getValue(tabletId + "-currentTerm") == (int) ev.getValue("currentTerm")
        && (int) state.getValue(tabletId + "-committedIndex") > (int) ev
            .getValue("committedIndex")) {
      return true;
    }
    return false;
  }

  public boolean isDisjointMsgs(Event e1, Event e2) {
    // In KUDU, if both messages are involving two sets of different tablets,
    // then those two messages will update different sets of states.
    String tabletId1 = e1.getValue("tabletId").toString();
    String tabletId2 = e2.getValue("tabletId").toString();
    return !tabletId1.equals(tabletId2);
  }

  @Override
  public boolean isCMDependent(boolean[] wasNodeOnline, LocalState[] wasLocalState, Event msg,
      NodeCrashTransition crash) {
    return true;
  }

  @Override
  public boolean isCCDependent(boolean[] wasNodeOnline, LocalState[] wasLocalState,
      NodeCrashTransition crash1, NodeCrashTransition crash2) {
    return true;
  }

  @Override
  public boolean isCRSDependent(boolean[] wasNodeOnline, LocalState[] wasLocalstate,
      Transition event) {
    // TODO Auto-generated method stub
    return true;
  }

  @Override
  public boolean isRSSDependent(boolean[] wasNodeOnline, LocalState[] wasLocalState,
      Transition event) {
    return true;
  }

  @Override
  public boolean isNotDiscardReorder(boolean[] wasNodeOnline, LocalState[] wasLocalState,
      NodeOperationTransition crashOrRebootEvent, Event msg) {
    return true;
  }

}
