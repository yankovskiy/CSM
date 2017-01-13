package ru.neverdark.csm.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class DbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "gpsdata";
    private static final int DATABASE_VERSION = 1;

    DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(Schema.Tables.CREATE_SUMMARY);
        db.execSQL(Schema.Tables.CREATE_GPSLOG);
        db.execSQL(Schema.Indices.CREATE_TRAIN_ID_IDX);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(Schema.Indices.DROP_TRAIN_ID_IDX);
        db.execSQL(Schema.Tables.DROP_GPSLOG);
        db.execSQL(Schema.Tables.DROP_SUMMARY);
        onCreate(db);
    }
}
