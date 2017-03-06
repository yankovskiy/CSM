package ru.neverdark.csm.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import ru.neverdark.csm.R;
import ru.neverdark.csm.data.GPSData;
import ru.neverdark.csm.db.Db;
import ru.neverdark.csm.db.GpslogTable;
import ru.neverdark.csm.db.SummaryTable;
import ru.neverdark.csm.fragments.ConfirmDialog;
import ru.neverdark.csm.fragments.EditTrainingDialog;
import ru.neverdark.csm.fragments.MainFragment;
import ru.neverdark.csm.utils.Utils;

public class TrainingFinishAcitivty extends AppCompatActivity implements ConfirmDialog.NoticeDialogListener, OnMapReadyCallback, EditTrainingDialog.OnEditTrainingDialogListener {

    private static final String TAG = "TrainingFinishAcitivty";
    private long mTrainingId;

    private TextView mDistanceTv;
    private TextView mTotalTimeTv;
    private TextView mMaxSpeedTv;
    private TextView mAverageSpeedTv;
    private TextView mMaxAltitudeTv;
    private TextView mFinishTimeTv;
    private TextView mDescriptionTv;

    private long mFinishDateInMillis;
    private GoogleMap mGoogleMap;
    private LatLngBounds mBounds;
    private TextView mUpAltitudeTv;
    private TextView mDownAltitudeTv;
    private SummaryTable.Record mSummaryRecord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training_finish);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        GPSData data = (GPSData) intent.getSerializableExtra(MainFragment.TRAINING_DATA);

        Log.v(TAG, "onCreate: " + String.valueOf(data == null));

        mTrainingId = intent.getLongExtra(MainFragment.TRAININD_ID, 0);
        mFinishDateInMillis = intent.getLongExtra(MainFragment.TRAINING_FINISH_DATE, 0);

        // готовим данные для добавления в базу. Поля обозначенные как null и 0 будут заполненны
        // позже по завершению AsyncTask
        mSummaryRecord = new SummaryTable.Record(
                mTrainingId,
                mFinishDateInMillis,
                null, // заметка о тренировке
                true,
                Math.round(data.distance),
                Utils.convertMillisToTime(data.ascend_time + data.descend_time + data.plain_time),
                data.average_speed,
                data.max_speed,
                Math.round(data.up_distance),
                Math.round(data.down_distance),
                (int)Math.round(data.max_altitude),
                (int)Math.round(data.up_altitude),
                (int)Math.round(data.down_altitude),
                TimeZone.getDefault().getID(),
                (int)data.ascend_time,
                (int)data.descend_time,
                (int)data.plain_time,
                data.ascend_average_speed,
                data.ascend_max_speed,
                data.descend_average_speed,
                data.descend_max_speed,
                data.plain_average_speed,
                data.plain_max_speed
        );
        MapView mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(null);
        mapView.getMapAsync(this);
        bindObjects();

        new CollectSummaryTask().execute(mTrainingId);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.finish, menu);
        return true;
    }

    private void bindObjects() {
        mDistanceTv = (TextView) findViewById(R.id.distance_value);
        mTotalTimeTv = (TextView) findViewById(R.id.total_time_value);
        mAverageSpeedTv = (TextView) findViewById(R.id.average_speed_value);
        mMaxSpeedTv = (TextView) findViewById(R.id.max_speed_value);
        mMaxAltitudeTv = (TextView) findViewById(R.id.max_altitude_value);
        mFinishTimeTv = (TextView) findViewById(R.id.finish_time_value);
        mDescriptionTv = (TextView) findViewById(R.id.description);
        mUpAltitudeTv = (TextView) findViewById(R.id.up_altitude_value);
        mDownAltitudeTv = (TextView) findViewById(R.id.down_altitude_value);

        mDescriptionTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openEditTrainingDialog();
            }
        });
    }

    private void saveTrainingResult() {
        Db.getInstance(this).getSummaryTable().updateRecordData(mSummaryRecord);

        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onBackPressed() {
        handleCancelAction();
    }

    private void handleCancelAction() {
        Log.v(TAG, "handleCancelAction: enter");
        ConfirmDialog dialog = ConfirmDialog.getInstance(R.string.title_cancel_training, R.string.message_cancel_training);
        dialog.show(getSupportFragmentManager(), null);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            handleCancelAction();
            return true;
        } else if (id == R.id.action_save_training) {
            saveTrainingResult();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDialogPositiveClick(AppCompatDialogFragment dialog) {
        Intent intent = new Intent();
        intent.putExtra(MainFragment.TRAININD_ID, mTrainingId);
        setResult(RESULT_CANCELED, intent);
        finish();
    }

    @Override
    public void onDialogNegativeClick(AppCompatDialogFragment dialog) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        }
        mGoogleMap = googleMap;
        mGoogleMap.getUiSettings().setMapToolbarEnabled(false);
        mGoogleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                // для запрета клика
            }
        });
    }

    @Override
    public void onAcceptNewDescription(String newDescription) {
        mSummaryRecord.description = newDescription;
        mDescriptionTv.setText(newDescription);
    }

    private class CollectSummaryTask extends AsyncTask<Long, Void, Void> {
        private PolylineOptions rectOptions = new PolylineOptions();

        @Override
        protected Void doInBackground(Long... trainingId) {
            Log.v(TAG, "doInBackground: ");

            Cursor cursor = Db.getInstance(getApplicationContext()).getGpslogTable().getRecordsForTraining(trainingId[0]);

            cursor.moveToFirst();
            int latitudeIndex = cursor.getColumnIndex(GpslogTable.Entry.COLUMN_LATITUDE);
            int longitudeIndex = cursor.getColumnIndex(GpslogTable.Entry.COLUMN_LONGITUDE);

            double latitude;
            double longitude;
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            rectOptions.color(Color.RED).width(4).geodesic(true);
            do {
                latitude = cursor.getDouble(latitudeIndex);
                longitude = cursor.getDouble(longitudeIndex);
                LatLng latLng = new LatLng(latitude, longitude);
                rectOptions.add(latLng);
                builder.include(latLng);
            } while (cursor.moveToNext());
            cursor.close();
            mBounds = builder.build();

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Log.v(TAG, "onPostExecute: ");

            String kmch = getString(R.string.kmch);
            String km = getString(R.string.km);
            String m = getString(R.string.m);

            Date date = new Date(mFinishDateInMillis);
            String finishDateStr = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(date);

            String distanceStr = String.format(Locale.US, "%d %s %d %s", mSummaryRecord.distance / 1000, km, mSummaryRecord.distance % 1000, m);
            String maxSpeedStr = String.format(Locale.US, "%.2f %s", Utils.convertMeterToKm(mSummaryRecord.max_speed), kmch);
            String averageSpeedStr = String.format(Locale.US, "%.2f %s", Utils.convertMeterToKm(mSummaryRecord.average_speed), kmch);
            String maxAltitudeStr = String.format(Locale.US, "%d %s", mSummaryRecord.max_altitude, m);
            String upAltitude = String.format(Locale.US, "%d %s", mSummaryRecord.up_altitude, m);
            String downAltitude = String.format(Locale.US, "%d %s", mSummaryRecord.down_altitude, m);

            mTotalTimeTv.setText(mSummaryRecord.total_time);
            mDistanceTv.setText(distanceStr);
            mMaxSpeedTv.setText(maxSpeedStr);
            mAverageSpeedTv.setText(averageSpeedStr);
            mMaxAltitudeTv.setText(maxAltitudeStr);
            mUpAltitudeTv.setText(upAltitude);
            mDownAltitudeTv.setText(downAltitude);
            mFinishTimeTv.setText(finishDateStr);

            if (mGoogleMap != null) {
                mGoogleMap.addPolyline(rectOptions);
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(mBounds, 0));

                mGoogleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                    @Override
                    public void onMapLoaded() {
                        mGoogleMap.snapshot(new SnapshotMap());
                    }
                });
            } else {
                Toast.makeText(TrainingFinishAcitivty.this, "Map is not ready", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void openEditTrainingDialog() {
        EditTrainingDialog dialog = EditTrainingDialog.getInstance(null);
        dialog.show(getSupportFragmentManager(), null);
    }

    private class SnapshotMap implements GoogleMap.SnapshotReadyCallback {
        @Override
        public void onSnapshotReady(Bitmap bitmap) {
            Log.v(TAG, "onSnapshotReady: ");
            new BitmapSaveTask().execute(bitmap);
        }
    }

    private class BitmapSaveTask extends AsyncTask<Bitmap, Void, Void> {

        @Override
        protected Void doInBackground(Bitmap... bitmaps) {
            Log.v(TAG, "doInBackground: ");
            Bitmap bitmap = bitmaps[0];
            String fileName = Utils.getSnapshotNameById(mTrainingId);
            try {
                FileOutputStream outputStream = openFileOutput(fileName, Context.MODE_PRIVATE);
                bitmap.compress(Bitmap.CompressFormat.PNG, 60, outputStream);
                outputStream.flush();
                outputStream.close();
                Log.v(TAG, "doInBackground: file " + fileName + " saved");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
