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
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
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

import ru.neverdark.csm.R;
import ru.neverdark.csm.data.GPSData;
import ru.neverdark.csm.db.Db;
import ru.neverdark.csm.db.GpslogTable;
import ru.neverdark.csm.db.SummaryTable;
import ru.neverdark.csm.fragments.ConfirmDialog;
import ru.neverdark.csm.fragments.MainFragment;
import ru.neverdark.csm.utils.Utils;

public class TrainingFinishAcitivty extends AppCompatActivity implements ConfirmDialog.NoticeDialogListener, OnMapReadyCallback {

    private static final String TAG = "TrainingFinishAcitivty";
    private GPSData mData;
    private long mTrainingId;

    private TextView mDistanceTv;
    private TextView mTotalTimeTv;
    private TextView mMaxSpeedTv;
    private TextView mAverageSpeedTv;
    private TextView mMaxAltitudeTv;
    private TextView mUpDistanceTv;
    private TextView mDownDistanceTv;
    private TextView mFinishDateTv;
    private EditText mDescriptionEd;

    private long mFinishDateInMillis;
    private String mTotalTimeStr;
    private GoogleMap mGoogleMap;
    private LatLngBounds mBounds;
    private TextView mUpAltitudeTv;
    private TextView mDownAltitudeTv;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training_finish);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        mData = (GPSData) intent.getSerializableExtra(MainFragment.TRAINING_DATA);
        Log.v(TAG, "onCreate: " + String.valueOf(mData == null));

        mTrainingId = intent.getLongExtra(MainFragment.TRAININD_ID, 0);
        mFinishDateInMillis = intent.getLongExtra(MainFragment.TRAINING_FINISH_DATE, 0);
        MapView mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(null);
        mapView.getMapAsync(this);
        bindObjects();

        new CollectSummaryTask().execute(mTrainingId);
    }

    private void bindObjects() {
        mDistanceTv = (TextView) findViewById(R.id.distance);
        mTotalTimeTv = (TextView) findViewById(R.id.total_time);
        mAverageSpeedTv = (TextView) findViewById(R.id.average_speed);
        mMaxSpeedTv = (TextView) findViewById(R.id.max_speed);
        mMaxAltitudeTv = (TextView) findViewById(R.id.max_altitude);
        mUpDistanceTv = (TextView) findViewById(R.id.up_distance);
        mDownDistanceTv = (TextView) findViewById(R.id.down_distance);
        mFinishDateTv = (TextView) findViewById(R.id.finish_date);
        mDescriptionEd = (EditText) findViewById(R.id.description);
        mUpAltitudeTv = (TextView) findViewById(R.id.up_altitude);
        mDownAltitudeTv = (TextView) findViewById(R.id.down_altitude);
    }

    public void onButtonClick(View view) {
        if (view.getId() == R.id.training_done_button) {
            saveTrainingResult();
        }
    }

    private void saveTrainingResult() {
        Db.getInstance(this).getSummaryTable().updateRecordData(new SummaryTable.Record(
                mTrainingId,
                mFinishDateInMillis,
                mDescriptionEd.getText().toString(),
                true,
                mData.distance,
                mTotalTimeStr,
                mData.average_speed,
                mData.max_speed,
                mData.up_distance,
                mData.down_distance,
                mData.max_altitude,
                mData.up_altitude,
                mData.down_altitude
        ));

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

    private class CollectSummaryTask extends AsyncTask<Long, Void, Integer> {
        static final int SUCCESS_STATUS = 0;
        static final int FAIL_STATUS = 1;
        private PolylineOptions rectOptions = new PolylineOptions();
        private long mTimeDiff;

        @Override
        protected Integer doInBackground(Long... trainingId) {
            Cursor cursor = Db.getInstance(getApplicationContext()).getGpslogTable().getRecordsForTraining(trainingId[0]);
            if (cursor.getCount() > 1) {
                cursor.moveToFirst();
                long startTime = cursor.getLong(cursor.getColumnIndex(GpslogTable.Entry.COLUMN_TIMESTAMP));
                double latitude;
                double longitude;
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                rectOptions.color(Color.RED).width(4).geodesic(true);
                do {
                    latitude = cursor.getDouble(cursor.getColumnIndex(GpslogTable.Entry.COLUMN_LATITUDE));
                    longitude = cursor.getDouble(cursor.getColumnIndex(GpslogTable.Entry.COLUMN_LONGITUDE));
                    LatLng latLng = new LatLng(latitude, longitude);
                    rectOptions.add(latLng);
                    builder.include(latLng);
                } while (cursor.moveToNext());
                mBounds = builder.build();
                cursor.moveToLast();
                long endTime = cursor.getLong(cursor.getColumnIndex(GpslogTable.Entry.COLUMN_TIMESTAMP));
                cursor.close();
                mTimeDiff = endTime - startTime;
            } else {
                cursor.close();
                return FAIL_STATUS;
            }

            return SUCCESS_STATUS;
        }

        @Override
        protected void onPostExecute(Integer status) {
            if (status == FAIL_STATUS) {
                // TODO сообщить об ошибке
                Log.v(TAG, "onPostExecute: error");
                mGoogleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                    @Override
                    public void onMapLoaded() {
                        mGoogleMap.snapshot(new SnapshotMap());
                    }
                });
            } else {
                // TODO показать основной интерфейс
                Log.v(TAG, "onPostExecute: success");

                int milliseconds = (int) (mTimeDiff % 1000);
                int seconds = (int) (mTimeDiff / 1000) % 60 ;
                int minutes = (int) ((mTimeDiff / (1000 * 60)) % 60);
                int hours   = (int) (mTimeDiff / (1000 * 60 * 60));

                mTotalTimeStr = String.format(Locale.US, "%02d:%02d:%02d.%03d", hours, minutes, seconds, milliseconds);

                Date date = new Date(mFinishDateInMillis);
                String strDateTime = new SimpleDateFormat("EEE, MMM d, ''yy'T'HH:mm:ss", Locale.getDefault()).format(date);

                String distanceStr = String.format(Locale.US, "%s: %d km %d m", getString(R.string.distance), mData.distance / 1000, mData.distance % 1000);
                String maxSpeedStr = String.format(Locale.US, "%s: %.2f", getString(R.string.max_speed), mData.max_speed * 3.6);
                String averageSpeedStr = String.format(Locale.US, "%s: %.2f", getString(R.string.average_speed), mData.average_speed * 3.6);
                String maxAltitudeStr = String.format(Locale.US, "%s: %d", getString(R.string.max_altitude), mData.max_altitude);
                String upDistanceStr = String.format(Locale.US, "%s: %d km %d m", getString(R.string.up_distance), (mData.up_distance / 1000), (mData.up_distance % 1000));
                String downDistanceStr = String.format(Locale.US, "%s: %d km %d m", getString(R.string.down_distance), (mData.down_distance / 1000), (mData.down_distance % 1000));
                String totalTimeStr = String.format(Locale.US, "%s: %s", getString(R.string.total_time), mTotalTimeStr);
                String finishDateStr = String.format(Locale.US, "%s: %s", getString(R.string.finish_date), strDateTime);
                String upAltitude = String.format(Locale.US, "%s: %d m", getString(R.string.up_altitude), mData.up_altitude);
                String downAltitude = String.format(Locale.US, "%s: %d m", getString(R.string.down_altitude), mData.down_altitude);

                mTotalTimeTv.setText(totalTimeStr);
                mDistanceTv.setText(distanceStr);
                mMaxSpeedTv.setText(maxSpeedStr);
                mAverageSpeedTv.setText(averageSpeedStr);
                mMaxAltitudeTv.setText(maxAltitudeStr);
                mUpDistanceTv.setText(upDistanceStr);
                mDownDistanceTv.setText(downDistanceStr);
                mUpAltitudeTv.setText(upAltitude);
                mDownAltitudeTv.setText(downAltitude);

                mFinishDateTv.setText(finishDateStr);

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
