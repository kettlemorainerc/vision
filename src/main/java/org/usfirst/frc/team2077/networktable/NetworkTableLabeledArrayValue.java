package org.usfirst.frc.team2077.networktable;

import edu.wpi.first.networktables.*;

public class NetworkTableLabeledArrayValue extends NetworkTableArrayValue implements AutoCloseable {
    private NetworkTableValue labels;

    public NetworkTableLabeledArrayValue(NetworkTable table, String key, String... labels) {
        super(table, key);
        initLabels(key, labels);
    }

    public NetworkTableLabeledArrayValue(String key, String... labels) {
        super(key);
        initLabels(key, labels);
    }

    private void initLabels(String key, String[] labels) {
        this.labels = new NetworkTableValue(table,  key + "/labels");
        this.labels.setStringArray(labels);
    }

    public void setLabel(int index, String label) {
        String[] existing = labels.getStringArray();
        existing[index] = label;
        labels.setStringArray(existing);
    }
}
