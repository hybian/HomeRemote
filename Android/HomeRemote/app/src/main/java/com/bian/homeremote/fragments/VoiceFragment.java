package com.bian.homeremote.fragments;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.idst.nls.NlsClient;
import com.alibaba.idst.nls.NlsListener;
import com.alibaba.idst.nls.StageListener;
import com.alibaba.idst.nls.internal.protocol.NlsRequest;
import com.alibaba.idst.nls.internal.protocol.NlsRequestProto;
import com.bian.homeremote.MainActivity;
import com.bian.homeremote.R;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;


public class VoiceFragment extends Fragment{

    private Button speakBtn;
    private TextView text0, text1, text2, text3;
    private String testResult = "";
    private Handler repeatUpdateHandler = new Handler();
    private boolean isSpeaking = false;

    //=======================================
    private boolean isRecognizing = false;
    private NlsClient mNlsClient;
    private NlsRequest mNlsRequest;
    private Context context;
    //=======================================


    public VoiceFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_voice, container, false);

        speakBtn = (Button) view.findViewById(R.id.speakBtn);
        text0 = (TextView) view.findViewById(R.id.text0);
        text1 = (TextView) view.findViewById(R.id.text1);
        text2 = (TextView) view.findViewById(R.id.text2);
        text3 = (TextView) view.findViewById(R.id.text3);

        context = getActivity().getApplicationContext();
        String appkey = "nls-service"; //请设置申请到的Appkey
        mNlsRequest = initNlsRequest();
        mNlsRequest  = initNlsRequest();
        mNlsRequest.setApp_key(appkey);    //appkey请从 "快速开始" 帮助页面的appkey列表中获取
        mNlsRequest.setAsr_sc("opu");      //设置语音格式

        NlsClient.openLog(true);
        NlsClient.configure(getActivity().getApplicationContext()); //全局配置
        mNlsClient = NlsClient.newInstance(getActivity(), mRecognizeListener, mStageListener,mNlsRequest); //实例化NlsClient

        mNlsClient.setMaxRecordTime(60000);  //设置最长语音
        mNlsClient.setMaxStallTime(1000);    //设置最短语音
        mNlsClient.setMinRecordTime(1500);    //设置最大录音中断时间
        mNlsClient.setRecordAutoStop(false);  //设置VAD
        mNlsClient.setMinVoiceValueInterval(200); //设置音量回调时长


        speakBtn.setOnLongClickListener(
                new View.OnLongClickListener(){
                    public boolean onLongClick(View arg0) {

                        isRecognizing = true;
                        text0.setText("正在录音，请稍候！");
                        mNlsRequest.authorize(getString(R.string.aliyun_key), getString(R.string.aliyun_secret_key)); //请替换为用户申请到的数加认证key和密钥
                        mNlsClient.start();
                        isSpeaking = true;
                        //repeatUpdateHandler.post( new RptUpdater() );
                        return false;
                    }
                }
        );
        speakBtn.setOnTouchListener( new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if( (event.getAction()==MotionEvent.ACTION_UP || event.getAction()==MotionEvent.ACTION_CANCEL)
                        && isSpeaking ){
                    isSpeaking = false;
                    isRecognizing = false;
//                    text.setText("");
                    mNlsClient.stop();
                }
                return false;
            }
        });

        return view;
    }

    private NlsRequest initNlsRequest(){
        NlsRequestProto proto = new NlsRequestProto(context);
        return new NlsRequest(proto);
    }

    private NlsListener mRecognizeListener = new NlsListener() {

        @Override
        public void onRecognizingResult(int status, RecognizedResult result) {
            switch (status) {
                case NlsClient.ErrorCode.SUCCESS:
                    try{
                        JSONObject jsonObject = new JSONObject(result.asr_out);
                        testResult = jsonObject.getString("result");
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    text1.setText(testResult);
                    commandResolving(testResult);
                    break;
                case NlsClient.ErrorCode.RECOGNIZE_ERROR:
                    Toast.makeText(getContext(), "recognizer error", Toast.LENGTH_LONG).show();
                    break;
                case NlsClient.ErrorCode.RECORDING_ERROR:
                    Toast.makeText(getContext(),"recording error",Toast.LENGTH_LONG).show();
                    break;
                case NlsClient.ErrorCode.NOTHING:
                    Toast.makeText(getContext(),"nothing",Toast.LENGTH_LONG).show();
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

    private void commandResolving(String command){

        Resources resources = getResources();
        String[] power = resources.getStringArray(R.array.power);
        String[] channel = resources.getStringArray(R.array.channel);
        String[] volume = resources.getStringArray(R.array.volume);

        // 调台
        if( command.matches(".*\\d+.*") ) {
            int channelNum = Integer.parseInt(command.replaceAll("[\\D]", ""));
            text3.setText(String.valueOf(channelNum));
            ((MainActivity)getActivity()).publishMQTT("Kaiyuan/jidinghe", String.valueOf(channelNum));
        }
        else if ( command.contains("主页") ){
            text3.setText("主页");
            ((MainActivity)getActivity()).publishMQTT("Kaiyuan/jidinghe", getString(R.string.home));
        }
        else if( command.contains("菜单") ){ //菜单
            text3.setText("菜单");
            ((MainActivity)getActivity()).publishMQTT("Kaiyuan/jidinghe", getString(R.string.menu));
        }
        else if( command.contains("确定") ){ //OK
            text3.setText("确定");
            ((MainActivity)getActivity()).publishMQTT("Kaiyuan/jidinghe", getString(R.string.OK));
        }
        else if ( command.contains("返回") || command.contains("取消") ){
            text3.setText("返回");
            ((MainActivity)getActivity()).publishMQTT("Kaiyuan/jidinghe", getString(R.string.back));
        }
        else if ( command.contains("台") || command.contains("节目") ){ //换台
            for(int i=0; i<channel.length; i++){
                String item = channel[i];
                if( command.contains(item) && i<channel.length/2 ){
                    text3.setText("节目+");
                    ((MainActivity)getActivity()).publishMQTT("Kaiyuan/jidinghe", getString(R.string.channelUp));
                    break;
                }else if( command.contains(item) && i>=channel.length/2){
                    text3.setText("节目-");
                    ((MainActivity)getActivity()).publishMQTT("Kaiyuan/jidinghe", getString(R.string.channelDown));
                    break;
                }else if (command.equals("换台")){
                    text3.setText("节目+");
                    ((MainActivity)getActivity()).publishMQTT("Kaiyuan/jidinghe", getString(R.string.channelUp));
                    break;
                }else{}
            }
        }
        else if( command.contains("声") || command.contains("音") ){ // 音量
            for(int i=0; i<volume.length; i++){
                String item = volume[i];
                if( command.contains(item) && i<volume.length/2 ){
                    text3.setText("音量+");
                    ((MainActivity)getActivity()).publishMQTT("Kaiyuan/jidinghe", getString(R.string.volumeUp));
                    break;
                }else if( command.contains(item) && i>=volume.length/2){
                    text3.setText("音量-");
                    ((MainActivity)getActivity()).publishMQTT("Kaiyuan/jidinghe", getString(R.string.volumeDown));
                    break;
                }else if (command.contains("静")){
                    text3.setText("静音");
                    ((MainActivity)getActivity()).publishMQTT("Kaiyuan/jidinghe", getString(R.string.mute));
                    break;
                }else{}
            }
        }
        else if( command.contains("上") || command.contains("下") || command.contains("左") || command.contains("右") ){ // 方向键
            if( command.contains("上") ){
                text3.setText("⬆");
                ((MainActivity)getActivity()).publishMQTT("Kaiyuan/jidinghe", getString(R.string.up));
            } else if( command.contains("下") ){
                text3.setText("⬇");
                ((MainActivity)getActivity()).publishMQTT("Kaiyuan/jidinghe", getString(R.string.down));
            } else if( command.contains("左") ){
                text3.setText("⬅");
                ((MainActivity)getActivity()).publishMQTT("Kaiyuan/jidinghe", getString(R.string.left));
            } else{
                text3.setText("➡");
                ((MainActivity)getActivity()).publishMQTT("Kaiyuan/jidinghe", getString(R.string.right));
            }
        }
        else { // 电源
            for(int i=0; i<power.length; i++){
                String item = power[i];
                if( command.contains(item) ){
                    text3.setText("power");
                    ((MainActivity)getActivity()).publishMQTT("Kaiyuan/jidinghe", getString(R.string.power));
                }
            }
        }







    }

//    public void postResult(){
//        mNlsClient.start();
//        text.setText( testResult );
//        Log.d("WTF", testResult);
//        isRecognizing = false;
//        text.setText("");
//        mNlsClient.stop();
//    }

//    private class RptUpdater implements Runnable {
//        public void run() {
//            while( isSpeaking ){
//                postResult();
//                repeatUpdateHandler.postDelayed( new RptUpdater(), 1500 );
//            }
//        }
//    }






}
