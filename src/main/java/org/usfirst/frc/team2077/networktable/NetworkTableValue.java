package org.usfirst.frc.team2077.networktable;

import edu.wpi.first.networktables.*;
import edu.wpi.first.wpilibj.smartdashboard.*;

import java.util.*;

public class NetworkTableValue implements AutoCloseable {
    protected final NetworkTable table;
    protected final NetworkTableEntry currentEntry;
    protected Object currentVal;
    private int listenerHandle = 0;
    private List<Runnable> onChanged = new LinkedList<>();

    public NetworkTableValue(NetworkTable table, String key) {
        this.table = table;
        currentEntry = table.getEntry(key);

        listenerHandle = currentEntry.addListener(
            notif -> {
                if(notif.getEntry().exists()) {
                    currentVal = notif.value.getValue();
                } else currentVal = null;
                for(Runnable change: onChanged) change.run();
            },
            EntryListenerFlags.kUpdate |
            EntryListenerFlags.kLocal |
            EntryListenerFlags.kDelete |
            EntryListenerFlags.kNew
        );
    }

    public NetworkTableValue(String key) {
        this(NetworkTableInstance.getDefault().getTable("SmartDashboard"), key);
    }

    public Runnable addOnChanged(Runnable onChanged) {
        this.onChanged.add(onChanged);
        return onChanged;
    }

    public void removeOnChanged(Runnable onChanged) {
        this.onChanged.remove(onChanged);
    }

    public double getDouble() {
        if(currentVal == null) return 0D;
        return (double) currentVal;
    }
    public boolean getBoolean() {
        if(currentVal == null) return false;
        return (boolean) currentVal;
    }
    public String getString() {return (String) currentVal;}

    public double[] getDoubleArray() {
        if(currentVal == null) return null;
        double[] curr = (double[]) currentVal;
        double[] copy = new double[curr.length];
        System.arraycopy(curr, 0, copy, 0, copy.length);
        return copy;
    }
    public boolean[] getBooleanArray() {
        if(currentVal == null) return null;
        boolean[] curr = (boolean[]) currentVal;
        boolean[] copy = new boolean[curr.length];
        System.arraycopy(curr, 0, copy, 0, copy.length);
        return copy;
    }
    public String[] getStringArray() {
        if(currentVal == null) return null;
        String[] curr = (String[]) currentVal;
        String[] copy = new String[curr.length];
        System.arraycopy(curr, 0, copy, 0, copy.length);
        return copy;
    }

    public void setString(String value) {
        currentVal = value;
        currentEntry.setString(value);
    }
    public void setDouble(double value) {
        currentVal = value;
        currentEntry.setDouble(value);
    }
    public void setBoolean(boolean val) {
        currentVal = val;
        currentEntry.setBoolean(val);
    }
    public void setStringArray(String[] value) {
        currentVal = value;
        currentEntry.setStringArray(value);
    }
    public void setDoubleArray(double[] value) {
        currentVal = value;
        currentEntry.setDoubleArray(value);
    }
    public void setBooleanArray(boolean[] value) {
        currentVal = value;
        currentEntry.setBooleanArray(value);
    }

    @Override public void close() {
        if(listenerHandle != 0) currentEntry.removeListener(listenerHandle);
    }
}
