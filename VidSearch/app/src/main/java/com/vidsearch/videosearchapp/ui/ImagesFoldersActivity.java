package com.vidsearch.videosearchapp.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.vidsearch.videosearchapp.BuildConfig;
import com.vidsearch.videosearchapp.R;
import com.vidsearch.videosearchapp.databinding.ActivityImagesFoldersBinding;
import com.vidsearch.videosearchapp.db.VideoSearchDatabase;
import com.vidsearch.videosearchapp.db.entity.SelectedFaceEntity;
import com.vidsearch.videosearchapp.ui.utils.Constants;
import com.vidsearch.videosearchapp.ui.utils.ImagesFoldersMS;
import java.util.List;
import java.util.Stack;

public class ImagesFoldersActivity extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener {
    private static final String TAG = "ImagesFoldersActivity";
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    public static class ActionBarInfo {
        private String mTitle;
        private String mSubtitle;
        public ActionBarInfo(String title, String subTitle) {
            mTitle = title;
            mSubtitle = subTitle;
        }
    }
    private static Stack<ActionBarInfo> sStackActionBarInfo;
    private ActivityImagesFoldersBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (BuildConfig.DEBUG) {
            // TODO: enableStrictMode()
        }

        super.onCreate(savedInstanceState);
        mBinding = ActivityImagesFoldersBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        setSupportActionBar(mBinding.toolbar);

        if(ContextCompat.checkSelfPermission(ImagesFoldersActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(ImagesFoldersActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        }

        if (savedInstanceState == null) {
            List<ImagesFoldersMS.ImagesFolderMS> imagesFoldersMS = ImagesFoldersMS.getImagesFolders(getApplicationContext());
            VideoSearchDatabase db = VideoSearchDatabase.getVideoSearchDatabase(getApplicationContext());
            if (imagesFoldersMS.size() == 0) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.images_folders_content, new NoImagesFragment())
                        .commitNow();
            }
            else {
                Fragment imgsFoldersFragment = new ImagesFoldersMSFragment();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.images_folders_content, imgsFoldersFragment, ((Object) imgsFoldersFragment).getClass().getName())
                        .commitNow();

                Fragment selectedFacesFragment = new SelectedFacesFragment();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.selected_faces_content, selectedFacesFragment, ((Object) selectedFacesFragment).getClass().getName())
                        .commitNow();
                sStackActionBarInfo = new Stack<>();
                setActionBarInfo(getResources().getString(R.string.app_name),
                                 getResources().getString(R.string.select_folder));
            }
        }
        else { // setup the action bar
            setupDisplayHomeAsUp(); // with Up arrow enabled?
            if (sStackActionBarInfo == null) {
                sStackActionBarInfo = new Stack<>();
                setActionBarInfo(getResources().getString(R.string.app_name),
                        getResources().getString(R.string.select_folder));
            } else {
                ActionBarInfo actionBarInfo = sStackActionBarInfo.peek();
                getSupportActionBar().setTitle(actionBarInfo.mTitle);
                getSupportActionBar().setSubtitle(actionBarInfo.mSubtitle);

            }
        }

        getSupportFragmentManager().addOnBackStackChangedListener(this);

        FloatingActionButton searchVidsFAB = mBinding.videoSearchFAB;
        searchVidsFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ImagesFoldersActivity.this, VideosFolderProgressActivity.class);
                startActivityForResult(intent, Constants.FACE_MATCHING_PROGRESS_ACTIVITY_REQ_CODE);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.FACE_MATCHING_PROGRESS_ACTIVITY_REQ_CODE) {
            if (resultCode == 0)
                showNoVideosFragment();
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.images_folders_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_images_folders_settings:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setupDisplayHomeAsUp() {
        int count = getSupportFragmentManager().getBackStackEntryCount();
        boolean canGoBack = count > 0;
        getSupportActionBar().setDisplayHomeAsUpEnabled(canGoBack);
    }

    @Override
    public void onBackStackChanged() {
        setupDisplayHomeAsUp();
        int count = getSupportFragmentManager().getBackStackEntryCount();
        if (count+1 < sStackActionBarInfo.size()) {
            sStackActionBarInfo.pop();
            ActionBarInfo actionBarInfo = sStackActionBarInfo.peek();
            getSupportActionBar().setTitle(actionBarInfo.mTitle);
            getSupportActionBar().setSubtitle(actionBarInfo.mSubtitle);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        getSupportFragmentManager().popBackStack();
        return true;
    }

    public void setActionBarInfo(String title, String subTitle) {
        ActionBarInfo actionBarInfo = new ActionBarInfo(title, subTitle);
        sStackActionBarInfo.push(actionBarInfo);
        getSupportActionBar().setTitle(title);
        getSupportActionBar().setSubtitle(subTitle);
    }

    public void showImagesFolder(long folderId, String folderName) {
        FragmentManager manager = getSupportFragmentManager();
        setActionBarInfo(folderName + getResources().getString(R.string.folder),
                         getResources().getString(R.string.select_face));
        try {
            FragmentTransaction transaction = manager.beginTransaction();
            Fragment frag = new ImagesFacesFragment();
            Bundle args = new Bundle();
            args.putLong(Constants.KEY_IMAGES_FOLDER_ID, folderId);
            frag.setArguments(args);
            String backStateName = ((Object) frag).getClass().getName();
            transaction.replace(R.id.images_folders_content, frag, backStateName);
            transaction.addToBackStack(backStateName);
            transaction.commit();
        } catch (IllegalStateException exception) {
            Log.d(TAG, "Unable to commit fragment, could be activity as been killed in background. " + exception.toString());
        }
    }

    public void showNoImagesFragment(String folderName) {
        FragmentManager manager = getSupportFragmentManager();
        setActionBarInfo(folderName + getResources().getString(R.string.folder), "");
        try {
            FragmentTransaction transaction = manager.beginTransaction();
            Fragment frag = new NoImagesFragment();
            Bundle args = new Bundle();
            args.putString(Constants.KEY_IMAGES_FOLDER_NAME, folderName);
            frag.setArguments(args);
            String backStateName = ((Object) frag).getClass().getName();
            transaction.replace(R.id.images_folders_content, frag, backStateName);
            transaction.addToBackStack(backStateName);
            transaction.commit();
        } catch (IllegalStateException exception) {
            Log.d(TAG, "Unable to commit fragment, could be activity as been killed in background. " + exception.toString());
        }
    }

    public void showSelectedImage(SelectedFaceEntity entity) {
        FragmentManager manager = getSupportFragmentManager();
        setActionBarInfo(getResources().getString(R.string.selected_picture),
                         getResources().getString(R.string.select_face));
        try {
            FragmentTransaction transaction = manager.beginTransaction();
            Fragment frag = new SelectedImageFragment();
            Bundle args = new Bundle();
            args.putSerializable(Constants.KEY_FACE_ENTITY, entity);
            frag.setArguments(args);
            String backStateName = ((Object) frag).getClass().getName();
            transaction.replace(R.id.images_folders_content, frag, backStateName);
            transaction.addToBackStack(backStateName);
            transaction.commit();
        } catch (IllegalStateException exception) {
            Log.d(TAG, "Unable to commit fragment, could be activity as been killed in background. " + exception.toString());
        }
    }

    public void showNoVideosFragment() {
        FragmentManager manager = getSupportFragmentManager();
        setActionBarInfo(getResources().getString(R.string.search_videos), "");
        try {
            FragmentTransaction transaction = manager.beginTransaction();
            Fragment frag = new NoImagesFragment();
            Bundle args = new Bundle();
            args.putString(Constants.KEY_IMAGES_FOLDER_NAME, "");
            frag.setArguments(args);
            String backStateName = ((Object) frag).getClass().getName();
            transaction.replace(R.id.images_folders_content, frag, backStateName);
            transaction.addToBackStack(backStateName);
            transaction.commit();
        } catch (IllegalStateException exception) {
            Log.d(TAG, "Unable to commit fragment, could be activity as been killed in background. " + exception.toString());
        }
    }

    public void showVideoSearchButton(int visibility) {
        mBinding.videoSearchFAB.setVisibility(visibility);
    }
}
