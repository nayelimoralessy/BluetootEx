package com.example.bluetoothex;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.session.MediaSession;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_ENABLE_BT = 0;
    private static final int REQUEST_DISCOVER_BT = 1;
    private static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private BluetoothDevice mmDevice;
    private ConnectThread connectThread;
    private AcceptThread accThread;
    private SuccessConnect mmConnected;
    private MediaSession mMedia;

    TextView mStatusBlue, mOutput;              //CHANGE NAME
    ImageView mBlueIv;
    Button mOnBtn, mOffBtn, mDiscoverBtn, mPairedBtn, mWorkn;

    BluetoothAdapter mBlueAdapter;
    BluetoothHeadset bluetoothHeadset;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mStatusBlue = findViewById(R.id.statusBlue);
        mOutput = findViewById(R.id.output);
        mBlueIv = findViewById(R.id.bluetoothIv);
        mOnBtn = findViewById(R.id.onBtn);
        mOffBtn = findViewById(R.id.offBtn);
        mDiscoverBtn = findViewById(R.id.discoverableBtn);
        mPairedBtn = findViewById(R.id.pairedBtn);
        mWorkn = findViewById(R.id.WorkingorNot);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        // Adapter
        mBlueAdapter = BluetoothAdapter.getDefaultAdapter();
        //BluetoothProfile.ServiceListener profileListener = new BluetoothProfile.ServiceListener() {
        // @Override
//            public void onServiceConnected(int profile, BluetoothProfile proxy) {
//                if (profile == BluetoothProfile.HEADSET) {
//                    bluetoothHeadset = (BluetoothHeadset) proxy;
//                }
//            }
//            @Override
//            public void onServiceDisconnected(int profile) {
//                if (profile == BluetoothProfile.HEADSET) {
//                    bluetoothHeadset = null;
//                }
//            }
//        };

//        mBlueAdapter.getProfileProxy(getApplicationContext(), profileListener, BluetoothProfile.HEADSET);
//        mBlueAdapter.closeProfileProxy(BluetoothProfile.HEADSET, bluetoothHeadset);

        if (mBlueAdapter == null) {
            mStatusBlue.setText("Bluetooth is not available");
        } else {
            mStatusBlue.setText("Bluetooth is available");
        }

        // Set image to bluetooth status (on/off)
        if (mBlueAdapter.isEnabled()) {
            mBlueIv.setImageResource(R.drawable.ic_action_on);
        }
        else {
            mBlueIv.setImageResource(R.drawable.ic_action_off);
        }

        // On Btn click
        mOnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBlueAdapter.isEnabled()) {
                    showToast("Turning on bluetooth...");
                } else {
                    showToast("Bluetooth is already on");
                }

            }
        });
        // Off btn click
        mOffBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBlueAdapter.isEnabled()) {
                    mBlueAdapter.disable();
                    showToast("Turning Bluetooth off");
                    mBlueIv.setImageResource(R.drawable.ic_action_off);
                } else {
                    showToast("Bluetooth is already off");
                }
            }
        });
        // Discover bluetooth btn click
        mDiscoverBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBlueAdapter.isDiscovering()) {
                    showToast("Making your device discoverable");
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    startActivityForResult(intent, REQUEST_DISCOVER_BT);
                }
            }
        });
        // Get paired devices btn click
        mPairedBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBlueAdapter.isEnabled()) {
                    mOutput.setText("Paired Devices");
                    Set<BluetoothDevice> devices = mBlueAdapter.getBondedDevices();

                    for (BluetoothDevice device : devices) {
                        ParcelUuid[] uuid = device.getUuids();
                        mOutput.append("\nDevice: " + device.getName());// + "," + device);// uuid"  + uuid[0]
                    }
                } else {
                    // Bluetooth is off so cannot get paired devices
                    showToast("Turn on bluetooth to get paired devices");
                }
            }
        });
        mWorkn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBlueAdapter.isEnabled()) {
                    if (connectThread != null) {
                        connectThread.cancel();
                        connectThread = null;
                    }
                    if (accThread == null) {
                        accThread = new AcceptThread();
                        accThread.start();
                    }
                    connectThread = new ConnectThread(mBlueAdapter.getRemoteDevice("90:7A:58:2F:28:C5"));//00:B4:F5:29:44:FC"));
                    connectThread.start();
                }
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK) {
                    // Bluetooth is on
                    mBlueIv.setImageResource(R.drawable.ic_action_on);
                    showToast("Bluetooth is on");
                } else {
                    // User denied to turn on bluetooth
                    showToast("Couldn't turn on bluetooth");
                }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    // Toast message function
    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;
        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            BluetoothServerSocket tmp = null;
            // MY_UUID is the app's UUID string, also used by the client code.
            try {
                tmp = mBlueAdapter.listenUsingRfcommWithServiceRecord(getApplicationContext().getPackageName(), UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));
                //pink headphones uuid : 00001108-0000-1000-8000-00805f9b34fb"));
            } catch (IOException e) {
                Log.e(TAG, "Error", e);
            }
            mmServerSocket = tmp;
        }
        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned.
            try {
                socket = mmServerSocket.accept();
            } catch (IOException e) {
                Log.e(TAG, "Error", e);
            }
            if (socket != null) {
                Connected(socket, mmDevice);
            }
        }
        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error: Could not close the connect socket", e);
            }
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket to connect with the given BluetoothDevice.
            // MY_UUID is the app's UUID string, also used in the server code.
            try {

                tmp = device.createRfcommSocketToServiceRecord(uuid);//UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));//00001108-0000-1000-8000-00805f9b34fb"));
            } catch (IOException e) {
                Log.e(TAG, "Error", e);
            }
            mmSocket = tmp;
        }
        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            mBlueAdapter.cancelDiscovery(); //its buggy so recommended to take it off
            try {
                mmSocket.connect();
            } catch (IOException connectException) {
                try {
                    mmSocket.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error:Could not close the connect socket", e);
                }
                return;
            }
            mOutput.post(new Runnable() {
                public void run() {
                    String msg = "Success";
                    mOutput.setText(msg);
                }
            });
            Connected(mmSocket, mmDevice);
        }
        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error:Could not close socket", e);
            }
        }
    }
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private class SuccessConnect extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInput;
        //private final OutputStream mmOutput;

        public SuccessConnect(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            //OutputStream tmpOut = null;
            try {
                tmpIn = mmSocket.getInputStream();
                //tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error", e);
            }
            mmInput = tmpIn;
           // mmOutput = tmpOut;
        }
        public void run() {
            mOutput.post(new Runnable() {
                public void run() {
                    String msg = "Connected";
                    mOutput.setText(msg);
                }
            });
            //Should get data here
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error: Could not close socket", e);
            }
        }
    }
    private void Connected(BluetoothSocket mmSocket, BluetoothDevice mmDevice) {
        mmConnected = new SuccessConnect(mmSocket);
        mmConnected.start();
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////

//    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
//    private void dataRx(){
//        mMedia = new MediaSession(this, "MyMediaSession");
//
//        // Overridden methods in the MediaSession.Callback class.
//        mMedia.setCallback(new MediaSession.Callback()
//        {
//            @Override
//            public boolean onMediaButtonEvent (Intent mediaButtonIntent){
//                Log.d(TAG, "onMediaButtonEvent called: " + mediaButtonIntent);
//                KeyEvent ke = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
//                if (ke != null && ke.getAction() == KeyEvent.ACTION_DOWN) {
//                    int keyCode = ke.getKeyCode();
//                    Log.d(TAG, "onMediaButtonEvent Received command: " + ke);
//                }
//                return super.onMediaButtonEvent(mediaButtonIntent);
//            }
//            @Override
//            public void onSkipToNext () {
//                Log.d(TAG, "onSkipToNext called (media button pressed)");
//                Toast.makeText(getApplicationContext(), "onSkipToNext called", Toast.LENGTH_SHORT).show();
//                //skipToNextPlaylistItem(); // Handle this button press.
//                super.onSkipToNext();
//            }
//            @Override
//            public void onSkipToPrevious () {
//                Log.d(TAG, "onSkipToPrevious called (media button pressed)");
//                Toast.makeText(getApplicationContext(), "onSkipToPrevious called", Toast.LENGTH_SHORT).show();
//                //skipToPreviousPlaylistItem(); // Handle this button press.
//                super.onSkipToPrevious();
//            }
//            @Override
//            public void onPause () {
//                Log.d(TAG, "onPause called (media button pressed)");
//                Toast.makeText(getApplicationContext(), "onPause called", Toast.LENGTH_SHORT).show();
//                //mpPause(); // Pause the player.
//                super.onPause();
//            }
//            @Override
//            public void onPlay () {
//                Log.d(TAG, "onPlay called (media button pressed)");
//                //mpStart(); // Start player/playback.
//                super.onPlay();
//            }
//        });
//        mMedia.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
//        mMedia.setActive(true);
//    }
}