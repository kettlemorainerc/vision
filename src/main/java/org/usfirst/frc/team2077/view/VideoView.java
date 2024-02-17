package org.usfirst.frc.team2077.view;

import org.slf4j.*;
import org.usfirst.frc.team2077.projection.*;
import org.usfirst.frc.team2077.source.VideoSource;

import java.awt.*;
import java.nio.IntBuffer;
import java.util.Arrays;

public abstract class VideoView {
    private static final Logger LOGGER = LoggerFactory.getLogger(VideoView.class);

    private int[] pixelMapping;
    protected final Dimension resolution;
    protected final RenderingProjection renderProjection;
    protected final SourceProjection sourceProjection;

    protected VideoView(
          Dimension resolution,
          RenderingProjection renderProjection,
          SourceProjection sourceProjection
    ) {
        this.resolution = resolution;
        this.renderProjection = renderProjection;
        this.sourceProjection = sourceProjection;
    }

    public Dimension getResolution() {
        return resolution;
    }

    public abstract void processFrame(IntBuffer frameBuffer);

    /** Maps pixels from the source buffer mutably to the view array. */
    protected final void mapPixels(IntBuffer source, IntBuffer view) {
        if(pixelMapping.length == 0) {
            return;
        }

        for(int idx = 0 ; idx < pixelMapping.length ; idx += 2) {
            view.put(pixelMapping[idx], source.get(pixelMapping[idx + 1]));
        }
    }

    /** Prepares this view for use by the given source. Must be called after both objects have been initialized */
    public synchronized void forSource(VideoSource source) {
        Dimension sourceResolution = source.getResolution();

        int[] newMap = new int[2 * resolution.width * resolution.height];
        final int x = 0;
        final int y = 1;

        int[][] viewIndexRange = intMinMax();
        double[][] viewPixelRange = doubleMinMax();
        double[][] renderingPixelRange = doubleMinMax();
        double[][] renderingRange = doubleMinMax();
        double[][] renderingSphereRange = doubleMinMax();
        double[][] worldSphereRange = doubleMinMax();
        double[][] sourceSphereRange = doubleMinMax();
        double[][] sourceRange = doubleMinMax();
        int[][] sourcePixelRange = intMinMax();
        
        int i = 0;
        
        for(int viewY = 0 ; viewY < resolution.height ; viewY++) {
            for(int viewX = 0 ; viewX < resolution.width ; viewX++) {
                double[] pixel = new double[] {viewX + 0.5, viewY + 0.5};
                
                double[] renderPixels = renderProjection.transformViewToRendering(pixel[x], pixel[y]);
                if(renderPixels == null) {
                    LOGGER.info("({}, {}) - null render", viewX, viewY);
                    continue;
                }
                
                double[] renderXY = renderProjection.transformPixelToCartesian(renderPixels[x], renderPixels[y]);
                double[] renderSphere = renderProjection.renderingProjection(renderXY[x], renderXY[y]);
                if(renderSphere == null) {
                    LOGGER.info("({}, {}) - null render sphere", viewX, viewY);
                    continue;
                }
                
                double[] worldSphere = renderProjection.transformRenderingToWorld(renderSphere[x], renderSphere[y]);
                double[] sourceSphere = renderProjection.isGlobal() ?
                      sourceProjection.transformGlobalWorldToSource(worldSphere[x], worldSphere[y]) :
                      sourceProjection.transformNominalWorldToSource(worldSphere[x], worldSphere[y]);
                double[] sourceXY = sourceProjection.sourceProjection(sourceSphere[x], sourceSphere[y]);
                if(sourceXY == null) {
                    LOGGER.info("({}, {}) - null sourceXY", viewX, viewY);
                    continue;
                }
                
                double[] sourcePixel = sourceProjection.transformCartesianToPixel(sourceXY[x], sourceXY[y]);
                if(sourcePixel == null) {
                    LOGGER.info("({}, {}) - null sourcePx", viewX, viewY);
                    continue;
                }

                int sourceX = (int) Math.round(sourcePixel[x]);
                int sourceY = (int) Math.round(sourcePixel[y]);

                boolean withinWidth = sourceX >= 0 && sourceX < resolution.width;
                boolean withinHeight = sourceY >= 0 && sourceY < resolution.height;
                if(!withinHeight || !withinWidth) {
                    LOGGER.info("({}, {}) - ({}, {}) is not valid", viewX, viewY, sourceX, sourceY);
                    continue;
                }

                minMax(viewIndexRange, new int[]{viewX, viewY});
                minMax(viewPixelRange, pixel);
                minMax(renderingPixelRange, renderPixels);
                minMax(renderingRange, renderXY);
                minMax(renderingSphereRange, renderSphere);
                minMax(worldSphereRange, worldSphere);
                minMax(sourceSphereRange, sourceSphere);
                minMax(sourceRange, sourceXY);
                minMax(sourcePixelRange, new int[] {sourceX, sourceY});

                int viewPixelIdx = (viewY * resolution.width) + viewX;
                int sourcePixelIdx = (sourceY * sourceResolution.width) + sourceX;

                newMap[i] = viewPixelIdx;
                i += 1;
                newMap[i] = sourcePixelIdx;
                i += 1;
            }
        }

        logRange("View index", viewIndexRange);
        logRange("View pixel", viewPixelRange);
        logRange("Rendering pixel", renderingPixelRange);
        logRange("Rendering", renderingRange);
        logRange("Rendering sphere", renderingSphereRange);
        logRange("World sphere", worldSphereRange);
        logRange("Source sphere", sourceSphereRange);
        logRange("Source", sourceRange);
        logRange("Source Pixel", sourcePixelRange);

        // only copy used elements (i)
        LOGGER.info("Mapping {} elements", i);
        this.pixelMapping = Arrays.copyOf(newMap, i);
    }

    private static void minMax(
          int[][] minMax,
          int[] values
    ) {
        minMax[0][0] = Math.min(minMax[0][0], values[0]);
        minMax[0][1] = Math.max(minMax[0][1], values[0]);
        minMax[1][0] = Math.min(minMax[1][0], values[1]);
        minMax[1][1] = Math.max(minMax[1][1], values[1]);
    }

    private static void minMax(
          double[][] minMax,
          double[] values
    ) {
        minMax[0][0] = Math.min(minMax[0][0], values[0]);
        minMax[0][1] = Math.max(minMax[0][1], values[0]);
        minMax[1][0] = Math.min(minMax[1][0], values[1]);
        minMax[1][1] = Math.max(minMax[1][1], values[1]);
    }

    private static int[][] intMinMax() {
        return new int[][]{
              // minimum value, maximum value defaults are literally polar opposite so any value seen will initialize there
              {Integer.MAX_VALUE, Integer.MIN_VALUE},
              {Integer.MAX_VALUE, Integer.MIN_VALUE}
        };
    }

    private static double[][] doubleMinMax() {
        return new double[][]{
              {Double.MAX_VALUE, Double.MIN_VALUE},
              {Double.MAX_VALUE, Double.MIN_VALUE}
        };
    }

    private static void logRange(String identifier, int[][] range) {
        LOGGER.info(
              "{} Range: [minX={}][maxX={}][minY={}][maxY={}]",
              identifier,
              range[0][0], range[0][1],
              range[1][0], range[1][1]
        );
    }

    private static void logRange(String identifier, double[][] range) {
        LOGGER.info(
              "{} Range: [minX={}][maxX={}][minY={}][maxY={}]",
              identifier,
              range[0][0], range[0][1],
              range[1][0], range[1][1]
        );
    }
}
