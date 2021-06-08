package org.usfirst.frc.team2077.video.sources;

import com.jcraft.jsch.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.logging.*;
import java.util.logging.Logger;

import static java.util.concurrent.TimeUnit.*;
import static java.util.logging.Level.*;

public class RemoteCommandBinding extends Thread {
    private static final java.util.logging.Logger LOG = Logger.getLogger(RemoteCommandBinding.class.getName());

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
    }

    private void getNewConnectionPid() throws InterruptedException {
        exec = null;
        while(exec == null) {
            getNextPid.get().ifPresent(this::updateExec);
            MILLISECONDS.sleep(100);
        }
    }

    @Override
    public void run() {
        while(true) {
            try {
                waitForCommandToReturn();
                getNewConnectionPid();
            } catch(JSchException e) {
                LOG.log(INFO, "Failed to connect to target remote", e);
            } catch(InterruptedException e) { // Basically, we need to stop running
                LOG.info("Interrrupted while binding to remote command");
                break;
            }
        }
    }
}
