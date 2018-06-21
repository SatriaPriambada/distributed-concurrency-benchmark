package edu.uchicago.cs.ucare.dmck.interceptor;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.cassandra.db.SystemKeyspace;
import org.apache.cassandra.db.commitlog.CommitLog;

public class ResetWatcher implements Runnable {

    private static final Logger LOG = Logger.getLogger(ResetWatcher.class);

    private static int nodeId;
    private static String resetFilePath;

    public ResetWatcher(int nodeId) {
        this.nodeId = nodeId;
        this.resetFilePath = System.getProperty("user.dir") + "/resetNodeStates-" + nodeId;
    }

    public void run() {
        LOG.info("Reset Watcher is waiting for " + resetFilePath);

        File resetFile = new File(resetFilePath);
        while (!Thread.interrupted()) {
            if (resetFile.exists()) {
                LOG.info("Resetting states");
                File memtableResetFile = new File(System.getProperty("user.dir") + "/memtableReset-" + nodeId);
                try {
                    if (memtableResetFile.createNewFile()) {
                        LOG.info("memtableResetFile is created");
                    } else {
                        LOG.info("ERROR in creating memtableResetFile");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                LOG.info("Deleting " + resetFilePath);
                if (resetFile.delete()) {
                    LOG.info(resetFilePath + " is deleted");
                } else {
                    LOG.info("ERROR in deleting " + resetFilePath);
                }

                try
                {
                    CommitLog.instance.recover();
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }

                InterceptionLayer.resetSenderSequencer();
                InterceptionLayer.resetReceiverSequencer();
            } else {
                try {
                    Thread.sleep(50);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
