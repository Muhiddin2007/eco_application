package com.example.eco;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import android.content.Intent;
import android.Manifest;


public class MainActivity extends AppCompatActivity {

    private static final int MESSAGE_READ = 1;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private ConnectedThread connectedThread;

    private static int total_sum = 0;

    private static int showCard = 1;

    private TextView countTextViewBottle;

//    private View greetingLayout;
//    private View cardPageLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView sendButton = findViewById(R.id.finishButton);

        connectBluetooth();

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage("0");
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }

        Button captureImageButton = findViewById(R.id.captureImageButton);
        captureImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage("1");
            }
        });

    }

//    private void showCardPage() {
//        greetingLayout.setVisibility(View.GONE);
//        cardPageLayout.setVisibility(View.VISIBLE);
//    }
//    private void closeCardPage(){
//        greetingLayout.setVisibility(View.VISIBLE);
//        cardPageLayout.setVisibility(View.GONE);
//    }

    private void connectBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            showToast("Bluetooth is not supported on this device");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            showToast("Bluetooth is not enabled");
            return;
        }

        String deviceAddress = "00:21:11:01:55:71"; // Replace with your Bluetooth module's MAC address
        bluetoothDevice = bluetoothAdapter.getRemoteDevice(deviceAddress);

        try {
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            bluetoothSocket.connect();
            outputStream = bluetoothSocket.getOutputStream();
            showToast("Connected to Bluetooth device");

            connectedThread = new ConnectedThread(bluetoothSocket);
            connectedThread.start();

//            greetingLayout = findViewById(R.id.greetingLayout);
//            cardPageLayout = findViewById(R.id.cardPageLayout);

            showCard = 0;
        } catch (IOException e) {
            showToast("Failed to connect to Bluetooth device");
            e.printStackTrace();
        }
    }

    private void sendMessage(String message) {
        try {
            outputStream.write(message.getBytes());
            showToast("Message sent: " + message);
        } catch (IOException e) {
            showToast("Failed to send message");
            e.printStackTrace();
        }
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;

            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "Error in InputStream", Toast.LENGTH_SHORT).show();
            }

            mmInStream = tmpIn;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);
                    handler.obtainMessage(MESSAGE_READ, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "Error writing to Arduino", Toast.LENGTH_SHORT).show();
            }
        }
    }



    private final Handler handler = new Handler(new Handler.Callback() {
        public boolean handleMessage(Message msg) {
            String readMessage = (String) msg.obj;
            String[] cardData = readMessage.split("\\|");

// Convert string values to integers
            int countBottle = Integer.parseInt(cardData[0]);
            int countCigarette = Integer.parseInt(cardData[2]);

// Multiply the counts by 150
            int priceBottle = countBottle * 100;
            int priceCigarette = countCigarette * 50;

            total_sum =  priceBottle + priceCigarette;

            TextView countTextViewBottle = findViewById(R.id.countTextViewBottle);
            TextView priceTextViewBottle = findViewById(R.id.priceTextViewBottle);
            countTextViewBottle.setText("Soni: " + countBottle + " ta");
            priceTextViewBottle.setText("Narxi: " + priceBottle + " so’m");

            TextView countTextViewCigarette = findViewById(R.id.countTextViewCigarette);
            TextView priceTextViewCigarette = findViewById(R.id.priceTextViewCigarette);
            countTextViewCigarette.setText("Soni: " + countCigarette + " ta");
            priceTextViewCigarette.setText("Narxi: " + priceCigarette + " so’m");

            TextView totalSumTextView = findViewById(R.id.totalSumTextView);
            totalSumTextView.setText("Jami: " + total_sum + " so’m");

            return true;
        }
    });


    private void showToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            bluetoothSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
