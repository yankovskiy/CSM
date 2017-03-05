package ru.neverdark.csm.abs;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import ru.neverdark.csm.db.GpslogTable;
import ru.neverdark.csm.db.SummaryTable;

public abstract class AbsTrackExporter extends AbsExporter {
    public AbsTrackExporter(Context context, SummaryTable.Record summaryRecord, List<GpslogTable.TrackRecord> trackPoints, ExportLisener callback) {
        super(context, summaryRecord, trackPoints, callback);
    }

    protected String getTrackFileName(String extension) throws IOException {
        File dir = new File(Environment.getExternalStorageDirectory(), "tracks");
        if (!dir.exists()) {
            if (!dir.mkdir()) {
               throw new IOException("Could not create directory " + dir.getPath());
            }
        }
        Date date = new Date(getSummaryRecord().finish_date);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone(getSummaryRecord().timezone));
        return dir.getPath() + "/" + sdf.format(date) + "." + extension;
    }
}
