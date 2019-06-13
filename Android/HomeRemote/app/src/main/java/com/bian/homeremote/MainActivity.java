package com.bian.homeremote;

import android.os.Bundle;
import android.speech.tts.Voice;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.bian.homeremote.fragments.ManualFragment;
import com.bian.homeremote.fragments.SettingFragment;
import com.bian.homeremote.fragments.VoiceFragment;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {

    private FragmentManager fragmentManager;
    private FragmentTransaction transaction;

    private ManualFragment manual;
    private VoiceFragment voice;
    private SettingFragment setting;

    private MqttAndroidClient client;
    private String host;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initFragments();
        initMQTT();

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {


        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {

            // init fragment manager, begin transaction
            fragmentManager = getSupportFragmentManager();
            transaction = fragmentManager.beginTransaction();

            switch (item.getItemId()) {
                case R.id.navigation_manual:
                    transaction.replace(R.id.content, manual).commit();
                    return true;
                case R.id.navigation_voice:
                    transaction.replace(R.id.content, voice).commit();
                    return true;
                case R.id.navigation_settings:
                    transaction.replace(R.id.content, setting).commit();
                    return true;
            }
            return false;
        }

    };



    private void initFragments(){
        // init fragment manager, begin transaction
        fragmentManager = getSupportFragmentManager();
        transaction = fragmentManager.beginTransaction();

        // instantiation
        manual = new ManualFragment();
        voice = new VoiceFragment();
        setting = new SettingFragment();

        // open with manual fragment
        transaction.replace(R.id.content, manual).commit();
    }

    private void initMQTT() {
        host = getString(R.string.mosquitto_host);
        String clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(), host, clientId);

        try {
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // connected
                    Toast.makeText(MainActivity.this, "MQTT 加载成功", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(MainActivity.this, "MQTT 加载失败", Toast.LENGTH_LONG).show();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void publishMQTT(String topic, String payload) {
        byte[] encodedPayload = new byte[0];
        try {
            encodedPayload = payload.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            client.publish(topic, message);
            Toast.makeText(MainActivity.this, "MQTT: "+payload+" published!", Toast.LENGTH_LONG).show();
        } catch (MqttException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }



}
