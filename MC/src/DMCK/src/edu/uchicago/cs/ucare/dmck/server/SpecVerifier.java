package edu.uchicago.cs.ucare.dmck.server;

public class SpecVerifier {

  public ModelCheckingServerAbstract dmck;

  public boolean verify() {
    return true;
  }

  public String verificationDetail() {
    return "No Verification has been set";
  }

}
