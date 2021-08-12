package com.quick.completionassygt;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.HashMap;

public class ManageSessionUser {
    private SharedPreferences preferences;
    private static final String PREF_NAME = "session";
    private final String KEY_IS_LOGIN = "isLogin";

    static final String KEY_USERNAME = "username";
    static final String KEY_PASSWORD = "password";

    Context mContext;
    ManageSessionUser(Context context) {
        mContext = context;
        preferences = mContext.getSharedPreferences(PREF_NAME, 0); //PrivateMode
    }

    void createUserSession(String username, String password) {
        SharedPreferences.Editor edit;
        edit = preferences.edit();

        edit.putBoolean(KEY_IS_LOGIN, true);
        edit.putString(KEY_USERNAME, username);
        edit.putString(KEY_PASSWORD, password);

        edit.apply();
    }

    HashMap<String, String> getUserData() {
        HashMap<String, String> userData = new HashMap<>();

        userData.put(KEY_USERNAME, preferences.getString(KEY_USERNAME, null));
        userData.put(KEY_PASSWORD, preferences.getString(KEY_PASSWORD, null));

        return userData;
    }

    String getUser() {
        return preferences.getString(KEY_USERNAME, "");
    }

    Boolean isUserLogin() {
        return preferences.getBoolean(KEY_IS_LOGIN, false);
    }

    public void logoutUser() {
        preferences.edit().clear().apply();

        // After logout redirect user to Login Activity
        Intent i = new Intent(mContext, LoginActivity.class);

        // Closing all the Activities
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Add new Flag to start new Activity
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // Staring Login Activity
        mContext.startActivity(i);
    }

}
