package ru.neverdark.csm.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.View;
import android.widget.EditText;

import ru.neverdark.csm.R;

public class EditTrainingDialog extends AppCompatDialogFragment {

    private String mDescription;
    private EditText mDescriptionEd;

    public static EditTrainingDialog getInstance(String description) {
        EditTrainingDialog dialog = new EditTrainingDialog();
        dialog.mDescription = description;
        return dialog;
    }

    public interface OnEditTrainingDialogListener {
        void onAcceptNewDescription(String newDescription);
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
        builder.setTitle(R.string.traning_description);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mCallback.onAcceptNewDescription(mDescriptionEd.getText().toString());
            }
        });
        builder.setNegativeButton(R.string.cancel, null);

        View view = View.inflate(getContext(), R.layout.edit_training_dialog, null);
        builder.setView(view);

        mDescriptionEd = (EditText) view.findViewById(R.id.description);
        mDescriptionEd.setText(mDescription);

        return builder.create();
    }
}
