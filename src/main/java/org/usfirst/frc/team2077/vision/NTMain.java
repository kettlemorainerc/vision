package org.usfirst.frc.team2077.vision;

import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableEvent;
import edu.wpi.first.networktables.NetworkTableInstance;

import java.util.EnumSet;


public class NTMain extends org.usfirst.frc.team2077.vision.Main {

    public static NetworkTableInstance networkTable_;

    public static void main( String[] args ) {
        init( args );

        networkTable_ = NetworkTableInstance.getDefault();
        networkTable_.startServer();
        networkTable_.startClient4(properties_.getProperty("network-tables-server", "127.0.0.1"));
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                networkTable_.stopClient();
                networkTable_.stopServer();
            }
        });
        
        NetworkTableEntry visionView = networkTable_.getEntry("VisionView");
        networkTable_.addListener(
                visionView,
                EnumSet.allOf(NetworkTableEvent.Kind.class),
                en -> {
                    VisionView view = views_.get(en.valueData.value.getString());
                    visionFrame_.setContentPane(view.getJComponent());
                    if (!visionFrame_.isVisible()) {
                        visionFrame_.pack();
                        visionFrame_.setVisible(true);
                    }
                    visionFrame_.revalidate();
                    visionFrame_.repaint();
                }
        );
        
        String viewName = visionView.getString(null);
        VisionView view = views_.get(viewName);

        if (view != null) {
            visionFrame_.setContentPane(view.getJComponent());
            if (!visionFrame_.isVisible()) {
                visionFrame_.pack();
                visionFrame_.setVisible(true);
            }
            visionFrame_.revalidate();
            visionFrame_.repaint();
        }
        else {
            (new Thread() {
                public void run() {

                    for ( ;; ) {
                        try {
                            Thread.sleep( 10 * 1000 );
                        } catch ( Exception ex ) {
                        }
                    }
                }
            }).start();
        }
    }
}
