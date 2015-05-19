package me.aerovulpe.crawler.fragments;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.preferences.DeletablePreference;
import me.aerovulpe.crawler.util.AndroidUtils;

/**
 * Created by Aaron on 19/05/2015.
 */
public class SettingsFragment extends PreferenceFragment {
    public static final String SLIDESHOW_INTERVAL_KEY = "slideshowInterval";
    public static final String PHOTO_TRANSITION_KEY = "photoTransition";
    public static final String DESC_SWITCHER_INTERVAL_KEY = "descSwitcherInterval";
    public static final String DISABLE_NOTIFICATIONS_KEY = "disableNotifications";
    public static final String DOWNLOAD_OFF_WIFI_KEY = "downloadOffWifi";
    public static final String CACHE_SIZE_KEY = "cacheSize";
    public static final String DELETE_CACHE_KEY = "deleteCache";
    public static final String OTHER_SETTINGS_KEY = "otherSettings";
    private PreferenceScreen mPreferenceScreen;
    private EditTextPreference mSlideshowIntervalPref;
    private ListPreference mPhotoTransitionPref;
    private EditTextPreference mDescSwitcherIntervalPref;
    private SwitchPreference mDisableNotificationsPref;
    private SwitchPreference mDownloadOffWifiPref;
    private EditTextPreference mCacheSizePref;
    private DeletablePreference mDeleteCachePref;
    private Preference mAboutPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mPreferenceScreen = getPreferenceScreen();
        mSlideshowIntervalPref = (EditTextPreference) findPreference(SLIDESHOW_INTERVAL_KEY);
        mPhotoTransitionPref = (ListPreference) findPreference(PHOTO_TRANSITION_KEY);
        mDescSwitcherIntervalPref = (EditTextPreference) findPreference(DESC_SWITCHER_INTERVAL_KEY);
        mDisableNotificationsPref = (SwitchPreference) findPreference(DISABLE_NOTIFICATIONS_KEY);
        mDownloadOffWifiPref = (SwitchPreference) findPreference(DOWNLOAD_OFF_WIFI_KEY);
        mCacheSizePref = (EditTextPreference) findPreference(CACHE_SIZE_KEY);

        PreferenceCategory otherCategory = (PreferenceCategory) findPreference(OTHER_SETTINGS_KEY);
        mDeleteCachePref = new DeletablePreference(getActivity());
        mDeleteCachePref.setDialogTitle("Are you sure you want to delete the cached photos?");
        mDeleteCachePref.setTitle("Delete cached photos");
        mDeleteCachePref.setSummary("Click to delete cached photos (will be downloaded again during next run).");
        mDeleteCachePref.setDialogIcon(android.R.drawable.ic_delete);
        mDeleteCachePref.setPersistent(true);
        mDeleteCachePref.setKey(DELETE_CACHE_KEY);

        mAboutPref = new Preference(getActivity());
        mAboutPref.setPersistent(false);
        mAboutPref.setTitle("About Crawler");
        mAboutPref.setSummary("Hello, World");

        otherCategory.addPreference(mDeleteCachePref);
        otherCategory.addPreference(mAboutPref);

        //hide this option from non-phone devices such as GoogleTV
        if (!AndroidUtils.hasTelephony(getActivity())) {
            otherCategory.removePreference(mDownloadOffWifiPref);
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }
}
