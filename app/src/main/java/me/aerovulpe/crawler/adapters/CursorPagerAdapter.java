package me.aerovulpe.crawler.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

public abstract class CursorPagerAdapter extends PagerAdapter {

    private boolean mDataValid;
    private Cursor mCursor;
    private Context mContext;

    public CursorPagerAdapter(Context context, Cursor cursor) {
        mCursor = cursor;
        mDataValid = cursor != null;
        mContext = context;
    }

    /**
     * Returns the cursor.
     *
     * @return the cursor.
     */
    public Cursor getCursor() {
        return mCursor;
    }

    /**
     * @see android.widget.ListAdapter#getCount()
     */
    public int getCount() {
        if (mDataValid && mCursor != null) {
            return mCursor.getCount();
        } else {
            return 0;
        }
    }

    /**
     * @see android.widget.ListAdapter#getView(int, View, ViewGroup)
     */

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        if (!mDataValid) {
            throw new IllegalStateException("this should only be called when the cursor is valid");
        }
        if (!mCursor.moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }

        View v = newView(mContext, mCursor, container);
        bindView(v, mContext, mCursor);

        (container).addView(v);

        return v;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        (container).removeView((View) object);
    }

    /**
     * Makes a new view to hold the data pointed to by cursor.
     *
     * @param context Interface to application's global information
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the correct
     *                position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created view.
     */
    public abstract View newView(Context context, Cursor cursor, ViewGroup parent);

    /**
     * Bind an existing view to the data pointed to by cursor
     *
     * @param view    Existing view, returned earlier by newView
     * @param context Interface to application's global information
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the correct
     *                position.
     */
    public abstract void bindView(View view, Context context, Cursor cursor);

    /**
     * Swap in a new Cursor, returning the old Cursor. The returned old Cursor is <em>not</em> closed.
     *
     * @param newCursor The new cursor to be used.
     * @return Returns the previously set Cursor, or null if there was not one. If the given new Cursor
     * is the same instance is the previously set Cursor, null is also returned.
     */
    public Cursor swapCursor(Cursor newCursor) {
        if (newCursor == mCursor) {
            return null;
        }
        Cursor oldCursor = mCursor;
        mCursor = newCursor;
        if (newCursor != null) {
            mDataValid = true;
            // notify the observers about the new cursor
            notifyDataSetChanged();
        } else {
            mDataValid = false;
            // notify the observers about the lack of a data set
            notifyDataSetChanged();
        }
        return oldCursor;
    }
}