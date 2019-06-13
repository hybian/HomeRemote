package com.alibaba.idst.nlsdemo;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.alibaba.idst.R;
import com.alibaba.idst.nls.NlsClient;
import com.alibaba.idst.nls.NlsListener;
import com.alibaba.idst.nls.StageListener;
import com.alibaba.idst.nls.internal.protocol.NlsRequest;
import com.alibaba.idst.nls.internal.protocol.NlsRequestProto;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

import static android.R.attr.text;

public class PublicAsrActivity extends Activity {

    private boolean isRecognizing = false;
    private EditText mFullEdit;
    private EditText mResultEdit;
    private Button mStartButton;
    private Button mStopButton;
    private NlsClient mNlsClient;
    private NlsRequest mNlsRequest;
    private Context context;
    private boolean mSpeak = false;

    private MqttAndroidClient client;
    private String host = "tcp://test.mosquitto.org:1883";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_public_asr);
        context = getApplicationContext();
        mFullEdit = (EditText) findViewById(R.id.editText2);
        mResultEdit = (EditText) findViewById(R.id.editText);
        mStartButton = (Button) findViewById(R.id.button);
        mStopButton = (Button) findViewById(R.id.button2);

        String appkey = "nls-service"; //请设置申请到的Appkey
        mNlsRequest = initNlsRequest();
        mNlsRequest  = initNlsRequest();
        mNlsRequest.setApp_key(appkey);    //appkey请从 "快速开始" 帮助页面的appkey列表中获取
        mNlsRequest.setAsr_sc("opu");      //设置语音格式
        
        /*设置热词相关属性*/
        //mNlsRequest.setAsrVocabularyId("vocabid");
        /*设置热词相关属性*/

        NlsClient.openLog(true);
        NlsClient.configure(getApplicationContext()); //全局配置
        mNlsClient = NlsClient.newInstance(this, mRecognizeListener, mStageListener,mNlsRequest);                          //实例化NlsClient

        mNlsClient.setMaxRecordTime(60000);  //设置最长语音
        mNlsClient.setMaxStallTime(1000);    //设置最短语音
        mNlsClient.setMinRecordTime(500);    //设置最大录音中断时间
        mNlsClient.setRecordAutoStop(false);  //设置VAD
        mNlsClient.setMinVoiceValueInterval(200); //设置音量回调时长

        initMQTT();

        initStartRecognizing();
        initStopRecognizing();
    }

    private NlsRequest initNlsRequest(){
        NlsRequestProto proto = new NlsRequestProto(context);
        //proto.setApp_user_id(""); //设置在应用中的用户名，可选
        return new NlsRequest(proto);

    }

//    private void initStartRecognizing(){
//        mStartButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                isRecognizing = true;
//                mResultEdit.setText("正在录音，请稍候！");
//                mNlsRequest.authorize("LTAIOtruVDqOxmrm", "wjR56qB64cjJbVCW4Oime53EkAtLzT"); //请替换为用户申请到的数加认证key和密钥
//                mNlsClient.start();
//                mStartButton.setText("录音中。。。");
//            }
//        });
//    }
        private void initStartRecognizing(){
            mStartButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    isRecognizing = true;
                    mResultEdit.setText("正在录音，请稍候！");
                    mNlsRequest.authorize("LTAIOtruVDqOxmrm", "wjR56qB64cjJbVCW4Oime53EkAtLzT"); //请替换为用户申请到的数加认证key和密钥
                    mNlsClient.start();
                    mStartButton.setText("录音中。。。");
                    mSpeak = true;
                    return false;
                }
            });
        }


//    private void initStopRecognizing(){
//        mStopButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                isRecognizing = false;
//                mResultEdit.setText("");
//                mNlsClient.stop();
//                mStartButton.setText("开始 录音");
//
//            }
//        });
//    }
    private void initStopRecognizing(){
        mStartButton.setOnTouchListener( new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if( (event.getAction()==MotionEvent.ACTION_UP || event.getAction()==MotionEvent.ACTION_CANCEL)
                        && mSpeak ){
                    mSpeak = false;
                    isRecognizing = false;
                    mNlsClient.stop();
                }
                return false;
            }
        });
    }

    private NlsListener mRecognizeListener = new NlsListener() {

        @Override
        public void onRecognizingResult(int status, RecognizedResult result) {
            //Toast.makeText(PublicAsrActivity.this, String.valueOf(status), Toast.LENGTH_LONG).show();
            switch (status) {
                case NlsClient.ErrorCode.SUCCESS:

                    //==========================================================
                    String testResult = "";
                    try{
                        JSONObject jsonObject = new JSONObject(result.asr_out);
                        testResult = jsonObject.getString("result");
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    if (testResult.equals("电源")){
                        //testMQTT("Kaiyuan/jidinghe", testResult);
                        testMQTT("Kaiyuan/jidinghe", "power");
                        Toast.makeText(PublicAsrActivity.this, "电源", Toast.LENGTH_LONG).show();
                    }
                    //==========================================================

                    Log.i("asr", "[demo]  callback onRecognizResult " + result.asr_out);
                    mResultEdit.setText(result.asr_out);
                    mFullEdit.setText(testResult);
                    //mFullEdit.setText(result.results);
                    break;
                case NlsClient.ErrorCode.RECOGNIZE_ERROR:
                    Toast.makeText(PublicAsrActivity.this, "recognizer error", Toast.LENGTH_LONG).show();
                    break;
                case NlsClient.ErrorCode.RECORDING_ERROR:
                    Toast.makeText(PublicAsrActivity.this,"recording error",Toast.LENGTH_LONG).show();
                    break;
                case NlsClient.ErrorCode.NOTHING:
                    Toast.makeText(PublicAsrActivity.this,"nothing",Toast.LENGTH_LONG).show();
                    break;
            }
            isRecognizing = false;
        }


    } ;

    private StageListener mStageListener = new StageListener() {
        @Override
        public void onStartRecognizing(NlsClient recognizer) {
            super.onStartRecognizing(recognizer);    //To change body of overridden methods use File | Settings | File Templates.
        }

        @Override
        public void onStopRecognizing(NlsClient recognizer) {
            super.onStopRecognizing(recognizer);    //To change body of overridden methods use File | Settings | File Templates.
        }

        @Override
        public void onStartRecording(NlsClient recognizer) {
            super.onStartRecording(recognizer);    //To change body of overridden methods use File | Settings | File Templates.
        }

        @Override
        public void onStopRecording(NlsClient recognizer) {
            super.onStopRecording(recognizer);    //To change body of overridden methods use File | Settings | File Templates.
        }

        @Override
        public void onVoiceVolume(int volume) {
            super.onVoiceVolume(volume);
        }

    };


    private void initMQTT()
    {
        String clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(), host, clientId);

        try {
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    Toast.makeText(PublicAsrActivity.this, "success", Toast.LENGTH_LONG).show();
                    Log.d("WTF", "onSuccess");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Log.d("WTF", "onFailure");
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void testMQTT(String topic, String payload)
    {
        byte[] encodedPayload = new byte[0];
        try {
            encodedPayload = payload.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            client.publish(topic, message);
        } catch (MqttException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }


}
