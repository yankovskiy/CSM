package ru.neverdark.widgets;

import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;


public class Antenna extends LinearLayout {
    private View mLow;
    private View mMedium;
    private View mHigh;
    private int mSignal;
    private Context mContext;

    public static final int SIGNAL_LOW = 0;
    public static final int SIGNAL_LOW_PLUS = 1;
    public static final int SIGNAL_MEDIUM = 2;
    public static final int SIGNAL_MEDIUM_PLUS = 3;
    public static final int SIGNAL_HIGH = 4;
    public static final int SIGNAL_NO = 5;

    private int mNoSignalColor;
    private int mMediumSignalColor;
    private int mHighSignalColor;

    public Antenna(Context context) {
        super(context);
        init();
    }

    public Antenna(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public Antenna(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public Antenna(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }


    private void init() {
        mContext = getContext();
        inflate(mContext, R.layout.antenna, this);
        mLow = findViewById(R.id.low_quality);
        mMedium = findViewById(R.id.medium_quality);
        mHigh = findViewById(R.id.high_quality);

        int color = ContextCompat.getColor(mContext, R.color.textColorTertiary);
        mNoSignalColor = color;
        mMediumSignalColor = ContextCompat.getColor(mContext, R.color.textColorSecondary);
        mHighSignalColor = ContextCompat.getColor(mContext, R.color.textColorPrimary);

        mLow.setBackgroundColor(mNoSignalColor);
        mMedium.setBackgroundColor(mNoSignalColor);
        mHigh.setBackgroundColor(mNoSignalColor);
    }

    public void setSignal(int signal) {
        mSignal = signal;

        switch (mSignal) {
            case SIGNAL_NO:
                mLow.setBackgroundColor(mNoSignalColor);
                mMedium.setBackgroundColor(mNoSignalColor);
                mHigh.setBackgroundColor(mNoSignalColor);
                break;
            case SIGNAL_LOW:
                mLow.setBackgroundColor(mHighSignalColor);
                mMedium.setBackgroundColor(mNoSignalColor);
                mHigh.setBackgroundColor(mNoSignalColor);
                break;
            case SIGNAL_LOW_PLUS:
                mLow.setBackgroundColor(mHighSignalColor);
                mMedium.setBackgroundColor(mMediumSignalColor);
                mHigh.setBackgroundColor(mNoSignalColor);
                break;
            case SIGNAL_MEDIUM:
                mLow.setBackgroundColor(mHighSignalColor);
                mMedium.setBackgroundColor(mHighSignalColor);
                mHigh.setBackgroundColor(mNoSignalColor);
                break;
            case SIGNAL_MEDIUM_PLUS:
                mLow.setBackgroundColor(mHighSignalColor);
                mMedium.setBackgroundColor(mHighSignalColor);
                mHigh.setBackgroundColor(mMediumSignalColor);
                break;
            case SIGNAL_HIGH:
                mLow.setBackgroundColor(mHighSignalColor);
                mMedium.setBackgroundColor(mHighSignalColor);
                mHigh.setBackgroundColor(mHighSignalColor);
                break;
        }
    }

    public int getSignal() {
        return mSignal;
    }
}
