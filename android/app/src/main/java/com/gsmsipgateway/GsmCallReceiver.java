package com.gsmsipgateway;
import android.content.*;
import android.telephony.TelephonyManager;
import android.util.Log;

public class GsmCallReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
            Intent si = new Intent(context, GsmSipBridgeService.class);
            si.setAction("ACTION_INCOMING_CALL");
            si.putExtra("caller_number", number != null ? number : "Unknown");
            context.startForegroundService(si);
        }
        if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
            Intent si = new Intent(context, GsmSipBridgeService.class);
            si.setAction("ACTION_CALL_ENDED");
            context.startService(si);
        }
    }
}
