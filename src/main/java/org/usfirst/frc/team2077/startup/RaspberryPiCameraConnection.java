package org.usfirst.frc.team2077.startup;

import com.jcraft.jsch.*;
import org.slf4j.Logger;

import java.net.*;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

public class RaspberryPiCameraConnection extends Thread {
    private static final Logger LOG = getLogger(RaspberryPiCameraConnection.class);

    private final String user, password, targetIp, command;
    private final int port = 5800;

    public RaspberryPiCameraConnection(
          PiVideoFeed source,
          String user,
          String password,
          String piIp
    ) {
        setDaemon(true);
        setName("Raspberry Pi Video Feed");

        this.user = user;
        this.password = password;
        this.targetIp = piIp;

        String cameraCommand = String.join(
              " | ",
              source.toString(),
              String.join(
                    " ! ",
                    "gst-launch-1.0 -v fdsrc",
                    "h264parse",
                    "queue max-size-time=1000000000 leaky=2",
                    "rtph264pay config-interval=1",
                    "udpsink host=$LOCALHOST port=5801"
              )
        );

        command = String.join(
              "; ",
              "killall raspivid", // kill any currently running raspivid commands
              "sleep 1s", // probably give the previous command time to actually end
              cameraCommand
        );
    }

    @Override public void run() {
        JSch jsch = new JSch();

        while(true) {
            Session session = null;

            while(session == null) {
                try {
                    session = jsch.getSession(user, targetIp, port);
                    session.setUserInfo(new UserInfo() {
                        @Override public String getPassphrase() {return password;}
                        @Override public String getPassword() {return password;}
                        @Override public boolean promptPassword(String message) {return true;}
                        @Override public boolean promptPassphrase(String message) {return false;}
                        @Override public boolean promptYesNo(String message) {return true;}
                        @Override public void showMessage(String message) {}
                    });
                    session.connect();
                } catch(JSchException e) {
                    session = null;
                    LOG.error("Error occurred while connecting to the raspberry pi @ {}", targetIp, e);
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch(InterruptedException ignored) {
                    }
                }
            }

            try {
                ChannelExec exec = (ChannelExec) session.openChannel("exec");
                exec.setCommand(command.replace("$LOCALHOST", getReturnAddress(targetIp)));
                exec.setInputStream(null);
                exec.setOutputStream(null);
                exec.setErrStream(null);

                exec.connect();

                while(exec.isConnected() && !exec.isEOF() && !exec.isClosed()) {
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch(InterruptedException ignored) {}
                }

                if(!exec.isClosed()) {
                    exec.sendSignal("KILL");
                }
                exec.disconnect();
            } catch(Exception e) {
                LOG.error("Failed to run or continue running raspberry pi command", e);
            }
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
        try ( final DatagramSocket socket = new DatagramSocket() ) {
            socket.connect( InetAddress.getByName( remote ), 10002 );
            return socket.getLocalAddress().getHostAddress();
        }
    }

    private static void assertPercentage(double value) {
        if(value < 0 || value > 1) {
            throw new IllegalArgumentException("Value must be a percentage (0-1)");
        }
    }

    public static class PiVideoFeed {
        private Integer width, height, bitrate, framesPerSecond = 20, timeout = 0, device = 1;

        private double roiX = 0, roiY = 0, roiW = 1, roiH = 1;

        public PiVideoFeed width(int width) {
            this.width = width;
            return this;
        }

        public PiVideoFeed height(int height) {
            this.height = height;
            return this;
        }

        public PiVideoFeed bitrate(int bitrate) {
            this.bitrate = bitrate;
            return this;
        }

        public PiVideoFeed fps(int fps) {
            this.framesPerSecond = fps;
            return this;
        }

        public PiVideoFeed timeout(int timout) {
            this.timeout = timout;
            return this;
        }

        public PiVideoFeed device(int device) {
            this.device = device;
            return this;
        }

        /**
         * All parameters are percentages (0-1) and set the position of the camera's
         * sensor that should be used for the extracted image.
         */
        public PiVideoFeed regionOfInterest(double x, double y, double w, double h) {
            assertPercentage(x);
            assertPercentage(y);
            assertPercentage(w);
            assertPercentage(h);

            this.roiX = x;
            this.roiY = y;
            this.roiW = w;
            this.roiH = h;
            return this;
        }

        @Override public String toString() {
            StringBuilder builder = new StringBuilder("raspivid ")
                  .append("-roi ")
                  .append(roiX)
                  .append(',')
                  .append(roiY)
                  .append(',')
                  .append(roiW)
                  .append(',')
                  .append(roiH)
                  .append(' ')
                  .append("-fps ")
                  .append(framesPerSecond)
                  .append(" -t ")
                  .append(timeout)
                  .append(" -md ")
                  .append(device);

            if(width != null) {
                builder.append(" -w ").append(width);
            }

            if(height != null) {
                builder.append(" -h ").append(height);
            }

            if(bitrate != null) {
                builder.append(" -b ").append(bitrate);
            }

            return builder.append(" -o -").toString();
        }
    }
}
