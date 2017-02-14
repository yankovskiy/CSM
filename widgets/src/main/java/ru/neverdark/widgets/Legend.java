package ru.neverdark.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import android.widget.TextView;


public class Legend extends RelativeLayout {

    private TextView mColorTv;
    private TextView mTitleTv;
    private TextView mValueTv;

    public Legend(Context context) {
        super(context);
        init(null);
    }

    public Legend(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public Legend(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public Legend(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        Context context = getContext();
        inflate(context, R.layout.legend, this);

        mColorTv = (TextView) findViewById(R.id.color);
        mTitleTv = (TextView) findViewById(R.id.title);
        mValueTv = (TextView) findViewById(R.id.value);

        String percents = null;
        String title = null;
        String value = null;
        int color = -1;

        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.Legend,
                    0, 0);

            try {
                color = a.getColor(R.styleable.Legend_color, -1);
                value = a.getString(R.styleable.Legend_value);
                percents = a.getString(R.styleable.Legend_percents);
                title = a.getString(R.styleable.Legend_title);
            } finally {
                a.recycle();
            }
        }

        if (color != -1) {
            mColorTv.setBackgroundColor(color);
        }

        if (percents != null) {
            mColorTv.setText(percents);
        }

        if (title != null) {
            mTitleTv.setText(title);
        }

        if (value != null) {
            mValueTv.setText(value);
        }
    }

    public String getValue() {
        return mValueTv.getText().toString();
    }

    public void setValue(String value) {
        mValueTv.setText(value);
    }

    public void setValue(int resId) {
        mValueTv.setText(resId);
    }

    public String getTitle() {
        return mTitleTv.getText().toString();
    }

    public void setTitle(String value) {
        mTitleTv.setText(value);
    }

    public void setTitle(int resId) {
        mTitleTv.setText(resId);
    }

    public String getPercents() {
        return mColorTv.getText().toString();
    }

    public void setPercents(String value) {
        mColorTv.setText(value);
    }

    public void setPercents(int resId) {
        mColorTv.setText(resId);
    }

    public void setColor(int colorId) {
        mColorTv.setBackgroundColor(ContextCompat.getColor(getContext(), colorId));
    }

    public int getColor() {
        return mColorTv.getDrawingCacheBackgroundColor();
    }

}
