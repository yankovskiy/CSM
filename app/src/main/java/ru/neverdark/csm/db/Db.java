package ru.neverdark.csm.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public class Db {
    private static Db mInstance;
    private GpslogTable mGpslogTable;
    private SummaryTable mSummaryTable;
    private SQLiteDatabase mDatabase;
    private DbHelper mDbHelper;

    public static Db getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new Db();
            mInstance.mDbHelper = new DbHelper(context);
            mInstance.mDatabase = mInstance.mDbHelper.getWritableDatabase();
            mInstance.mGpslogTable = new GpslogTable(mInstance.mDatabase);
            mInstance.mSummaryTable = new SummaryTable(mInstance.mDatabase);
            mInstance.mDatabase.execSQL("PRAGMA foreign_keys = ON");
        }
        return mInstance;
    }

    public GpslogTable getGpslogTable() {
        return mGpslogTable;
    }

    public SummaryTable getSummaryTable() {
        return mSummaryTable;
    }
}
