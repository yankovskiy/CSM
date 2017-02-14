package ru.neverdark.csm.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.provider.BaseColumns;

import java.util.ArrayList;
import java.util.List;

public class GpslogTable {
    private final SQLiteDatabase mDb;

    GpslogTable(SQLiteDatabase database) {
        mDb = database;
    }

    public void saveData(Location location, double distance, long trainingId) {
        long timestamp = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put(Entry.COLUMN_TRAINING_ID, trainingId);
        values.put(Entry.COLUMN_TIMESTAMP, timestamp);
        values.put(Entry.COLUMN_ALTITUDE, location.getAltitude());
        values.put(Entry.COLUMN_LATITUDE, location.getLatitude());
        values.put(Entry.COLUMN_LONGITUDE, location.getLongitude());
        values.put(Entry.COLUMN_SPEED, location.getSpeed());
        values.put(Entry.COLUMN_ACCURACY, location.getAccuracy());
        values.put(Entry.COLUMN_DISTANCE, distance);
        mDb.insert(Entry.TABLE_NAME, null, values);
    }

    public Cursor getRecordsForTraining(long trainId) {
        String[] projection = {Entry.COLUMN_LATITUDE, Entry.COLUMN_LONGITUDE};
        String selection = Entry.COLUMN_TRAINING_ID + " = ?";
        String[] selectionArgs = {String.valueOf(trainId)};

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
        public static final String COLUMN_ACCURACY = "accuracy";
        public static final String COLUMN_DISTANCE = "distance";
    }

    public static class TrackRecord {
        public double latitude;
        public double longitude;
        public double altitude;
        public float speed;
        public long timestamp;
        public double distance;

        public TrackRecord(double latitude, double longitude, double altitude, float speed, long timestamp, double distance) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.altitude = altitude;
            this.speed = speed;
            this.timestamp = timestamp;
            this.distance = distance;
        }

        public TrackRecord() {
        }
    }

    public List<TrackRecord> getTrackPoints(long trainId) {
        List<TrackRecord> records = new ArrayList<>();

        String[] projection = {
                Entry.COLUMN_LATITUDE,
                Entry.COLUMN_LONGITUDE,
                Entry.COLUMN_ALTITUDE,
                Entry.COLUMN_SPEED,
                Entry.COLUMN_TIMESTAMP,
                Entry.COLUMN_DISTANCE
        };
        String selection = Entry.COLUMN_TRAINING_ID + " = ?";
        String[] selectionArgs = {String.valueOf(trainId)};

        Cursor c = mDb.query(Entry.TABLE_NAME, projection, selection, selectionArgs, null, null, null);
        if(c.moveToFirst()) {
            int latitude = c.getColumnIndex(Entry.COLUMN_LATITUDE);
            int longitude = c.getColumnIndex(Entry.COLUMN_LONGITUDE);
            int altitude = c.getColumnIndex(Entry.COLUMN_ALTITUDE);
            int speed = c.getColumnIndex(Entry.COLUMN_SPEED);
            int timestamp = c.getColumnIndex(Entry.COLUMN_TIMESTAMP);
            int distance = c.getColumnIndex(Entry.COLUMN_DISTANCE);

            do {
                TrackRecord record = new TrackRecord(
                        c.getDouble(latitude),
                        c.getDouble(longitude),
                        c.getDouble(altitude),
                        c.getFloat(speed),
                        c.getLong(timestamp),
                        c.getDouble(distance)
                );
                records.add(record);
            } while (c.moveToNext());
        }
        c.close();
        return records;
    }
}
