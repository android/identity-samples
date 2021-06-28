package com.google.samples.smartlock.sms_verify;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.annotation.Nullable;

/**
 * Created by pmatthews on 9/14/16.
 */

public class PrefsHelper {
    private static final String TAG = PrefsHelper.class.getSimpleName();

    private static final String KEY_PHONE = "phone_number";
    private static final String KEY_VERIFIED = "phone_verified";
    private static final String KEY_SECRET_OVERRIDE = "secret_override";

    private final SharedPreferences prefs;

    public PrefsHelper(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void setPhoneNumber(String phoneNumber) {
        putString(KEY_PHONE, phoneNumber);
    }

    public void removePhoneNumber() {
        remove(PrefsHelper.KEY_PHONE);
    }

    public String getPhoneNumber(@Nullable String defaultValue) {
        return getString(KEY_PHONE, defaultValue);
    }

    public void setVerified(boolean verified) {
        putBoolean(KEY_VERIFIED, verified);
    }

    public void removeVerified() {
        remove(PrefsHelper.KEY_VERIFIED);
    }

    public boolean getVerified(boolean defaultValue) {
        return getBoolean(KEY_VERIFIED, defaultValue);
    }

    public String getSecretOverride(@Nullable String defaultValue) {
        return getString(KEY_SECRET_OVERRIDE, defaultValue);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return prefs.getBoolean(key, defaultValue);
    }

    private String getString(String key, String defaultValue) {
        return prefs.getString(key, defaultValue);
    }

    private void putBoolean(String key, boolean value) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    private void putString(String key, String value) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.apply();
    }

    private void remove(String key) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(key);
        editor.apply();
    }
}
