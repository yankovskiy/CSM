package ru.neverdark.csm.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import ru.neverdark.csm.R;

public class UfoMenuAdapter extends ArrayAdapter<UfoMenuItem> {
    private final Context mContext;
    private final int mResource;

    public UfoMenuAdapter(Context context, int resource) {
        this(context, resource, new ArrayList<UfoMenuItem>());
    }

    private UfoMenuAdapter(Context context, int resource, ArrayList<UfoMenuItem> menuItems) {
        super(context, resource, menuItems);
        mContext = context;
        mResource = resource;
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getId();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View row = convertView;

        RowHolder holder = null;

        if (row == null) {
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(mResource, parent, false);
            holder = new RowHolder();
            holder.mMenuIcon = (ImageView) row.findViewById(R.id.ufo_menu_icon);
            holder.mMenuLabel = (TextView) row.findViewById(R.id.ufo_menu_label);
            row.setTag(holder);
        } else {
            holder = (RowHolder) row.getTag();
        }

        UfoMenuItem item = getItem(position);
        holder.mMenuIcon.setImageDrawable(item.getMenuIcon());
        holder.mMenuLabel.setText(item.getMenuLabel());

        return row;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getView(position, convertView, parent);
    }

    public int getPositionByActivityType(int activityType) {
        for (int i = 0; i < getCount(); i++) {
            if (getItemId(i) == activityType) {
                return i;
            }
        }

        return 0;
    }

    private static class RowHolder {
        private ImageView mMenuIcon;
        private TextView mMenuLabel;
    }
}
