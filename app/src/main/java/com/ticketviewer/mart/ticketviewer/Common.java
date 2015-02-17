package com.ticketviewer.mart.ticketviewer;


import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Created by Mart on 2/3/2015.
 */
public class Common{




    private static int mHasMifareClassicSupport = 0;

    private static NfcAdapter mNfcAdapter;


    public static boolean hasMifareClassicSupport() {
        if (mHasMifareClassicSupport != 0) {
            return mHasMifareClassicSupport == 1;
        }

        // Check for the MifareClassic class.
        // It is most likely there on all NFC enabled phones.
        // Therefore this check is not needed.

        try {
            Class.forName("android.nfc.tech.MifareClassic");
        } catch( ClassNotFoundException e ) {
            // Class not found. Devices does not support Mifare Classic.
            return false;
        }


        // Check if there are NFC libs with "brcm" in their names.
        // "brcm" libs are for devices with Broadcom chips. Broadcom chips
        // don't support Mifare Classic
        File libsFolder = new File("/system/lib");
        File[] libs = libsFolder.listFiles();
        for (File lib : libs) {
            if (lib.isFile()
                    && lib.getName().startsWith("libnfc")
                    && lib.getName().contains("brcm")) {
                mHasMifareClassicSupport = -1;
                return false;
            }
        }
        mHasMifareClassicSupport = 1;
        return true;
    }

    /**
     * Get the App wide used NFC adapter.
     * @return NFC adapter.
     */
    public static NfcAdapter getNfcAdapter() {
        return mNfcAdapter;
    }

    /**
     * Set the App wide used NFC adapter.
     * @param nfcAdapter The NFC adapter that should be used.
     */
    public static void setNfcAdapter(NfcAdapter nfcAdapter) {
        mNfcAdapter = nfcAdapter;
    }

    public static  String hexToBinary(String hex) {
        int i = Integer.parseInt(hex, 16);
        String bin = Integer.toBinaryString(i);
        return bin;
    }

    public static int getTickets(String bin)
    {
        int numberOfTickets = -1;
        boolean firstOne = false;
        char[] binArray = bin.toCharArray();
        for(int i=1; i< bin.length(); i++){
            if(binArray[i] == '1')
            {
                firstOne = true;
                numberOfTickets++;
            }
            if (firstOne && binArray[i] == '0')
            {
                numberOfTickets++;
            }
        }
        return numberOfTickets;
    }

    public static String readTag(Tag tag){
        MifareUltralight mul=MifareUltralight.get(tag);
        try {
            mul.connect();
            byte[] payload=mul.readPages(3);
            return new String(payload, Charset.forName("US-ASCII"));
        }
        catch (  IOException e) {

        }
        finally {
            if (mul != null) {
                try {
                    mul.close();
                }
                catch (      IOException e) {

                }
            }
        }
        return null;
    }

}
