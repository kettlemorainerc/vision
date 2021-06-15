package org.usfirst.frc.team2077.video.sources;

import com.jcraft.jsch.*;
import org.usfirst.frc.team2077.video.*;
import org.usfirst.frc.team2077.video.interfaces.*;
import org.usfirst.frc.team2077.video.projections.*;
import org.usfirst.frc.team2077.vvcommon.*;

import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.logging.Logger;
import java.util.logging.*;
import java.util.regex.*;

import static java.util.logging.Level.*;

/**
 * Base {@link VideoSource} implementation. Includes code to execute a startup command on a remote host as a part of the
 * local startup sequence (to be split into a separate class eventually). The remote command is restarted if the source
 * stops receiving frame updates after some interval. This restart logic is ill-tuned and may be causing more harm than
 * good. It needs work.
 * <p>
 * Configuration properties:
 * <dl>
 * <dt>&lt;source&gt;.projection</dt>
 * <dd>Fully qualified name of a {@link SourceProjection} class. The projection will be instantiated by calling a
 * constructor taking parameters (String name, VideoSource videoSource), where <code>name</code> is its configuration
 * property prefix and <code>videoSource</code> is the calling VideoSource. The proper source projection, with proper
 * configuration values, should implement the camera lens projection function, mapping cartesian source image
 * coordinates to the corresponding spherical vector coordinates being projected. See
 * {@link org.usfirst.frc.team2077.video.test.FisheyeCalibration} for more on how to do this.</dd>
 * <dt>&lt;source&gt;.camera-NS-position</dt>
 * <dd>&nbsp;</dd>
 * <dt>&lt;source&gt;.camera-EW-position</dt>
 * <dd>&nbsp;</dd>
 * <dt>&lt;source&gt;.camera-height</dt>
 * <dd>Camera lens location relative to the global system origin, in physical units. Used by projections that utilize
 * absolute camera position as well as angle, such as {@link org.usfirst.frc.team2077.video.projections.Birdseye}.</dd>
 * <dt>&lt;source&gt;.remote</dt>
 * <dd>Name or address of host on which to on which to execute an optional command associated with starting this source.
 * If not supplied, no remote startup command will be executed.</dd>
 * <dt>&lt;source&gt;.login</dt>
 * <dd>&nbsp;</dd>
 * <dt>&lt;source&gt;.password</dt>
 * <dd>Login credentials for remote startup execution.</dd>
 * <dt>&lt;source&gt;.command</dt>
 * <dd>Remote startup command.</dd>
 * </dl>
 * <p>
 * \u00A9 2018
 *
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public abstract class AbstractSource implements VideoSource {

    private static final Logger LOG = Logger.getGlobal();

    protected static final java.util.Timer timer_ = new Timer();
    // camera placement // TODO: remove
    public final double cameraX_;
    public final double cameraY_;
    public final double cameraZ_;
    protected final String name_, remote_, user_, password_, command_;
    // camera transform
    protected final SourceProjection projection_;
    protected final Map<String, int[]> viewMap_ = new LinkedHashMap<>();
    protected Dimension resolution_ = null;
    protected Set<RenderedView> views_ = new HashSet<>();
    protected TimeOut timeOut;
    AtomicReference<ChannelExec> exec_ = new AtomicReference<>();
    final Object execLock_ = new Object() {
    };

    // for binding when present
    private Optional<Integer> lastStreamPid = Optional.empty();

    // TODO: split remote functions into separate class

    /**
     * Reads configuration properties and initializes optional remote command and camera geometry.
     *
     * @param name Key for configuration properties and {@link Main#getSource}.
     */
    public AbstractSource(String name) {

        name_ = name;

        // remote server access
        remote_ = Main.getProperties().getProperty(name_ + ".remote");
        user_ = Main.getProperties().getProperty(name_ + ".user", "pi");
        password_ = Main.getProperties().getProperty(name_ + ".password", "raspberry");
        command_ = Main.getProperties().getProperty(name_ + ".command");
        if (user_ != null && remote_ != null && command_ != null) {
            System.out.println("INFO:" + name_ + ": " + user_ + "@" + remote_ + " " + command_);
        }

        // camera location
        char cameraOrientation = Main.getProperties()
                                     .getProperty(name_ + ".camera-orientation", "N")
                                     .toUpperCase()
                                     .charAt(0); // N|S|E|W
        AffineTransform worldToCamera = new AffineTransform();
        worldToCamera.rotate((Math.PI / 180.) * (cameraOrientation == 'E' ? 90. : cameraOrientation == 'S' ? 180. : cameraOrientation == 'W' ? 270. : 0.));
        double[] cameraPosition = {
            Double.parseDouble(Main.getProperties()
                                   .getProperty(name_ + ".camera-EW-position", "0.0")),
            Double.parseDouble(Main.getProperties()
                                   .getProperty(name_ + ".camera-NS-position", "0.0"))
        };
        worldToCamera.transform(cameraPosition, 0, cameraPosition, 0, 1);
        cameraX_ = cameraPosition[0];
        cameraY_ = cameraPosition[1];
        cameraZ_ = Double.parseDouble(Main.getProperties()
                                          .getProperty(name_ + ".camera-height", "24.0"));

        SourceProjection projection = null;
        try {
            Class<?> projectionClass = Class.forName(Main.getProperties()
                                                         .getProperty(
                                                                 name + ".projection",
                                                                 Perspective.class.getName()
                                                         ));
            projection = (SourceProjection) projectionClass.getDeclaredConstructor(String.class, VideoSource.class)
                                                           .newInstance(name_, this);
        } catch (Exception ex) {
            System.out.println("SEVERE:" + "Camera projection initialization failed.");
            ex.printStackTrace(System.out);
        }
        projection_ = projection;
    }

    @Override
    public SourceProjection getProjection() {
        return projection_;
    }

    private Optional<Integer> getStreamPid() {
        JSch check = new JSch();

        try {
            Session checkSes = check.getSession(user_, remote_);
            checkSes.setUserInfo(new SimpleUser(password_));
            checkSes.connect();

            ChannelExec shell = (ChannelExec) checkSes.openChannel("exec");

            // TODO: Figure out if there's a high probability of command collision
            shell.setCommand(String.format(
                "ps -e | grep -Eo '\\s*[0-9]+.*%s' | sed -E 's/\\s*([0-9]+).*/\\1/'",
                findPidCommand(command_)
            ));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            shell.setInputStream(null);
            shell.setOutputStream(out);
            shell.setErrStream(out);

            shell.connect();

            while (shell.isConnected() && shell.getExitStatus() == -1 && !shell.isEOF() && !shell.isClosed()) {
                LOG.info("Checking for gst-launch-1.0: " + out);
                try { TimeUnit.MILLISECONDS.sleep(200); }
                catch (InterruptedException e) {
                    LogRecord rec = new LogRecord(SEVERE, "Failed to wait for existing stream check.");
                    rec.setThrown(e);
                    LOG.log(rec);
                }
            }

            LOG.info("Exit code: " + shell.getExitStatus());

            String result = out.toString();
            LOG.info("Shell output:\n" + result);

            try {
                return Optional.of(Integer.parseInt(Pattern.compile("([0-9]+)").matcher(result).group(1)));
            } catch (NumberFormatException ex) { // there was some issue getting the last PID so we won't be able to bind correctly
            }
        } catch (JSchException e) {
            System.err.println(e);
        }

        return Optional.empty();
    }

    private boolean hasRunningStream() {
        lastStreamPid = getStreamPid();

        return lastStreamPid.isPresent();
    }

    private void runRemoteCommand() {
        RemoteCommand.newBuilder()
                     .name(name_)
                     .user(user_)
                     .remote(remote_)
                     .password(password_)
                     .command(command_)
                     .timeOut(timeOut)
                     .lock(execLock_)
                     .build()
                     .start();
    }

    private void bindRemoteCommand() {
        new RemoteCommandBinding(
            user_,
            remote_,
            new SimpleUser(password_),
            timeOut,
            lastStreamPid.orElseThrow(),
            this::getStreamPid
        );
    }

    public static void main(String[] args) {
        Properties props = Utilities.readProperties(new String[] {"aimingFEThree2020"});

        Main.properties_ = props;

        Map<String, VideoSource> sources = new HashMap<>();

        Main.initSources(props, sources);

        LOG.info("Fisheye has running stream: " + ((AbstractSource) sources.get("Fisheye")).hasRunningStream());

        System.exit(0);
    }

    /**
     * Optionally starts a remote video data server in a thread that restarts it if the stream times out. Needs work.
     */
    protected void runRemote() { // TODO: configurable timeout/restart logic, etc, etc

        timeOut = new TimeOut(TimeUnit.SECONDS.toMillis(30));
        if(hasRunningStream()) {
            bindRemoteCommand();
        } else {
            runRemoteCommand();
        }

        timer_.schedule(new TimerTask() {
            @Override
            public void run() {
                if (timeOut.hasTimedOut() && (exec_.get() != null)) {
                    System.out.println("WARNING:" + name_ + ": Frame update timeout (" + timeOut.lastDiff(TimeUnit.SECONDS) + " sec) @ " + remote_ + ".");
                    exec_.set(null);
                    synchronized (execLock_) {
                        execLock_.notifyAll();
                    }
                }
            }
        }, TimeUnit.SECONDS.toMillis(10), TimeUnit.SECONDS.toMillis(1));
    }

    @Override
    public String getName() {
        return name_;
    }

    @Override
    public Dimension getResolution() {
        return resolution_;
    }

    /**
     * Updates a view image with pixels from an incoming source frame.
     *
     * @param view              View to be updated.
     * @param sourceFramePixels Pixel data for one full frame from this video source.
     */
    protected void processFrame(RenderedView view, IntBuffer sourceFramePixels) {

        if (!view.isLive()) {
            return;
        }

        if (viewMap_.get(view.getName()) == null) { // TODO: viewMap_ synchronization
            buildMap(view); // TODO: run this in a BG thread
        }

        // if (!view.isLive()) {
        // return;
        // }

        int[] viewMap = viewMap_.get(view.getName());
        if (viewMap.length == 0) {
            // nothing from this video stream appears in the view
            return;
        }

        view.processFrame(sourceFramePixels, viewMap);
    }

    /**
     * Builds table of where pixel data from this source appears in any view that uses it. Each rendering using this
     * source generates its own subtable, and these are merged into a single table of index pairs {viewPixelIndex0,
     * sourcePixelIndex0, viewPixelIndex1, sourcePixelIndex1... viewPixelIndexN, sourcePixelIndexN} to be used to update
     * pixels in the view image whenever a new frame arrives from the source.
     *
     * @param view View whose pixel source map is being updated.
     */
    protected void buildMap(RenderedView view) {

        int[] viewMapTmp = new int[view.getResolution().width * view.getResolution().height];
        for (int i = 0; i < viewMapTmp.length; i++) {
            viewMapTmp[i] = -1;
        }
        for (Rendering rendering : view.getRenderings()) {
            if (rendering.getVideoSource() == this) {
                int[] renderingMap = rendering.getMap();
                for (int i = 0; i < renderingMap.length; i += 2) {
                    viewMapTmp[renderingMap[i]] = renderingMap[i + 1];
                }
            }
        }
        int n = 0;
        for (int i = 0; i < viewMapTmp.length; i++) {
            if (viewMapTmp[i] != -1) {
                n++;
            }
        }
        int[] viewMap = new int[2 * n];
        n = 0;
        for (int i = 0; i < viewMapTmp.length; i++) {
            if (viewMapTmp[i] != -1) {
                viewMap[n++] = i;
                viewMap[n++] = viewMapTmp[i];
            }
        }
        viewMap_.put(view.getName(), viewMap); // save map even if empty so it won't be recomputed
    }

    @Override
    public void activateViews(Collection<VideoView> views) { // TODO: clarify the purpose of this

        synchronized (views_) {
            for (RenderedView view : views_) {
                view.setLive(views.contains(view));
            }
            views_.clear();
            for (VideoView view : views) {
                if (view instanceof RenderedView) {
                    views_.add((RenderedView) view);
                }
            }
            for (RenderedView view : views_) {
                if (!viewMap_.containsKey(view.getName())) {
                    viewMap_.put(view.getName(), null);
                }
                view.setLive(true);
            }
        }
    }

    /**
     * Splits the given command ({@code within}) into it's constituent parts very loosely to try and determine which command
     * would show up in a unix command {@code ps -e} list.
     * <br />
     * EX. The expected return for
     * {@code killall raspivid; sleep 1s; raspivid -md 1 -w 768 -h 540 -t 0 -fps 20 -roi .1,0,.8,1 -b 750000 -o - |
     * gst-launch-1.0 -v fdsrc ! h264parse ! queue max-size-time=100000000 leaky=2 ! rtph264pay config-interval=-1 !
     * udpsink host=$LOCALHOST port=5801}
     *  would be the {@link String} {@code gst-launch-1.0}
     * @param within the overall command that will be run
     * @throws IllegalArgumentException if within cannot be parsed into ANY command
     */
    private static String findPidCommand(String within) {
        String[] individualCommands = within.split("[;|]");
        String[] lastCommandAndArgs = individualCommands[individualCommands.length - 1].split("[\\s]+");

        for(String com : lastCommandAndArgs) {
            if(com.length() != 0) {
                return com;
            }
        }

        throw new IllegalArgumentException("No command was found within given command: \"" + within + "\"");
    }

} // AbstractSource
