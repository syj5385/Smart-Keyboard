package jjun.smartkeyboard;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class KeyboardService extends Service {
    private static final String TAG = "KeyboardService";

    public IBinder binder = new MainBinder();

    private BluetoothLeManager ble;

    private boolean enabledSendChar = true;

    public class MainBinder extends Binder {
        public KeyboardService getService() {
            return KeyboardService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setFilter();
        Log.d(TAG,"Start Keyboard Service");
        ble = new BluetoothLeManager(this,this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(BluetoothLeManager.BLUETOOTH_RECEIVED_DATA)){
                String receivedString = new String(intent.getByteArrayExtra(BluetoothLeManager.BLUETOOTH_RECEIVED_DATA));
                if(receivedString.contains("$OK#")){
                    setEnabledSendChar(true);
                }
            }
        }
    };

    private void setFilter(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothLeManager.STATE_CONNECTED_ACTION);
        filter.addAction(BluetoothLeManager.STATE_DISCONNECTED_ACTION);
        filter.addAction(BluetoothLeManager.BLUETOOTH_RECEIVED_DATA);
        registerReceiver(mReceiver,filter);
    }

    public BluetoothLeManager getBle(){
        return ble;
    }

    public void setEnabledSendChar(boolean enable){

        enabledSendChar = enable;
    }

    public boolean isEnabledSendChar(){
        return enabledSendChar;
    }

    public void sendCharacterToTarget(char charac){
        Log.d(TAG,"state -> " + getBle().getState());
        if(getBle().getState() == BluetoothLeManager.STATE_CONNECTED) {
            String header = "$REQ#";
            byte[] payload = new byte[header.length() + 2];
            int index = 0;

            for (index = 0; index < header.length(); index++) {
                payload[index] = (byte) header.charAt(index);
            }
            payload[index++] = (byte) charac;
            payload[index++] = (byte) '#';

            getBle().write(payload);
            Log.d(TAG,"try to send data to Target on KeyboardService");

        }
    }
}
