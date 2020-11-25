package com.example.iota;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import org.apache.commons.lang3.ObjectUtils;
import org.iota.jota.IotaAPI;
import org.iota.jota.builder.AddressRequest;
import org.iota.jota.dto.response.GetNewAddressResponse;
import org.iota.jota.dto.response.GetNodeInfoResponse;
import org.iota.jota.dto.response.SendTransferResponse;
import org.iota.jota.error.ArgumentException;
import org.iota.jota.model.Transfer;
import org.iota.jota.pow.pearldiver.PearlDiverLocalPoW;
import org.iota.jota.utils.SeedRandomGenerator;
import org.iota.jota.utils.TrytesConverter;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Transaction extends AppCompatActivity implements View.OnClickListener {

    public TextView textView_userID;
    private TextView textView_maximumTime;
    private Button buttonTransaction;

    String address = "FNTWVFJINYFPRKTVEJ9PHDVWGPJCJSEMYIDORVZDVIGPMJYJDYHPHNIMSZVHY9XVEBMHMYRAIUWHAEEEDRGZCEFCPY";
    String maximumTime = "24 hours";
    int endTime = 0;
    TimePicker picker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        buttonTransaction = (Button) findViewById(R.id.buttonTransaction);
        buttonTransaction.setOnClickListener(this);

        picker=(TimePicker)findViewById(R.id.timePicker1);
        picker.setIs24HourView(true);

        textView_userID = (TextView) findViewById(R.id.textView_userID);
        textView_maximumTime = (TextView) findViewById(R.id.textView_maximumTime);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String value = extras.getString("key");
            String[] separated = value.split(",");

            // Check if format of QR string is valid
            // If yes parse User ID
            // Otherwise go back to Main Activity
            if (separated.length>1) {
                String uid[] = separated[1].split("=");
                if (uid.length>1) {
                    textView_userID.setText("Your User ID: "+uid[1]);
                }
                else {
                    Intent i = new Intent(Transaction.this, MainActivity.class);
                    Toast.makeText(Transaction.this, "No valid parking slot",Toast.LENGTH_LONG).show();
                    startActivity(i);
                }
            }
            else {
                Intent i = new Intent(Transaction.this, MainActivity.class);
                Toast.makeText(Transaction.this, "No valid parking slot",Toast.LENGTH_LONG).show();
                startActivity(i);
            }
        }
        // Perform HTTP Request for requesting maximum parking time
        new HTTPTask().execute();
    }


    @Override
    public void onClick(View view) {
        // Get Parking time from time picker
        int hour, minute;
        if (Build.VERSION.SDK_INT >= 23 ) {
            hour = picker.getHour();
            minute = picker.getMinute();
        }
        else {
            hour = picker.getCurrentHour();
            minute = picker.getCurrentMinute();
        }
        endTime = minute*60+hour*60+60;

        int actualTime = (int) (System.currentTimeMillis());
        endTime = endTime + actualTime;

        // Run IOTA Transaktion
        new TransaktionTask().execute();
    }

    private class TransaktionTask extends AsyncTask<Void, Void, Void> {
        String resultString = "";
        @Override
        protected Void doInBackground(Void... voids) {
            IotaAPI api = new IotaAPI.Builder().protocol("https")
                    .host("nodes.devnet.iota.org")
                    .port(443)
                    .build();
            int depth = 3;
            int minimumWeightMagnitude = 9;
            String mySeed = "HDJQDECGENCIOEOADTGTIJEZLWQ9CZENNTXLNSTX9HGCEIOAXENJZTZEAGWDGVLLF9HHGWRUKURTSZDUY";

            int securityLevel = 2;
            int value = 1;

            JSONObject object = new JSONObject();
            try {
                object.put("uid", "E24F43FFFE44C3FC");
                object.put("lic", "LB-AB-1234");
                object.put("ts", new Integer(endTime));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            String message = TrytesConverter.asciiToTrytes(object.toString());
            String tag = "HELLOWORLD";

            Transfer TransactionTest = new Transfer(address, value, message, tag);

            ArrayList<Transfer> transfers = new ArrayList<Transfer>();

            transfers.add(TransactionTest);

            long balance = 0;
            try {
                balance = api.getBalance(100, address);
            } catch (ArgumentException e) {
                // Handle error
                e.printStackTrace();
            }

            // Do transaktion if enough balance is available
            if (balance>=value) {
                try {
                    //System.out.printf("Sending 1 i to %s", address);
                    SendTransferResponse response = api.sendTransfer(mySeed, securityLevel, depth, minimumWeightMagnitude, transfers, null, null, false, false, null);
                    //System.out.println(response.getTransactions());
                    resultString = "Transaction successfull";
                } catch (ArgumentException e) {
                    // Handle error
                    e.printStackTrace();
                    resultString = "Could not run transaction";
                }
            }
            else {
                resultString = "Not enough balance";
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Toast.makeText(Transaction.this, resultString, Toast.LENGTH_LONG).show();
        }
    }


    private class HTTPTask extends AsyncTask<Void, Void, Void> {
        OkHttpClient client = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .build();
        @Override
        protected Void doInBackground(Void... voids) {

            Request request = new Request.Builder()
                    .url("http://ec2-18-195-72-120.eu-central-1.compute.amazonaws.com:65432")
                    .addHeader("uid", "E24F43FFFE44C3FC")  // add request headers
                    .addHeader("Connection","close")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String address2="";
                        String maximumTime2="";
                        final String myResponse = response.body().string();
                        try {
                            JSONObject jsonObj = new JSONObject(myResponse);
                            address2 = jsonObj.getString("Adr");
                            maximumTime2 = jsonObj.getString("Tim");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        //Check if new address is valid
                        // Otherwise use default address
                        if (address2.length()==91) {
                            address = address2;
                            int max = Integer.parseInt(maximumTime2);
                            int hours = max/60/60;
                            int minutes = max/60;
                            if (hours<24)
                                maximumTime=Integer.toString(hours) + " hours and " + Integer.toString(minutes) + "minutes";
                        }
                    }
                }
                });
            return null;
        }
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (maximumTime!= "null")
                textView_maximumTime.setText("Maximum Parking time is " + maximumTime);
        }
    }
}
