package org.usfirst.frc.team2077.video.core;

import java.awt.Dimension;
import java.lang.reflect.Constructor;
import java.util.Arrays;

import org.usfirst.frc.team2077.video.Main;
import org.usfirst.frc.team2077.video.interfaces.RenderedView;
import org.usfirst.frc.team2077.video.interfaces.Rendering;
import org.usfirst.frc.team2077.video.interfaces.RenderingProjection;
import org.usfirst.frc.team2077.video.interfaces.SourceProjection;
import org.usfirst.frc.team2077.video.interfaces.VideoSource;

/**
 * {@link Rendering} implementation.
 * <p>
 * \u00A9 2018
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public class DefaultRendering implements Rendering {

    protected final String name_;
    
    protected final RenderedView view_;
    
    protected final VideoSource videoSource_;
    
    protected final RenderingProjection renderingProjection_;
    
    protected final SourceProjection sourceProjection_;

    /**
     * 
     * @param name Key for configuration properties.
     * @param view Parent view.
     */
    public DefaultRendering(String name, RenderedView view) {

        name_ = name;
        view_ = view;
                    
        videoSource_ = Main.getSource(Main.getProperties().getProperty(name_ + ".video"));
        if (videoSource_ == null) {
            System.out.println("SEVERE:" + "No video source for " + name_ + ".");
        }
        sourceProjection_ = videoSource_ != null ? videoSource_.getProjection() : null;
        
        RenderingProjection projection = null;
        try {
            Class<?> projectionClass = Class.forName(Main.getProperties().getProperty(name_ + ".projection", sourceProjection_.getClass().getName()));
            System.out.println(name_ + ": Projection class: " + projectionClass);
            Constructor<?> constr = projectionClass.getConstructor(String.class, RenderedView.class);
            System.out.println(name_ + ": Constructor: " + constr);
            projection = (RenderingProjection) constr.newInstance(name_, view_);
        } catch (Exception ex) {
            System.out.println("SEVERE:" + "Projection initialization failed  for " + name_ + ".");
            ex.printStackTrace(System.out);
        }
        renderingProjection_ = projection;
         
    }
    
    @Override
    public String getName() {
        return name_;
    }
    
    @Override
    public RenderedView getView() {
        return view_;
    }
    
    @Override
    public VideoSource getVideoSource() {
        return videoSource_;
    }

    public double[] getTargetOf(int viewPixelX, int viewPixelY) {

        double[] viewPixel = {viewPixelX+.5, viewPixelY+.5}; // double pixel values are pixel center

        double[] renderingPixel = renderingProjection_.transformViewToRendering(viewPixel[0], viewPixel[1]);
        if (renderingPixel == null) { // TODO: decide which methods may return null
            return null;
        }

        double[] renderingXY = renderingProjection_.transformPixelToCartesian(renderingPixel[0], renderingPixel[1]);

        double[] renderingSpherical = renderingProjection_.renderingProjection(renderingXY[0], renderingXY[1]);
        if (renderingSpherical == null) {
            return null;
        }

        double[] worldSpherical = renderingProjection_.transformRenderingToWorld(renderingSpherical[0], renderingSpherical[1]);

        double[] sourceSpherical = renderingProjection_.isGlobal()
                ? sourceProjection_.transformGlobalWorldToSource(worldSpherical[0], worldSpherical[1])
                : sourceProjection_.transformNominalWorldToSource(worldSpherical[0], worldSpherical[1]);

        double[] sourceXY = sourceProjection_.sourceProjection(sourceSpherical[0], sourceSpherical[1]);
        if (sourceXY == null) {
            return null;
        }

        return sourceProjection_.transformCartesianToPixel(sourceXY[0], sourceXY[1]);
    }

    @Override
    public int[] getMap() {
        Dimension sourceResolution = videoSource_.getResolution();
        Dimension viewResolution = view_.getResolution();

        int[] map = new int[2 * viewResolution.width * viewResolution.height];
        int i = 0;

        for (int viewPixelY = 0; viewPixelY < viewResolution.height; viewPixelY++) {
            for (int viewPixelX = 0; viewPixelX < viewResolution.width; viewPixelX++) {
                double[] sourcePixel = getTargetOf(viewPixelX, viewPixelY);

                if (sourcePixel == null) {
                    continue;
                }

                int sourcePixelX = (int)Math.round(sourcePixel[0]);
                int sourcePixelY = (int)Math.round(sourcePixel[1]);
                if (sourcePixelX < 0 || sourcePixelX >= sourceResolution.width || sourcePixelY < 0 || sourcePixelY >= sourceResolution.height) {
                    continue;
                }
               
                int viewPixelIndex = (viewPixelY * viewResolution.width) + viewPixelX;
                int sourcePixelIndex = (sourcePixelY * sourceResolution.width) + sourcePixelX;
                
                map[i++] = viewPixelIndex;
                map[i++] = sourcePixelIndex;
            }
        }

        return Arrays.copyOf(map, i);
    }

}
