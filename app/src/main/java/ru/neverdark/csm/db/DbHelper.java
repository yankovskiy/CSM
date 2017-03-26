package ru.neverdark.csm.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

class DbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "gpsdata";
    private static final int DATABASE_VERSION = 2;

    DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(Schema.Tables.CREATE_SUMMARY);
        db.execSQL(Schema.Tables.CREATE_GPSLOG);
        db.execSQL(Schema.Indices.CREATE_TRAIN_ID_IDX);
    }

    private static final String TAG = "DbHelper";

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.v(TAG, "onUpgrade: old = " + oldVersion + " new = " + newVersion);
        switch (oldVersion) {
            case 1:
                updateDb(db, Schema.Updates.V2);
        }
    }

    private void updateDb(SQLiteDatabase db, String[] queries) {
        Log.v(TAG, "updateDb: ");
        for (int i = 0; i < queries.length; i++) {
            Log.v(TAG, "updateDb: " + queries[i]);
            db.execSQL(queries[i]);
        }
    }
}
