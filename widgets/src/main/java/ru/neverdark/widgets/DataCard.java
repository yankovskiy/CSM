package ru.neverdark.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.widget.TextView;

//https://developer.android.com/training/custom-views/create-view.html
public class DataCard extends CardView {
    private TextView mTitleTv;
    private TextView mValueTv;

    public DataCard(Context context) {
        super(context);
        init(null);
    }

    public DataCard(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public DataCard(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        Context context = getContext();
        inflate(context, R.layout.data_card, this);
        mTitleTv = (TextView) findViewById(R.id.title);
        mValueTv = (TextView) findViewById(R.id.value);

        String titleStr = null;
        String valueStr = null;
        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.DataCard,
                    0, 0);

            try {
                titleStr = a.getString(R.styleable.DataCard_title);
                valueStr = a.getString(R.styleable.DataCard_value);
            } finally {
                a.recycle();
            }
        }

        if (titleStr != null) {
            mTitleTv.setText(titleStr);
        }

        if (valueStr != null) {
            mValueTv.setText(valueStr);
        }
    }

    public String getTitle() {
        return mTitleTv.getText().toString();
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

    public void setTitle(String title) {
        mTitleTv.setText(title);
    }

    public void setTitle(int resId) {
        mTitleTv.setText(resId);
    }
}
