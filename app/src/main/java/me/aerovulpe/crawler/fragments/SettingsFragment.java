package me.aerovulpe.crawler.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
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
import me.aerovulpe.crawler.request.CategoriesRequest;
import me.aerovulpe.crawler.request.RequestService;
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
    public static final int MEGABYTE_TO_BYTE_FACTOR = 1048576;
    public static final String DEFAULT_VALUE_CACHE_SIZE = "400";
    public static final String DEFAULT_VALUE_PHOTO_TRANSITION = "DEFAULT";

    private int mOldCacheValue;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        EditTextPreference slideshowIntervalPref = (EditTextPreference) findPreference(SLIDESHOW_INTERVAL_KEY);
        slideshowIntervalPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                try {
                    int number = Integer.parseInt(newValue.toString());
                    if (number < 1 || number > 300) {
                        Toast.makeText(getActivity(), getString(R.string.slideshow_guide),
                                Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    return true;
                } catch (NumberFormatException e) {
                    Toast.makeText(getActivity(), getString(R.string.slideshow_warning),
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        });
        EditTextPreference descSwitcherIntervalPref = (EditTextPreference) findPreference(DESC_SWITCHER_INTERVAL_KEY);
        descSwitcherIntervalPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                try {
                    int number = Integer.parseInt(newValue.toString());
                    if (number < 1 || number > 120) {
                        Toast.makeText(getActivity(), getString(R.string.desc_switcher_guide),
                                Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    return true;
                } catch (NumberFormatException e) {
                    Toast.makeText(getActivity(), getString(R.string.desc_switcher_warning),
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        });
        SwitchPreference disableNotificationsPref = (SwitchPreference) findPreference(DISABLE_NOTIFICATIONS_KEY);
        disableNotificationsPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((Boolean) newValue) {
                    Activity activity = getActivity();
                    Intent intent = new Intent(activity, RequestService.class);
                    intent.setAction(RequestService.ACTION_CLEAR_ALL_NOTIFICATIONS);
                    activity.startService(intent);
                }
                return true;
            }
        });
        SwitchPreference downloadOffWifiPref = (SwitchPreference) findPreference(DOWNLOAD_OFF_WIFI_KEY);
        downloadOffWifiPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (((Boolean) newValue)) {
                    new CategoriesRequest(getActivity()).execute();
                    ImageLoader.getInstance().denyNetworkDownloads(false);
                } else {
                    Activity activity = getActivity();
                    boolean isConnectedToWifi = AndroidUtils.isConnectedToWifi(activity);
                    boolean isConnectedToWired = AndroidUtils.isConnectedToWired(activity);

                    if (!isConnectedToWifi && !isConnectedToWired)
                        ImageLoader.getInstance().denyNetworkDownloads(true);
                }
                return true;
            }
        });
        EditTextPreference cacheSizePref = (EditTextPreference) findPreference(CACHE_SIZE_KEY);
        cacheSizePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                try {
                    int currentCacheValue = Integer.parseInt(newValue.toString());
                    if (currentCacheValue < 10) {
                        Toast.makeText(getActivity(), getString(R.string.cache_size_guide),
                                Toast.LENGTH_SHORT).show();
                        return false;
                    } else if (currentCacheValue < mOldCacheValue) {
                        CrawlerApplication.clearImageCacheInit(getActivity(),
                                currentCacheValue * MEGABYTE_TO_BYTE_FACTOR);
                        return true;
                    } else if (currentCacheValue > mOldCacheValue) {
                        CrawlerApplication.initImageLoader(getActivity(),
                                currentCacheValue * MEGABYTE_TO_BYTE_FACTOR);
                        return true;
                    }
                    return false;
                } catch (NumberFormatException e) {
                    Toast.makeText(getActivity(), getString(R.string.cache_size_warning),
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        });

        PreferenceCategory otherCategory = (PreferenceCategory) findPreference(OTHER_SETTINGS_KEY);
        DeletablePreference deleteCachePref = new DeletablePreference(getActivity());
        deleteCachePref.setDialogTitle(getString(R.string.delete_cache_dialog_title));
        deleteCachePref.setTitle(getString(R.string.delete_cache_title));
        deleteCachePref.setSummary(getString(R.string.delete_cache_summary));
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

        Preference morePref = new Preference(getActivity());
        morePref.setPersistent(false);
        morePref.setTitle(getString(R.string.more_pref_title));
        morePref.setSummary(getString(R.string.more_pref_summary));
        morePref.setIntent(new Intent(Intent.ACTION_VIEW,
                Uri.parse(getString(R.string.aerisvulpe_dev_url))));

        otherCategory.addPreference(deleteCachePref);
        otherCategory.addPreference(morePref);

        //hide this option from non-phone devices
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
        String transformerType = settings.getString(PHOTO_TRANSITION_KEY, DEFAULT_VALUE_PHOTO_TRANSITION);
        switch (transformerType != null ? transformerType : DEFAULT_VALUE_PHOTO_TRANSITION) {
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
                .getString(CACHE_SIZE_KEY, DEFAULT_VALUE_CACHE_SIZE));
    }

    public static int getCurrentCacheValueInBytes(Context context) {
        return getCurrentCacheValue(context) * MEGABYTE_TO_BYTE_FACTOR;
    }
}
