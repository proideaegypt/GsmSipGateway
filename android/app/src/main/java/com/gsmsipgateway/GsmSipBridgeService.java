package com.gsmsipgateway;
import android.app.*;
import android.media.AudioManager;
import android.content.*;
import android.os.*;
import android.telecom.TelecomManager;
import android.util.Log;
import androidx.core.app.NotificationCompat;

public class GsmSipBridgeService extends Service implements LinphoneEngine.BridgeCallback {
    private static final String TAG = "GsmSipBridgeService";
    private static final String CH = "gsm_sip_bridge";
    private static final int RING_INTERVAL_MS = 5000;
    private LinphoneEngine sip;
    private String host, user, pass, ext;
    private int port, answerRings;
    private boolean isSipRegistered = false;
    private boolean bridgeInProgress = false;
    private int bridgeAttempts = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable bridgeRunnable = this::bridgeToExtension;
    private final Runnable answerRunnable = this::answerAndBridge;
    private AudioManager audioManager;
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
        audioManager = getSystemService(AudioManager.class);
        PowerManager pm = getSystemService(PowerManager.class);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG + ":bridge");
            wakeLock.setReferenceCounted(false);
            wakeLock.acquire(10 * 60 * 1000L);
        }
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
        answerRings = Math.max(1, p.getInt("answer_rings", 1));
        isSipRegistered = false;
        bridgeInProgress = false;
        bridgeAttempts = 0;
        handler.removeCallbacks(answerRunnable);
        handler.removeCallbacks(bridgeRunnable);
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
                    bridgeInProgress = true;
                    bridgeAttempts = 0;
                    handler.removeCallbacks(answerRunnable);
                    handler.removeCallbacks(bridgeRunnable);
                    int answerDelayMs = Math.max(0, answerRings - 1) * RING_INTERVAL_MS;
                    updateNote("Incoming: " + caller + " | answering after ring " + answerRings);
                    handler.postDelayed(answerRunnable, answerDelayMs);
                    break;
                case "ACTION_CALL_ENDED":
                    handler.removeCallbacks(answerRunnable);
                    handler.removeCallbacks(bridgeRunnable);
                    bridgeInProgress = false;
                    bridgeAttempts = 0;
                    if (sip != null) sip.hangup();
                    resetAudio();
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

    private void answerAndBridge() {
        if (!bridgeInProgress) {
            return;
        }
        prepareAudio();
        autoAnswer();
        handler.removeCallbacks(bridgeRunnable);
        handler.postDelayed(bridgeRunnable, 1200);
    }

    private void bridgeToExtension() {
        if (!bridgeInProgress) {
            return;
        }
        if (!isSipRegistered) {
            scheduleBridgeRetry("SIP not registered yet");
            return;
        }

        Log.d(TAG, "SIP registered, bridging to extension " + ext);
        updateNote("Dialing SIP extension...");
        boolean success = sip.callSip(ext, host, port);
        if (!success) {
            scheduleBridgeRetry("Failed to dial " + ext);
        }
    }

    @Override public void onSipRegistered() {
        isSipRegistered = true;
        Log.d(TAG, "SIP Registration successful");
        updateNote("SIP Registered - Ready");
    }

    @Override public void onSipRegistrationFailed() {
        isSipRegistered = false;
        Log.e(TAG, "SIP Registration failed, will retry in 2000ms");
        updateNote("Registration Failed - Retrying...");
        handler.postDelayed(() -> {
            if (sip != null) {
                Log.d(TAG, "Retrying SIP registration...");
                sip.register(host, port, user, pass);
            }
        }, 2000);
    }

    @Override public void onSipCallConnected() { updateNote("Bridge Active"); }
    @Override public void onSipCallEnded()     {
        bridgeInProgress = false;
        bridgeAttempts = 0;
        resetAudio();
        updateNote("Ready - Waiting...");
    }

    private void scheduleBridgeRetry(String reason) {
        bridgeAttempts++;
        if (bridgeAttempts > 10) {
            bridgeInProgress = false;
            updateNote("Bridge failed");
            Log.e(TAG, reason + ", giving up after " + bridgeAttempts + " attempts");
            return;
        }
        Log.w(TAG, reason + ", retrying in 500ms");
        handler.postDelayed(bridgeRunnable, 500);
    }

    private void prepareAudio() {
        if (audioManager != null) {
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            audioManager.setSpeakerphoneOn(false);
            audioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        }
    }

    private void resetAudio() {
        if (audioManager != null) {
            audioManager.setMode(AudioManager.MODE_NORMAL);
            audioManager.abandonAudioFocus(null);
        }
    }

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
    @Override public void onDestroy() {
        handler.removeCallbacks(answerRunnable);
        handler.removeCallbacks(bridgeRunnable);
        resetAudio();
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        if (sip != null) sip.destroy();
        super.onDestroy();
    }
}
