package org.usfirst.frc.team2077.projection;

import com.squedgy.frc.team2077.april.tags.AprilTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.opencv.core.Point;
import org.opencv.osgi.OpenCVNativeLoader;
import org.slf4j.*;
import org.usfirst.frc.team2077.processor.TestAprilTag;
import org.usfirst.frc.team2077.source.FrameSource;
import org.usfirst.frc.team2077.util.*;
import org.usfirst.frc.team2077.video.Main;
import org.usfirst.frc.team2077.video.core.*;
import org.usfirst.frc.team2077.video.interfaces.*;
import org.usfirst.frc.team2077.video.projections.SineProjection;
import org.usfirst.frc.team2077.video.sources.AbstractSource;
import org.usfirst.frc.team2077.video.test.ImageSource;
import org.usfirst.frc.team2077.view.View;

import java.awt.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class SineEquality {
    private static final Logger logger = LoggerFactory.getLogger(SineEquality.class);
    static {
        new OpenCVNativeLoader().init();
        try {
            AprilTag.initialize();
        } catch (Exception | Error e) {
            System.err.println("aprilt tags 2077 dll " + (new File("./april_tags_2077.dll").exists() ? "" : "doesn't ") +  "exists!");
            System.err.println(new File(".").getAbsolutePath());
            System.err.flush();
            System.load(new File("./april_tags_2077.dll").getAbsolutePath());
        }
    }

    private static final String newSourceName = "whatever";
    private static final String oldSourceName = "img";
    private static final String newTargetName = "new-sine";
    private static final String oldTargetName = "old-sine";

    final SineSource newSrc;
    final SineRender newTarget;

    final SineProjection oldSrc;
    final SineProjection oldTarget;

    final FrameSource newSource;
    final View newView;
    final AbstractSource oldSource;
    final RenderedView oldView;
    
    public SineEquality() throws IOException {
        Properties props = Main.properties_ = new Properties();
        Main.sources_ = new HashMap<>();
        
        putProps(props, newSourceName + ".",
            "projection", SineSource.class.getName(),
                "focal-length", 1
        );

        putProps(props, newTargetName + ".",
                "projection", SineRender.class.getName(),
                "resolution", "1600x1000",
                "frame-processor", TestAprilTag.class.getName()
        );
        
        putProps(props, oldSourceName + ".",
                "image", "1280x720.png",
                "projection", SineProjection.class.getName(),
                "focal-length", 1
        );

        putProps(props, oldTargetName + ".",
                "resolution", "1600x1000",
                "video", oldSourceName,
                "projection", SineProjection.class.getName()
        );
        
        
        SuperProperties runProps = new SuperProperties(props);
        newSource = new Whatever(runProps);
        oldSource = new ImageSource(oldSourceName);
        Main.sources_.put(oldSourceName, oldSource);
        oldView = new DefaultView(oldTargetName);
        
        newView = new View(runProps, newTargetName, newSource);
        newView.buildSourceMapping(runProps, newSource);

        newTarget = new SineRender(runProps, newView);
        newSrc = new SineSource(runProps, newSource, newTarget);
        oldSrc = new SineProjection(oldSourceName, oldSource);
        this.oldTarget = new SineProjection(oldTargetName, oldView);
    }

    @Test void source_transformations_are_equivalent() {
        assertEqual(
                newSrc.sourceProjection(new SphericalPoint(1 , 1)),
                oldSrc.sourceProjection(1, 1),
                false
        );

        assertEqual(
                newSrc.worldToSource(new SphericalPoint(1, 1)),
                oldSrc.transformNominalWorldToSource(1, 1),
                false
        );

        assertEqual(
                newSrc.normalizedToPixel(new Point(.5, .5)),
                oldSrc.transformCartesianToPixel(.5, .5),
                false
        );
    }
    
    @ParameterizedTest
    @MethodSource("getPoints")
    void transformations_changed_are_equivalent(Point start) {
        SphericalPoint spherical = null;
        double[] curr = new double[]{start.x, start.y};
        assertEquals(oldTarget.bounds_, newTarget.bounds);

        assertEqual(
                start = newTarget.transformRawToDisplay(start),
                curr = oldTarget.transformViewToRendering(curr[0], curr[1]),
                false
        );

        if(start == null) return;

        assertEqual(
                start = newTarget.transformPixelToNormalized(start),
                curr = oldTarget.transformPixelToCartesian(curr[0], curr[1]),
                false
        );

        if(start == null) return;

        assertEqual(
                spherical = newTarget.renderingProjection(start),
                curr = oldTarget.renderingProjection(curr[0], curr[1]),
                false
        );

        if(spherical == null) return;

        assertEqual(
                spherical = newTarget.renderingToWorld(spherical),
                curr = oldTarget.transformRenderingToWorld(curr[0], curr[1]),
                false
        );

        if(spherical == null) return;

        assertEqual(
                spherical = newSrc.worldToSource(spherical),
                curr = oldSrc.transformNominalWorldToSource(curr[0], curr[1]),
                false
        );

        if(spherical == null) return;

        assertEqual(
                start = newSrc.sourceProjection(spherical),
                curr = oldSrc.sourceProjection(curr[0], curr[1]),
                false
        );

        if(start == null) return;

        assertEqual(
                start = newSrc.normalizedToPixel(start),
                curr = oldSrc.transformCartesianToPixel(curr[0], curr[1]),
                false
        );
    }

    @Test void maps_are_equivalent() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Dimension ov = oldView.getResolution();
        Dimension os = oldSource.getResolution();
        Dimension ns = newSource.getResolution();
        Dimension nv = new Dimension(newView.width, newView.height);

        assertEquals(ov, nv);
        assertEquals(os, ns);

        logger.info("old ({}x{}) -> ({}x{})", os.width, os.height, ov.width, ov.height);
        logger.info("new ({}x{}) -> ({}x{})", ns.width, ns.height, nv.width, nv.height);

        DefaultRendering map = (DefaultRendering) oldView.getRenderings().iterator().next();
        SuperProperties runProps = new SuperProperties(Main.properties_);

        int idx = 0;
        for(int y = 0; y < newView.height; y++) {
            for(int x = 0; x < newView.width; x++, idx++) {
                assertEqual(
                        newSource.getTargetFor(new Point(x, y), runProps, newView),
                        map.getTargetOf(x, y),
                        true
                );
            }
        }

        logger.info("targets match up");

        oldSource.buildMap(oldView);
        int[] m1 = oldSource.viewMap_.get(oldView.getName());
        IndexMap m2 = newSource.buildMap(new SuperProperties(Main.properties_), newView);

        logger.info("maps built");

        for(int i = 0 ; i < m1.length; i += 2) {
            assertEquals(m1[i + 1], m2.getMapped(m1[i]));
        }

        logger.info("mapped values match up");

        m2.forEach((f, t) -> {
            for(int i = 0; i < m1.length; i += 2) {
                if(m1[i] == f) {
                    assertEquals(m1[i + 1], t);
                    return;
                }
            }
        });
    }

    public static void assertEqual(Point p, double[] xy, boolean round) {
        if(round) {
            if(xy != null) {
                xy[0] = Math.round(xy[0]);
                xy[1] = Math.round(xy[1]);
            }
        }

        double[] next = p != null ? new double[]{p.x, p.y} : null;

        assertArrayEquals(xy, next, 0.001, () -> Arrays.toString(xy) + " to equal " + Arrays.toString(next));
    }

    public static void assertEqual(SphericalPoint p, double[] ap, boolean round) {
        if(round) {
            if(ap != null) {
                ap[0] = Math.round(ap[0]);
                ap[1] = Math.round(ap[1]);
            }
        }

        double[] next = null;
        if(p != null) next = new double[]{p.azimuth, p.polar};

        assertArrayEquals(ap, next);
    }

    private static class Whatever extends FrameSource {
        private final Dimension dimension = new Dimension(1280, 720);
        public Whatever(SuperProperties runProps) {
            super(runProps, "whatever");
        }

        @Override public Dimension getResolution() {return dimension;}
        @Override protected void processFrame() {}
        @Override public void run() {}
        @Override protected void cleanup() {}
    }
    
    private static void putProps(Properties base, String prefix, Object...entries) {
        if((entries.length % 2) != 0) throw new IllegalArgumentException("Entries must have an equal amount of keys and values");
        
        SuperProperties props = new SuperProperties(base, prefix);
        for(int i = 0 ; i < entries.length; i += 2) {
            props.put(String.valueOf(entries[i]), entries[i + 1]);
        }
    }

    private static Stream<? extends Arguments> getPoints() {
        return Stream.of(
                Arguments.of(new Point(50, 50)),
                Arguments.of(new Point(0, 0)),
                Arguments.of(new Point(1280, 720))
        );
    }
}