package org.usfirst.frc.team2077.video.sources;

import com.jcraft.jsch.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.logging.*;
import java.util.logging.Logger;

import static java.util.concurrent.TimeUnit.*;
import static java.util.logging.Level.*;
import static org.usfirst.frc.team2077.logging.FormatFormatter.*;

public class RemoteCommandBinding extends Thread {
    private static final java.util.logging.Logger LOG = getLogger();

    private final String user, host;
    private String exec;
    private final UserInfo info;
    private final Supplier<Optional<Integer>> getNextPid;
    private final TimeOut timeOut;

    public RemoteCommandBinding(String user, String host, UserInfo info, TimeOut timeOut, int processId, Supplier<Optional<Integer>> getNextPid) {
        this.user = user;
        this.host = host;
        this.info = info;
        this.timeOut = timeOut;
        updateExec(processId);
        this.getNextPid = getNextPid;
    }

    private void updateExec(int nextPid) {
        exec = String.format("while [ -d /proc/%s ]; do sleep .25 ; done", nextPid);
    }

    private void waitForCommandToReturn() throws JSchException, InterruptedException {
        JSch connection = new JSch();

        Session session = connection.getSession(user, host);
        session.setUserInfo(info);
        session.connect();

        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(exec);
        channel.connect();

        while(channel.isConnected() && !channel.isClosed() && !channel.isEOF()) {
            // only reset timeout if we're actively connected to remote
            timeOut.reset();
            MILLISECONDS.sleep(500);
        }
        // the exec returned, we'll need to get a new one
        exec = null;
    }

    private void getNewConnectionPid() throws InterruptedException {
        getNextPid.get().ifPresent(this::updateExec); // try 1 time without pause
        while(exec == null && !isInterrupted()) { // retry every .1 seconds until we either get a new exec or get interrupted
            MILLISECONDS.sleep(100);
            getNextPid.get().ifPresent(this::updateExec);
        }
    }

    @Override
    public void run() {
        while(true) {
            try {
                waitForCommandToReturn();
                // we could do an exec == null check, but look at the last line of waitForCommandToReturn
                getNewConnectionPid();
            } catch(JSchException e) {
                LOG.log(INFO, "Failed to connect to target remote", e);
            } catch(InterruptedException e) { // Basically, we need to stop running
                LOG.info("Interrupted while binding to remote command");
                break;
            }
        }
    }
}
