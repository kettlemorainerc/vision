package org.usfirst.frc.team2077.source;

import com.jcraft.jsch.Session;
import org.slf4j.*;
import org.usfirst.frc.team2077.projection.*;
import org.usfirst.frc.team2077.util.*;
import org.usfirst.frc.team2077.view.View;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.opencv.core.Point;

/**
 * Something that can generate images, intended to be registered as "frames" of a video.
 * <p></p>
 * <h3>Supported Configuration properties</h3>
 * <dl>
 *     <dt>projection</dt>
 *     <dd>The projection that should be used for mapping raw pixels to (x, y) pixel space. Should be a {@link SourceProjection}</dd>
 * </dl>
 * Can be a parent of a {@link RemoteCommand}, see what configuration properties are supported there
 */
public abstract class FrameSource extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(FrameSource.class);


    public final String name;
    private final Class<?> projection;
    protected final List<View> myViews = new LinkedList<>();
    protected final SuperProperties props;
    private boolean closed;
    private final Timer process = new Timer();
    protected final int targetFps;

    public FrameSource(SuperProperties runProps, String name) {
        setName(name);
        props = new SuperProperties(runProps, name);
        this.name = name;
        targetFps = props.getInt("fps", 40);
        try {
            this.projection = props.getReferencedClass("projection");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        setUncaughtExceptionHandler((thread, ex) -> {
            logger.info("{} failed to run", getClass().getSimpleName(), ex);
            System.exit(1);
        });

        long delay = TimeUnit.SECONDS.toMillis( 1) / targetFps;
        this.process.scheduleAtFixedRate(new TimerTask() {@Override public void run() {
            FrameSource.this.processFrame();
        }}, 0, delay);
    }

    public abstract Dimension getResolution();

    private RemoteCommand remote;
    private AtomicReference<Session> session = new AtomicReference<>();

    protected abstract void processFrame();

    public void start() {
        closed = false;
        if(props.get("remote.ip") != null) {
            if(remote != null) {
                remote.interrupt();
            }
            remote = new RemoteCommand(props, session);
            remote.start();
        }

        super.start();
    }

    protected final boolean closed() {return closed;}

    private final Object lock = new Object();
    public void run() {}

    protected abstract void cleanup();

    public final void close() {
        if(closed) return;

        closed = true;
        cleanup();
        lock.notify();
    }

    public void bindView(View view) {
        this.myViews.add(view);
    }

    public final Point getTargetFor(Point p, RenderProjection renderProjection, SourceProjection sourceProjection) {
        p.x += 0.5;
        p.y += 0.5;
        Point rendering = renderProjection.transformRawToDisplay(p);
        if(rendering == null) {
//            logger.info("Failed to transform \"{}\" to display point", p);
            return null;
        }

        Point normalized = renderProjection.transformPixelToNormalized(rendering);
        SphericalPoint projected = renderProjection.renderingProjection(normalized);
        if(projected == null) {
//            logger.info("Failed to transform \"{}\" -> \"{}\" to projection", rendering, normalized);
            return null;
        }

        SphericalPoint world = renderProjection.renderingToWorld(projected);
        SphericalPoint sourceSphere = sourceProjection.worldToSource(world);
        Point normalizedSource = sourceProjection.sourceProjection(sourceSphere);
        if(normalizedSource == null) {
//            logger.info("Failed to transform \"{}\" -> \"{}\" -> \"{}\" to normalized source point", projected, world, sourceSphere);
            return null;
        }

        return sourceProjection.normalizedToPixel(normalizedSource);
    }

    public final Point getTargetFor(Point p, SuperProperties runProps, View against) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        RenderProjection renderProjection = (RenderProjection) against.projection.getConstructor(
                    SuperProperties.class,
                    View.class
            )
            .newInstance(runProps, against);

        SourceProjection sourceProjection = (SourceProjection) projection.getConstructor(
                        SuperProperties.class,
                        FrameSource.class,
                        RenderProjection.class
                )
                .newInstance(runProps, this, renderProjection);

        return getTargetFor(p, renderProjection, sourceProjection);
    }
    public final IndexMap buildMap(SuperProperties runProps, View against) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        RenderProjection renderProjection = (RenderProjection) against.projection.getConstructor(
                SuperProperties.class,
                View.class
        )
                .newInstance(runProps, against);

        if(renderProjection.forward != null) {
            for (int y = 0; y < against.projectionTransformation.rows(); y++) {
                for(int x = 0; x < against.projectionTransformation.cols(); x++) {
                    against.projectionTransformation.put(y, x, renderProjection.forward[y][x]);
                }
            }
        }

        SourceProjection sourceProjection = (SourceProjection) projection.getConstructor(
                SuperProperties.class,
                FrameSource.class,
                RenderProjection.class
        )
                .newInstance(runProps, this, renderProjection);

        Dimension myDimension = getResolution();

        IndexMap map = new IndexMap();
        int viewPixel = 0;
        for(int y = 0; y < against.height; y++) {
            for(int x = 0; x < against.width; x++, viewPixel++) {
                Point p = new Point(x, y);
                Point source = getTargetFor(p, renderProjection, sourceProjection);

                if(source == null || source.x < 0 || source.x >= myDimension.width || source.y < 0 || source.y >= myDimension.height) continue;

                int sourcePixel = (int) ((source.y * myDimension.width) + source.x);

                map.addMapping(viewPixel, sourcePixel);
            }
        }

        return map;
    }
}
