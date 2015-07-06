package me.aerovulpe.crawler.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import me.aerovulpe.crawler.R;

/**
 * Created by Aaron on 05/07/2015.
 */
public class FileExplorerFragment extends DialogFragment {
    private static final String TAG = "FileExplorerFragment";
    private static final String ARG_TITLE = "me.aerovulpe.crawler.FileExplorerFragment.title";
    // Stores names of traversed directories
    ArrayList<String> mTraversedDirs = new ArrayList<>();
    // Check if the first level of the directory structure is the one showing
    private boolean mIsFirstLvl = true;

    private Item[] mDirItems;
    private File mPath = Environment.getExternalStorageDirectory();
    private String mChosenDir;
    private ListAdapter mAdapter;
    private OnDirectorySelectedListener mOnDirectorySelectedListener;
    private String mTitle;

    public static FileExplorerFragment newInstance(String title) {
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        FileExplorerFragment fragment = new FileExplorerFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTitle = getArguments().getString(ARG_TITLE);
        loadFileList();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        if (mDirItems == null) {
            Log.e(TAG, "No files loaded");
            return builder.create();
        }

        builder.setTitle(R.string.choose_a_directory)
                .setAdapter(mAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mChosenDir = mDirItems[which].mFile;
                        File selectedDir = new File(mPath + "/" + mChosenDir);
                        if (selectedDir.isDirectory()) {
                            mIsFirstLvl = false;

                            // Add chosen directory to list of traversed directories
                            mTraversedDirs.add(mChosenDir);
                            mPath = selectedDir;
                            reloadDialog();
                        } else if (mChosenDir.equals(getString(R.string.up)) && !selectedDir.exists()) {
                            // We can safely assume the up item was clicked
                            // Present directory removed from list
                            String dirStr = mTraversedDirs.remove(mTraversedDirs.size() - 1);
                            // Modify the path to exclude present directory
                            String pathStr = mPath.toString();
                            mPath = new File(pathStr.substring(0,
                                    pathStr.lastIndexOf(dirStr)));

                            // If there are no more directories in the list,
                            // it's the first level
                            if (mTraversedDirs.isEmpty())
                                mIsFirstLvl = true;
                            reloadDialog();
                        } else {
                            View promptView = LayoutInflater.from(activity).inflate(R.layout.new_folder_prompt,
                                    (ViewGroup) getView(), false);
                            AlertDialog.Builder promptBuilder = new AlertDialog.Builder(activity);
                            promptBuilder.setView(promptView);
                            final EditText userInput = (EditText) promptView.findViewById(R.id.userInput);
                            userInput.setText(mTitle);
                            promptBuilder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mChosenDir = userInput.getText().toString();
                                    String path = mPath + "/" + mChosenDir;
                                    File newDir = new File(path);
                                    if (newDir.mkdir()) {
                                        mIsFirstLvl = false;
                                        mTraversedDirs.add(mChosenDir);
                                        mPath = newDir;
                                        show(activity.getFragmentManager(), getTag());
                                    }

                                }
                            }).setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                    show(activity.getFragmentManager(), getTag());
                                }
                            }).show();
                        }
                    }

                    private void reloadDialog() {
                        loadFileList();
                        dismiss();
                        show(getFragmentManager(), getTag());
                    }
                });
        builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
            }
        }).setPositiveButton(getString(R.string.save), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mOnDirectorySelectedListener != null)
                    mOnDirectorySelectedListener.onDirectorySelected(mPath.getAbsolutePath());
            }
        });

        return builder.create();
    }

    public void setOnDirectorySelectedListener(OnDirectorySelectedListener
                                                       onDirectorySelectedListener) {
        mOnDirectorySelectedListener = onDirectorySelectedListener;
    }

    private void loadFileList() {
        // Checks whether the path exists
        String failedMsg = getString(R.string.unable_to_write_to_sd_card);
        try {
            if (!(mPath.mkdir() || mPath.isDirectory()))
                throw new Exception(failedMsg);
        } catch (Exception e) {
            Log.e(TAG, failedMsg);
            Toast.makeText(getActivity(), failedMsg, Toast.LENGTH_LONG).show();
            getDialog().dismiss();
            return;
        }

        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                File selectedFile = new File(dir, filename);
                return selectedFile.isDirectory() && !selectedFile.isHidden();
            }
        };

        String[] dirList = mPath.list(filter);
        int dirsLength = dirList.length;
        mDirItems = new Item[dirsLength];
        for (int i = 0; i < dirsLength; i++) {
            mDirItems[i] = new Item(dirList[i], R.drawable.ic_folder_black_24dp);
        }
        Item[] temp;
        if (mIsFirstLvl) {
            temp = new Item[dirsLength + 1];
            System.arraycopy(mDirItems, 0, temp, 1, dirsLength);
            temp[0] = new Item(getString(R.string.add_new_folder), R.drawable.ic_add_black_24dp);
        } else {
            temp = new Item[dirsLength + 2];
            System.arraycopy(mDirItems, 0, temp, 2, dirsLength);
            temp[0] = new Item(getString(R.string.up), R.drawable.ic_keyboard_arrow_up_black_24dp);
            temp[1] = new Item(getString(R.string.add_new_folder), R.drawable.ic_add_black_24dp);
        }
        mDirItems = temp;

        mAdapter = new ArrayAdapter<Item>(getActivity(), android.R.layout.select_dialog_item,
                android.R.id.text1, mDirItems) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                // Creates view
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                // Put the image on the textview
                textView.setCompoundDrawablesWithIntrinsicBounds(mDirItems[position].mIcon, 0, 0, 0);
                // Add padding between image and text
                int padding = (int) (5 * getResources().getDisplayMetrics().density + 0.5f);
                textView.setCompoundDrawablePadding(padding);
                return view;
            }
        };
    }

    private class Item {
        public String mFile;
        public int mIcon;

        public Item(String file, int icon) {
            mFile = file;
            mIcon = icon;
        }

        @Override
        public String toString() {
            return mFile;
        }
    }

    public interface OnDirectorySelectedListener {
        void onDirectorySelected(String chosenDirectory);
    }
}
