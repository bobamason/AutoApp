package org.masonapps.autoapp.obd2;


public class PID {

    private final int hexCode;
    private final String description;

    public PID(int hexCode, String description) {
        this.hexCode = hexCode;
        this.description = description;
    }

    public int getHexCode() {
        return hexCode;
    }

    public String getHexCodeString() {
        return Integer.toHexString(hexCode);
    }

    public String getDescription() {
        return description;
    }

}
