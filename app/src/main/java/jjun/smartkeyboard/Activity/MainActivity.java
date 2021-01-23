package jjun.smartkeyboard.Activity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.inputmethodservice.Keyboard;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

import jjun.smartkeyboard.BluetoothLeManager;
import jjun.smartkeyboard.CustomAdapter3.Custom3_Item;
import jjun.smartkeyboard.CustomAdapter3.CustomAdapter3;
import jjun.smartkeyboard.KeyboardService;
import jjun.smartkeyboard.R;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private LinearLayout voice, keyboard;
    private ImageView bluetooth;

    private KeyboardService service;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_main);
        getSupportActionBar().hide();
        initializeView();
        if(!checkPermission()){
            Toast.makeText(getApplicationContext(),"사용 권한을 승인하지 않아 어플리케이션을 종료합니다.",Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeView(){
        voice = findViewById(R.id.voice);
        keyboard = findViewById(R.id.keyboard);
        View.OnClickListener viewClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(service.getBle().getState() == BluetoothLeManager.STATE_CONNECTED) {
                    Intent intent = new Intent(MainActivity.this, EditActivity.class);
                    switch (v.getId()) {
                        case R.id.voice:
                            intent.putExtra(EditActivity.METHOD_OF_KEYBOARD, EditActivity.REQUEST_VOICE);
                            break;

                        case R.id.keyboard:
                            intent.putExtra(EditActivity.METHOD_OF_KEYBOARD, EditActivity.REQUEST_KEYBOARD);
                            break;
                    }

                    startActivity(intent);
                    overridePendingTransition(R.anim.move_from_bottom, R.anim.move_to_top);
                }
                else{
                    Toast.makeText(getApplicationContext(),"블루투스를 연결해 주세요.",Toast.LENGTH_SHORT).show();
                }
            }
        };
        voice.setOnClickListener(viewClickListener);
        keyboard.setOnClickListener(viewClickListener);

        bluetooth = findViewById(R.id.bluetooth);
        bluetooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(service == null){
                    return;
                }
                else{
                    if(service.getBle().getState()==BluetoothLeManager.STATE_CONNECTED){
                        BluetoothLeManager manager = service.getBle();
                        manager.disconnect();
                        Toast.makeText(MainActivity.this,"연결을 해제 중입니다.",Toast.LENGTH_SHORT).show();
                    }
                    else{
                        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                        if(adapter == null){
                            Toast.makeText(getApplicationContext(),"블루투스를 지원하지 않습니다.",Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if(adapter.isEnabled() == false){
                            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            startActivityForResult(intent,BluetoothLeManager.REQUEST_ENABLE_BT);
                            return;
                        }

                        initializeScanDevice();
                    }
                }



            }
        });

    }

    private boolean checkPermission(){
        boolean granted = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int permissionResult0 = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            int permissionResult1 = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
            int permissionResult2 = checkSelfPermission(Manifest.permission.RECORD_AUDIO);

            if (permissionResult0== PackageManager.PERMISSION_DENIED
                    || permissionResult1 == PackageManager.PERMISSION_DENIED
                    || permissionResult2 == PackageManager.PERMISSION_DENIED     ) {

                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
                        || shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)
                        || shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                    dialog.setTitle("권한이 필요합니다.").setMessage("이 기능을 사용하기 위해서는 단말기의 권한이 필요합니다. 계속 하시겠습니까")
                            .setPositiveButton("네", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        requestPermissions(new String[]{
                                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.RECORD_AUDIO}, 1000);
                                    }
                                }
                            }).setNegativeButton("아니오", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            return;
                        }
                    }).create().show();

                } else {
                    requestPermissions(
                            new String[]{
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.RECORD_AUDIO
                            },1000);
                }
            }
            else{
                granted = true;
            }
        }
        return granted;
    }

    private boolean scanning = false;
    private BluetoothAdapter btadapter;
    private ArrayList<BluetoothDevice> mLeDevice;
    private Handler mHandler = new Handler();
    private CustomAdapter3 scanAdapter;
    private static final int SCAN_TIMEOVER = 10;
    private AlertDialog dialog;
    private ListView deviceList;
    private TextView scanInfo;



    private void initializeScanDevice(){
         scanAdapter = new CustomAdapter3(this);
         mLeDevice = new ArrayList<>();
         AlertDialog.Builder builder = new AlertDialog.Builder(this);
         LinearLayout layout = (LinearLayout)View.inflate(this,R.layout.scandevice,null);
         layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
         builder.setView(layout);
         deviceList = layout.findViewById(R.id.devicelist);
         scanInfo = layout.findViewById(R.id.scaninfo);
         deviceList.setOnItemClickListener(btDeviceSelectClickListener);
         scanLeDevice(true);
         dialog = builder.create();
         dialog.show();
         dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
             @Override
             public void onDismiss(DialogInterface dialog) {
                 scanLeDevice(false);
             }
         });
    }

    private AdapterView.OnItemClickListener btDeviceSelectClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            scanLeDevice(false);
            Custom3_Item thisItem = (Custom3_Item)(parent.getAdapter().getItem(position));
            String name = thisItem.getData()[0];
            String address = thisItem.getData()[1];
            Toast.makeText(getApplicationContext(),"name : "  + name + "\naddress : " + address,Toast.LENGTH_SHORT).show();
            scanInfo.setText("Try to connect " + address);
            BluetoothLeManager manager = service.getBle();
            String[] btinfo = {name, address};
            manager.setBtInfo(btinfo);
            manager.connect();

        }
    };


    private void scanLeDevice(final boolean enable){
        if(btadapter == null){
            btadapter = BluetoothAdapter.getDefaultAdapter();
        }
        if(enable){
            scanning = false;
            btadapter.stopLeScan(mLeScanCallback);

            scanning = true;
            btadapter.startLeScan(mLeScanCallback);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            scanLeDevice(false);
                            mHandler.obtainMessage(SCAN_TIMEOVER).sendToTarget();
//                            scanLeDevice(true);
                        }
                    },5000);
                }
            }).start();
        }
        else{
            scanning = false;
            btadapter.stopLeScan(mLeScanCallback);
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(!mLeDevice.contains(bluetoothDevice)) {
                        if(bluetoothDevice.getName() != null ) {
                            mLeDevice.add(bluetoothDevice);
                            scanAdapter.addItem(new Custom3_Item(getResources().getDrawable(R.drawable.device_image), bluetoothDevice.getName(), bluetoothDevice.getAddress()));
                            deviceList.setAdapter(scanAdapter);
                            Log.d(TAG,"Device : " + bluetoothDevice.getName());

                        }
                    }
                }
            });
        }
    };


    @Override
    protected void onResume() {
        super.onResume();
        setFilter();
        startService(KeyboardService.class, KeyboardConnection,null );
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
//        unbindService(KeyboardConnection);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        service.getBle().disconnect();
        unbindService(KeyboardConnection);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothLeManager.STATE_CONNECTED_ACTION.equals(action)) {
                bluetooth.setImageDrawable(getResources().getDrawable(R.drawable.bluetooth_on));
                dialog.dismiss();
                Toast.makeText(MainActivity.this, "블루투스가 연결되었습니다.", Toast.LENGTH_SHORT).show();
            }
            if(BluetoothLeManager.STATE_DISCONNECTED_ACTION.equals(action)){
                bluetooth.setImageDrawable(getResources().getDrawable(R.drawable.bluetooth_off));
                if(scanInfo != null){
                    scanInfo.setText("블루투스 연결을 실패하였습니다.");

                }
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

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            service = null;
            Log.e(TAG,"Service Disconnected");
        }
    };

    private void setFilter(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothLeManager.STATE_CONNECTED_ACTION);
        filter.addAction(BluetoothLeManager.STATE_DISCONNECTED_ACTION);
        registerReceiver(mReceiver,filter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == BluetoothLeManager.REQUEST_ENABLE_BT){
            Toast.makeText(this,"Result : " + resultCode, Toast.LENGTH_SHORT).show();
            if(resultCode == -1){
                // Granted
                scanLeDevice(true);
            }
            else if(resultCode == 0){
                // not Granted
                Toast.makeText(getApplicationContext(),"블루투스가 실행되지 않아 5초 후 어플리케이션을 종료합니다.",Toast.LENGTH_LONG).show();
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                },5000);

            }
        }
    }
}
