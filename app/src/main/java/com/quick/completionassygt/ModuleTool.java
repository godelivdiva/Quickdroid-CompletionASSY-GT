package com.quick.completionassygt;

import android.os.StrictMode;

/**
 * Created by Admin on 9/13/2017.
 * Ini adalah methode yang reusable bagi setiap aplikasi
 */

public class ModuleTool {
    public void allowNetworkOnMainThread() {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }
}
