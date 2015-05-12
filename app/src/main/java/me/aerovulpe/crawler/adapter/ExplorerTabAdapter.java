package me.aerovulpe.crawler.adapter;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentPagerAdapter;

import me.aerovulpe.crawler.fragments.ExplorerFragment;

/**
 * Created by Aaron on 12/05/2015.
 */
public class ExplorerTabAdapter extends FragmentPagerAdapter {
    private static final String[] mTabNames = {"Tumblr", "Flickr", "Picasa"};

    public ExplorerTabAdapter(FragmentManager fragmentManager) {
        super(fragmentManager);
    }

    @Override
    public Fragment getItem(int position) {
        return ExplorerFragment.newInstance(position, ExplorerFragment.DEFAULT_POSITION);
    }


    @Override
    public int getCount() {
        return mTabNames.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mTabNames[position];
    }
}
