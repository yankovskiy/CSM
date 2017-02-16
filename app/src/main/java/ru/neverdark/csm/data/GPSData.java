package ru.neverdark.csm.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.Serializable;

import ru.neverdark.csm.R;

public class GPSData implements Serializable{

    public float latitude;          // текущая широта
    public float longitude;         // текущая долгтоа
    public float altitude;          // текущая высота
    public float speed;             // текущая скорость
    public float distance;          // пройденное расстояние
    public float max_speed;         // максимальная скорость
    public float average_speed;     // средняя скорость
    public float max_altitude;      // максимальная высота
    public float up_distance;       // дистанция движения в гору
    public float down_distance;     // дистанция движения с горы
    public float up_altitude;       // набор высоты
    public float down_altitude;     // потеря высоты
    public float accuracy;          // погрешность местоположения в метрах
    public int ascend_time;        // время подъема в мс
    public int descend_time;       // время спуска в мс
    public double max_ascend_gradient;      // максимальный уклон на подъеме
    public double average_ascend_gradient;  // средний уклон на подъеме
    public double max_descend_gradient;     // максимальный уклон на спуске
    public double average_descend_gradient; // средний уклон на спуске

    public void save(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat("altitude", altitude);
        editor.putFloat("latitude", latitude);
        editor.putFloat("longitude", longitude);
        editor.putFloat("speed", speed);
        editor.putFloat("average-speed", average_speed);
        editor.putFloat("distance", distance);
        editor.putFloat("max-speed", max_speed);
        editor.putFloat("max-altitude", max_altitude);
        editor.putFloat("up-distance", up_distance);
        editor.putFloat("down-distance", down_distance);
        editor.putFloat("up-altitude", up_altitude);
        editor.putFloat("down-altitude", down_altitude);
        editor.putFloat("accuracy", accuracy);
        editor.putInt("ascend-time", ascend_time);
        editor.putInt("descend-time", descend_time);
        editor.putFloat("max-ascend-gradient", (float) max_ascend_gradient);
        editor.apply();
    }

    public void load(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        speed = sharedPref.getFloat("speed", 0);
        latitude = sharedPref.getFloat("latitude", 0);
        longitude = sharedPref.getFloat("longitude", 0);
        altitude = sharedPref.getFloat("altitude", 0);
        average_speed = sharedPref.getFloat("average-speed", 0);
        distance = sharedPref.getFloat("distance", 0);
        max_speed = sharedPref.getFloat("max-speed", 0);
        max_altitude = sharedPref.getFloat("max-altitude", 0);
        up_distance = sharedPref.getFloat("up-distance", 0);
        down_distance = sharedPref.getFloat("down-distance", 0);
        up_altitude = sharedPref.getFloat("up-altitude", 0);
        down_altitude = sharedPref.getFloat("down-altitude", 0);
        accuracy = sharedPref.getFloat("accuracy", 0);
        ascend_time = sharedPref.getInt("ascend-time", 0);
        descend_time = sharedPref.getInt("descend-time", 0);
        max_ascend_gradient = sharedPref.getFloat("max-ascend-gradient", 0);
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
        accuracy = data.accuracy;
        ascend_time = data.ascend_time;
        descend_time = data.descend_time;
        max_ascend_gradient = data.max_ascend_gradient;
    }
}
