package me.aerovulpe.crawler.activities;

import android.app.FragmentManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import me.aerovulpe.crawler.PhotoManagerActivity;
import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.base.BaseActivity;
import me.aerovulpe.crawler.fragments.AlbumListFragment;


public class MainActivity extends BaseActivity implements PhotoManagerActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent = getIntent();
        if (intent.hasExtra(AlbumListFragment.ARG_ACCOUNT_ID)) {
            createAlbumListInstance(intent.getExtras().getString(AlbumListFragment.ARG_ACCOUNT_ID));
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        return super.onOptionsItemSelected(item);
    }

    public void createAlbumListInstance(String accountID) {
        FragmentManager manager = getFragmentManager();
        if (manager.findFragmentByTag(accountID) != null) return;

        AlbumListFragment fragment = AlbumListFragment.newInstance(accountID);
        manager.beginTransaction()
                .add(R.id.container, fragment, accountID)
                .commit();
    }

    @Override
    public void onAlbumListInteraction(Uri uri) {

    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}
