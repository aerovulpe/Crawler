package me.aerovulpe.crawler.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.base.BaseActivity;
import me.aerovulpe.crawler.fragments.SlideShowFragment;


public class MainActivity extends BaseActivity {
    SlideShowFragment mSlideShowFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            mSlideShowFragment = new SlideShowFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, mSlideShowFragment)
                    .commit();
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


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(SlideShowFragment.LOG_PREFIX, "Keyevent in activity" + keyCode);
        if (mSlideShowFragment != null){
            //Basically some key-aliases for GoogleTV buttons
            switch (keyCode) {
                //hardcoded some keyevents in order to support 2.1
                //case KeyEvent.KEYCODE_MEDIA_STOP:
                //case KeyEvent.KEYCODE_MEDIA_PAUSE:
                case 86:
                case 127:
                    mSlideShowFragment.actionPauseSlideshow();

                    return true;
                //case KeyEvent.KEYCODE_MEDIA_PLAY:
                case 126:
                    mSlideShowFragment.actionResumeSlideshow();

                    return true;

                case KeyEvent.KEYCODE_MEDIA_NEXT:
                case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                    mSlideShowFragment.setUserCreatedTouchEvent(true);
                    mSlideShowFragment.getGallery().onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT, new KeyEvent(0, 0));
                    return true;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                case KeyEvent.KEYCODE_MEDIA_REWIND:
                    mSlideShowFragment.setUserCreatedTouchEvent(true);
                    mSlideShowFragment.getGallery().onKeyDown(KeyEvent.KEYCODE_DPAD_LEFT, new KeyEvent(0, 0));
                    return true;

                default:
                    Log.d(SlideShowFragment.LOG_PREFIX, "Unhandled keyevent " + keyCode);
                    break;
            }

        }
        return super.onKeyDown(keyCode, event);
    }

}
