package com.gsmsipgateway;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.sip.*;
import android.util.Log;

public class LinphoneEngine {
    private static final String TAG = "SipEngine";
    private SipManager sipManager;
    private SipProfile localProfile;
    private SipAudioCall currentCall;
    private BridgeCallback callback;
    private String host, user;

    public interface BridgeCallback {
        void onSipRegistered();
        void onSipRegistrationFailed();
        void onSipCallConnected();
        void onSipCallEnded();
    }

    public LinphoneEngine(Context ctx, BridgeCallback cb) {
        this.callback = cb;
        if (SipManager.isVoipSupported(ctx) && SipManager.isApiSupported(ctx)) {
            sipManager = SipManager.newInstance(ctx);
        } else {
            Log.e(TAG, "SIP not supported on this device");
        }
    }

    public void register(String host, int port, String user, String pass) {
        this.host = host;
        this.user = user;
        if (sipManager == null) {
            Log.e(TAG, "register skipped: SIP manager unavailable");
            if (callback != null) callback.onSipRegistrationFailed();
            return;
        }
        try {
            if (localProfile != null) {
                try { sipManager.close(localProfile.getUriString()); } catch (Exception ignored) {}
            }
            SipProfile.Builder builder = new SipProfile.Builder(user, host);
            builder.setPassword(pass);
            builder.setPort(port);
            builder.setProtocol("UDP");
            builder.setOutboundProxy(host + ":" + port);
            builder.setAutoRegistration(true);
            localProfile = builder.build();

            Intent intent = new Intent();
            intent.setAction("android.SipDemo.INCOMING_CALL");
            PendingIntent pi = PendingIntent.getBroadcast(
                null, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            );
            sipManager.open(localProfile, pi, null);
            sipManager.setRegistrationListener(localProfile.getUriString(),
                new SipRegistrationListener() {
                    @Override public void onRegistering(String localProfileUri) {
                        Log.d(TAG, "Registering...");
                    }
                    @Override public void onRegistrationDone(String localProfileUri, long expiryTime) {
                        Log.d(TAG, "Registered OK | uri=" + localProfileUri + " | expires=" + expiryTime);
                        if (callback != null) callback.onSipRegistered();
                    }
                    @Override public void onRegistrationFailed(String localProfileUri, int errorCode, String errorMessage) {
                        Log.e(TAG, "Registration failed: " + errorMessage);
                        if (callback != null) callback.onSipRegistrationFailed();
                    }
                });
        } catch (Exception e) {
            Log.e(TAG, "register error: " + e.getMessage());
            if (callback != null) callback.onSipRegistrationFailed();
        }
    }

    public boolean callSip(String ext, String remoteHost, int remotePort) {
        if (sipManager == null || localProfile == null) {
            Log.e(TAG, "callSip skipped: SIP stack not ready");
            return false;
        }
        if (ext == null || ext.trim().isEmpty()) {
            Log.e(TAG, "callSip skipped: empty bridge extension");
            return false;
        }
        try {
            String cleanExt = ext.trim();
            String peerUri;
            if (cleanExt.startsWith("sip:")) {
                peerUri = cleanExt;
            } else if (cleanExt.contains("@")) {
                peerUri = "sip:" + cleanExt;
            } else {
                peerUri = "sip:" + cleanExt + "@" + remoteHost + ":" + remotePort;
            }
            Log.d(TAG, "Dialing peerUri=" + peerUri + " from " + localProfile.getUriString());
            currentCall = sipManager.makeAudioCall(
                localProfile.getUriString(),
                peerUri,
                new SipAudioCall.Listener() {
                    @Override public void onCallEstablished(SipAudioCall call) {
                        call.startAudio();
                        if (callback != null) callback.onSipCallConnected();
                    }
                    @Override public void onCallEnded(SipAudioCall call) {
                        if (callback != null) callback.onSipCallEnded();
                    }
                    @Override public void onError(SipAudioCall call, int errorCode, String errorMessage) {
                        Log.e(TAG, "Call error: " + errorMessage);
                        if (callback != null) callback.onSipCallEnded();
                    }
                }, 30
            );
            return currentCall != null;
        } catch (Exception e) {
            Log.e(TAG, "callSip error: " + e.getMessage());
            return false;
        }
    }

    public void hangup() {
        try {
            if (currentCall != null) {
                currentCall.endCall();
                currentCall.close();
                currentCall = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "hangup error: " + e.getMessage());
        }
    }

    public void destroy() {
        hangup();
        try {
            if (localProfile != null && sipManager != null) {
                sipManager.close(localProfile.getUriString());
            }
        } catch (Exception e) {
            Log.e(TAG, "destroy error: " + e.getMessage());
        }
    }
}
