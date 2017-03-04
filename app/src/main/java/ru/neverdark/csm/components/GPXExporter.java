package ru.neverdark.csm.components;

import android.util.Log;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import ru.neverdark.csm.abs.AbsTrackExporter;
import ru.neverdark.csm.db.GpslogTable;
import ru.neverdark.csm.db.SummaryTable;

public class GPXExporter extends AbsTrackExporter {
    private static final String TAG = "GPXExporter";

    public GPXExporter(SummaryTable.Record summaryRecord, List<GpslogTable.TrackRecord> trackPoints, ExportLisener callback) {
        super(summaryRecord, trackPoints, callback);
    }

    @Override
    protected Integer doInBackground(Void... params) {
        int status = FAIL_IMPORT;

        String header = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<gpx\n" +
                "xmlns=\"http://www.topografix.com/GPX/1/1\"\n" +
                "version=\"1.1\"\n" +
                "creator=\"Orendis\"\n" +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">";

        String footer = "</trkseg>\n" +
                "</trk>\n" +
                "</gpx>";

        String begin = "<trk>\n" +
                "<name>" + getSummaryRecord().description + "</name>\n" +
                "<trkseg>";

        try {
            String fileName = getTrackFileName("gpx");
            Log.v(TAG, "doInBackground: fileName = " + fileName);

            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            df.setTimeZone(TimeZone.getTimeZone("GMT"));
            Writer writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(fileName), "utf-8"));

            writer.write(header);
            writer.write(begin);

            for (GpslogTable.TrackRecord record : getTrackPoints()) {
                String beginBlock = String.format(Locale.US, "<trkpt lat=\"%f\" lon=\"%f\">", record.latitude, record.longitude);
                String time = String.format(Locale.US, "<time>%s</time>", df.format(new Date(record.timestamp)));
                String ele = String.format(Locale.US, "<ele>%.2f</ele>", record.altitude);
                String endBlock = "</trkpt>";

                writer.write(beginBlock);
                writer.write(time);
                writer.write(ele);
                writer.write(endBlock);
            }

            writer.write(footer);
            writer.close();
            status = SUCCESS_IMPORT;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return status;
    }
}
