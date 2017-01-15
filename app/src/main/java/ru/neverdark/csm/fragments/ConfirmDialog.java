package ru.neverdark.csm.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;

import ru.neverdark.csm.R;

public class ConfirmDialog extends AppCompatDialogFragment {
    private int mTitleId;
    private int mMessageId;

    public interface NoticeDialogListener {
        void onDialogPositiveClick(AppCompatDialogFragment dialog);
        void onDialogNegativeClick(AppCompatDialogFragment dialog);
    }

    private NoticeDialogListener mCallback;

    public static ConfirmDialog getInstance(int titleResId, int messageResId) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.mTitleId = titleResId;
        dialog.mMessageId = messageResId;
        return dialog;
    }

    public static ConfirmDialog getInstance(int titleResId, int messageResId, NoticeDialogListener callback) {
        ConfirmDialog dialog = getInstance(titleResId, messageResId);
        dialog.mCallback = callback;
        return dialog;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            // null или нет в зависимости от конструктора
            if (mCallback == null) {
                mCallback = (NoticeDialogListener) context;
            }
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement NoticeDialogListener");

        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(mTitleId)
                .setMessage(mMessageId)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mCallback.onDialogPositiveClick(ConfirmDialog.this);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mCallback.onDialogNegativeClick(ConfirmDialog.this);
                    }
                });

        return builder.create();
    }
}
