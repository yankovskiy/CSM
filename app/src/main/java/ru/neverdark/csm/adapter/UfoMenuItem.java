package ru.neverdark.csm.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;

/**
 * Created by ufo on 01.03.17.
 */
public class UfoMenuItem {
    private Drawable mMenuIcon;
    private int mMenuLabel;

    public Drawable getMenuIcon() {
        return mMenuIcon;
    }

    public int getMenuLabel() {
        return mMenuLabel;
    }

    public UfoMenuItem(Context context, int iconRes, int stringRes) {
        mMenuLabel = stringRes;
        mMenuIcon = ContextCompat.getDrawable(context, iconRes);
    }
}
