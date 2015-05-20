package me.aerovulpe.crawler.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.ToxicBakery.viewpager.transforms.AccordionTransformer;
import com.ToxicBakery.viewpager.transforms.BackgroundToForegroundTransformer;
import com.ToxicBakery.viewpager.transforms.CubeInTransformer;
import com.ToxicBakery.viewpager.transforms.CubeOutTransformer;
import com.ToxicBakery.viewpager.transforms.DefaultTransformer;
import com.ToxicBakery.viewpager.transforms.DepthPageTransformer;
import com.ToxicBakery.viewpager.transforms.FlipHorizontalTransformer;
import com.ToxicBakery.viewpager.transforms.ForegroundToBackgroundTransformer;
import com.ToxicBakery.viewpager.transforms.RotateDownTransformer;
import com.ToxicBakery.viewpager.transforms.RotateUpTransformer;
import com.ToxicBakery.viewpager.transforms.ScaleInOutTransformer;
import com.ToxicBakery.viewpager.transforms.StackTransformer;
import com.ToxicBakery.viewpager.transforms.TabletTransformer;
import com.ToxicBakery.viewpager.transforms.ZoomOutSlideTransformer;
import com.ToxicBakery.viewpager.transforms.ZoomOutTranformer;
import com.nostra13.universalimageloader.core.ImageLoader;

import me.aerovulpe.crawler.CrawlerApplication;
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
    private static final int DEFAULT_VALUE_SLIDESHOW_INTERVAL_INT = 5;
    private static final String DEFAULT_VALUE_SLIDESHOW_INTERVAL =
            DEFAULT_VALUE_SLIDESHOW_INTERVAL_INT + "";
    private static final int DEFAULT_VALUE_DESC_INTERVAL_INT = 5;
    private static final String DEFAULT_VALUE_DESC_INTERVAL
            = DEFAULT_VALUE_DESC_INTERVAL_INT + "";

    private int mOldCacheValue;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        EditTextPreference slideshowIntervalPref = (EditTextPreference) findPreference(SLIDESHOW_INTERVAL_KEY);
        slideshowIntervalPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                try {
                    int number = Integer.parseInt(newValue.toString());
                    if (number < 1 || number > 300) {
                        Toast.makeText(getActivity(), "Number of seconds must be between 1 and 300",
                                Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    return true;
                } catch (NumberFormatException e) {
                    Toast.makeText(getActivity(), "Please enter a valid number (minimum 1, maximum 300)",
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        });
        ListPreference photoTransitionPref = (ListPreference) findPreference(PHOTO_TRANSITION_KEY);
        EditTextPreference descSwitcherIntervalPref = (EditTextPreference) findPreference(DESC_SWITCHER_INTERVAL_KEY);
        descSwitcherIntervalPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                try {
                    int number = Integer.parseInt(newValue.toString());
                    if (number < 1 || number > 120) {
                        Toast.makeText(getActivity(), "Number of seconds must be between 1 and 120",
                                Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    return true;
                } catch (NumberFormatException e) {
                    Toast.makeText(getActivity(), "Please enter a valid number (minimum 1, maximum 120)",
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        });
        SwitchPreference disableNotificationsPref = (SwitchPreference) findPreference(DISABLE_NOTIFICATIONS_KEY);
        SwitchPreference downloadOffWifiPref = (SwitchPreference) findPreference(DOWNLOAD_OFF_WIFI_KEY);
        EditTextPreference cacheSizePref = (EditTextPreference) findPreference(CACHE_SIZE_KEY);
        cacheSizePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                try {
                    int currentCacheValue = Integer.parseInt(newValue.toString());
                    if (currentCacheValue < 10) {
                        Toast.makeText(getActivity(), "Please assign at least 10MB",
                                Toast.LENGTH_SHORT).show();
                        return false;
                    } else if (currentCacheValue < mOldCacheValue) {
                        CrawlerApplication.clearImageCache(getActivity());
                        return true;
                    } else if (currentCacheValue > mOldCacheValue) {
                        CrawlerApplication.initImageLoader(getActivity(), true);
                        return true;
                    }
                    return false;
                } catch (NumberFormatException e) {
                    Toast.makeText(getActivity(), "Please enter a valid number",
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        });

        PreferenceCategory otherCategory = (PreferenceCategory) findPreference(OTHER_SETTINGS_KEY);
        DeletablePreference deleteCachePref = new DeletablePreference(getActivity());
        deleteCachePref.setDialogTitle("Are you sure you want to delete the cached photos?");
        deleteCachePref.setTitle("Delete cached photos");
        deleteCachePref.setSummary("Click to delete cached photos (will be downloaded again during next run).");
        deleteCachePref.setDialogIcon(android.R.drawable.ic_delete);
        deleteCachePref.setPersistent(true);
        deleteCachePref.setKey(DELETE_CACHE_KEY);
        deleteCachePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                ImageLoader.getInstance().clearDiskCache();
                return true;
            }
        });

        Preference aboutPref = new Preference(getActivity());
        aboutPref.setPersistent(false);
        aboutPref.setTitle("About Crawler");
        aboutPref.setSummary("Hello, World");

        otherCategory.addPreference(deleteCachePref);
        otherCategory.addPreference(aboutPref);

        //hide this option from non-phone devices such as GoogleTV
        if (!AndroidUtils.hasTelephony(getActivity())) {
            otherCategory.removePreference(downloadOffWifiPref);
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    public static int getSlideshowIntervalMS(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String slideshowIntervalSeconds = settings.getString(SLIDESHOW_INTERVAL_KEY,
                DEFAULT_VALUE_SLIDESHOW_INTERVAL);
        try {
            return Integer.parseInt(slideshowIntervalSeconds) * 1000;
        } catch (NumberFormatException e) {
            return DEFAULT_VALUE_SLIDESHOW_INTERVAL_INT * 1000;
        }
    }

    public static ViewPager.PageTransformer getPageTransformer(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String transformerType = settings.getString(PHOTO_TRANSITION_KEY, "DEFAULT");
        switch (transformerType != null ? transformerType : "DEFAULT") {
            case "DEFAULT":
                return new DefaultTransformer();
            case "ACCORDION":
                return new AccordionTransformer();
            case "BACKGROUND_TO_FOREGROUND":
                return new BackgroundToForegroundTransformer();
            case "CUBE_IN":
                return new CubeInTransformer();
            case "CUBE_OUT":
                return new CubeOutTransformer();
            case "DEPTH_PAGE":
                return new DepthPageTransformer();
            case "FLIP_HORIZONTAL":
                return new FlipHorizontalTransformer();
            case "FOREGROUND_TO_BACKGROUND":
                return new ForegroundToBackgroundTransformer();
            case "ROTATE_DOWN":
                return new RotateDownTransformer();
            case "ROTATE_UP":
                return new RotateUpTransformer();
            case "SCALE_IN_OUT":
                return new ScaleInOutTransformer();
            case "STACK":
                return new StackTransformer();
            case "TABLET":
                return new TabletTransformer();
            case "ZOOM_IN":
                return new TabletTransformer();
            case "ZOOM_OUT_SLIDE":
                return new ZoomOutSlideTransformer();
            case "ZOOM_OUT":
                return new ZoomOutTranformer();
            default:
                return new DefaultTransformer();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mOldCacheValue = getCurrentCacheValue(getActivity());
    }

    public static int getDescIntervalMS(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String descIntervalSeconds = settings.getString(DESC_SWITCHER_INTERVAL_KEY,
                DEFAULT_VALUE_DESC_INTERVAL);
        try {
            return Integer.parseInt(descIntervalSeconds) * 1000;
        } catch (NumberFormatException e) {
            return DEFAULT_VALUE_DESC_INTERVAL_INT * 1000;
        }
    }

    public static boolean disableNotifications(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getBoolean(DISABLE_NOTIFICATIONS_KEY, false);
    }

    public static boolean downloadOffWifi(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getBoolean(DOWNLOAD_OFF_WIFI_KEY, false);
    }

    private static int getCurrentCacheValue(Context context) {
        return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context)
                .getString("cacheSize", "400"));
    }

    public static int getCurrentCacheValueInBytes(Context context) {
        return getCurrentCacheValue(context) * 1048576;
    }
}