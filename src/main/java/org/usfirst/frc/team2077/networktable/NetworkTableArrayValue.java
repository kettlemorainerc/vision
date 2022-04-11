package org.usfirst.frc.team2077.networktable;

import edu.wpi.first.networktables.*;

public class NetworkTableArrayValue extends NetworkTableValue implements AutoCloseable {

    public NetworkTableArrayValue(NetworkTable table, String key) {super(table, key);}
    public NetworkTableArrayValue(String key) {super(key);}

    @Override public double getDouble() {throw new UnsupportedOperationException("Cannot obtain double from array type");}
    @Override public boolean getBoolean() {throw new UnsupportedOperationException("Cannot obtain boolean from array type");}
    @Override public String getString() {throw new UnsupportedOperationException("Cannot obtain String from array type");}

    public double getDouble(int index) {
        return ((double[]) currentVal)[index];
    }

    public boolean getBoolean(int index) {
        return ((boolean[]) currentVal)[index];
    }

    public String getString(int index) {
        return ((String[]) currentVal)[index];
    }

    public void setDouble(int index, double d) {
        ((double[]) currentVal)[index] = d;
        setDoubleArray((double[]) currentVal);
    }

    public void setBoolean(int index, boolean b) {
        ((boolean[]) currentVal)[index] = b;
        setBooleanArray((boolean[]) currentVal);
    }
}
