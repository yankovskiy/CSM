package ru.neverdark.csm.activity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import java.util.List;

import ru.neverdark.csm.R;
import ru.neverdark.csm.db.Db;
import ru.neverdark.csm.db.GpslogTable;
import ru.neverdark.csm.db.SummaryTable;
import ru.neverdark.csm.fragments.StatsViewAscendTabFragment;
import ru.neverdark.csm.fragments.StatsViewGraphTabFragment;
import ru.neverdark.csm.fragments.StatsViewInfoTabFragment;
import ru.neverdark.csm.fragments.StatsViewMapTabFragment;
import ru.neverdark.csm.fragments.TrainingStatsFragment;

public class StatsViewActivity extends AppCompatActivity {
    private static final String TAG = "StatsViewActivity";
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
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
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
