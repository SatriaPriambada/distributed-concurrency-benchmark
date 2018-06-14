package edu.uchicago.cs.ucare.dmck.kudu;

import java.util.Properties;
import edu.uchicago.cs.ucare.dmck.event.Event;
import edu.uchicago.cs.ucare.dmck.server.FileWatcher;
import edu.uchicago.cs.ucare.dmck.server.ModelCheckingServerAbstract;

public class KuduFileWatcher extends FileWatcher {

  public KuduFileWatcher(String sPath, ModelCheckingServerAbstract dmck) {
    super(sPath, dmck);
  }

  @Override
  public synchronized void proceedEachFile(String filename, Properties ev) {
    if (filename.equals("kudu-ts-map")) {
      for (int i = 1; i < dmck.numNode; i++) {
        KuduWorkloadDriver.tabletServersUUID.put(i, ev.get(String.valueOf(i)).toString());
      }
      KuduWorkloadDriver.printTabletServersUUID();
    } else if (filename.startsWith("kudu-tablet-id-")) {
      String tabletId = ev.getProperty("tablet_id");
      if (!KuduWorkloadDriver.tabletIds.contains(tabletId)) {
        KuduWorkloadDriver.tabletIds.add(tabletId);
        KuduWorkloadDriver.printTabletsUUID();
      }
    } else if (filename.startsWith("kudu-event-")) {
      int sender = Integer.parseInt(ev.getProperty("sender"));
      int recv = Integer.parseInt(ev.getProperty("recv"));
      int type = Integer.parseInt(ev.getProperty("type"));
      long hashId = commonHashId(Long.parseLong(ev.getProperty("hashId")));

      Event event = new Event(hashId);
      event.addKeyValue(Event.FROM_ID, sender);
      event.addKeyValue(Event.TO_ID, recv);
      event.addKeyValue(Event.FILENAME, filename);
      event.addKeyValue("type", type);
      event.setVectorClock(dmck.getVectorClock(sender, recv));

      LOG.debug("Received Kudu Event file, sender=" + sender + ", recv=" + recv + ", type=" + type
          + ", hashId=" + hashId + ", filename=" + filename);

      dmck.offerPacket(event);
    } else if (filename.startsWith("kudu-write-op-event-")) {
      int sender = Integer.parseInt(ev.getProperty("sender"));
      int recv = Integer.parseInt(ev.getProperty("recv"));
      int type = Integer.parseInt(ev.getProperty("type"));
      int currentTerm = Integer.parseInt(ev.getProperty("currentTerm"));
      int committedIndex = Integer.parseInt(ev.getProperty("committedIndex"));
      String tabletId = ev.getProperty("tabletId");
      long hashId = commonHashId(Long.parseLong(ev.getProperty("hashId")));

      Event event = new Event(hashId);
      event.addKeyValue(Event.FROM_ID, sender);
      event.addKeyValue(Event.TO_ID, recv);
      event.addKeyValue(Event.FILENAME, filename);
      event.addKeyValue("type", type);
      event.addKeyValue("currentTerm", currentTerm);
      event.addKeyValue("committedIndex", committedIndex);
      event.addKeyValue("tabletId", tabletId);
      event.setVectorClock(dmck.getVectorClock(sender, recv));

      LOG.debug("Received Kudu Event file, sender=" + sender + ", recv=" + recv + ", type=" + type
          + ", currentTerm=" + currentTerm + ", committedIndex=" + committedIndex + ", hashId="
          + hashId + ", tabletId=" + tabletId + ", filename=" + filename);

      dmck.offerPacket(event);
    } else if (filename.startsWith("kudu-raft-state-update-")) {
      int sender = Integer.parseInt(ev.getProperty("sender"));
      String tabletId = ev.getProperty("tabletId");
      // Raft Role representation based on RaftPeerPB in consensus/metadata.proto
      // 999 - UNKNOWN_ROLE
      // 0 - FOLLOWER
      // 1 - LEADER
      // 2 - LEARNER
      // 3 - NON_PARTICIPANT
      // To understand the representation of each role, please refer to the metadata.proto doc.
      int raftRole = Integer.parseInt(ev.getProperty("raftRole"));

      if (KuduWorkloadDriver.tabletIds.contains(tabletId)) {
        LOG.debug("Update Raft State in node=" + sender + " with role=" + raftRole);
        dmck.localStates[sender].setKeyValue(tabletId + "-raftRole", raftRole);
      } else {
        LOG.debug("Ignore Raft State Update for tabletId=" + tabletId + " for node=" + sender
            + " with role=" + raftRole);
      }
    } else if (filename.startsWith("kudu-current-term-state-update-")) {
      int sender = Integer.parseInt(ev.getProperty("sender"));
      String tabletId = ev.getProperty("tabletId");
      int currentTerm = Integer.parseInt(ev.getProperty("currentTerm"));

      if (KuduWorkloadDriver.tabletIds.contains(tabletId)) {
        LOG.debug(
            "Update Current Term State in node=" + sender + " with currentTerm=" + currentTerm);
        dmck.localStates[sender].setKeyValue(tabletId + "-currentTerm", currentTerm);
      } else {
        LOG.debug("Ignore Current Term State Update for tabletId=" + tabletId + " for node="
            + sender + " with currentTerm=" + currentTerm);
      }
    } else if (filename.startsWith("kudu-committed-index-state-update-")) {
      int sender = Integer.parseInt(ev.getProperty("sender"));
      String tabletId = ev.getProperty("tabletId");
      int committedIndex = Integer.parseInt(ev.getProperty("committedIndex"));

      if (KuduWorkloadDriver.tabletIds.contains(tabletId)) {
        LOG.debug("Update Committed Index State in node=" + sender + " with committedIndex="
            + committedIndex);
        dmck.localStates[sender].setKeyValue(tabletId + "-committedIndex", committedIndex);
      } else {
        LOG.debug("Ignore Committed Index State Update for tabletId=" + tabletId + " for node="
            + sender + " with committedIndex=" + committedIndex);
      }
    }
  }

  @Override
  protected void sequencerEnablingSignal(Event packet) {
    // Since current DMCK integration with Kudu has not supported sequencer yet,
    // DMCK should just use common enabling signal function for now.
    commonEnablingSignal(packet);
  }

}
