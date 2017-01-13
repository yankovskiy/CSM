package ru.neverdark.csm.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.Serializable;

import ru.neverdark.csm.R;

public class GPSData implements Serializable{

    public float latitude;      // текущая широта
    public float longitude;     // текущая долгтоа
    public int altitude;        // текущая высота
    public int speed;           // текущая скорость
    public int distance;        // пройденное расстояние
    public int max_speed;       // максимальная скорость
    public float average_speed; // средняя скорость
    public int max_altitude;    // максимальная высота
    public int up_distance;     // дистанция движения в гору
    public int down_distance;   // дистанция движения с горы
    public int up_altitude;     // набор высоты
    public int down_altitude;   // потеря высоты

    public void save(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("altitude", altitude);
        editor.putFloat("latitude", latitude);
        editor.putFloat("longitude", longitude);
        editor.putInt("speed", speed);
        editor.putFloat("average-speed", average_speed);
        editor.putInt("distance", distance);
        editor.putInt("max-speed", max_speed);
        editor.putInt("max-altitude", max_altitude);
        editor.putInt("up-distance", up_distance);
        editor.putInt("down-distance", down_distance);
        editor.putInt("up-altitude", up_altitude);
        editor.putInt("down-altitude", down_altitude);
        editor.apply();
    }

    public void load(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        speed = sharedPref.getInt("speed", 0);
        latitude = sharedPref.getFloat("latitude", 0);
        longitude = sharedPref.getFloat("longitude", 0);
        altitude = sharedPref.getInt("altitude", 0);
        average_speed = sharedPref.getFloat("average-speed", 0);
        distance = sharedPref.getInt("distance", 0);
        max_speed = sharedPref.getInt("max-speed", 0);
        max_altitude = sharedPref.getInt("max-altitude", 0);
        up_distance = sharedPref.getInt("up-distance", 0);
        down_distance = sharedPref.getInt("down-distance", 0);
        up_altitude = sharedPref.getInt("up-altitude", 0);
        down_altitude = sharedPref.getInt("down-altitude", 0);
    }

    public void copyFrom(GPSData data) {
        latitude = data.latitude;
        longitude = data.longitude;
        altitude = data.altitude;
        speed = data.speed;
        distance = data.distance;
        max_speed = data.max_speed;
        average_speed = data.average_speed;
        max_altitude = data.max_altitude;
        up_distance = data.up_distance;
        down_distance = data.down_distance;
        up_altitude = data.up_altitude;
        down_altitude = data.down_altitude;
    }
}
