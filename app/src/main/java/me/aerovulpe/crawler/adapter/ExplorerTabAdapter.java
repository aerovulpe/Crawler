package me.aerovulpe.crawler.adapter;

import android.app.Fragment;
import android.app.FragmentManager;

import me.aerovulpe.crawler.fragments.ExplorerFragment;

/**
 * Created by Aaron on 12/05/2015.
 */
public class ExplorerTabAdapter extends SmartFragmentStatePagerAdapter {
    private static final String[] mTabNames = {"Tumblr", "Flickr", "Picasa"};
    private static final String[] mCategories = {"accessories", "Flickr", "Picasa"};

    public ExplorerTabAdapter(FragmentManager fragmentManager) {
        super(fragmentManager);
    }

    @Override
    public Fragment getItem(int position) {
        return ExplorerFragment.newInstance(position, mCategories[position]);
    }


    @Override
    public int getCount() {
        return mCategories.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mTabNames[position];
    }
}