package com.gsmsipgateway;
import com.facebook.react.*;
import com.facebook.react.bridge.*;
import com.facebook.react.uimanager.*;
import java.util.*;

public class SipBridgePackage implements ReactPackage {
    @Override public List<NativeModule> createNativeModules(ReactApplicationContext ctx) {
        return Collections.singletonList(new SipBridgeModule(ctx));
    }
    @Override public List<ViewManager> createViewManagers(ReactApplicationContext ctx) {
        return Collections.emptyList();
    }
}
