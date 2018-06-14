package edu.uchicago.cs.ucare.dmck.cassandra;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import org.apache.log4j.Logger;
import edu.uchicago.cs.ucare.dmck.server.SpecVerifier;

public class CassVerifier extends SpecVerifier {

  private static final Logger LOG = Logger.getLogger(CassVerifier.class);

  private Properties kv = new Properties();

  private String errorType = "";

  private String owner;
  private String value_1;
  private String value_2;
  private String applied_1, applied_2;

  @Override
  public boolean verify() {
    boolean result = true;
    if (!checkDataConsistency()) {
      result = false;
    }
    if (checkNullPointerException()) {
      result = false;
    }
    return result;
  }

  @Override
  public String verificationDetail() {
    String result = "";
    result += "owner=" + owner + " ;";
    result += " value_1=" + value_1 + " ;";
    result += " value_2=" + value_2 + " ;";
    result += " applied_1=" + applied_1 + " ;";
    result += " applied_2=" + applied_2 + " ;";

    result += "\n";
    result += errorType;
    return result;
  }

  private boolean checkDataConsistency() {
    try {
      /*
       * LOG.debug("Executing checkDataConsistency script.");
       * Runtime.getRuntime().exec(this.dmck..workingDirPath + "/checkConsistency.sh").waitFor();
       */

      readData();

      LOG.debug("Read DataConsistency file.");
      FileInputStream verifyInputStream = new FileInputStream(dmck.workingDirPath + "/temp-verify");
      kv.load(verifyInputStream);
      owner = kv.getProperty("owner");
      value_1 = kv.getProperty("value_1");
      value_2 = kv.getProperty("value_2");

      applied_1 = this.dmck.workloadHasApplied.containsKey(1) ? this.dmck.workloadHasApplied.get(1)
          : "false";
      applied_2 = this.dmck.workloadHasApplied.containsKey(2) ? this.dmck.workloadHasApplied.get(2)
          : "false";

      if (value_1.equals("A") && (value_2.equals("B"))) {
        errorType = "Cass-6023";
        return false;
      }

      if (owner.equals("user_2") && applied_1.equals("false")) {
        errorType = "Cass-6013";
        return false;
      }

      return true;
    } catch (Exception e) {
      LOG.error("Failed to check data consistency.");
      LOG.error(e);
      return true;
    }
  }

  private void readData() {
    File resultFile = new File(this.dmck.workingDirPath + "/temp-verify");
    if (resultFile.exists()) {
      resultFile.delete();
    }

    for (int i = 0; i < 1; i++) {
      String filePath = this.dmck.workingDirPath + "/readData-" + i;
      File file = new File(filePath);
      try {
        if (file.createNewFile()) {
          LOG.info(filePath + " is created");
        } else {
          LOG.info("Error in creating " + filePath);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    while (!resultFile.exists()) {
      try {
        Thread.sleep(50);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private boolean checkNullPointerException() {
    boolean npe = false;
    for (int i = 0; i < this.dmck.numNode; i++) {
      npe |= this.dmck.nullPointerException[i];
    }
    if (npe) {
      errorType = "Cass-5925";
      return true;
    }
    return false;
  }

}
