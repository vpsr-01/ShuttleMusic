package com.simplecity.amp_library.utils;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.simplecity.amp_library.ShuttleApplication;

public class SettingsManager {

    private SharedPreferences getSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(ShuttleApplication.getInstance());
    }

    @Nullable
    protected String getString(@NonNull String key) {
        return getString(key, null);
    }

    @NonNull
    protected String getString(@NonNull String key, @NonNull String defaultValue) {
        return getSharedPreferences().getString(key, defaultValue);
    }

    protected void setString(@NonNull String key, @Nullable String value) {
        final SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putString(key, value);
        editor.apply();
    }

    protected boolean getBool(@NonNull String key, boolean defaultValue) {
        return getSharedPreferences().getBoolean(key, defaultValue);
    }

    protected void setBool(@NonNull String key, boolean value) {
        final SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    protected int getInt(@NonNull String key, int defaultValue) {
        return getSharedPreferences().getInt(key, defaultValue);
    }

    protected void setInt(@NonNull String key, int value) {
        final SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putInt(key, value);
        editor.apply();
    }

    protected long getLong(@NonNull String key, long defaultValue) {
        return getSharedPreferences().getLong(key, defaultValue);
    }

    protected void setLong(@NonNull String key, long value) {
        final SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putLong(key, value);
        editor.apply();
    }

    public void remove(@NonNull String key) {
        final SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.remove(key);
        editor.apply();
    }

}
