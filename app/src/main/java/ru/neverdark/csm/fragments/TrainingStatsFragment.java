package ru.neverdark.csm.fragments;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Locale;

import ru.neverdark.csm.R;
import ru.neverdark.csm.adapter.TrainingStatsAdapter;
import ru.neverdark.csm.adapter.TrainingStatsItemTouchHelperCallback;
import ru.neverdark.csm.db.Db;
import ru.neverdark.csm.db.SummaryTable;
import ru.neverdark.csm.utils.Utils;

public class TrainingStatsFragment extends Fragment implements TrainingStatsAdapter.OnListInteractionListener {
    private RecyclerView mRecyclerView;
    private ProgressBar mProgressBar;
    private View mView;
    private TrainingStatsAdapter mAdapter;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public TrainingStatsFragment() {
    }

    public static TrainingStatsFragment newInstance() {
        TrainingStatsFragment fragment = new TrainingStatsFragment();
        return fragment;
    }

    @Override
    public void onClickItem(SummaryTable.Record item) {

    }

    @Override
    public void onRemoveItem(SummaryTable.Record item, int position) {
        Snackbar mySnackBar = Snackbar.make(mView, R.string.trip_removed, Snackbar.LENGTH_LONG);
        mySnackBar.setAction(R.string.cancel, new SnackClickListener(item, position));
        mySnackBar.addCallback(new SnackCallback(getContext(), item._id));
        mySnackBar.show();
    }

    private class AsyncStatsLoader extends AsyncTask<Void, Void, Void> {
        private List<SummaryTable.Record> mData;

        @Override
        protected Void doInBackground(Void... params) {
            mData = Db.getInstance(getContext()).getSummaryTable().getAllRecords();
            return null;
        }

        @Override
        protected void onPreExecute() {
            mProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mAdapter = new TrainingStatsAdapter(getContext(), mData, TrainingStatsFragment.this);
            ItemTouchHelper.Callback callback = new TrainingStatsItemTouchHelperCallback(mAdapter);
            ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
            mRecyclerView.setAdapter(mAdapter);
            touchHelper.attachToRecyclerView(mRecyclerView);
            mProgressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_training_stats, container, false);

        mRecyclerView = (RecyclerView) mView.findViewById(R.id.list);
        mProgressBar = (ProgressBar) mView.findViewById(R.id.progressBar);
        asyncLoadData();

        return mView;
    }

    private void asyncLoadData() {
        new AsyncStatsLoader().execute();
    }

    private class SnackClickListener implements View.OnClickListener {
        private final SummaryTable.Record mItem;
        private final int mPosition;

        public SnackClickListener(SummaryTable.Record item, int position) {
            this.mItem = item;
            this.mPosition = position;
        }

        @Override
        public void onClick(View view) {
            mAdapter.insertItem(mItem, mPosition);
        }
    }

    private static class SnackCallback extends BaseTransientBottomBar.BaseCallback<Snackbar> {
        private final long mRecordId;
        private final Context mContext;

        public SnackCallback(Context context, long id) {
            mContext = context;
            mRecordId = id;
        }

        @Override
        public void onDismissed(Snackbar transientBottomBar, int event) {
            if (event != BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_ACTION) {
                String fileName = Utils.getSnapshotNameById(mRecordId);
                File file = new File(mContext.getFilesDir(), fileName);
                if (file.exists()) {
                    file.delete();
                }
                Db.getInstance(mContext).getSummaryTable().deleteRecord(mRecordId);
            }
            super.onDismissed(transientBottomBar, event);
        }
    }
}
