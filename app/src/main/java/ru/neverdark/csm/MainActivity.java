package ru.neverdark.csm;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.List;

import ru.neverdark.csm.activity.SettingsActivity;
import ru.neverdark.csm.components.TrackerService;
import ru.neverdark.csm.fragments.MainFragment;
import ru.neverdark.csm.fragments.SocialNetworkDialog;
import ru.neverdark.csm.fragments.TrainingStatsFragment;
import ru.neverdark.csm.utils.Constants;
import ru.neverdark.csm.utils.Utils;
import ru.neverdark.widgets.Antenna;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, MainFragment.OnFragmentInteractionListener {

    public static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_BACKUP_DATABASE = 1;
    private static final int PERMISSION_REQUEST_RESTORE_DATABASE = 2;

    private MainFragment mMainFragment;
    private ActionBarDrawerToggle mToggle;
    private DrawerLayout mDrawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        mToggle = new ActionBarDrawerToggle(
                this, mDrawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawer.setDrawerListener(mToggle);
        mToggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        if (BuildConfig.DEBUG) {
            navigationView.getMenu().findItem(R.id.nav_other).setVisible(true);
        }

        /* Intent будет содержать значение TRACKER_SERVICE_STARTED (true) если мы пришли из уведомления */
        Intent intent = getIntent();
        boolean mIsServiceRunning = intent.getBooleanExtra(TrackerService.TRACKER_SERVICE_STARTED, false);

        /* Если мы пришли из уведомления, то сервис однозначно запущен и дополнительная проверка не нужна */
        if (!mIsServiceRunning) {
            mIsServiceRunning = Utils.isServiceRunning(this, TrackerService.class);
            Log.v(TAG, "onCreate: service started " + mIsServiceRunning);
        }

        mMainFragment = MainFragment.newInstance(mIsServiceRunning);
        getSupportFragmentManager().beginTransaction().add(R.id.main_content_fragment, mMainFragment).commit();

        setDrawerDisabledState(mIsServiceRunning);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MainFragment.REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                Log.v(TAG, "onActivityResult: result_ok");
                mMainFragment.startGeoClient();
            } else if (resultCode == RESULT_CANCELED) {
                Log.v(TAG, "onActivityResult: result_canceled");
                // TODO отобразить информационное сообщение о необходимости включить GPS
            }
        } else if (requestCode == MainFragment.REQUETST_CHECK_SETTINGS_FOR_SERVICE) {
            if (resultCode == RESULT_OK) {
                Log.v(TAG, "onActivityResult: for servie ok");
                mMainFragment.startGeoClient();
                mMainFragment.checkAccuracyAndRunTracker();
            } else if (resultCode == RESULT_CANCELED) {
                Log.v(TAG, "onActivityResult: for service canceled");
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onBackPressed() {

        if (mDrawer.isDrawerOpen(GravityCompat.START)) {
            mDrawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (!item.isChecked()) {
            Log.v(TAG, "onNavigationItemSelected: ");
            if (id == R.id.nav_training) {
                mMainFragment = MainFragment.newInstance(false);
                getSupportFragmentManager().beginTransaction().replace(R.id.main_content_fragment, mMainFragment).commit();
            } else if (id == R.id.nav_stats) {
                TrainingStatsFragment fragment = TrainingStatsFragment.newInstance();
                getSupportFragmentManager().beginTransaction().replace(R.id.main_content_fragment, fragment).commit();
            } else if (id == R.id.nav_backup_db) {
                backupDatabase();
            } else if (id == R.id.nav_settings) {
                openSettings();
            } else if (id == R.id.nav_rate_app) {
                rateApp();
            } else if (id == R.id.nav_social_network) {
                showSocialNetworkDialog();
            } else if (id == R.id.nav_send_mail) {
                sendMail();
            } else if (id == R.id.nav_restore_db) {
                restoreDatabase();
            }
        }
        mDrawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void sendMail() {
        Intent mailto = new Intent(Intent.ACTION_SEND);
        mailto.setType("plain/text");
        mailto.putExtra(Intent.EXTRA_EMAIL, new String[] { getString(R.string.pref_email) });
        mailto.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
        startActivity(Intent.createChooser(mailto, getString(R.string.pref_selectEmailApplication)));
    }

    private void showSocialNetworkDialog() {
        SocialNetworkDialog dialog = new SocialNetworkDialog();
        dialog.show(getSupportFragmentManager(), null);
    }

    private void rateApp() {
        Intent marketIntent = new Intent(Intent.ACTION_VIEW);
        marketIntent.setData(Uri.parse("market://details?id=" + Constants.PACKAGE_NAME));
        startActivity(marketIntent);
    }

    private void restoreDatabase() {
        if (Utils.isExternalStorageWritable()) {
            if (checkAndRequirePermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSION_REQUEST_RESTORE_DATABASE)) {
                new RestoreDBTask().execute("gpsdata");
            }
        } else {
            Toast.makeText(this, R.string.external_storage_not_available, Toast.LENGTH_LONG).show();
        }
    }

    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void backupDatabase() {
        if (Utils.isExternalStorageWritable()) {
            if (checkAndRequirePermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSION_REQUEST_BACKUP_DATABASE)) {
                new BackupDBTask().execute("gpsdata");
            }
        } else {
            Toast.makeText(this, R.string.external_storage_not_available, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_BACKUP_DATABASE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                backupDatabase();
            }
        } else if (requestCode == PERMISSION_REQUEST_RESTORE_DATABASE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                restoreDatabase();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
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

    private void setDrawerDisabledState(boolean isDisabled) {
        if (isDisabled) {
            setTitle("00:00:00");
            mToggle.setDrawerIndicatorEnabled(false);
            mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        } else {
            setTitle(R.string.app_name);
            mToggle.setDrawerIndicatorEnabled(true);
            mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        }
    }

    @Override
    public void stopTrackerService() {
        stopService(new Intent(this, TrackerService.class));
        mBound = false;
        setDrawerDisabledState(false);
    }

    @Override
    public void startTrackerService() {
        startService(new Intent(this, TrackerService.class));
        setDrawerDisabledState(true);
    }

    @Override
    public List<LatLng> getLatLngList() {
        if (mBound) {
            return mService.getLatLngList();
        } else {
            return null;
        }
    }

    private boolean mBound;
    private TrackerService mService;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            Log.v(TAG, "onServiceConnected: ");
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            TrackerService.LocalBinder binder = (TrackerService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;

            mMainFragment.onTrackerServiceConnected();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.v(TAG, "onServiceDisconnected: ");
            mBound = false;
        }
    };

    @Override
    public void bindTrackerService() {
        if (!mBound) {
            Intent intent = new Intent(this, TrackerService.class);
            boolean bind = bindService(intent, mConnection, 0);
            Log.v(TAG, "bindTrackerService: " + bind);
        } else {
            Log.v(TAG, "bindTrackerService: already bind");
        }
    }

    @Override
    public void unbindTrackerService() {
        Log.v(TAG, "unbindTrackerService: ");
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    private class RestoreDBTask extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... params) {
            return importDatabase(params[0]);
        }

        @Override
        protected void onPostExecute(Boolean status) {
            if (!status) {
                Toast.makeText(MainActivity.this, R.string.restore_database_fail, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(MainActivity.this, R.string.restoure_database_success, Toast.LENGTH_LONG).show();
            }
        }

        private boolean importDatabase(String databaseName) {
            boolean status = false;
            try {
                String backupDBPath = "logs.db";
                File currentDB = getDatabasePath(databaseName);
                File mBackupDbFile = new File(getExternalFilesDir(null), backupDBPath);

                if (mBackupDbFile.exists()) {
                    if (currentDB.exists()) {
                        currentDB.delete();
                    }

                    Log.v(TAG, "importDatabase: " + currentDB.getAbsolutePath());
                    FileChannel src = new FileInputStream(mBackupDbFile).getChannel();
                    FileChannel dst = new FileOutputStream(currentDB).getChannel();
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();
                    status = true;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            return status;
        }
    }

    private class BackupDBTask extends AsyncTask<String, Void, Boolean> {

        private File mBackupDbFile;

        @Override
        protected Boolean doInBackground(String... params) {
            return exportDatabse(params[0]);
        }

        @Override
        protected void onPostExecute(Boolean status) {
            if (status) {
                if (mBackupDbFile.exists()) {
                    Uri path = Uri.fromFile(mBackupDbFile);
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    String to[] = {"artemyankovskiy@gmail.com"};
                    intent.setType("application/x-sqlite3");
                    intent.putExtra(Intent.EXTRA_EMAIL, to);
                    intent.putExtra(Intent.EXTRA_STREAM, path);
                    intent.putExtra(Intent.EXTRA_SUBJECT, "Database for analyse");
                    startActivity(Intent.createChooser(intent, "Send email..."));
                } else {
                    status = false;
                }
            }

            if (!status) {
                Toast.makeText(MainActivity.this, R.string.backup_database_fail, Toast.LENGTH_LONG).show();
            }
        }

        private boolean exportDatabse(String databaseName) {
            boolean status = false;
            try {
                String backupDBPath = "logs.db";
                File currentDB = getDatabasePath(databaseName);
                mBackupDbFile = new File(getExternalFilesDir(null), backupDBPath);

                if (currentDB.exists()) {
                    if (mBackupDbFile.exists()) {
                        mBackupDbFile.delete();
                    }
                    FileChannel src = new FileInputStream(currentDB).getChannel();
                    FileChannel dst = new FileOutputStream(mBackupDbFile).getChannel();
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();
                    status = true;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            return status;
        }
    }
}
