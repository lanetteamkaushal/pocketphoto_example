package com.lge.pocketphoto.sample;

import com.lge.pocketphoto.bluetooth.*;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.app.Activity;
import android.content.Intent;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import android.annotation.TargetApi;

import android.os.Build;
import android.os.Bundle;
import android.os.Environment;

import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    String imgUri;
    Uri imgUri1;
    private BluetoothFileTransfer mBluetoothFileTransfer = null;
    private PrintProgressAsync mProgress;

    public static final int BLUETOOTH_RESPONSE_TARGET_BUSY = 1;

    // Device Error - Paper Jam
    public static final int BLUETOOTH_RESPONSE_TARGET_JAM = 2;

    // Device Error -Paper Empty
    public static final int BLUETOOTH_RESPONSE_TARGET_EMPTY = 3;

    // Device Error - Wrong Paper
    public static final int BLUETOOTH_RESPONSE_TARGET_WRONG_PAPER = 4;

    // Device Error - Data Error
    public static final int BLUETOOTH_RESPONSE_TARGET_DATA_ERROR = 5;

    // Device Error - Cover Opened
    public static final int BLUETOOTH_RESPONSE_TARGET_COVER_OPEN = 6;

    // Device Error - System Error
    public static final int BLUETOOTH_RESPONSE_TARGET_SYSTEM_ERROR = 7;

    // Device Error - Low Battery
    public static final int BLUETOOTH_RESPONSE_TARGET_BATTERY_LOW = 8;

    // Device Error - High Temperature
    public static final int BLUETOOTH_RESPONSE_TARGET_HIGH_TEMPERATURE = 10;

    // Device Error - Low Temperature
    public static final int BLUETOOTH_RESPONSE_TARGET_LOW_TEMPERATURE = 11;

    // Device Error - Cooling Mode
    public static final int BLUETOOTH_RESPONSE_TARGET_COOLING_MODE = 22;

    private static final int REQUEST_ENABLE_BT = 0;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    BluetoothAdapter mBluetoothAdapter;
    BluetoothDevice mDevice;
    BluetoothSocket mSocket;
    ConnectedThread th;
    File f;
    ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //	Button for selection image to print
        Button mButton = (Button) findViewById(R.id.button1);
        mButton.setOnClickListener(
                new OnClickListener() {
                    public void onClick(View v) {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/png");
                        startActivityForResult(intent, 0);
                    }
                });

        progress = (ProgressBar) findViewById(R.id.idProgressBar);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(getApplicationContext(), "Bluetooth not available", Toast.LENGTH_LONG).show();
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

    }

    //	After select image to print, this method is called
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (0) {
            case 0:
                try {
                    imgUri1 = data.getData();
                    imgUri = getRealPathFromURI(imgUri1 = data.getData());
                    Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                    if (pairedDevices.size() > 0) {
                        // Loop through paired devices
                        for (BluetoothDevice device : pairedDevices) {

                            if (!TextUtils.isEmpty(device.getName()) && device.getName().contains("Polaroid")) {
                                mDevice = device;
                                connectSocket();
                            }
                        }
                    }

                    /*******************************************/

                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }


    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    public String getRealPathFromURI(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
        return cursor.getString(idx);
    }

    void sendFailState(int error) {
        String errStr = null;
        Log.d(TAG, "sendFailState: Error code :"+error);
        switch (error) {

            case MainActivity.BLUETOOTH_RESPONSE_TARGET_BUSY:
                errStr = "BUSY";
                break;

            case MainActivity.BLUETOOTH_RESPONSE_TARGET_JAM:
                errStr = "DATA ERROR";
                break;

            case MainActivity.BLUETOOTH_RESPONSE_TARGET_EMPTY:
                errStr = "PAPER EMPTY";
                break;

            case MainActivity.BLUETOOTH_RESPONSE_TARGET_WRONG_PAPER:
                errStr = "PAPER MISMATCH";
                break;

            case MainActivity.BLUETOOTH_RESPONSE_TARGET_DATA_ERROR:
                errStr = "DATA ERROR";
                break;

            case MainActivity.BLUETOOTH_RESPONSE_TARGET_COVER_OPEN:
                errStr = "COVER OPEN";
                break;

            case MainActivity.BLUETOOTH_RESPONSE_TARGET_SYSTEM_ERROR:
                errStr = "SYSTEM ERROR";
                break;

            case MainActivity.BLUETOOTH_RESPONSE_TARGET_BATTERY_LOW:
                errStr = "BATTERY LOW";
                break;

            case MainActivity.BLUETOOTH_RESPONSE_TARGET_HIGH_TEMPERATURE:
                errStr = "HIGH TEMPERATURE";
                break;

            case MainActivity.BLUETOOTH_RESPONSE_TARGET_LOW_TEMPERATURE:
                errStr = "LOW TEMPERATURE";
                break;

            case MainActivity.BLUETOOTH_RESPONSE_TARGET_COOLING_MODE:
                errStr = "HIGH TEMPERATURE";
                break;

        }

        Toast.makeText(MainActivity.this, "PocketPhoto Error state : " + errStr, Toast.LENGTH_LONG).show();
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Opptransfer.BLUETOOTH_SOCKET_CONNECTED:
                    break;

                case Opptransfer.BLUETOOTH_SOCKET_CONNECT_FAIL:
                    // Connect Fail -> Try to find new device
                    if (mProgress != null) mProgress.setProgress(0);
                    Toast.makeText(MainActivity.this, "Cannot connect to paired device.", Toast.LENGTH_LONG).show();
                    mBluetoothFileTransfer.startDiscovery();
                    break;

                // Cancel while transfer image data via Bluetooth or Receive device error
                case Opptransfer.BLUETOOTH_SEND_FAIL:
                    sendFailState((int) msg.arg1);
                    mBluetoothFileTransfer = null;
                    if (mProgress != null) mProgress.setProgress(0);
                    break;

                // Sending image data via Bluetooth
                case Opptransfer.BLUETOOTH_SEND_PACKET:
                    int per = (int) ((msg.arg1 / (float) msg.arg2) * 100);
                    if (mProgress != null) mProgress.setProgress(per);
                    break;

                // Complete to send image data
                case Opptransfer.BLUETOOTH_SEND_COMPLETE:
                    mBluetoothFileTransfer = null;
                    Toast.makeText(MainActivity.this, "Send Complete", Toast.LENGTH_LONG).show();
                    break;
            }
        }

    };


    public void connectSocket() {
        ConnectThread thconnect = new ConnectThread(mDevice);
        thconnect.start();


    }

    public void clickme(View v) {

        long len = 0;
        f = new File(imgUri);

        len = f.length();

        if (th != null) {
            th.write(getPrintReadyInt(len));
        } else {
            Log.e(TAG, "Thread is null");
        }
    }


    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
            }
            mmSocket = tmp;
        }

        @TargetApi(Build.VERSION_CODES.KITKAT)
        public void run() {
            // Cancel discovery because it will slow down the connection
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                }
                return;
            }

            // Do work to manage the connection (in a separate thread)
            // manageConnectedSocket(mmSocket);

            th = new ConnectedThread(mmSocket);
            th.start();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "Socket Created", Toast.LENGTH_LONG).show();
                }
            });

            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


            //  String s = "*CA";
            // byte[] b =s.getBytes(StandardCharsets.US_ASCII);


        }

        /**
         * Will cancel an in-progress connection, and close the socket
         */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }


    public byte[] getPrintReadyInt(long paramInt) {

        byte[] b = new byte[42];


        //  m_nPacket[0] = 27;
       /* int i = 27;
        b[0] = (byte)27;
        b[1] = (byte)42;
        b[2] = (byte)67;

        b[3] = (byte)65;
        b[4] = (byte)0;
        b[5] = (byte)0;
        b[6] = (byte)0;
        b[7] = (byte)0;
        b[8] = (byte)1;
        b[9] = (byte)53;
        b[10] = (byte)126;

        b[11] = (byte)1;
        b[12] = (byte)0;
        b[13] = (byte)0;
        b[14] = (byte)0;
        b[15] = (byte)0;

        Log.v("TAG",b+"");*/
        // int paramInt = 5000;
        byte[] m_nPacket = new byte['Ϩ'];
        m_nPacket[0] = 27;
        m_nPacket[1] = 42;
        m_nPacket[2] = 67;
        m_nPacket[3] = 65;
        m_nPacket[4] = 0;
        m_nPacket[5] = 0;
        m_nPacket[6] = 0;
        m_nPacket[7] = 0;
        m_nPacket[8] = ((byte) ((0xFF0000 & paramInt) >> 16));
        m_nPacket[9] = ((byte) ((0xFF00 & paramInt) >> 8));
        m_nPacket[10] = (byte) (paramInt & 0xFF);
        m_nPacket[11] = 0;
        m_nPacket[12] = 0;
        m_nPacket[13] = 0;
        m_nPacket[14] = 0;
        m_nPacket[15] = 0;


        return m_nPacket;
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;

        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()
            Log.v("TAG", "Thread Started... You can write the data");
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // construct a string from the valid bytes in the buffer
                    printMessage(bytes, -1, buffer);
                    // Send the obtained bytes to the UI activity
                    Log.v("TAG", "Bytes read -- > " + buffer);

                    if (buffer[6] == 0x04 && buffer[8] == 0x00) {

                        Log.v("TAG", "Inside over here");
                        Looper.prepare();
                        mBluetoothFileTransfer = new BluetoothFileTransfer(MainActivity.this, null, imgUri1, mHandler);
                        mProgress = new PrintProgressAsync(progress,
                                null);
                        mProgress.execute();
                        mBluetoothFileTransfer.checkBluetooth();

                        //Send Print

                       /* int size = (int) f.length();
                        byte[] bytes1 = new byte[size];
                        try {
                            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(f));
                            long size1  = f.length();
                            buf.read(bytes1);
                            int offset = 0;


                            write(bytes1);

                           // }
                            buf.close();
                        } catch (FileNotFoundException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }*/
                    }

                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }

    private void printMessage(int bytes, int i, byte[] buffer) {
        byte[] readBuf = (byte[]) buffer;
        String readMessage = new String(readBuf, 0, bytes);
        Log.d(TAG, "printMessage: " + readMessage + ":" + bytes);
    }


}
