package ru.neverdark.csm.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.provider.BaseColumns;

public class GpslogTable {
    private final SQLiteDatabase mDb;

    GpslogTable(SQLiteDatabase database) {
        mDb = database;
    }

    public void saveData(Location location, long trainingId) {
        long timestamp = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put(Entry.COLUMN_TRAINING_ID, trainingId);
        values.put(Entry.COLUMN_TIMESTAMP, timestamp);
        values.put(Entry.COLUMN_ALTITUDE, location.getAltitude());
        values.put(Entry.COLUMN_LATITUDE, location.getLatitude());
        values.put(Entry.COLUMN_LONGITUDE, location.getLongitude());
        values.put(Entry.COLUMN_SPEED, location.getSpeed());

        mDb.insert(Entry.TABLE_NAME, null, values);
    }

    public Cursor getRecordsForTraining(long trainingId) {
        String[] projection = {Entry.COLUMN_TIMESTAMP, Entry.COLUMN_LATITUDE, Entry.COLUMN_LONGITUDE};
        String selection = Entry.COLUMN_TRAINING_ID + " = ?";
        String[] selectionArgs = {String.valueOf(trainingId)};

        return mDb.query(Entry.TABLE_NAME, projection, selection, selectionArgs, null, null, null);
    }

    public static class Entry implements BaseColumns {
        public static final String TABLE_NAME = "gpslog";
        public static final String COLUMN_LATITUDE = "latitude";
        public static final String COLUMN_LONGITUDE = "longitude";
        public static final String COLUMN_ALTITUDE = "altitude";
        public static final String COLUMN_SPEED = "speed";
        public static final String COLUMN_TIMESTAMP = "timestamp";
        public static final String COLUMN_TRAINING_ID = "train_id";
    }
}
