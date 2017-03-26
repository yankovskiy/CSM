package ru.neverdark.csm.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.List;

import ru.neverdark.csm.R;
import ru.neverdark.csm.abs.AbsExporter;
import ru.neverdark.csm.components.GPXExporter;
import ru.neverdark.csm.components.KMLExporter;
import ru.neverdark.csm.data.ActivityTypes;
import ru.neverdark.csm.db.Db;
import ru.neverdark.csm.db.GpslogTable;
import ru.neverdark.csm.db.SummaryTable;
import ru.neverdark.csm.fragments.EditTrainingDialog;
import ru.neverdark.csm.fragments.StatsViewAscendTabFragment;
import ru.neverdark.csm.fragments.StatsViewGraphTabFragment;
import ru.neverdark.csm.fragments.StatsViewInfoTabFragment;
import ru.neverdark.csm.fragments.StatsViewMapTabFragment;
import ru.neverdark.csm.fragments.TrainingStatsFragment;
import ru.neverdark.csm.utils.Utils;

public class StatsViewActivity extends AppCompatActivity implements AbsExporter.ExportLisener, EditTrainingDialog.OnEditTrainingDialogListener, StatsViewInfoTabFragment.OnDescriptionListener {
    public static final int TRAINING_DESCRIPTION_CHANGED = RESULT_FIRST_USER + 1;
    private static final String TAG = "StatsViewActivity";
    private static final int PERMISSION_REQUEST_EXPORT_GPX = 1;
    public static final String DESCRIPTION = "description";
    private static final int PERMISSION_REQUEST_EXPORT_KML = 2;
    public static final String ACTIVITY_TYPE = "activity_type";
    private SummaryTable.Record mSummaryRecord;
    private List<GpslogTable.TrackRecord> mTrackPoints;
    private ViewPager mViewPager;
    private ProgressBar mProgressBar;
    private StatsViewInfoTabFragment mInfoTabFrag;
    private StatsViewMapTabFragment mMapTabFrag;
    private StatsViewGraphTabFragment mGraphTabFrag;
    private StatsViewAscendTabFragment mAscendTabFrag;
    private boolean mIsLoaded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats_view);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mSummaryRecord = (SummaryTable.Record) getIntent().getSerializableExtra(TrainingStatsFragment.STATS_DATA);

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mViewPager = (ViewPager) findViewById(R.id.container);

        new DataLoader().execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.stats_view_common, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_export_gpx:
                exportToGpx();
                break;
            case R.id.action_export_kml:
                exportToKml();
                break;
            case R.id.action_edit_training:
                openEditTrainingDialog(true);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void exportToKml() {
        if (Utils.isExternalStorageWritable()) {
            if (checkAndRequirePermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSION_REQUEST_EXPORT_KML)) {
                new KMLExporter(this, mSummaryRecord, mTrackPoints, this).execute();
            }
        } else {
            Toast.makeText(this, R.string.external_storage_not_available, Toast.LENGTH_LONG).show();
        }
    }

    private void exportToGpx() {
        if (Utils.isExternalStorageWritable()) {
            if (checkAndRequirePermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSION_REQUEST_EXPORT_GPX)) {
                new GPXExporter(this, mSummaryRecord, mTrackPoints, this).execute();
            }
        } else {
            Toast.makeText(this, R.string.external_storage_not_available, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_EXPORT_GPX && isGrantPerms(grantResults)) {
            exportToGpx();
        } else if (requestCode == PERMISSION_REQUEST_EXPORT_KML && isGrantPerms(grantResults)) {
            exportToKml();
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private boolean isGrantPerms(@NonNull int[] grantResults) {
        return grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Проверяет наличие прав и в случае необходимости запрашивает их
     *
     * @param permission  права доступа для проверки и запроса разрешения
     * @param requestCode код запроса прав доступа для обработки onRequestPermissionsResult
     * @return true если права доступа есть
     */
    private boolean checkAndRequirePermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
            return false;
        }
    }

    @Override
    public void onExportFinishedSuccess() {
        Toast.makeText(this, R.string.track_export_success, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onExportFinishedFail() {
        Toast.makeText(this, R.string.track_export_fail, Toast.LENGTH_LONG).show();
    }

    private void openEditTrainingDialog(boolean fullEdit) {
        EditTrainingDialog dialog;
        if (fullEdit) {
            dialog = EditTrainingDialog.getInstance(mSummaryRecord.activity_type, mSummaryRecord.description);
        } else {
            dialog = EditTrainingDialog.getInstance(mSummaryRecord.description);
        }
        dialog.show(getSupportFragmentManager(), null);
    }

    @Override
    public void onAcceptNewDescription(String newDescription) {
        onAcceptTripEdit(ActivityTypes.UNKNOWN, newDescription);
    }

    @Override
    public void onAcceptTripEdit(int newActivityType, String newDescription) {
        updateUI(newActivityType, newDescription);
        updateDataInDb(newActivityType, newDescription);
        notifyParent(newActivityType, newDescription);
    }

    private void updateUI(int activityType, String newDescription) {
        if (activityType != ActivityTypes.UNKNOWN) {
            mInfoTabFrag.updateActivityType(activityType);
        }
        mInfoTabFrag.updateDesription(newDescription);
    }

    private void updateDataInDb(int activityType, String newDescription) {
        if (activityType != ActivityTypes.UNKNOWN) {
            mSummaryRecord.activity_type = activityType;
        }

        mSummaryRecord.description = newDescription;
        Db.getInstance(this).getSummaryTable().updateRecordData(mSummaryRecord);
    }

    private void notifyParent(int activityType, String newDescription) {
        Intent data = new Intent();
        data.putExtra(DESCRIPTION, newDescription);
        data.putExtra(ACTIVITY_TYPE, activityType);
        setResult(TRAINING_DESCRIPTION_CHANGED, data);
    }

    @Override
    public void onDescriptionClick() {
        openEditTrainingDialog(false);
    }

    private enum TABS {
        INFO(R.drawable.ic_tab_info),
        MAP(R.drawable.ic_tab_map),
        GRAPH(R.drawable.ic_tab_graph),
        ASCEND(R.drawable.ic_tab_ascend);

        private final int mIconRes;

        TABS(int iconRes) {
            mIconRes = iconRes;
        }

        public int getIcon() {
            return mIconRes;
        }
    }

    private class DataLoader extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            mIsLoaded = false;
            mViewPager.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... params) {
            mTrackPoints = Db.getInstance(StatsViewActivity.this).getGpslogTable().getTrackPoints(mSummaryRecord._id);
            mAscendTabFrag = StatsViewAscendTabFragment.getInstance(mSummaryRecord);
            mGraphTabFrag = StatsViewGraphTabFragment.getInstance(getApplicationContext(), mTrackPoints);
            mMapTabFrag = StatsViewMapTabFragment.getInstance(mTrackPoints);
            mInfoTabFrag = StatsViewInfoTabFragment.getInstance(mSummaryRecord);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Log.v(TAG, "onPostExecute: ");
            int tabsCount = TABS.values().length;

            mViewPager.setOffscreenPageLimit(tabsCount - 1);
            mViewPager.setAdapter(new CustomAdapter(getSupportFragmentManager()));

            TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
            tabLayout.setupWithViewPager(mViewPager);

            for (int i = 0; i < tabsCount; i++) {
                tabLayout.getTabAt(i).setIcon(TABS.values()[i].getIcon());
            }

            mProgressBar.setVisibility(View.GONE);
            mViewPager.setVisibility(View.VISIBLE);
            mIsLoaded = true;
        }
    }

    private class CustomAdapter extends FragmentPagerAdapter {
        CustomAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            TABS tab = TABS.values()[position];

            if (tab == TABS.MAP) {
                return mMapTabFrag;
            } else if (tab == TABS.INFO) {
                return mInfoTabFrag;
            } else if (tab == TABS.GRAPH) {
                return mGraphTabFrag;
            } else if (tab == TABS.ASCEND) {
                return mAscendTabFrag;
            }

            return null;
        }

        @Override
        public int getCount() {
            return TABS.values().length;
        }
    }
}
