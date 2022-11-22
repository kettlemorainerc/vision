package org.usfirst.frc.team2077.source;

import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.usfirst.frc.team2077.util.SuperProperties;

import java.io.*;
import java.net.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.*;

/**
 * <p>
 * Remote command with the expectation that it's properties are in a sub-property relative
 * to it's parent's configuration.
 * </p>
 * <p>
 * For example, if you have a source whose configuration is behind the prefix "source" the sources remote command's
 * properties would be expected to be prefixed with "source.remote". Such as "source.remote.ip" and
 * "source.remote.command".
 * </p>
 * <br>
 * <h3>Supported properties</h3>
 * <dl>
 *     <dt>ip</dt>
 *     <dd>The target ip for connecting using SSH. <b>REQUIRED</b></dd>
 *     <dt>command</dt>
 *     <dd>The command to run on successfully connectin to the remote "ip". <b>REQUIRED</b></dd>
 *     <dt>user</dt>
 *     <dd>The username to use to login to the remote "ip". Defaults to the default raspberry pi username</dd>
 *     <dt>pass</dt>
 *     <dd>The password to use to login to the remote "ip". Defaults to the default raspberry pi password</dd>
 * </dl>
 */
public class RemoteCommand extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(RemoteCommand.class);
    private final String remote, user, pass, command;
    private final AtomicReference<Session> ref;
    private final SuperProperties props;

    private ChannelExec exec;
    private long execStart;
    private final Object execLock = new Object();

    public RemoteCommand(SuperProperties parentProps, AtomicReference<Session> ref) {
        this.props = new SuperProperties(parentProps, "remote");
        this.remote = props.get("ip");
        this.user = props.get("user", "pi");
        this.pass = props.get("pass", "raspberry");
        this.command = props.get("command");
        this.ref = ref;

        setDaemon(true);
    }

    private Session getSession(JSch from) {
        Session session = ref.get();

        while(!interrupted() && session == null) {
            try {
                session = from.getSession(user, remote, 5800);
                session.setUserInfo(new UserInfo() {
                    @Override public String getPassphrase() {return pass;}
                    @Override public String getPassword() {return pass;}
                    @Override public boolean promptPassword(String message) {return true;}
                    @Override public boolean promptPassphrase(String message) {return true;}
                    @Override public boolean promptYesNo(String message) {return true;}
                    @Override public void showMessage(String message) {}
                });
                session.connect();
            } catch (JSchException e) {
                e.printStackTrace(System.out);
                session = null;
                try {
                    wait(TimeUnit.SECONDS.toMillis(1));
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

        return session;
    }

    private ChannelExec initializeCommand(Session session) throws JSchException {
        execStart = System.currentTimeMillis();
        ChannelExec exec = (ChannelExec) session.openChannel("exec");
        exec.setCommand(command.replace("$LOCALHOST", getReturnAddress(remote)));
        exec.setInputStream(null);
        exec.setOutputStream(new ByteArrayOutputStream());
        exec.setErrStream(new ByteArrayOutputStream());
        exec.connect();

        return exec;
    }

    private void logCommandResults(ChannelExec exec) throws IOException {
        logger.info("Remote exited/killed " + command);
        logger.info("stdout:\n");
        logger.info(exec.getOutputStream().toString());
        logger.info("stderr:\n");
        logger.info(exec.getErrStream().toString());
        logger.info("");
        logger.info("Exit code: " + exec.getExitStatus());
    }

    private void execRemote(Session session) {
        try {
            execStart = System.currentTimeMillis();
            ChannelExec exec = initializeCommand(session);

            while(!interrupted() && this.exec != null && ref.get() != null && exec.isConnected() && !exec.isEOF() && ! exec.isClosed()) {
                synchronized (execLock) {
                    try {
                        execLock.wait(TimeUnit.SECONDS.toMillis(1));
                    } catch (InterruptedException e) {}
                }
            }

            if(!exec.isClosed()) {
                exec.sendSignal("KILL");
            }

            logCommandResults(exec);

            if(this.ref.get() == null) session.disconnect();;
        } catch (Exception e) {
            logger.error("Issuer running remote command @{}", remote, e);

            this.ref.set(null);
            this.exec = null;
            try {
                wait(TimeUnit.SECONDS.toMillis(1));
            } catch (InterruptedException ex) {}
        }
    }

    @Override public void run() {
        try {
            JSch jsch = new JSch();

            while(!interrupted()) {
                Session session = getSession(jsch);
                ref.set(session);
                execRemote(session);
            }
        } catch (Exception e) {
            logger.warn("Uncaught exception dealing with {}@{}: ", user, remote, e);
        }
    }

    public static String getReturnAddress(String remote) {
        try(DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName(remote), 10002);
            return socket.getLocalAddress().getHostAddress();
        } catch (SocketException | UnknownHostException e) {
            throw new RuntimeException("Failed to locate/retrieve our address from our target raspberry pi", e);
        }
    }
}
