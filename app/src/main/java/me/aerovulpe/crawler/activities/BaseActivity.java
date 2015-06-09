package me.aerovulpe.crawler.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import me.aerovulpe.crawler.CrawlerApplication;
import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.data.CrawlerContract;
import me.aerovulpe.crawler.fragments.AddEditAccountFragment;
import me.aerovulpe.crawler.request.Request;

/**
 * Created by Aaron on 28/02/2015.
 */
public abstract class BaseActivity extends AppCompatActivity {
    private Toolbar mToolbar;

    @Override
    protected void onStart() {
        super.onStart();
        activateToolbar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        CrawlerApplication.initImageLoader(this);
    }

    protected Toolbar activateToolbar() {
        if (mToolbar == null) {
            mToolbar = (Toolbar) findViewById(R.id.app_bar);
            setSupportActionBar(mToolbar);
        }

        return mToolbar;
    }

    protected Toolbar activateToolbarWithHome(boolean isOn) {
        activateToolbar();
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(isOn);
        }
        return mToolbar;
    }

    public void showError(String title, String message, final boolean killActivity) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if (killActivity)
                    finish();
            }
        });
        builder.setMessage(message);
        builder.show();
    }

    protected void showRemoveAccountDialog(final String accountID) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.are_you_sure_delete);
        builder.setNegativeButton(R.string.no, null);
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sendBroadcast(new Intent(Request.buildCancelAction(accountID)));
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Request.removeAlbumRequestData(BaseActivity.this, accountID);
                    }
                }, 5000);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        getContentResolver().delete(CrawlerContract.AccountEntry.CONTENT_URI,
                                CrawlerContract.AccountEntry.COLUMN_ACCOUNT_ID + " == '" +
                                        accountID + "'", null);
                    }
                }).start();
            }
        });
        builder.create().show();
    }

    protected void showEditAccountDialog(int accountType, String id, String name) {
        AddEditAccountFragment dialog = AddEditAccountFragment.newInstance(accountType, id, name);
        dialog.show(getFragmentManager(), "accountEditDialog");
    }

    /**
     * Shows the dialog for adding a new account.
     */
    protected void showAddAccountDialog() {
        AddEditAccountFragment dialog = AddEditAccountFragment.newInstance();
        dialog.show(getFragmentManager(), "accountAddDialog");
    }

}
