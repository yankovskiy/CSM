package ru.neverdark.csm.abs;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import ru.neverdark.csm.R;
import ru.neverdark.csm.data.GPSData;

public abstract class AbsTabFragment extends Fragment {
    private int mLayoutId;
    private OnTabNaviListener mTabNaviCallback;
    private GPSData mGpsData;

    public AbsTabFragment() {

    }

    protected void setData(int layoutId, OnTabNaviListener listener) {
        mLayoutId = layoutId;
        mTabNaviCallback = listener;
    }

    protected void repeatUpdateAfterResumed(GPSData data) {
        mGpsData = data;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGpsData != null) {
            updateUI(mGpsData);
            mGpsData = null;
        }
    }

    public abstract void updateUI(GPSData data);
    public abstract void resetUI();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(mLayoutId, container, false);
        View prevTab = view.findViewById(R.id.prev_tab);
        View nextTab = view.findViewById(R.id.next_tab);

        OnPrevNextClickListener clickListener = new OnPrevNextClickListener();

        if (prevTab != null) {
            prevTab.setOnClickListener(clickListener);
        }

        if (nextTab != null) {
            nextTab.setOnClickListener(clickListener);
        }

        return view;
    }

    private class OnPrevNextClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.prev_tab:
                    mTabNaviCallback.onPrevTab();
                    break;
                case R.id.next_tab:
                    mTabNaviCallback.onNextTab();
                    break;
            }
        }
    }
}
