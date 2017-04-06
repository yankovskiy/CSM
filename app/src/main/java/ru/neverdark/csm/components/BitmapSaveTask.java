package ru.neverdark.csm.components;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import ru.neverdark.csm.utils.Utils;

/**
 * Created by ufo on 05.04.17.
 */

public class BitmapSaveTask extends AsyncTask<Bitmap, Void, Integer> {
    private static final int SUCCESS_SAVE = 0;
    private static final int FAIL_SAVE = 1;
    private final Context mContext;
    private final OnSaveBitmapListener mCallback;
    private final long mRecordId;

    public BitmapSaveTask(Context context, long recordId, OnSaveBitmapListener callback) {
        mCallback = callback;
        mRecordId = recordId;
        mContext = context;
    }

    @Override
    protected Integer doInBackground(Bitmap... bitmaps) {
        Bitmap bitmap = bitmaps[0];
        String fileName = Utils.getSnapshotNameById(mRecordId);
        File file = new File(mContext.getFilesDir(), fileName);
        if (file.exists()) {
            file.delete();
        }

        int status = FAIL_SAVE;

        try {
            FileOutputStream outputStream = mContext.openFileOutput(fileName, Context.MODE_PRIVATE);
            bitmap.compress(Bitmap.CompressFormat.PNG, 60, outputStream);
            outputStream.flush();
            outputStream.close();
            status = SUCCESS_SAVE;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return status;
    }

    @Override
    protected void onPreExecute() {
        if (mCallback != null) {
            mCallback.onPreExecute();
        }
    }

    @Override
    protected void onPostExecute(Integer status) {
        if (mCallback != null) {
            mCallback.onPostExecute();

            if (status == SUCCESS_SAVE) {
                mCallback.onSaveSuccess();
            } else {
                mCallback.onSaveFail();
            }
        }
    }
    public interface OnSaveBitmapListener {
        void onSaveSuccess();

        void onSaveFail();

        void onPreExecute();

        void onPostExecute();
    }
}
