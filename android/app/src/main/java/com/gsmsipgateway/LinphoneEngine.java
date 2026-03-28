package com.gsmsipgateway;
import android.content.Context;
import android.util.Log;
import org.linphone.core.*;

public class LinphoneEngine implements CoreListener {
    private static final String TAG = "LinphoneEngine";
    private Core core;
    private BridgeCallback callback;

    public interface BridgeCallback {
        void onSipRegistered();
        void onSipCallConnected();
        void onSipCallEnded();
    }

    public LinphoneEngine(Context ctx, BridgeCallback cb) {
        this.callback = cb;
        Factory f = Factory.instance();
        f.setDebugMode(false, TAG);
        core = f.createCore(null, null, ctx);
        core.addListener(this);
    }

    public void register(String host, int port, String user, String pass) {
        try {
            AuthInfo auth = Factory.instance().createAuthInfo(user, null, pass, null, null, host);
            core.addAuthInfo(auth);
            AccountParams p = core.createAccountParams();
            p.setIdentityAddress(Factory.instance().createAddress("sip:" + user + "@" + host));
            p.setServerAddress(Factory.instance().createAddress("sip:" + host + ":" + port + ";transport=udp"));
            p.setRegisterEnabled(true);
            p.setExpires(3600);
            Account acc = core.createAccount(p);
            core.addAccount(acc);
            core.setDefaultAccount(acc);
            core.start();
            Log.d(TAG, "Registering on " + host + ":" + port);
        } catch (Exception e) { Log.e(TAG, e.getMessage()); }
    }

    public void callSip(String ext, String host) {
        try {
            CallParams p = core.createCallParams(null);
            p.setAudioEnabled(true);
            p.setVideoEnabled(false);
            core.inviteAddressWithParams(Factory.instance().createAddress("sip:" + ext + "@" + host), p);
        } catch (Exception e) { Log.e(TAG, e.getMessage()); }
    }

    public void hangup() {
        if (core != null && core.getCurrentCall() != null) core.getCurrentCall().terminate();
    }

    @Override
    public void onAccountRegistrationStateChanged(Core c, Account a, RegistrationState s, String m) {
        if (s == RegistrationState.Ok && callback != null) callback.onSipRegistered();
    }

    @Override
    public void onCallStateChanged(Core c, Call call, Call.State s, String m) {
        if (s == Call.State.Connected && callback != null) callback.onSipCallConnected();
        if ((s == Call.State.End || s == Call.State.Error) && callback != null) callback.onSipCallEnded();
    }

    public void destroy() { if (core != null) core.stop(); }
}
