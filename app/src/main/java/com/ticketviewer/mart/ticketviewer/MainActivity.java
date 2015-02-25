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
    //AdView mAdView;
    IntentFilter[] filters;
    String[][] techs;
    PendingIntent pendingIntent;
    NfcAdapter adapter;


    /*
    Tag refers to card with nfc enabled! just in case some didn't know about that before.
    Also Mifare ultralight is the type of card used for non-rechargeable transit cards in Quebec
    Mifare is the brand and ultralight is the model
     */

    /**
     * Method called when the application is created
     * this method sets up attributes and check for valid nfc adapter
     * prepare the intent filters if mfc adapter is ready to go
     * TODO: validation should be on different methods
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        label =  (TextView) findViewById(R.id.textView4);
        alert = (TextView) findViewById(R.id.textView5);
        adapter = NfcAdapter.getDefaultAdapter(this);
        /*mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);*/
        if (adapter == null) {
            // Stop here, we definitely need NFC
           label.setText("Desoler, mais ton appareil ne supporte pas le NFC, il serait temps de rehausser non?");
            finish(); //should we or not???
            return;
        }

        if (!adapter.isEnabled()) {//Look for enabled NFC
            label.setText
                    ("Active ton NFC!");
            alert.setText("NFC non activé");
            alert.setTextColor(Color.RED);
            Toast.makeText(getApplicationContext(), "Please activate NFC and press Back to return to the application!", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));//Redirect the user in the settings menu
        } else {
            alert.setText("NFC activé");
            alert.setTextColor(Color.GREEN);
            label.setText
                   ("Passer une carte...");
        }

        //Getting the intents ready and configure them for the techs we want, in this case is NFCa and tech discovered
        pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter mifare = new IntentFilter((NfcAdapter.ACTION_TECH_DISCOVERED));
        filters = new IntentFilter[] { mifare };
        techs = new String[][] { new String[] {  NfcA.class.getName() } };

        handleIntent(getIntent());
    }

    /**
     *Method called when the application is resumed, it enables the foreground dispatch
     * and resume the ad view
     */
    @Override
    protected void onResume() {
       /* if (mAdView != null) {
            mAdView.resume();
        }*/
        adapter.enableForegroundDispatch(this, pendingIntent, filters, techs);
        super.onResume();
    }

    /**
     * method called when the application is being paused\
     * it stops the ads and disable the foreground dispatch
     */
    @Override
    protected void onPause() {
       /* if (mAdView != null) {
            mAdView.pause();
        }*/
        adapter.disableForegroundDispatch(this);
        super.onPause();
    }

    /**
     * method called when the application is destroyed
     * this overrided method is use for killing the ads only.
     */
    @Override
    public void onDestroy() {
       /* if (mAdView != null) {
            mAdView.destroy();
        }*/
        super.onDestroy();
    }

    /**
     * This method gets called, when a new Intent gets associated with the current activity instance
     * In our case this method gets called, when the user attaches a Tag to the device.
     * @param intent the intent received when a new one is created
     */
    @Override
    protected void onNewIntent(Intent intent) {
        /**

         */
        handleIntent(intent);
    }

    /**
     * this methods is the main point of the application
     * it checks if the intent is valid and if yes, it starts the whole process for getting tickets
     * @param intent the intent created by the nfcAdapter
     */
    private void handleIntent(Intent intent) {
        String action = intent.getAction();

        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag != null) {
            String tagInfo = readTag(tag);
            label.setText(tagInfo);
        }
    }

    /**
     * Method try to read the tag with the mifare ultralight library
     * TODO: check for mifare ultralight tag
     *
     * This methods reads the tag at the sector 3 and 10
     *Its creates two payloads in byte with returned Data from sector
     * @param tag the tag readed by the nfc adapter
     * @return String, the string returned is the one created by findTickets
     * @see  private String findTickets(String payload, String payload2)
     */
    public String readTag(Tag tag) {

        MifareUltralight mifare = null;
        try {
             mifare = MifareUltralight.get(tag);//We suppose the tag is a mifare ultralight one,
            // with more time we should implement a checkup before.
            mifare.connect();
            byte[] payload = mifare.readPages(3);
            byte[] payload2 = mifare.readPages(10);
            String hex = byteToHex(payload);
            String hex2 = byteToHex(payload2);
            return new String(findTickets(hex, hex2));
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "We were not able to retrieve Data from the tag, sorry", Toast.LENGTH_LONG).show();
        }
        finally {
            if (mifare != null) {
                try {
                    mifare.close();
                }
                catch (IOException e) {
                    Toast.makeText(getApplicationContext(), "Something went wrong when closing connection", Toast.LENGTH_LONG).show();
                }
            }
        }
        return null;
    }

    /**
     *  Convert an array of bytes into array of hexs with stringbuilder
     * @param bytes array of bytes to convert
     * @return the array bytes converted to hex
     * @see java.lang.StringBuilder
     */
    private String byteToHex(byte[] bytes)
    {
        StringBuilder sbuilder = new StringBuilder(bytes.length * 2);
        //Convert array of byte into a strings of hex
        for(byte b :bytes)
        {
            sbuilder.append(String.format("%02x", b & 0xff));
        }
        return sbuilder.toString();
    }

    /**
     *This method finds the tickets within the payloads received in parameters
     * the method check for new card, used card and empty card for adapting the returned message
     * toward the situation therefore, the second payload is only used if the card is new
     * @param payload main payload used, its the one retreived from the sector3
     * @param payload2 optional payload used in case the card is new, comes from the sector 10
     * @return the message the end-user will see
     */
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
            if(binArray[i] == '1')//finding the first used tickets
            {
                firstOne = true;
                numberOfTicketsALL++;
            }
            if (firstOne && binArray[i] == '0')//After the first used tickets is found, every 0 after are unused tickets
            {
                numberOfTickets++;
                numberOfTicketsALL++;
            }
        }

        if (firstOne == false && numberOfTickets == 0)//Card used but not empty
        {
            int i = Integer.parseInt(newCard);
            i = i*2;
            return messageNewCard + " Il te reste " + i + " passages";
        }
        else if(numberOfTickets == 0)//card empty... sorry
        {
            return messageEmpty1 + numberOfTicketsALL + messageEmpty2;
        }
        else
        {
            if(numberOfTickets > 1)//multiple tickets remaining
            {
                return numberOfTickets + "passages" + messageUsed + numberOfTicketsALL;
            }
            else{//only one ticket remaining
                return numberOfTickets + " passage restant sur " + numberOfTicketsALL;
            }
        }
    }

    /**
     * convert a string of hex into a string of bins
     * @param hex string fo hexs
     * @return string of binary
     */
    public String hexToBinary(String hex) {
        int i = Integer.parseInt(hex, 16);
        String bin = Integer.toBinaryString(i);
        return bin;
    }




    //Next methods are Android Studio pre-generated methods we haven't modified them

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
