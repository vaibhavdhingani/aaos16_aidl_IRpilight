package com.android.rpiledcontrol;

import android.app.Activity;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.content.Context;
import android.os.BatteryManager;
import android.os.ServiceManager;
import android.hardware.rpilight.IRpilight;
import android.widget.Toast;

public class MainActivity extends Activity {

    private ServiceConnection serviceConnection;
    private static final String TAG = "RpiLEDControl"; 
    private IRpilight mRpilightService;
    IBinder mRpilightBinder;
    public final String PACKAGE_CLUSTERHAL = "android.hardware.rpilight.IRpilight/default";


    private Button ledTurnOn, ledTurnOff;
    private TextView stateTextView;
    boolean isLEDOn = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "Starting rpilight_apk");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        ledTurnOn = (Button)findViewById(R.id.ledTurnOn);
        ledTurnOff = (Button)findViewById(R.id.ledTurnOff);
        stateTextView = (TextView)findViewById(R.id.stateTextView);

        Log.i(TAG, "ledTurnOn ledTurnOff Created");
        Log.i(TAG, "Started");
        mRpilightBinder = ServiceManager.waitForDeclaredService(PACKAGE_CLUSTERHAL);
        Log.i(TAG, "Came out from ServiceManager.waitForDeclaredService");
        mRpilightService = IRpilight.Stub.asInterface(mRpilightBinder);
        Log.i(TAG, "Interface created");

        try {
            int state = isLEDOn ? 0 : 1;
            Log.i(TAG, "RpiLight : Setting LED to: " + state);
            int result = mRpilightService.ledControl(state);
            Log.i(TAG, "LED control result: " + result);
            isLEDOn = !isLEDOn;
        } catch (RemoteException e) {
            e.printStackTrace();
        }


        ledTurnOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "ledTurnOn Clicked");
                mRpilightBinder = ServiceManager.waitForDeclaredService(PACKAGE_CLUSTERHAL);
                Log.i(TAG, "Came out from ServiceManager.waitForDeclaredService");
                mRpilightService = IRpilight.Stub.asInterface(mRpilightBinder);
                Log.i(TAG, "Interface created");

                try {
                    int state = 1;
                    Log.i(TAG, "RpiLight : Setting LED to: " + state);
                    int result = mRpilightService.ledControl(state);
                    Log.i(TAG, "LED control result: " + result);
                    isLEDOn = !isLEDOn;
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

                Toast.makeText(MainActivity.this, "LED Turn On", Toast.LENGTH_SHORT).show();
                stateTextView.setText("LED is ON");

            }
        });



        ledTurnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "ledTurnOff Clicked");
                mRpilightBinder = ServiceManager.waitForDeclaredService(PACKAGE_CLUSTERHAL);
                Log.i(TAG, "Came out from ServiceManager.waitForDeclaredService");
                mRpilightService = IRpilight.Stub.asInterface(mRpilightBinder);
                Log.i(TAG, "Interface created");

                try {
                    int state = 0;
                    Log.i(TAG, "RpiLight : Setting LED to: " + state);
                    int result = mRpilightService.ledControl(state);
                    Log.i(TAG, "LED control result: " + result);
                    isLEDOn = !isLEDOn;

                } catch (RemoteException e) {
                    e.printStackTrace();
                }

                Toast.makeText(MainActivity.this, "LED Turn OFF", Toast.LENGTH_SHORT).show();
                stateTextView.setText("LED is OFF");
            }
        });

        //Intent intent = new Intent("android.hardware.rpilight.IRpilight");
        //intent.setPackage("android.hardware.rpilight"); // Set the package name of the service
        //Log.i(TAG, "Started");
        //bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        //Log.i(TAG, "Returned the service");

    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(serviceConnection);
    }

    private void updateStateText(boolean isChecked) {
        stateTextView.setText("LED State : ");
    }
}
