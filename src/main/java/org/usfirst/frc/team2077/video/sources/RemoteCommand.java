package org.usfirst.frc.team2077.video.sources;

import com.jcraft.jsch.*;

import java.net.*;
import java.util.concurrent.atomic.*;
import java.util.logging.*;
import java.util.logging.Logger;

public class RemoteCommand extends Thread {
    private static Logger LOG = Logger.getLogger("remote-command");
//    private AbstractSource
    private Session session_;
    private AtomicReference<ChannelExec> exec_;
    private final String name_, user_, remote_, password_, command_;
    private final int port;
    private final TimeOut timeOut;
    private final Object execLock_;

    public static Builder newBuilder() {
        return new Builder();
    }

    private RemoteCommand(
        String name_,
        String user_,
        String remote_,
        int port,
        String password_,
        String command_,
        AtomicReference<ChannelExec> exec_,
        TimeOut timeOut,
        Object execLock_
    ) {
        this.name_ = name_;
        this.user_ = user_;
        this.remote_ = remote_;
        this.port = port;
        this.password_ = password_;
        this.command_ = command_;
        this.timeOut = timeOut;
        this.exec_ = exec_;
        this.execLock_ = execLock_;
        setDaemon(true);
    }

    private Session determineSession() {
        JSch jsch = new JSch();
        Session session = session_;

        while (session == null) {
            System.out.println("INFO:" + name_ + ": SESSION:" + session_ + " " + session + " " + exec_ + " ");
            try {
                session = jsch.getSession(user_, remote_, port); // TODO: configurable port #
                session.setUserInfo(new SimpleUser(password_));
                session.connect(); // TODO: use timeout?
                System.out.println("INFO:" + name_ + ": Started remote session @ " + remote_ + ".");
            } catch (Exception ex) {
                System.out.println("WARNING:" + name_ + ": Problem starting remote session @ " + remote_ + ".");
                ex.printStackTrace(System.out);
                session = null;
                try {
                    Thread.sleep(1000);
                } catch (Exception ignored) {}
            }
        }

        return session;
    }

    @Override
    public void run() {
        try {
            // loop to start or restart remote command
            while (true) {
                // restart may recreate session or reuse old one depending on whether session_ has been cleared
                Session session = determineSession();
                ChannelExec exec = null;


                System.out.println("INFO:" + name_ + ": EXEC:" + session_ + " " + session + " " + exec_ + " " + exec);
                session_ = session;
                timeOut.reset();
                try {
                    exec = (ChannelExec) session.openChannel("exec");
                    exec.setCommand(command_.replace("$LOCALHOST", getReturnAddress(remote_)));
                    exec.setInputStream(null);
                    //                            ByteArrayOutputStream out = new ByteArrayOutputStream();
                    //                            ByteArrayOutputStream err = new ByteArrayOutputStream();
                    // exec.setOutputStream(System.err, true);
                    // exec.setErrStream(System.err, true);
                    exec.setOutputStream(System.out);
                    exec.setErrStream(System.out);
                    exec.connect();
                    exec_.set(exec);
                    System.out.println("INFO:" + name_ + ": Started remote command " + command_ + ".");
                    // wait until session_ or exec_ is cleared from outside or finishes
                    while ((exec_.get() != null) && (session_ != null) && exec.isConnected() && !exec.isEOF() && !exec.isClosed()) {
                        synchronized (execLock_) {
                            try {
                                execLock_.wait(1000);
                            } catch (Exception ex) {
                                // continue;
                            }
                        }
                    }
                    if (!exec.isClosed()) {
                        exec.sendSignal("KILL");
                    }
                    System.out.println("INFO:" + name_ + ": Exited remote command " + command_ + ".");
                    //                            System.out.write(out.toByteArray());
                    //                            System.out.write(err.toByteArray());
                    System.out.println("INFO:" + name_ + ": Remote command exit status: " + exec.getExitStatus());
                    exec.disconnect();
                    if (session_ == null) {
                        session.disconnect();
                    }
                } catch (Exception ex) {
                    LogRecord rec = new LogRecord(Level.WARNING, String.format("%s: Problem executing remote command @ %s", name_, remote_));
                    rec.setThrown(ex);
                    LOG.log(rec);

                    session_ = null;
                    exec_.set(null);
                    try {
                        Thread.sleep(1000);
                    } catch (Exception exx) {
                    }
                }
            }
        } catch (Exception ex) {
            System.out.println("SEVERE:" + name_ + ": Problem initializing JSCH for remote command execution @ " + remote_ + ".");
            ex.printStackTrace(System.out);
        }
    }

    /**
     * Chooses a network address through which the local host may be reached from a given remote host. Many remote
     * commands require a network address for sending data back to this program. Where the host has multiple network
     * adapters or addresses, not all may be reachable from the remote. A working address is identified by opening a
     * temporary DatagramSocket and getting its address on the local end.
     *
     * @param remote
     * @return An address through which the remote host can reach this local host.
     * @throws SocketException
     * @throws UnknownHostException
     */
    private static String getReturnAddress(String remote) throws SocketException, UnknownHostException {
        try (final DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName(remote), 10002);
            return socket.getLocalAddress()
                         .getHostAddress();
        }
    }

    public static class Builder {
        private String name, user, remote, password, command;
        private TimeOut timeOut;
        private AtomicReference<ChannelExec> exec;
        private Object lock;
        private int port;

        public Builder exec(AtomicReference<ChannelExec> val) {
            exec = val;
            return this;
        }

        public Builder name(String val) {
            name = val;
            return this;
        }

        public Builder user(String val) {
            user = val;
            return this;
        }

        public Builder remote(String val) {
            remote = val;
            return this;
        }

        public Builder password(String val) {
            password = val;
            return this;
        }

        public Builder command(String val) {
            command = val;
            return this;
        }

        public Builder timeOut(TimeOut val) {
            timeOut = val;
            return this;
        }

        public Builder lock(Object val) {
            lock = val;
            return this;
        }

        public Builder port(int val) {
            port = val;
            return this;
        }

        public RemoteCommand build() {
            return new RemoteCommand(
              name,
              user,
              remote,
              port,
              password,
              command,
              exec,
              timeOut,
              lock
            );
        }
    }
}
