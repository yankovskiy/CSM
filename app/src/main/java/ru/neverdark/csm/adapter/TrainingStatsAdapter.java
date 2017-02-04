package ru.neverdark.csm.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ru.neverdark.csm.R;
import ru.neverdark.csm.db.SummaryTable;
import ru.neverdark.csm.utils.Utils;

public class TrainingStatsAdapter extends RecyclerView.Adapter<TrainingStatsAdapter.ViewHolder> implements ItemTouchHelperAdapter {

    private final List<SummaryTable.Record> mValues;
    private final OnListInteractionListener mListener;
    private final Context mContext;
    private final String mKmch;
    private final String mKm;

    public void insertItem(SummaryTable.Record item, int position) {
        mValues.add(position, item);
        notifyItemInserted(position);
    }

    public interface OnListInteractionListener {
        void onClickItem(SummaryTable.Record item);
        void onRemoveItem(SummaryTable.Record item, int position);
    }

    public TrainingStatsAdapter(Context context, List<SummaryTable.Record> items, OnListInteractionListener listener) {
        mValues = items;
        mListener = listener;
        mContext = context;
        mKmch = mContext.getString(R.string.kmch);
        mKm = mContext.getString(R.string.km);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_training_stats_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        SummaryTable.Record record = mValues.get(position);
        holder.mItem = record;

        Date date = new Date(record.finish_date);
        holder.mAverageSpeedTv.setText(String.format(Locale.US, "%.2f %s", record.average_speed * 3.6, mKmch));
        holder.mDistanceTv.setText(String.format(Locale.US, "%.3f %s", (float) record.distance / 1000, mKm));
        holder.mTotalTimeTv.setText(record.total_time);
        holder.mFinishDate.setText(new SimpleDateFormat("EEE, dd.MM.yyyy", Locale.getDefault()).format(date));

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onClickItem(holder.mItem);
                }
            }
        });

        new AsyncSnapshotLoad().execute(holder);
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    @Override
    public void onItemMove(int fromPosition, int toPosition) {
        // список тренировок не подразумевает перемещение элементов
    }

    @Override
    public void onItemDismiss(int position) {
        SummaryTable.Record record = mValues.get(position);

        mValues.remove(position);
        notifyItemRemoved(position);
        mListener.onRemoveItem(record, position);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final View mView;
        private final TextView mDistanceTv;
        private final TextView mTotalTimeTv;
        private final TextView mAverageSpeedTv;
        private final TextView mFinishDate;
        private final ImageView mMapSnapshot;
        private SummaryTable.Record mItem;

        ViewHolder(View view) {
            super(view);
            mView = view;
            mDistanceTv = (TextView) view.findViewById(R.id.distance);
            mTotalTimeTv = (TextView) view.findViewById(R.id.duration);
            mAverageSpeedTv = (TextView) view.findViewById(R.id.average_speed);
            mFinishDate = (TextView) view.findViewById(R.id.finish_date);
            mMapSnapshot = (ImageView) view.findViewById(R.id.map_snapshot);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mDistanceTv.getText() + "'";
        }
    }

    private class AsyncSnapshotLoad extends AsyncTask<ViewHolder, Void, Bitmap>{
        private ViewHolder mHolder;

        @Override
        protected Bitmap doInBackground(ViewHolder... viewHolders) {
            Bitmap snapshot = null;
            mHolder = viewHolders[0];
            String filename = Utils.getSnapshotNameById(mHolder.mItem._id);

            try {
                FileInputStream inputStream = mContext.openFileInput(filename);
                snapshot = BitmapFactory.decodeStream(inputStream);
                inputStream.close();
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }

            return snapshot;
        }

        @Override
        protected void onPostExecute(Bitmap snapshot) {
            if (snapshot != null) {
                mHolder.mMapSnapshot.setImageBitmap(snapshot);
            }
        }
    }
}
