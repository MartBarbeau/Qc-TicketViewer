package com.ticketviewer.mart.ticketviewer;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.io.IOException;


public class MainActivity extends Activity {

    public static final String MIME_TEXT_PLAIN = "text/plain";

    TextView label;
    TextView alert;
    AdView mAdView;
    IntentFilter[] filters;
    String[][] techs;
    PendingIntent pendingIntent;
    NfcAdapter adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        label =  (TextView) findViewById(R.id.textView4);
        alert = (TextView) findViewById(R.id.textView5);
        adapter = NfcAdapter.getDefaultAdapter(this);
        mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        if (adapter == null) {
            // Stop here, we definitely need NFC
           label.setText("Desoler, mais ton appareil ne supporte pas le NFC, il serait temps de rehausser non?");
            //finish();
            return;
        }

        if (!adapter.isEnabled()) {
            label.setText
                    ("Active ton NFC!");
            alert.setText("NFC non activé");
            alert.setTextColor(Color.RED);
            Toast.makeText(getApplicationContext(), "Please activate NFC and press Back to return to the application!", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
        } else {
            alert.setText("NFC activé");
            alert.setTextColor(Color.GREEN);
            label.setText
                   ("Passer une carte...");
        }

        pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter mifare = new IntentFilter((NfcAdapter.ACTION_TECH_DISCOVERED));
        filters = new IntentFilter[] { mifare };
        techs = new String[][] { new String[] {  NfcA.class.getName() } };

        handleIntent(getIntent());
    }

    @Override
    protected void onResume() {
        if (mAdView != null) {
            mAdView.resume();
        }
        adapter.enableForegroundDispatch(this, pendingIntent, filters, techs);
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (mAdView != null) {
            mAdView.pause();
        }
        adapter.disableForegroundDispatch(this);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (mAdView != null) {
            mAdView.destroy();
        }
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        /**
         * This method gets called, when a new Intent gets associated with the current activity instance
         * In our case this method gets called, when the user attaches a Tag to the device.
         */
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();

        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag == null) {

        } else {
            String tagInfo = readTag(tag);
            label.setText(tagInfo);
        }
    }

    public String readTag(Tag tag) {
        MifareUltralight mifare = MifareUltralight.get(tag);
        try {
            mifare.connect();
            byte[] payload = mifare.readPages(3);
            byte[] payload2 = mifare.readPages(10);
            String hex = byteToHex(payload);
            String hex2 = byteToHex(payload2);
            return new String(findTickets(hex, hex2));
        } catch (IOException e) {

        }
        finally {
            if (mifare != null) {
                try {
                    mifare.close();
                }
                catch (IOException e) {

                }
            }
        }
        return null;
    }

    private String byteToHex(byte[] bytes)
    {
        StringBuilder sbuilder = new StringBuilder(bytes.length * 2);
        for(byte b :bytes)
        {
            sbuilder.append(String.format("%02x", b & 0xff));
        }
        return sbuilder.toString();
    }

    private String findTickets(String payload, String payload2)
    {
        String otp  = payload.substring(0, 8);
        String newCard = payload2.substring(0, 4);
        String messageEmpty1 = "Votre carte de ";
        String messageEmpty2 = " passages est vide";
        String messageUsed = " restants sur ";
        String messageNewCard = "Carte neuve!!! Jète la pas!!!!!!!";
        int numberOfTickets = 0;
        int numberOfTicketsALL = 0;
        boolean firstOne = false;
        String otpToBin = hexToBinary(otp);
        char[] binArray = otpToBin.toCharArray();
        for(int i=0; i< otpToBin.length(); i++){
            if(binArray[i] == '1')
            {
                firstOne = true;
                numberOfTicketsALL++;
            }
            if (firstOne && binArray[i] == '0')
            {
                numberOfTickets++;
                numberOfTicketsALL++;
            }
        }
        if (firstOne == false && numberOfTickets == 0)
        {
            int i = Integer.parseInt(newCard);
            i = i*2;
            return messageNewCard + " Il te reste " + i + " passages";
        }
        else if(numberOfTickets == 0)
        {
            return messageEmpty1 + numberOfTicketsALL + messageEmpty2;
        }
        else
        {
            if(numberOfTickets > 1)
            {
                return numberOfTickets + "passages" + messageUsed + numberOfTicketsALL;
            }
            else{
                return numberOfTickets + " passage restant sur " + numberOfTicketsALL;
            }
        }
    }

    public String hexToBinary(String hex) {
        int i = Integer.parseInt(hex, 16);
        String bin = Integer.toBinaryString(i);
        return bin;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
