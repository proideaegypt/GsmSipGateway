package com.gsmsipgateway;
import android.app.*;
import android.content.*;
import android.os.*;
import android.telecom.TelecomManager;
import android.util.Log;
import androidx.core.app.NotificationCompat;

public class GsmSipBridgeService extends Service implements LinphoneEngine.BridgeCallback {
    private static final String TAG = "GsmSipBridgeService";
    private static final String CH = "gsm_sip_bridge";
    private LinphoneEngine sip;
    private String host, user, pass, ext;
    private int port;

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(1, note("Initializing..."));
        reload();
    }

    private void reload() {
        SharedPreferences p = getSharedPreferences("sip_config", MODE_PRIVATE);
        host = p.getString("host", "192.168.1.100");
        port = p.getInt("port", 5060);
        user = p.getString("username", "android_gsm1");
        pass = p.getString("password", "");
        ext  = p.getString("bridge_ext", "1000");
        if (sip != null) sip.destroy();
        sip = new LinphoneEngine(this, this);
        sip.register(host, port, user, pass);
        updateNote("Registering on FreePBX...");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case "ACTION_INCOMING_CALL":
                    String caller = intent.getStringExtra("caller_number");
                    updateNote("Incoming: " + caller);
                    autoAnswer();
                    new Handler(Looper.getMainLooper()).postDelayed(() -> sip.callSip(ext, host), 1200);
                    break;
                case "ACTION_CALL_ENDED":
                    if (sip != null) sip.hangup();
                    updateNote("Ready - Waiting...");
                    break;
                case "ACTION_RELOAD":
                    reload();
                    break;
            }
        }
        return START_STICKY;
    }

    private void autoAnswer() {
        try {
            TelecomManager tm = (TelecomManager) getSystemService(TELECOM_SERVICE);
            if (tm != null) tm.acceptRingingCall();
            Log.d(TAG, "GSM auto-answered");
        } catch (SecurityException e) { Log.e(TAG, e.getMessage()); }
    }

    @Override public void onSipRegistered()    { updateNote("SIP Registered - Ready"); }
    @Override public void onSipCallConnected() { updateNote("Bridge Active"); }
    @Override public void onSipCallEnded()     { updateNote("Ready - Waiting..."); }

    private void createChannel() {
        NotificationChannel c = new NotificationChannel(CH, "GSM-SIP Bridge", NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(c);
    }
    private Notification note(String t) {
        return new NotificationCompat.Builder(this, CH)
            .setContentTitle("GSM-SIP Gateway").setContentText(t)
            .setSmallIcon(android.R.drawable.ic_menu_call).setOngoing(true).build();
    }
    private void updateNote(String t) { getSystemService(NotificationManager.class).notify(1, note(t)); }

    @Override public IBinder onBind(Intent i) { return null; }
    @Override public void onDestroy() { if (sip != null) sip.destroy(); super.onDestroy(); }
}
