package org.masonapps.autoapp.obd2;

import java.util.ArrayList;

/**
 * Created by Bob on 10/24/2015.
 */
public class ODB2Parser {
    
    public static final String[] DTCStartLookup = {"P0", "P1", "P2", "P3", 
            "C0", "C1", "C2", "C3",
            "B0", "B1", "B2", "B3",
            "U0", "U1", "U2", "U3"};

    public static ArrayList<String> parseTroubleCodes(String response) throws IllegalArgumentException{
        final ArrayList<String> list = new ArrayList<>();
        final ArrayList<Byte> bytes = new ArrayList<>();
        String[] byteStrings = response.split("\\s");
        byte b;
        for (int i = 0; i < byteStrings.length; i++) {
            try {
                b = Byte.parseByte(byteStrings[i], 16);
                if(b == 0x43)continue;
                bytes.add(b);
            } catch (NumberFormatException ignored){}
        }
        if(bytes.size() % 2 != 0) 
            throw new IllegalArgumentException("unable to parse trouble codes");
        int temp;
        for (int i = 0; i < bytes.size() - 1; i += 2) {
            temp = bytes.get(i) << 8 | bytes.get(i + 1);
            if(temp == 0) continue;
            list.add(DTCStartLookup[temp >> 12 & 0xF] + Integer.toHexString(temp >> 8 & 0xF) + Integer.toHexString(temp >> 4 & 0xF) + Integer.toHexString(temp & 0xF));
        }
        return list;
    }
}
