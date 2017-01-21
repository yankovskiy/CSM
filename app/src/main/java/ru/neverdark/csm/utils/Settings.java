package ru.neverdark.csm.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.android.gms.maps.GoogleMap;

public final class Settings {
    private static final String FILE_NAME = "prefs";
    private static final String MAP_TYPE = "map-type";
    private static final String MAP_ZOOM = "map-zoom";
    private static Settings mInstance;
    private final SharedPreferences mPrefs;
    private final SharedPreferences.Editor mEditor;

    public static Settings getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new Settings(context);
        }

        return mInstance;
    }

    private Settings(Context context) {
        mPrefs = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        mEditor = mPrefs.edit();
    }

    public void saveMapType(int mapType) {
        mEditor.putInt(MAP_TYPE, mapType);
        mEditor.apply();
    }

    public int loadMapType() {
        return mPrefs.getInt(MAP_TYPE, GoogleMap.MAP_TYPE_NORMAL);
    }

    public void saveMapZoom(float zoom) {
        mEditor.putFloat(MAP_ZOOM, zoom);
        mEditor.apply();
    }

    public float loadMapZoom() {
        return mPrefs.getFloat(MAP_ZOOM, 15);
    }
}
