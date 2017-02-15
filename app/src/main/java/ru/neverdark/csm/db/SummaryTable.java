package ru.neverdark.csm.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SummaryTable {
    private final SQLiteDatabase mDb;

    SummaryTable(SQLiteDatabase database) {
        mDb = database;
    }

    public long createTempRecord() {
        ContentValues values = new ContentValues();
        values.put(Entry.COLUMN_IS_COMMITED, 0);
        return mDb.insert(Entry.TABLE_NAME, null, values);
    }

    public void deleteRecord(long recordId) {
        String selection = Entry._ID + " = ?";
        String[] selectionArgs = {String.valueOf(recordId)};
        mDb.delete(Entry.TABLE_NAME, selection, selectionArgs);
    }

    /**
     * Возвращает список всех тренировок
     *
     * @return список тренировок
     */
    public List<Record> getAllRecords() {
        List<Record> records = new ArrayList<>();
        String orderBy = Entry.COLUMN_FINISH_DATE + " desc";
        String where = Entry.COLUMN_IS_COMMITED + " = ?";
        String[] whereArgs = {"1"};

        Cursor c = mDb.query(Entry.TABLE_NAME, Entry.COLUMN_ALL, where, whereArgs, null, null, orderBy);
        if (c.moveToFirst()) {
            int id = c.getColumnIndex(Entry._ID);
            int finish_date = c.getColumnIndex(Entry.COLUMN_FINISH_DATE);
            int description = c.getColumnIndex(Entry.COLUMN_DESCRIPTION);
            int is_commited = c.getColumnIndex(Entry.COLUMN_IS_COMMITED);
            int distance = c.getColumnIndex(Entry.COLUMN_DISTANCE);
            int total_time = c.getColumnIndex(Entry.COLUMN_TOTAL_TIME);
            int average_speed = c.getColumnIndex(Entry.COLUMN_AVERAGE_SPEED);
            int max_speed = c.getColumnIndex(Entry.COLUMN_MAX_SPEED);
            int up_distance = c.getColumnIndex(Entry.COLUMN_UP_DISTANCE);
            int down_distance = c.getColumnIndex(Entry.COLUMN_DOWN_DISTANCE);
            int max_altitude = c.getColumnIndex(Entry.COLUMN_MAX_ALTITUDE);
            int up_altitude = c.getColumnIndex(Entry.COLUMN_UP_ALTITUDE);
            int down_altitude = c.getColumnIndex(Entry.COLUMN_DOWN_ALTITUDE);
            int timezone = c.getColumnIndex(Entry.COLUMN_TIMEZONE);
            int ascend_time = c.getColumnIndex(Entry.COLUMN_ASCEND_TIME);
            int descend_time = c.getColumnIndex(Entry.COLUMN_DESCEND_TIME);
            int plain_time = c.getColumnIndex(Entry.COLUMN_PLAIN_TIME);

            do {
                Record record = new Record(
                        c.getLong(id),
                        c.getLong(finish_date),
                        c.getString(description),
                        c.getInt(is_commited) == 1,
                        c.getInt(distance),
                        c.getString(total_time),
                        c.getFloat(average_speed),
                        c.getFloat(max_speed),
                        c.getInt(up_distance),
                        c.getInt(down_distance),
                        c.getInt(max_altitude),
                        c.getInt(up_altitude),
                        c.getInt(down_altitude),
                        c.getString(timezone),
                        c.getInt(ascend_time),
                        c.getInt(descend_time),
                        c.getInt(plain_time)
                );
                records.add(record);
            } while (c.moveToNext());
        }
        c.close();
        return records;
    }

    /**
     * Обновляет данные о тренировке
     *
     * @param data данные для сохранения тренировки
     */
    public void updateRecordData(Record data) {
        String selection = Entry._ID + " = ?";
        String[] selectionArgs = {String.valueOf(data._id)};
        ContentValues values = new ContentValues();
        values.put(Entry.COLUMN_IS_COMMITED, data.is_commited);
        values.put(Entry.COLUMN_FINISH_DATE, data.finish_date);
        values.put(Entry.COLUMN_DESCRIPTION, data.description);
        values.put(Entry.COLUMN_DISTANCE, data.distance);
        values.put(Entry.COLUMN_TOTAL_TIME, data.total_time);
        values.put(Entry.COLUMN_AVERAGE_SPEED, data.average_speed);
        values.put(Entry.COLUMN_MAX_SPEED, data.max_speed);
        values.put(Entry.COLUMN_UP_DISTANCE, data.up_distance);
        values.put(Entry.COLUMN_DOWN_DISTANCE, data.down_distance);
        values.put(Entry.COLUMN_MAX_ALTITUDE, data.max_altitude);
        values.put(Entry.COLUMN_UP_ALTITUDE, data.up_altitude);
        values.put(Entry.COLUMN_DOWN_ALTITUDE, data.down_altitude);
        values.put(Entry.COLUMN_TIMEZONE, data.timezone);
        values.put(Entry.COLUMN_ASCEND_TIME, data.ascend_time);
        values.put(Entry.COLUMN_DESCEND_TIME, data.descend_time);
        values.put(Entry.COLUMN_PLAIN_TIME, data.plain_time);
        mDb.update(Entry.TABLE_NAME, values, selection, selectionArgs);
    }

    public static class Record implements Serializable {
        public Record(long _id, long finish_date, String description, boolean is_commited,
                      int distance, String total_time, float average_speed, float max_speed,
                      int up_distance, int down_distance, int max_altitude, int up_altitude,
                      int down_altitude, String timezone, int ascend_time, int descend_time,
                      int plain_time) {
            this._id = _id;
            this.finish_date = finish_date;
            this.description = description;
            this.is_commited = is_commited;
            this.distance = distance;
            this.total_time = total_time;
            this.average_speed = average_speed;
            this.max_speed = max_speed;
            this.up_distance = up_distance;
            this.down_distance = down_distance;
            this.max_altitude = max_altitude;
            this.up_altitude = up_altitude;
            this.down_altitude = down_altitude;
            this.timezone = timezone;
            this.ascend_time = ascend_time;
            this.descend_time = descend_time;
            this.plain_time = plain_time;
        }

        public Record() {

        }

        public long _id;
        public long finish_date;
        public String description;
        public boolean is_commited;
        public int distance;
        public String total_time;
        public float average_speed;
        public float max_speed;
        public int up_distance;
        public int down_distance;
        public int max_altitude;
        public int up_altitude;
        public int down_altitude;
        public String timezone;
        public int ascend_time;
        public int descend_time;
        public int plain_time;
    }

    public static class Entry implements BaseColumns {
        public static final String TABLE_NAME = "summary";
        public static final String COLUMN_FINISH_DATE = "finish_date";
        public static final String COLUMN_DESCRIPTION = "description";
        public static final String COLUMN_IS_COMMITED = "is_commited";
        public static final String COLUMN_DISTANCE = "distance";
        public static final String COLUMN_TOTAL_TIME = "total_time";
        public static final String COLUMN_AVERAGE_SPEED = "average_speed";
        public static final String COLUMN_MAX_SPEED = "max_speed";
        public static final String COLUMN_UP_DISTANCE = "up_distance"; // общая дистанция движения в гору
        public static final String COLUMN_DOWN_DISTANCE = "down_distance"; // общая дистанция движения с горы
        public static final String COLUMN_MAX_ALTITUDE = "max_altitude";
        public static final String COLUMN_UP_ALTITUDE = "up_altitude"; // набор высоты
        public static final String COLUMN_DOWN_ALTITUDE = "down_altitude"; // потеря высоты
        public static final String COLUMN_TIMEZONE = "timezone";
        public static final String COLUMN_ASCEND_TIME = "ascend_time";
        public static final String COLUMN_DESCEND_TIME = "descend_time";
        public static final String COLUMN_PLAIN_TIME = "plain_time";

        public static final String[] COLUMN_ALL = {
                _ID,
                COLUMN_FINISH_DATE,
                COLUMN_DESCRIPTION,
                COLUMN_IS_COMMITED,
                COLUMN_DISTANCE,
                COLUMN_TOTAL_TIME,
                COLUMN_AVERAGE_SPEED,
                COLUMN_MAX_SPEED,
                COLUMN_UP_DISTANCE,
                COLUMN_DOWN_DISTANCE,
                COLUMN_MAX_ALTITUDE,
                COLUMN_UP_ALTITUDE,
                COLUMN_DOWN_ALTITUDE,
                COLUMN_TIMEZONE,
                COLUMN_ASCEND_TIME,
                COLUMN_DESCEND_TIME,
                COLUMN_PLAIN_TIME
        };
    }
}
