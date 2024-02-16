package org.usfirst.frc.team2077.view;

import org.opencv.core.*;
import org.usfirst.frc.team2077.projection.*;
import org.usfirst.frc.team2077.view.processors.FrameProcessor;

import javax.swing.*;
import java.awt.*;
import java.awt.Point;
import java.awt.image.*;
import java.lang.reflect.Field;
import java.nio.*;

public class OpenCvView extends VideoView {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(OpenCvView.class);

    private final FrameProcessor processor;
    private final Object latch = new Object();
    private final MatBackedImage[] frames = new MatBackedImage[2];
    private final MatBackedImage overlay;
    private final int[][] nextFrame;
    private ProcessFrame frameState = ProcessFrame.ONE;
    private final Thread displayThread;
    private final JFrame frame;

    public OpenCvView(
            Dimension resolution,
            RenderingProjection renderProjection,
            SourceProjection sourceProjection,
            FrameProcessor processor
    ) {
        super(resolution, renderProjection, sourceProjection);
        this.processor = processor;
        frames[0] = new MatBackedImage(resolution, 24, new int[] {0x00FF0000, 0x0000FF00, 0x000000FF});
        frames[1] = new MatBackedImage(resolution, 24, new int[] {0x00FF0000, 0x0000FF00, 0x000000FF});
        overlay = new MatBackedImage(resolution, 32, new int[] {0x00FF0000, 0x0000FF00, 0x000000FF, 0xFF000000});
        nextFrame = new int[][] {
                ((BufferBackedDataBuffer) frames[0].image.getRaster().getDataBuffer()).data.array(),
                ((BufferBackedDataBuffer) frames[1].image.getRaster().getDataBuffer()).data.array()
        };

        displayThread = new Thread(this::continuallyProcessFrames);
        displayThread.setDaemon(true);

        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    @Override public void processFrame(IntBuffer frameBuffer) {
        synchronized(nextFrame) {
            mapPixels(frameBuffer, nextFrame[frameState.next]);
        }

        synchronized (latch) {
            latch.notify();
        }
    }

    public void continuallyProcessFrames() {
        while(true) {
            try {
                latch.wait();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                return;
            }

            synchronized(nextFrame) {
                swapProcessedFrame();
            }

            try {
                processor.processFrame(frames[frameState.processing].mat, overlay.mat);
            } catch(Exception e) {
                LOG.error("Error processing frame", e);
            }

            this.frame.setContentPane(frames[frameState.processing]);
            this.frame.pack();
            this.frame.repaint();

            System.gc();
        }
    }

    public void swapProcessedFrame() {
        synchronized(nextFrame) {
            frameState = frameState.next();
        }
    }

    private enum ProcessFrame {
        ONE(0, 1), TWO(1, 0);

        final int processing, next;

        ProcessFrame(int processing, int next) {
            this.processing = processing;
            this.next = next;
        }

        private ProcessFrame next() {
            return this == ONE ? TWO : ONE;
        }
    }

    private static class MatBackedImage extends JComponent {
        private final Mat mat;
        private final BufferedImage image;

        public MatBackedImage(Dimension resolution, int bits, int[] masks) {
            this.mat = new Mat(resolution.height, resolution.width, CvType.CV_8UC4);

            int red, green, blue, alpha = 0x0;

            if(bits == 32) {
                red = masks[0];
                green = masks[1];
                blue = masks[2];
                alpha = masks[3];
            } else if(bits == 24) {
                red = masks[0];
                green = masks[1];
                blue = masks[2];
            } else {
                throw new IllegalArgumentException("Unsupported bit depth: " + bits);
            }

            ColorModel model = new DirectColorModel(
                    bits, red, green, blue, alpha
            );

            WritableRaster raster = Raster.createPackedRaster(
                    new BufferBackedDataBuffer(bufferBackedByMat(mat)),
                    resolution.width, resolution.height,
                    resolution.width,
                    masks,
                    null
            );
            this.image = new BufferedImage(model, raster, false, null);
        }

        @Override public void paint(Graphics g) {

        }
    }

    public static class Raster extends WritableRaster {
        public Raster(SampleModel sampleModel, DataBuffer dataBuffer) {
            super(sampleModel, dataBuffer, new Point(0, 0));
        }
    }

    public static class BufferBackedDataBuffer extends DataBuffer {
        private final IntBuffer data;

        public BufferBackedDataBuffer(IntBuffer data) {
            super(DataBuffer.TYPE_INT, data.limit());
            this.data = data;
        }

        @Override public int getElem(int bank, int i) {
            return data.get(i);
        }

        @Override public void setElem(int bank, int i, int val) {
            data.put(i, val);
        }
    }

    private static IntBuffer bufferBackedByMat(Mat mat) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(0);

        long address = mat.dataAddr();
        try {
            Field addr = Buffer.class.getDeclaredField("address");
            addr.setAccessible(true);
            Field cap = Buffer.class.getDeclaredField("capacity");
            cap.setAccessible(true);

            addr.setLong(buffer, address);
            cap.setInt(buffer, (int) mat.total() * Integer.BYTES);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        return buffer.asIntBuffer();
    }
}
