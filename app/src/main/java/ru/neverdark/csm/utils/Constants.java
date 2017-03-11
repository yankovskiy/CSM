package ru.neverdark.csm.utils;

import ru.neverdark.csm.BuildConfig;

public final class Constants {
    public static final String PACKAGE_NAME = BuildConfig.APPLICATION_ID;
    public static final boolean SAVE_POINTS_WITHOUT_SPEED = false;
    /**
     * Количество сегментов трека для которых производится усреднение высоты
     */
    public static final int AVERAGE_ALTITUDE_SEGMENT_COUNT = 5;
}
