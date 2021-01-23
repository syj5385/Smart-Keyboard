package jjun.smartkeyboard;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;

import java.util.List;
import java.util.UUID;


/**
 * Created by comm on 2018-08-07.
 */

public class BluetoothLeManager {

    private static final String TAG = "BluetoothLeManager";

    protected Context context;
    protected KeyboardService iotService;
    protected BluetoothAdapter btAdapter;
    protected BluetoothManager btManager;
    protected BluetoothGatt mGatt;
    private Handler mHandler;

    // MACRO define
    public static final int REQUEST_ENABLE_BT = 1;

    public static final String STATE_CONNECTED_ACTION = "jjun.smartkeyboard.BluetoothLeManager.STATE_CONNECTED";
    public static final String STATE_FAILED_ACTION = "jjun.smartkeyboard.BluetoothLeManager.STATE_FAILED";
    public static final String STATE_DISCONNECTED_ACTION  = "jjun.smartkeyboard.BluetoothLeManager.STATE_DISCONNECTED";

    public static final String BLUETOOTH_DATA_AVAILABLE = "BLUETOOTH_DATA_AVAILABLE";
    public static final String BLUETOOTH_RECEIVED_DATA = "BLUETOOTH_RECEIVERD_DATA";
    public static final String EXTRA_DATA = "EXTRA_DATA";
    public final static UUID UUID_HEART_RATE_MEASUREMENT = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");

    //Bluetooth Connection information
    protected String[] btInfo;
    public static final int NAME = 0;
    public static final int ADDRESS = 1;
    public int id = 0;

    //Bluetooth connection state
    protected int state = STATE_NONE;
    public static final int STATE_CONNECTED = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_FAIL = 3;
    public static final int STATE_NONE = 4;
    public static final int STATE_DISCONNECTED = 5;

    private ConnectingThread connectingThread;
    private ConnectedThread connectedThread;

    private boolean discovered = false;


    public BluetoothLeManager(KeyboardService service, Context context) {
        this.iotService = service;
        this.context = context;
        this.btInfo = btInfo;
        if(!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
//            Toast.makeText(context,"이 기기는 BLE를 지원하지 않습니다,",Toast.LENGTH_SHORT).show();
            Log.d(TAG,"이 기기는 BLE를 지원하지 않습니다.");
        }
        else{
            btManager = (BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE);
            btAdapter = btManager.getAdapter();
            Log.d(TAG,"finished to initialize Bluetooth LE");
        }

    }

    public void connect(){
        Log.d(TAG,"connect BLE Device");
        final BluetoothDevice device = btAdapter.getRemoteDevice(btInfo[ADDRESS]);
        connectingThread = new ConnectingThread(device);
        connectingThread.start();
    }

    private boolean requestDisconnect = false;
    public void disconnect(){
//        if(state == STATE_CONNECTED){
            requestDisconnect = true;
            mGatt.disconnect();
//        }
    }

    public void destory(){
        if(state != STATE_CONNECTED){
            btAdapter = null;


        }
    }

    public void write(byte[] data){
        if(characteristic != null){
            Log.d(TAG,"try to write data on BLE Manager");
            characteristic.setValue(data);
            mGatt.writeCharacteristic(characteristic);
        }
    }

    public int getState(){
        return state;
    }

    public void setBtInfo(String[] btInfo){
        this.btInfo = btInfo;
    }

    public String getBtAddress(){
        return btInfo[ADDRESS];
    }

    public String getBtName(){
        return btInfo[NAME];
    }

    public int getId(){
        return id;
    }

    // Private Method
    // ConnectingThread
    private class ConnectingThread extends Thread {

        private BluetoothDevice device;
        public ConnectingThread(BluetoothDevice device) {
            super();
            this.device = device;

        }

        @Override
        public void run() {
            super.run();
            Log.d(TAG,"run Connecting Thread");
            if(state == STATE_CONNECTED){
                Log.d(TAG,"Bluetooth Le state : STATE_CONNECTED");
                return;
            }
            if(mGatt != null) {
                mGatt = null;
            }
            Log.d(TAG,"Connect BluetoothGatt");
            mGatt = device.connectGatt(context, false, gattCallback);
        }
    }

    private class ConnectedThread extends Thread {
        private final int INTERVAL_TO_READ = 1500;
        private BluetoothGattDescriptor descriptor;

        public ConnectedThread() {
            super();
        }

        @Override
        public void run() {
            super.run();
//            while(state == STATE_CONNECTED){
//                long prev = System.currentTimeMillis();
////                String get = "hello";
////                characteristic.setValue(get);
//                mGatt.writeCharacteristic(characteristic);
////                Log.d(TAG,new String(descriptor.getValue()));
//
//                long current = System.currentTimeMillis();
//                try{
//                    Thread.sleep(INTERVAL_TO_READ - (current-prev));
//                }catch (InterruptedException e){
//
//                }
//            }
        }
    }

    private BluetoothGattService IO_Service;
    private BluetoothGattCharacteristic characteristic;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private String CUSTOM_SERVICE = "0000ffe0-0000-1000-8000-00805f9b34fb";
    private String CUSTOM_CHARACTERISTIC = "0000ffe1-0000-1000-8000-00805f9b34fb";

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.d(TAG,"onConnectionStateChange : " + newState);
            switch(newState){
                case BluetoothProfile.STATE_CONNECTED :
                    if(!discovered){
                        if(gatt.discoverServices() == false){
                            gatt.disconnect();
                        }
                    }
                    state = STATE_CONNECTED;
                    Log.d(TAG,"state -> connected");
                    setCharacteristicNotification(true);
                    context.sendBroadcast(new Intent(STATE_CONNECTED_ACTION));

                    break;

                case BluetoothProfile.STATE_DISCONNECTED :
                    state = STATE_DISCONNECTED;
                    Log.d(TAG,"state -> disconnected (retry) -> address : " + btInfo[ADDRESS]);
                    try{
                        Thread.sleep(1000);
                    }catch (InterruptedException e){

                    }
                    if(!requestDisconnect && btAdapter != null) {
                        final BluetoothDevice device = btAdapter.getRemoteDevice(btInfo[ADDRESS]);
                        connectingThread = null;
                        connectingThread = new ConnectingThread(device);
                        connectingThread.start();
                    }
                    context.sendBroadcast(new Intent(STATE_DISCONNECTED_ACTION));
                    break;


                default :
                    state = STATE_NONE;
                    Log.d(TAG,"STATE NONE");
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.d(TAG,"onServiceDiscovered");
            List<BluetoothGattService> services = gatt.getServices();
            for(int i=0; i<services.size() ; i++){
                for(int j=0; j<services.get(i).getCharacteristics().size() ; j++){
                    final BluetoothGattCharacteristic characteristic = services.get(i).getCharacteristics().get(j);
                    final int charaProp = characteristic.getProperties();
                    if((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0){
                        mNotifyCharacteristic = characteristic;
//                        mGatt.setCharacteristicNotification(characteristic,true);
                    }

                }
            }

            characteristic = mGatt.getService(UUID.fromString(CUSTOM_SERVICE)).getCharacteristic(UUID.fromString(CUSTOM_CHARACTERISTIC));
            discovered=true;
            connectedThread = new ConnectedThread();
//            connectedThread.start();
//            context.sendBroadcast(intent);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.d(TAG,"onCharacteristic Read -> " + status);
            broadcastUpdate("READ",gatt.getService(UUID.fromString(CUSTOM_SERVICE)).getCharacteristic(UUID.fromString(CUSTOM_CHARACTERISTIC)));
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.d(TAG,"onCharacteristic Write");
//            gatt.readCharacteristic(characteristic);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
//            Log.d(TAG,"onCharacteristic changed");
//            broadcastUpdate(IoTDevice.BLUETOOTH_RECEIVED_DATA,characteristic);
            Intent intent = new Intent(BLUETOOTH_RECEIVED_DATA);
            intent.putExtra(BLUETOOTH_RECEIVED_DATA,characteristic.getValue());
            intent.putExtra("id",id);
            context.sendBroadcast(intent);

//            byte[] received = characteristic.getValue();
//            Log.d(TAG,new String(received));

//            Log.d(TAG,"I'm " + id);
//            Log.d(TAG,"value0 : " + ((int)characteristic.getValue()[0] & 0xff));
//            Log.d(TAG,"value1 : " + ((int)characteristic.getValue()[1] & 0xff));
        }
    };

    private void broadcastUpdate(final String action){
        final Intent intent = new Intent(action);
        context.sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d(TAG, "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d(TAG, "Heart rate format UINT8.");
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            Log.d(TAG, String.format("Received heart rate: %d", heartRate));
            intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
        } else {
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
//                Log.d(TAG,"Extra data : " + new String(data) + "\n" + stringBuilder.toString());
                intent.putExtra(BLUETOOTH_RECEIVED_DATA, new String(data) + "\n" + stringBuilder.toString());
                intent.putExtra("id",id);

            }
        }
        context.sendBroadcast(intent);

    }

    public void setCharacteristicNotification(boolean enable){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(mGatt == null || mNotifyCharacteristic == null){
                    try{
                        Thread.sleep(1);
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }

                }
                mGatt.setCharacteristicNotification(mNotifyCharacteristic, true);
            }
        }).start();

    }



}
