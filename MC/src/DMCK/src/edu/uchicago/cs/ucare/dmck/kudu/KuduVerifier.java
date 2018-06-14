package edu.uchicago.cs.ucare.dmck.kudu;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import edu.uchicago.cs.ucare.dmck.server.SpecVerifier;

public class KuduVerifier extends SpecVerifier {
  String reason = "There is no bug found based on the log checking.";

  @Override
  public boolean verify() {
    // To catch KUDU-2118 error.
    return checkEachNodeLog();
  }

  @Override
  public String verificationDetail() {
    return reason;
  }

  public boolean checkEachNodeLog() {
    FileInputStream kuduLogStream = null;
    BufferedReader kuduLogReader = null;
    try {
      // Read output.log of all KUDU nodes.
      for (int i = 0; i < dmck.numNode; i++) {
        kuduLogStream = new FileInputStream(
            dmck.workingDirPath + "/log/" + dmck.getTestId() + "/node-" + i + "/output.log");
        kuduLogReader = new BufferedReader(new InputStreamReader(kuduLogStream));

        String line = kuduLogReader.readLine();
        while (line != null) {
          // If an assertion is violated, the KUDU output.log will contain Check failed error,
          // which means a bug is found.
          if (line.contains("Check failed")) {
            reason = "An assertion has been violated in node-" + i + ".";
            if (line.contains(
                "Waiting is not allowed to be used on this thread to prevent server-wide latency aberrations and deadlocks.")) {
              reason +=
                  "\nSymptom is similar to KUDU-2118, where the node crashes due to deadlock in a thread.";
            }
            return false;
          }
          line = kuduLogReader.readLine();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        kuduLogReader.close();
        kuduLogStream.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return true;
  }
}
