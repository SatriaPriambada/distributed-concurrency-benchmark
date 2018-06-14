package edu.uchicago.cs.ucare.dmck.scm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.uchicago.cs.ucare.dmck.server.ModelCheckingServerAbstract;
import edu.uchicago.cs.ucare.dmck.server.SpecVerifier;
import edu.uchicago.cs.ucare.dmck.util.LocalState;

public class SCMVerifier extends SpecVerifier {

  protected static final Logger LOG = LoggerFactory.getLogger(SCMVerifier.class);

  boolean error;
  LocalState receiver;

  public SCMVerifier() {
    error = false;
    receiver = new LocalState();
  }

  public SCMVerifier(ModelCheckingServerAbstract modelCheckingServer) {
    this.dmck = modelCheckingServer;
  }

  @Override
  public boolean verify() {
    receiver = dmck.localStates[0];
    if ((int) receiver.getValue("vote") != 4) {
      error = true;
      return false;
    } else {
      return true;
    }
  }

  @Override
  public String verificationDetail() {
    if (error) {
      return "Receiver vote is not 4, but " + receiver;
    } else {
      return "Receiver is in correct state. The vote is " + receiver;
    }
  }

}
