package edu.uchicago.cs.ucare.dmck.transition;

import java.io.Serializable;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.uchicago.cs.ucare.dmck.event.Event;
import edu.uchicago.cs.ucare.dmck.server.ModelCheckingServerAbstract;

@SuppressWarnings("serial")
public class PacketSendTransition extends Transition implements Serializable {

  final static Logger LOG = LoggerFactory.getLogger(PacketSendTransition.class);

  public static final String ACTION = "packetsend";
  private static final short ACTION_HASH = (short) ACTION.hashCode();
  public static final Comparator<PacketSendTransition> COMPARATOR =
      new Comparator<PacketSendTransition>() {
        public int compare(PacketSendTransition o1, PacketSendTransition o2) {
          Long i1 = o1.getPacket().getId();
          Long i2 = o2.getPacket().getId();
          return i1.compareTo(i2);
        }
      };

  protected ModelCheckingServerAbstract dmck;
  protected Event packet;

  public PacketSendTransition(ModelCheckingServerAbstract dmck, Event packet) {
    this.dmck = dmck;
    this.packet = packet;
    this.id = packet.getToId();
  }

  @Override
  public boolean apply() {
    if (packet.isObsolete()) {
      LOG.debug("Trying to commit obsolete packet");
    }
    try {
      boolean result = dmck.commitAndWait(this);
      return result;
    } catch (InterruptedException e) {
      LOG.error(e.getMessage());
      return false;
    }
  }

  @Override
  public long getTransitionId() {
    Long hash = ((long) ACTION_HASH) << 16;
    hash = hash | (0x0000FFFF & packet.getId());
    return hash;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((packet == null) ? 0 : packet.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    PacketSendTransition other = (PacketSendTransition) obj;
    if (packet == null) {
      if (other.packet != null)
        return false;
    } else if (!packet.equals(other.packet))
      return false;
    return true;
  }

  public String toString() {
    return "packetsend tid=" + getTransitionId() + " " + packet.toString();
  }

  public static PacketSendTransition[] buildTransitions(ModelCheckingServerAbstract dmck,
      Event[] packets) {
    PacketSendTransition[] packetTransitions = new PacketSendTransition[packets.length];
    for (int i = 0; i < packets.length; ++i) {
      packetTransitions[i] = new PacketSendTransition(dmck, packets[i]);
    }
    return packetTransitions;
  }

  public static LinkedList<PacketSendTransition> buildTransitions(ModelCheckingServerAbstract dmck,
      List<Event> packets) {
    LinkedList<PacketSendTransition> packetTransitions = new LinkedList<PacketSendTransition>();
    for (Event packet : packets) {
      packetTransitions.add(new PacketSendTransition(dmck, packet));
    }
    return packetTransitions;
  }

  public Event getPacket() {
    return packet;
  }

  @Override
  public int[][] getVectorClock() {
    return packet.getVectorClock();
  }

  @Override
  public synchronized PacketSendTransition clone() {
    return new PacketSendTransition(this.dmck, this.packet.clone());
  }

  public Object getValue(String key) {
    return this.packet.getValue(key);
  }

  public boolean isSystemSpecificConcurrentEvent(PacketSendTransition otherEvent) {
    if (ModelCheckingServerAbstract.DMCK_NAME.equals("cassChecker")) {
      // Extra checking of concurrency for Cassandra System.
      int lastCR1 = (int) this.getValue("clientRequest");
      int lastCR2 = (int) otherEvent.getValue("clientRequest");
      if (lastCR1 != lastCR2) {
        return true;
      }

      String verb1 = (String) this.packet.getValue("verb");
      String verb2 = (String) otherEvent.packet.getValue("verb");
      int sendNode1 = this.packet.getFromId();
      int sendNode2 = otherEvent.packet.getFromId();
      int recvNode1 = this.packet.getToId();
      int recvNode2 = otherEvent.packet.getToId();
      if (verb1.equals("PAXOS_PROPOSE") && verb2.equals("PAXOS_PREPARE")
          && (sendNode1 == sendNode2) && (recvNode1 == recvNode2)) {
        return true;
      }
    } else if (ModelCheckingServerAbstract.DMCK_NAME.equals("kuduChecker")) {
      // Extra checking of concurrency for Kudu System.
      String tabletId1 = this.getValue("tabletId").toString();
      String tabletId2 = otherEvent.getValue("tabletId").toString();
      if (!tabletId1.equals(tabletId2)) {
        return true;
      }
    }
    return false;
  }

}
