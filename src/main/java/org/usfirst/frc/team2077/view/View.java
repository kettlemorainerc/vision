package org.usfirst.frc.team2077.view;

import org.opencv.core.*;
import org.usfirst.frc.team2077.processor.Processor;
import org.usfirst.frc.team2077.source.FrameSource;
import org.usfirst.frc.team2077.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.*;

import org.slf4j.*;

public class View extends JComponent {
    private static final Logger logger = LoggerFactory.getLogger(View.class);

    public final String name;
    private final String label, infoFileName;

    public final int width, height;
    public final Class<?> projection;
    public final Mat projectionTransformation;
    private final Processor frameProcessor;
    public IndexMap sourceMapping;
    private final boolean interpolate;
    public final JFrame frame;
    private final Rectangle draw = new Rectangle();

    private BufferedImage img, overlay;
    private DataBufferInt imgBuf, overlayBuf;
    private int[] imgData, overlayData;
    private Graphics2D overlayGraphics;

    public View(SuperProperties runProperties, String name, FrameSource source) {
        SuperProperties myProps = new SuperProperties(runProperties, name);
        this.frame = new JFrame();
        frame.add(this);
        this.setBackground(Color.black);
        this.setOpaque(true);

        this.name = name;
        this.interpolate = myProps.getBoolean("interpolate", false);

        label = myProps.get("label");
        infoFileName = myProps.get("frame-info");

        String resolution = myProps.get("resolution");
        String[] parts = resolution.split("[xX]");
        width = Integer.parseInt(parts[0], 10);
        height = Integer.parseInt(parts[1], 10);
        frame.setPreferredSize(new Dimension(width, height));
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        try {
            projection = myProps.getReferencedClass("projection");
            projectionTransformation = new Mat(3, 3, CvType.CV_64F);
            Class<?> frame = myProps.getReferencedClass("frame-processor");

            frameProcessor = (Processor) frame.getConstructor(
                    SuperProperties.class
            )
                    .newInstance(myProps);
        } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        addComponentListener(new ComponentListener() {
            @Override public void componentShown(ComponentEvent e) {
                double scale = Math.min(frame.getWidth() / (double) width, frame.getHeight() / (double) height);
                double drawWidth = Math.round(View.this.width * scale);
                double drawHeight = Math.round(View.this.height * scale);

                double widthDif = frame.getWidth() - drawWidth;
                double heightDif = frame.getHeight() - drawHeight;

                draw.setRect(widthDif / 2, heightDif / 2, drawWidth, drawHeight);
                logger.info("{}", draw);
            }
            @Override public void componentResized(ComponentEvent e) {componentShown(e);}
            @Override public void componentMoved(ComponentEvent e) {}
            @Override public void componentHidden(ComponentEvent e) {}
        });

        source.bindView(this);
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D graphics = (Graphics2D) g;
        graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                interpolate ?
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR :
                        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
        );

        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setColor(Color.WHITE);
        g.drawImage(img, draw.x, draw.y, draw.width, draw.height, null);
        g.drawImage(overlay, draw.x, draw.y, draw.width, draw.height, null);
    }

    public void buildSourceMapping(SuperProperties runProps, FrameSource source) {
        try {
            Dimension resolution = source.getResolution();

            int size = resolution.width * resolution.height;
            this.byteBufA = ByteBuffer.allocate(size * 4);
            this.intBufA = this.byteBufA.asIntBuffer();
            this.byteBufDataA = this.byteBufA.array();

            this.byteBufB = ByteBuffer.allocate(size * 4);
            this.intBufB = this.byteBufB.asIntBuffer();
            this.byteBufDataB = this.byteBufB.array();

            WritableRaster imgRast = COLOR_MODEL.createCompatibleWritableRaster(resolution.width, resolution.height);
            this.img = new BufferedImage(COLOR_MODEL, imgRast, false, null);
            DataBufferInt curBuf = imgBuf = ((DataBufferInt) this.img.getData().getDataBuffer());
            this.imgData = curBuf.getData();
            int[][] banks = curBuf.getBankData();
            for(int i = 1; i < banks.length; i++) banks[i] = imgData;

            WritableRaster overRast = OVERLAY_MODEL.createCompatibleWritableRaster(resolution.width, resolution.height);
            this.overlay = new BufferedImage(OVERLAY_MODEL, overRast, false, null);
            this.overlayGraphics = this.overlay.createGraphics();
            curBuf = overlayBuf = ((DataBufferInt) this.overlay.getData().getDataBuffer());
            this.overlayData = curBuf.getData();
            banks = curBuf.getBankData();
            for(int i = 1; i < banks.length; i++) banks[i] = imgData;

            this.sourceMapping = source.buildMap(runProps, this);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private IntBuffer intBufA, intBufB;
    private ByteBuffer byteBufA, byteBufB;
    private byte[] byteBufDataA, byteBufDataB;

    private Mat tmpOverlay;
    private static final Color clear = new Color(0, 0, 0, 0);

    public void process(Mat bgraImg) {
        if(!frame.isVisible() || sourceMapping.isEmpty() || byteBufDataA == null) return;
        tmpOverlay = frameProcessor.process(bgraImg);

        if(tmpOverlay != null) {
            readMatToRgb(bgraImg, img, tmpOverlay, this.overlay);
        } else {
            readMatToRgb(bgraImg, img);

            overlayGraphics.setColor(clear);
            overlayGraphics.fill(overlayGraphics.getClipBounds());
            overlay.flush();
        }

        frame.repaint();
    }

    private void readMatToRgb(Mat a, BufferedImage aImg, Mat b, BufferedImage bImg) {
        a.get(0, 0, byteBufDataA);
        b.get(0, 0, byteBufDataB);
        intBufA.rewind();
        intBufB.rewind();

        int rows = a.rows();
        int cols = a.cols();
        for(int row = 0; row < rows; row++) {
            for(int col = 0; col < cols; col++) {
                aImg.setRGB(col, row, intBufA.get());
                bImg.setRGB(col, row, intBufB.get());
            }
        }

        aImg.flush();
        bImg.flush();
    }

    private void readMatToRgb(Mat mat, BufferedImage img) {
        mat.get(0, 0, byteBufDataA);
        intBufA.rewind();

        int rows = mat.rows();
        int cols = mat.cols();
        for(int row = 0; row < rows; row++) {
            for(int col = 0; col < cols; col++) {
                img.setRGB(col, row, intBufA.get());
            }
        }

        img.flush();
    }

    private static final ColorModel COLOR_MODEL;
    private static final ColorModel OVERLAY_MODEL;

    static {
        // RGBA
        COLOR_MODEL = new DirectColorModel(
                32,
                0x000000FF,
                0x0000FF00,
                0x00FF0000,
                0xFF000000
        );

        // BGRA
        OVERLAY_MODEL = new DirectColorModel(
                32,
                0x000000FF,
                0x0000FF00,
                0x00FF0000,
                0xFF000000
        );
//        if(ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
//            // BGRA
//        } else {
//            // ARGB
//            COLOR_MODEL = new DirectColorModel(
//                    32,
//                    0x00FF0000,
//                    0x0000FF00,
//                    0x000000FF,
//                    0xFF000000
//            );
//        }
    }
}
