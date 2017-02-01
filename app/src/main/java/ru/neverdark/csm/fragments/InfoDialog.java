package ru.neverdark.csm.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;

import ru.neverdark.csm.R;

public class InfoDialog extends AppCompatDialogFragment {
    private int mTitleId;
    private int mMessageId;

    public static InfoDialog getInstance(int titleResId, int messageResId) {
        InfoDialog dialog = new InfoDialog();
        dialog.mTitleId = titleResId;
        dialog.mMessageId = messageResId;
        return dialog;
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(mTitleId)
                .setMessage(mMessageId)
                .setNegativeButton(R.string.ok, null);
        return builder.create();
    }
}
