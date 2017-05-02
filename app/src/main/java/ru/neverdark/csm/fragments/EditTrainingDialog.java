package ru.neverdark.csm.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

import ru.neverdark.csm.R;
import ru.neverdark.csm.adapter.UfoMenuAdapter;
import ru.neverdark.csm.adapter.UfoMenuItem;
import ru.neverdark.csm.data.ActivityTypes;

public class EditTrainingDialog extends AppCompatDialogFragment {

    private String mDescription;
    private EditText mDescriptionEd;
    private Spinner mActivityTypeSpinner;
    private int mActivityType = 0;

    public static EditTrainingDialog getInstance(String description) {
        EditTrainingDialog dialog = new EditTrainingDialog();
        dialog.mDescription = description;
        return dialog;
    }

    public static EditTrainingDialog getInstance(int activityType, String description) {
        EditTrainingDialog dialog = getInstance(description);
        dialog.mActivityType = activityType;
        return dialog;
    }

    public interface OnEditTrainingDialogListener {
        void onAcceptNewDescription(String newDescription);
        void onAcceptTripEdit(int newActivityType, String newDescription);
    }

    private OnEditTrainingDialogListener mCallback;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            // null или нет в зависимости от конструктора
            if (mCallback == null) {
                mCallback = (OnEditTrainingDialogListener) context;
            }
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnEditTrainingDialogListener");

        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (mActivityType == 0) {
                    mCallback.onAcceptNewDescription(mDescriptionEd.getText().toString());
                } else {
                    mCallback.onAcceptTripEdit(getSelectedActivityType(), mDescriptionEd.getText().toString());
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, null);

        View view = View.inflate(getContext(), R.layout.edit_training_dialog, null);
        builder.setView(view);

        mDescriptionEd = (EditText) view.findViewById(R.id.description);
        mDescriptionEd.setText(mDescription);

        mActivityTypeSpinner = (Spinner) view.findViewById(R.id.activity_type);
        if (mActivityType != 0) {
            initSpinner();
            builder.setTitle(R.string.change);
        } else {
            builder.setTitle(R.string.traning_description);
        }

        return builder.create();
    }

    private static final String TAG = "EditTrainingDialog";

    private void initSpinner() {
        Log.v(TAG, "initSpinner: ");
        Context context = getContext();
        UfoMenuAdapter adapter = new UfoMenuAdapter(context, R.layout.ufo_menu_item);
        adapter.add(new UfoMenuItem(context, R.drawable.ic_walking_gray, R.string.walking, ActivityTypes.WALKING));
        adapter.add(new UfoMenuItem(context, R.drawable.ic_nordic_walking_gray, R.string.nordic_walking, ActivityTypes.NORDIC_WALKING));
        adapter.add(new UfoMenuItem(context, R.drawable.ic_hiking_gray, R.string.hiking, ActivityTypes.HIKING));
        adapter.add(new UfoMenuItem(context, R.drawable.ic_run_gray, R.string.run, ActivityTypes.RUN));
        adapter.add(new UfoMenuItem(context, R.drawable.ic_road_bike_gray, R.string.road_bike, ActivityTypes.ROAD_BIKE));
        adapter.add(new UfoMenuItem(context, R.drawable.ic_mtb_gray, R.string.mtb, ActivityTypes.MTB));
        adapter.add(new UfoMenuItem(context, R.drawable.ic_skateboard_gray, R.string.skateboard, ActivityTypes.SKATEBOARD));
        adapter.setDropDownViewResource(R.layout.ufo_menu_item);

        int position = adapter.getPositionByActivityType(mActivityType);
        mActivityTypeSpinner.setAdapter(adapter);
        mActivityTypeSpinner.setSelection(position);
        mActivityTypeSpinner.setVisibility(View.VISIBLE);
    }

    private int getSelectedActivityType() {
        return (int) mActivityTypeSpinner.getSelectedItemId();
    }
}
