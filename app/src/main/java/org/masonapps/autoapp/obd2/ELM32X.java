package org.masonapps.autoapp.obd2;

/**
 * Created by Bob Mason on 7/10/2018.
 */
public class ELM32X {
    public static final String COMMAND_INFO = "I";
    public static final String COMMAND_ECHO_OFF = "E0";
    public static final String COMMAND_AUTO_PROTOCOL = "SP 0";

    public static boolean checkInfoResponseCompatibility(String s) {
        return s.contains("ELM32");
    }
}
