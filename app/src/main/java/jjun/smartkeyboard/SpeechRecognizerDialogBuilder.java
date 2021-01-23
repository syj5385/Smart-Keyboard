package jjun.smartkeyboard;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class SpeechRecognizerDialogBuilder extends AlertDialog.Builder{
    public static final String RESULT_RECOGNIZER = "result_recognizer";
    private String TAG = "Recognizer";

    private SpeechRecognizer mRecognizer;

    private Context context;

    public SpeechRecognizerDialogBuilder(Context context) {
        super(context);
        this.context = context;
        initializeVoiceDialog();
        initializeVoiceRecognizer();
    }

    private TextView title, content;

    private void initializeVoiceDialog(){
        LinearLayout layout = (LinearLayout)View.inflate(context,R.layout.voicelayout,null);
        layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        setView(layout);

        title = layout.findViewById(R.id.info);
        content = layout.findViewById(R.id.content);


    }

    private void setContent(String content_string){
        try{
            content.setText(content_string);
        }
        catch (NullPointerException e){
            e.printStackTrace();
        }
    }

    private void initializeVoiceRecognizer(){
        setContent("말씀하세요.");
        if(mRecognizer == null){
            mRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            mRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    Log.d(TAG, "onReadyForSpeech");
                }

                @Override
                public void onBeginningOfSpeech() {

                }

                @Override
                public void onRmsChanged(float rmsdB) {

                }

                @Override
                public void onBufferReceived(byte[] buffer) {

                }

                @Override
                public void onEndOfSpeech() {
                    Log.d(TAG, "onEndOfSpeech");

                }

                @Override
                public void onError(int error) {

                }

                @Override
                public void onResults(Bundle results) {
                    Log.d(TAG, "onResult");
                    ArrayList<String> result  = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    setContent(result.get(0));
                    Intent intent = new Intent(RESULT_RECOGNIZER);
                    intent.putExtra(RESULT_RECOGNIZER,result.get(0));
                    context.sendBroadcast(intent);
                }

                @Override
                public void onPartialResults(Bundle partialResults) {

                }

                @Override
                public void onEvent(int eventType, Bundle params) {

                }
            });
        }
        Intent intent = new Intent(RecognizerIntent.ACTION_WEB_SEARCH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,context.getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"en-es");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,2);
        mRecognizer.startListening(intent);
    }

}
