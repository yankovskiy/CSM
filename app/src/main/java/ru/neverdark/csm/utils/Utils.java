package ru.neverdark.csm.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Environment;

import java.util.Locale;

public final class Utils {
    /**
     * Определение доступности внешнего хранилища для записи данных
     * @return true если внешнее хранилище доступно
     */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /**
     * Проверяет запущенность сервера по его класса
     * @param context контекст приложения
     * @param serviceClass класс сервиса для проверки
     * @return true если сервис запущен
     */
    public static boolean isServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Возвращает имя файла скриншота карты по id тренировки
     * @param recordId id тренировки
     * @return имя файла скриншота карты
     */
    public static String getSnapshotNameById(long recordId) {
        return String.format(Locale.US, "trip-map-%d.png", recordId);
    }
}
