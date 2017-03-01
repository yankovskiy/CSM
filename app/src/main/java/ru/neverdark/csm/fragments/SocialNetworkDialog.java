package ru.neverdark.csm.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;

import ru.neverdark.csm.R;
import ru.neverdark.csm.adapter.UfoMenuAdapter;
import ru.neverdark.csm.adapter.UfoMenuItem;

public class SocialNetworkDialog extends AppCompatDialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        Context context = getContext();
        final UfoMenuAdapter adapter = new UfoMenuAdapter(context, R.layout.ufo_menu_item);
        adapter.add(new UfoMenuItem(context, R.drawable.vk_24dp, R.string.vk));
        adapter.add(new UfoMenuItem(context, R.drawable.fb_24dp, R.string.facebook));
        adapter.add(new UfoMenuItem(context, R.drawable.gplus_24dp, R.string.gplus));

        builder.setTitle(R.string.social_network);
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                UfoMenuItem item = adapter.getItem(i);
                switch (item.getMenuLabel()) {
                    case R.string.vk:
                        vk();
                        break;
                    case R.string.facebook:
                        fb();
                        break;
                    case R.string.gplus:
                        gp();
                        break;
                }
            }
        });
        return builder.create();
    }

    private void gp() {
        openUrl("https://plus.google.com/communities/104446274470911093776");
    }

    private void fb() {
        openUrl("https://www.facebook.com/groups/orendis");
    }

    private void vk() {
        openUrl("https://vk.com/orendis");
    }

    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }
}
