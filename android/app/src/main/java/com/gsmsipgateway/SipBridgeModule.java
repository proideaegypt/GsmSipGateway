package com.gsmsipgateway;
import android.content.*;
import com.facebook.react.bridge.*;

public class SipBridgeModule extends ReactContextBaseJavaModule {
    public SipBridgeModule(ReactApplicationContext ctx) { super(ctx); }
    @Override public String getName() { return "SipBridge"; }

    @ReactMethod
    public void saveConfig(ReadableMap cfg, Promise promise) {
        try {
            SharedPreferences.Editor p = getReactApplicationContext()
                .getSharedPreferences("sip_config", Context.MODE_PRIVATE).edit();
            p.putString("host", cfg.getString("host"));
            p.putInt("port", cfg.getInt("port"));
            p.putString("username", cfg.getString("username"));
            p.putString("password", cfg.getString("password"));
            p.putString("bridge_ext", cfg.getString("bridgeExtension"));
            p.putInt("answer_rings", cfg.hasKey("answerRings") ? cfg.getInt("answerRings") : 1);
            p.apply();
            Intent i = new Intent(getReactApplicationContext(), GsmSipBridgeService.class);
            i.setAction("ACTION_RELOAD");
            getReactApplicationContext().startForegroundService(i);
            promise.resolve("Config saved - Service restarted");
        } catch (Exception e) { promise.reject("ERROR", e.getMessage()); }
    }
}
