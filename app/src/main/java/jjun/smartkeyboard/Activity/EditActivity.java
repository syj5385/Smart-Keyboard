package jjun.smartkeyboard.Activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

import jjun.smartkeyboard.BluetoothLeManager;
import jjun.smartkeyboard.KeyboardService;
import jjun.smartkeyboard.R;
import jjun.smartkeyboard.SpeechRecognizerDialogBuilder;

public class EditActivity extends AppCompatActivity {
    private static final String TAG = "EditActivity";
    private KeyboardService service;

    public static final String METHOD_OF_KEYBOARD = "jjun.smartkeyboard.METHOD_OF_KEYBOARD";
    public static final int REQUEST_KEYBOARD = 0;
    public static final int REQUEST_VOICE = 1;

    private int mMode = -1;

    private boolean running = false;

    private EditText paper;
    private TextView numberOfString;
    private TextView currentWriting, thischaracter;
    private InputMethodManager imm;
    private ArrayList<String> writing;
    private ArrayList<Character> sending;
    private TextView bottomText;

    private BtSendThread thread;


    private AlertDialog dialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        getSupportActionBar().hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        writing = new ArrayList<>();
        sending = new ArrayList<>();

        mMode = getIntent().getIntExtra(METHOD_OF_KEYBOARD,-1);
        final TextView modeText = findViewById(R.id.mode);
        ImageView modeImg = findViewById(R.id.mode_img);
        paper = findViewById(R.id.paper);
        imm = (InputMethodManager)EditActivity.this.getSystemService(INPUT_METHOD_SERVICE);
        numberOfString = findViewById(R.id.numOfString);
        currentWriting = findViewById(R.id.current_writing);
        currentWriting.setText("현재 작성 중인 문장 없음.");
        numberOfString.setText("(" + paper.getText().toString().length() + " / 100)");
        thischaracter = findViewById(R.id.thisCharacter);
        bottomText = findViewById(R.id.bottom_text);
        if(mMode == REQUEST_VOICE){
            bottomText.setText("말하기");
        }
        paper.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.length() <= 0) {
                    numberOfString.setText("(0 / 100)");
                    return;
                }

                if (s.charAt(s.length() - 1) == '\n') {
                    String enter = s.toString().split("\n")[0];
                    if(!checkVaildation(enter)){
                        Toast.makeText(EditActivity.this, "영문자만 입력할 수 있습니다.",Toast.LENGTH_SHORT).show();
                        paper.setText("");
                        return;
                    }

                    enter = enter.toLowerCase();
                    Log.d(TAG, "enter : " + enter);
                    if(writing.size()<5) {
                        writing.add(enter);
                        paper.setText("");
                        currentWriting.setText("작성 중인 문장 : " + writing.get(0) + " (" + writing.size() + " / 5)");
                        numberOfString.setText("(0/100)");
                        return;
                    }

                }
                numberOfString.setText("(" + s.length() + " / 100)");

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        if(mMode == REQUEST_KEYBOARD){
            modeImg.setImageDrawable(getResources().getDrawable(R.drawable.keyboard));
            modeText.setText("Keyboard Mode");
            LinearLayout keyboard_up = findViewById(R.id.keyboard_up);
            keyboard_up.setVisibility(View.VISIBLE);


            keyboard_up.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE | WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED);
                    paper.setFocusable(true);

                    imm.showSoftInput(paper,0);
                }
            });
        }
        else if(mMode == REQUEST_VOICE){
            modeImg.setImageDrawable(getResources().getDrawable(R.drawable.voice));
            modeText.setText("Voice Mode");
            paper.setFocusable(false);
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN | WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED);
            LinearLayout keyboard_up = findViewById(R.id.keyboard_up);
            keyboard_up.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder = new SpeechRecognizerDialogBuilder(EditActivity.this);
                    dialog = builder.create();
                    dialog.show();
                }
            });

        }

    }

    private boolean isSending = false;

    private class BlinkThread extends Thread{

        private boolean blink = false;
        private ImageView[] blinkImg   = new ImageView[2];
        public BlinkThread() {
            super();
            Log.d(TAG,"Blink Thread start");
            blink = false;
            blinkImg[0] = findViewById(R.id.leftblink);
            blinkImg[1] = findViewById(R.id.rightblink);
        }

        @Override
        public void run() {
            super.run();
            while(isSending) {
                if (blink) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            blinkImg[0].setVisibility(View.VISIBLE);
                            blinkImg[1].setVisibility(View.INVISIBLE);
                            blink = !blink;

                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            blinkImg[1].setVisibility(View.VISIBLE);
                            blinkImg[0].setVisibility(View.INVISIBLE);
                            blink = !blink;

                        }
                    });
                }
            }
            try{
                this.sleep(500);
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }

    private class BtSendThread extends Thread{
        private String thisString = "";
        private char thischarac;

        public BtSendThread() {
            super();
            Log.d(TAG,"BT Send Thread start!!!");
            running = true;
            if(service.getBle().getState() == BluetoothLeManager.STATE_CONNECTED) {
                service.setEnabledSendChar(true);
            }
        }

        @Override
        public void run() {
            super.run();
            while(running) {

                if (writing.size() > 0) {
                    if (sending.size() == 0) {
                        thisString = writing.remove(0);
                        Log.d(TAG, "thisString : " + thisString);
                        for (int i = 0; i < thisString.length(); i++)
                            sending.add(thisString.charAt(i));
                    }
                }
                if (sending.size() > 0) {
                    isSending = true;
                    if (service.isEnabledSendChar()) {
                        /* Send Next Character to target */

                        thischarac = (char) sending.remove(0);
                        Log.d(TAG, "update Character : " + thischarac);
                        service.sendCharacterToTarget(thischarac);
                        service.setEnabledSendChar(false);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (currentWriting != null && thischaracter != null) {
                                    currentWriting.setText("작성 중인 문장 : " + thisString + "(" + (writing.size() + 1) + "/5)");
                                    thischaracter.setText("'" + thischarac + "'");
                                }
                            }
                        });

                    } else {
                        /* Wait for Acknowledgement for previous character */
                        Log.d(TAG, "Wait for acknowledgement for previous character");
                    }
                }


                if (writing.size() == 0 && sending.size() == 0) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (currentWriting != null)
                                currentWriting.setText("현재 작성 중인 문장 없음.");
                            isSending = false;
                        }
                    });
                }


                try {
                    this.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        setFilter();
//        startService(KeyboardΩoardService.class, KeyboardConnection,null );
        Intent intent = new Intent(EditActivity.this,KeyboardService.class);
        bindService(intent,KeyboardConnection,Context.BIND_AUTO_CREATE);

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
        unbindService(KeyboardConnection);
        running = false;
        isSending = false;
//        writing = null ;
//        sending = null;
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(SpeechRecognizerDialogBuilder.RESULT_RECOGNIZER)){
                String result = intent.getStringExtra(SpeechRecognizerDialogBuilder.RESULT_RECOGNIZER);
                paper.setText(result);
                dialog.dismiss();
                if(!checkVaildation(result)){
                    Toast.makeText(EditActivity.this, "영문자만 입력할 수 있습니다.",Toast.LENGTH_SHORT).show();
                    paper.setText("");
                    return;
                }
                writing.add(result);

            }

        }
    };


    // Service Creation
    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {

        Intent startService = new Intent(this, service);
        if (extras != null && !extras.isEmpty()) {
            Set<String> keys = extras.keySet();
            for (String key : keys) {
                String extra = extras.getString(key);
                startService.putExtra(key, extra);
            }
        }

        Intent bindingIntent = new Intent(this,service);
        bindService(bindingIntent,serviceConnection, Context.BIND_AUTO_CREATE);
    }



    private final ServiceConnection KeyboardConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            service = ((KeyboardService.MainBinder) arg1).getService();

            Log.d(TAG,"Keyboard Service connection : " + String.valueOf(service));
            thread = new BtSendThread();
            thread.start();
            BlinkThread blinkThread = new BlinkThread();
            blinkThread.start();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            service = null;
            Log.e(TAG,"Service Disconnected");
        }
    };

    private void setFilter(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(SpeechRecognizerDialogBuilder.RESULT_RECOGNIZER);

        registerReceiver(mReceiver,filter);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.move_from_top,R.anim.move_to_bottom);
    }

    private boolean checkVaildation(String enter){
        boolean isValid = true;
        for(int i=0; i<enter.length(); i++){
            char thisChar = enter.charAt(i);
            if(!((thisChar >= 'a' && thisChar <= 'z')||(thisChar >= 'A' && thisChar <= 'Z')))
                return false;
        }

        return isValid;
    }

}
