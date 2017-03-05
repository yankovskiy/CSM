package ru.neverdark.csm.components;

import android.content.Context;
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

import ru.neverdark.csm.R;
import ru.neverdark.csm.abs.AbsTrackExporter;
import ru.neverdark.csm.db.GpslogTable;
import ru.neverdark.csm.db.SummaryTable;

public class KMLExporter extends AbsTrackExporter {
    private static final String TAG = "GPXExporter";

    public KMLExporter(Context context, SummaryTable.Record summaryRecord, List<GpslogTable.TrackRecord> trackPoints, ExportLisener callback) {
        super(context, summaryRecord, trackPoints, callback);
    }

    @Override
    protected Integer doInBackground(Void... params) {
        int status = FAIL_IMPORT;

        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd.MM.yyyy", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone(getSummaryRecord().timezone));
        String name = sdf.format(new Date(getSummaryRecord().finish_date));

        String header = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<kml xmlns=\"http://www.opengis.net/kml/2.2\"> <Document>\n" +
                " <name>" + getContext().getString(R.string.app_name) + "</name>\n" +
                " <description>" + getContext().getString(R.string.kml_description) +
                "</description> <Style id=\"redLine\">\n" +
                " <LineStyle>\n" +
                " <color>ff0000ff</color>\n" +
                " <width>4</width>\n" +
                " </LineStyle>\n" +
                " </Style> <Placemark>\n" +
                " <name>" + name + "</name>\n" +
                " <description>" + getSummaryRecord().description + "</description>\n" +
                " <styleUrl>#redLine</styleUrl>\n" +
                " <LineString>\n" +
                " <tessellate>1</tessellate>\n" +
                " <altitudeMode>clampToGround</altitudeMode>\n" +
                " <coordinates>\n";

        String footer = "</coordinates>\n" +
                " </LineString> </Placemark>\n" +
                " </Document> </kml>";

        try {
            String fileName = getTrackFileName("kml");
            Log.v(TAG, "doInBackground: fileName = " + fileName);

            Writer writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(fileName), "utf-8"));

            writer.write(header);

            for (GpslogTable.TrackRecord record : getTrackPoints()) {
                writer.write(String.format(Locale.US, "%f,%f,%d\n", record.longitude, record.latitude, (int)record.altitude));
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
