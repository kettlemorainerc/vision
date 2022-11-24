package org.usfirst.frc.team2077;

import com.squedgy.frc.team2077.april.tags.AprilTag;
import edu.wpi.first.networktables.NetworkTableInstance;
import org.freedesktop.gstreamer.Gst;
import org.opencv.osgi.OpenCVNativeLoader;
import org.slf4j.*;
import org.usfirst.frc.team2077.source.FrameSource;
import org.usfirst.frc.team2077.view.View;
import org.usfirst.frc.team2077.util.SuperProperties;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.bytedeco.opencv.global.opencv_core.getCudaEnabledDeviceCount;

public class Startup {
    private static final Logger logger = LoggerFactory.getLogger(Startup.class);
    public static NetworkTableInstance networktables;
    private static final String PROPERTIES_ENV_VAR = "PROPERTIES";
    public static final boolean CUDA_ENABLED;

    static {
        CUDA_ENABLED = getCudaEnabledDeviceCount() > 0;
    }

    public static Map<String, FrameSource> sources;
    public static List<View> views;

    public static void main(String[] args) throws ClassNotFoundException, IOException {
        // new OpenCVNativeLoader().init();
        // AprilTag.initialize();
        Gst.init();

        SuperProperties runProperties = new SuperProperties(getFirstValidProperties(args));
        initializeNetworkTables(runProperties);

        sources = buildRunSources(runProperties);
        logger.info("Sources {}", sources);
        views = buildRunViews(runProperties);
        logger.info("Views {}", views);

        sources.forEach((ignored, source) -> source.start());
    }

    private static Map<String, FrameSource> buildRunSources(SuperProperties runProps) throws ClassNotFoundException {
        List<FrameSource> sources = new LinkedList<>();
        String[] sourceTargets = runProps.getDelimited("sources", "\\|");

        for(String target : sourceTargets) {
            Class<?> type = runProps.getReferencedClass(target + ".type");
            FrameSource source;
            try {
                source = (FrameSource) type.getConstructor(
                        SuperProperties.class,
                        String.class
                )
                        .newInstance(runProps, target);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                logger.warn("Failed to build source \"{}\"", target, e);
                continue;
            }

            sources.add(source);
        }

        return sources.stream().collect(Collectors.toMap(s -> s.name, Function.identity()));
    }

    private static List<View> buildRunViews(SuperProperties runProps) {
        List<View> views = new LinkedList<>();
        String[] targetViews = runProps.getDelimited("views", "\\|");

        for(String target : targetViews) {
            String targetSource = runProps.get(target + ".source");
            View next = new View(runProps, target, sources.get(targetSource));
            views.add(next);
        }

        return views;
    }

    private static void initializeNetworkTables(SuperProperties runProperties) {
        networktables = NetworkTableInstance.getDefault();
        Runtime runtime = Runtime.getRuntime();

        if(runProperties.getBoolean("network-tables.local", false)) {
            networktables.startServer();
            networktables.startClient();

            runtime.addShutdownHook(new Thread(() -> {
                networktables.stopClient();
                networktables.stopServer();
            }));
        } else {
            networktables.startClient(runProperties.get("network-tables.server", "localhost"));

            runtime.addShutdownHook(new Thread(networktables::stopClient));
        }
    }


    private static Properties load(InputStream stream) throws IOException {
        Properties ret = new Properties();
        ret.load(stream);
        return ret;
    }

    private static Properties ofName(String file) {
        try (InputStream resource = Startup.class.getResourceAsStream("/" + file + ".properties")) {
            if(resource != null) {
                logger.debug("Attempting to load {} as resource", file);
                return load(resource);
            }
        } catch (IOException e) {
            logger.debug("Failed to load {} as a resource", file);
            throw new RuntimeException(e);
        }

        File second = Paths.get(".", file).toFile();

        if(second.exists()) {
            logger.debug("Attempting to load {} from file", file);
            try(FileInputStream fis = new FileInputStream(second)) {
                return load(fis);
            } catch (IOException e) {
                logger.debug("Failed to load {} as a file", file);
                throw new RuntimeException(e);
            }
        }

        logger.error("Failed to load {} to a configuration file.", file);
        throw new IllegalArgumentException(file + " could not be loaded as a properties file from resources or local file");
    }

    public static Properties getFirstValidProperties(String[] args) {
        List<String> potentialProperties = new LinkedList<>();

        for(String s : args) {
            if(!s.startsWith("-")) {
                try {
                    return ofName(s);
                } catch (IllegalArgumentException e) {}
            }
        }

        Map<String, String> env = System.getenv();
        if(env.containsKey(PROPERTIES_ENV_VAR)) {
            return ofName(env.get(PROPERTIES_ENV_VAR));
        }

        throw new IllegalStateException("Failed to load a requested configuration file from " + Arrays.toString(args) + " or the " + PROPERTIES_ENV_VAR + " environment variable");
    }

    public static void time(Runnable task, String label) {
        long start = System.currentTimeMillis();

        task.run();

        logger.info("{} millis to {}", System.currentTimeMillis() - start, label);
    }
}
