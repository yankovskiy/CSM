package ru.neverdark.csm.abs;

import android.content.Context;
import android.os.AsyncTask;

import java.util.List;

import ru.neverdark.csm.db.GpslogTable;
import ru.neverdark.csm.db.SummaryTable;

public abstract class AbsExporter extends AsyncTask<Void, Integer, Integer> {
    protected static final int SUCCESS_IMPORT = 0;
    protected static final int FAIL_IMPORT = 1;
    private final SummaryTable.Record mSummaryRecord;
    private final List<GpslogTable.TrackRecord> mTrackPoints;
    private final ExportLisener mCallback;
    private final Context mContext;

    public AbsExporter(Context context, SummaryTable.Record summaryRecord, List<GpslogTable.TrackRecord> trackPoints, ExportLisener callback) {
        mSummaryRecord = summaryRecord;
        mTrackPoints = trackPoints;
        mCallback = callback;
        mContext = context;
    }

    protected List<GpslogTable.TrackRecord> getTrackPoints() {
        return mTrackPoints;
    }

    protected SummaryTable.Record getSummaryRecord() {
        return mSummaryRecord;
    }

    protected ExportLisener getCallback() {
        return mCallback;
    }

    @Override
    protected void onPostExecute(Integer status) {
        if (status == SUCCESS_IMPORT) {
            mCallback.onExportFinishedSuccess();
        } else if (status == FAIL_IMPORT) {
            mCallback.onExportFinishedFail();
        }
    }

    public Context getContext() {
        return mContext;
    }

    public interface ExportLisener {
        void onExportFinishedSuccess();

        void onExportFinishedFail();
    }
}
