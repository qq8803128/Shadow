package com.tencent.shadow.test.plugin.particular_cases.plugin_service_for_host;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class TestService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
