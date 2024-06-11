package com.example.led_control_application_3;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;


class NetworkUtils {

    public static String sendPostRequest(String requestURL, String payload) {
        HttpsURLConnection urlConnection = null;
        BufferedReader reader = null;
        BufferedWriter writer = null;

        try {
            // Create the URL object
            URL url = new URL(requestURL);

            // Open the connection
            urlConnection = (HttpsURLConnection) url.openConnection();

            // Set the request method to POST
            urlConnection.setRequestMethod("POST");

            // Set headers if necessary
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty("Accept", "application/json");

            // Enable input and output streams
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);

            // Write the payload to the output stream
            OutputStream outputStream = urlConnection.getOutputStream();
            writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
            writer.write(payload);
            writer.flush();
            writer.close();
            outputStream.close();

            // Connect to the server
            urlConnection.connect();

            // Get the response code
            int responseCode = urlConnection.getResponseCode();
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                // Read the response
                reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            } else {
                return "Error: " + responseCode;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Exception: " + e.getMessage();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            try {
                if (reader != null) {
                    reader.close();
                }
                if (writer != null) {
                    writer.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

class HotspotHelper {
    public static WifiConfiguration getWifiApConfiguration(Context context) {
        try {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            Method method = wifiManager.getClass().getMethod("getWifiApConfiguration");
            return (WifiConfiguration) method.invoke(wifiManager);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String[] printHotspotInfo(Context context) {
        WifiConfiguration config = getWifiApConfiguration(context);
        if (config != null) {
            System.out.println("SSID: " + config.SSID);
            System.out.println("Password: " + config.preSharedKey);
            return new String[]{config.SSID, config.preSharedKey};
        } else {
            System.out.println("Unable to retrieve hotspot configuration.");
            return new String[]{"", ""};
        }
    }
}

public class WifiExchange {

    private String name;
    private String password;
    private Context context;
    private BluetoothLECrypted ble;

    public WifiExchange(Context context) {
        String[] data = HotspotHelper.printHotspotInfo(context);
        this.name = data[0];
        this.password = data[1];
        this.context = context;
        this.ble = new BluetoothLECrypted(context);
    }

    public void sendInfoThroughBLE(BluetoothGattCharacteristic mmCharacteristic, BluetoothGatt mmGatt) throws IOException {
        Log.d("PAIRED DEVICES", "message sent via ble.");
        mmCharacteristic.setValue(this.ble.encrypt("SSID:" + this.name + ",PASSWORD:" + this.password));
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED  && ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mmGatt.writeCharacteristic(mmCharacteristic);
    }

    public void sendInfoThroughWifi(String messageToSend){

        class SendPostRequestTask extends AsyncTask<String, Void, String> {
            @Override
            protected String doInBackground(String... params) {
                String url = params[0];
                String payload = params[1];
                return NetworkUtils.sendPostRequest(url, payload);
            }

            @Override
            protected void onPostExecute(String result) {
                // Handle the result here (update UI, show message, etc.)
                System.out.println("Response: " + result);
            }
        }


        String url = "https://127.0.0.1/";
        String payload = this.ble.encrypt(messageToSend);
        new SendPostRequestTask().execute(url, payload);
    }

}
