package ru.neverdark.csm.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;

import ru.neverdark.csm.R;
import ru.neverdark.csm.adapter.UfoMenuAdapter;
import ru.neverdark.csm.adapter.UfoMenuItem;
import ru.neverdark.csm.data.ActivityTypes;

public class ChooseActivityTypeDialog extends AppCompatDialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        Context context = getContext();
        final UfoMenuAdapter adapter = new UfoMenuAdapter(context, R.layout.ufo_menu_item);
        adapter.add(new UfoMenuItem(context, R.drawable.ic_walking_gray, R.string.walking, ActivityTypes.WALKING));
        adapter.add(new UfoMenuItem(context, R.drawable.ic_nordic_walking_gray, R.string.nordic_walking, ActivityTypes.NORDIC_WALKING));
        adapter.add(new UfoMenuItem(context, R.drawable.ic_hiking_gray, R.string.hiking, ActivityTypes.HIKING));
        adapter.add(new UfoMenuItem(context, R.drawable.ic_run_gray, R.string.run, ActivityTypes.RUN));
        adapter.add(new UfoMenuItem(context, R.drawable.ic_road_bike_gray, R.string.road_bike, ActivityTypes.ROAD_BIKE));
        adapter.add(new UfoMenuItem(context, R.drawable.ic_mtb_gray, R.string.mtb, ActivityTypes.MTB));


        builder.setTitle(R.string.choose_activity_type);
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                switch ((int) adapter.getItemId(i)) {
                    case ActivityTypes.WALKING:
                        mCallback.onWalingActivity();
                        break;
                    case ActivityTypes.NORDIC_WALKING:
                        mCallback.onNordikWalkingActivity();
                        break;
                    case ActivityTypes.HIKING:
                        mCallback.onHikingActivity();
                        break;
                    case ActivityTypes.RUN:
                        mCallback.onRunActivity();;
                        break;
                    case ActivityTypes.ROAD_BIKE:
                        mCallback.onRoadBikeActivity();
                        break;
                    case ActivityTypes.MTB:
                        mCallback.onMtbActivity();
                        break;
                }
            }
        });
        return builder.create();
    }

    public interface OnActivityTypeChooseListener {
        void onWalingActivity();
        void onNordikWalkingActivity();
        void onHikingActivity();
        void onRunActivity();
        void onRoadBikeActivity();
        void onMtbActivity();
    }

    private OnActivityTypeChooseListener mCallback;

    public static ChooseActivityTypeDialog getInstance(OnActivityTypeChooseListener callback) {
        ChooseActivityTypeDialog dialog = new ChooseActivityTypeDialog();
        dialog.mCallback = callback;
        return dialog;
    }
}
