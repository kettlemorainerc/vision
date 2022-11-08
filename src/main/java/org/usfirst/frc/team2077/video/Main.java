package org.usfirst.frc.team2077.video;

import org.usfirst.frc.team2077.video.core.*;
import org.usfirst.frc.team2077.video.interfaces.*;
import org.usfirst.frc.team2077.video.sources.*;
import org.usfirst.frc.team2077.vision.NTMain;
import org.usfirst.frc.team2077.vvcommon.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.nio.*;
import java.util.Timer;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.logging.*;

/**
 * Main class to read configuration. construct sources, views, and renderings, and start the video.
 * <p>
 * The system configuration file(s) are in java properties (String key/value) format,
 * and should contain at minimum one source and one view, keyed as "source0" and "view0".
 * Additional sources and views are tagged "source1", "source2", etc and "view1", etc.
 * <p>
 * \u00A9 2018
 *
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public class Main {

    public static final Logger logger_ = Logger.getLogger(Main.class.getName());

    protected static Properties properties_;

    protected static ByteOrder byteOrder_;

    protected static AtomicInteger frames_ = new AtomicInteger();

    protected static Map<String, VideoSource> sources_ = new LinkedHashMap<>();
    protected static Map<String, VideoView> views_ = new LinkedHashMap<>();
    protected static Collection<VideoView> mappedViews_ = new HashSet<>();

    protected static JFrame videoFrame_;
    protected static JFrame videoFrame2_;
    private static Thread networkTables;

    /**
     * @param args List of configuration property paths. Each path may be either an external
     *             file path or a resource name where "resources/" + name + ".properties" is in java.class.path.
     */
    public static void main(String[] args) {

        init(args);

        networkTables = new Thread(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            org.usfirst.frc.team2077.vision.NTMain.main(args);
        });

        networkTables.start();

        JToggleButton button0 = null;
        JComponent panel = new JPanel(new GridLayout(0, 2));
        ButtonGroup group = new ButtonGroup();
        for (VideoView v : views_.values()) {
            final VideoView view = v;
            JToggleButton button = new JToggleButton(new AbstractAction(properties_.getProperty(
                    view.getName() + ".label",
                    view.getName()
            )) {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent ae) {
                    videoFrame_.setContentPane(view.getJComponent());
                    videoFrame_.revalidate();
                    videoFrame_.repaint();
                    Set<VideoView> views = new HashSet<>();
                    for (VideoView view : view.getViews()) {
                        views.add(view);
                    }
                    views.addAll(mappedViews_);
                    for (VideoSource source : sources_.values()) {
                        source.activateViews(views);
                    }
                }
            });
            group.add(button);
            panel.add(button);
            if (button0 == null) {
                button0 = button;
            }
        }

        if (button0 != null) {
            if (views_.size() > 1) {
                final JFrame controlFrame_ = new JFrame("Video Selector");
                controlFrame_.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                controlFrame_.setAlwaysOnTop(true);
                controlFrame_.setContentPane(panel);
                controlFrame_.pack();
                controlFrame_.setVisible(true);
            }
            button0.doClick();
        }

        videoFrame_.pack();
        videoFrame_.setVisible(true);
    }


    /**
     * @param args List of configuration property paths. Each path may be either an external
     *             file path or a resource name where "resources/" + name + ".properties" is in java.class.path.
     */
    protected static void init(String[] args) {
        properties_ = Utilities.readProperties(args);

        String bo = properties_.getProperty("byte-order");
        // TODO: tested only for LE
        byteOrder_ = "BE".equalsIgnoreCase(bo) ? ByteOrder.BIG_ENDIAN : "LE".equalsIgnoreCase(bo) ? ByteOrder.LITTLE_ENDIAN : ByteOrder.nativeOrder();
        // TODO: check BufferedImage byte order

        initSources();

        initViews();

        startSources();

        // frame rate monitor
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                logger_.log(Level.INFO, "FPS: " + frames_.getAndSet(0) / 10);
            }
        }, 10000, 10000);

        videoFrame_ = new JFrame("Video Vision");
        videoFrame_.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        videoFrame_.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                for (VideoSource vs : sources_.values()) {
                    vs.stop();
                }
            }
        });
        videoFrame2_ = new JFrame("Video2");
        videoFrame2_.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        videoFrame2_.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                for (VideoSource vs : sources_.values()) {
                    vs.stop();
                }
            }
        });
    }

    private static void initSources() {

        // input video sources
        for (int i = 0; ; i++) {
            String name = properties_.getProperty("video" + i);
            if (name == null) {
                break;
            }
            try {
                Class<?> sourceClass = Class.forName(properties_.getProperty(
                        name + ".class",
                        GstreamerSource.class.getName()
                ));
                sources_.put(
                        name,
                        (VideoSource) sourceClass.getDeclaredConstructor(String.class)
                                                 .newInstance(name)
                );
            } catch (Exception ex) {
                logger_.log(Level.WARNING, "Video source " + name + " initialization failed.", ex);
            }
        }
        if (sources_.isEmpty()) {
            logger_.log(Level.SEVERE, "No valid video sources loaded, exiting.");
            System.exit(1);
        }
    }

    private static void initViews() {

        // display views
        for (int i = 0; ; i++) {
            String name = properties_.getProperty("view" + i);
            if (name == null) {
                break;
            }
            try {
                Class<?> viewClass = Class.forName(properties_.getProperty(
                        name + ".class",
                        DefaultView.class.getName()
                ));
                VideoView view = (VideoView) viewClass.getDeclaredConstructor(String.class)
                                                      .newInstance(name);
                views_.put(name, view);
                if (view.isMapped()) {
                    mappedViews_.add(view);
                    mappedViews_.add(view);
                }
            } catch (Exception ex) {
                logger_.log(Level.WARNING, "View " + name + " initialization failed.", ex);
            }
        }
        if (views_.isEmpty()) {
            logger_.log(Level.SEVERE, "No views configured, exiting.");
            System.exit(1);
        }
    }

    private static void startSources() {

        // start video sources
        for (VideoSource vs : sources_.values()) {
            vs.start(); // TODO: start on first use?
            vs.activateViews(mappedViews_);
        }
    }

    public static Properties getProperties() {
        return properties_;
    }

    public static ByteOrder getByteOrder() {
        return byteOrder_;
    }

    public static AtomicInteger getFrameCounter() {
        return frames_;
    }

    public static VideoView getView(String name) {
        return views_.get(name);
    }

    public static VideoSource getSource(String name) {
        return sources_.get(name);
    }
}
