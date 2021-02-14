package com.vidsearch.videosearchapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import com.vidsearch.videosearchapp.R;
import com.vidsearch.videosearchapp.databinding.ActivityImagesFolderProgressBinding;
import com.vidsearch.videosearchapp.images.ImageResizer;
import com.vidsearch.videosearchapp.ui.utils.Constants;

import androidx.appcompat.app.AppCompatActivity;

import static com.vidsearch.videosearchapp.ui.utils.Constants.KEY_IMAGES_FOLDER_ID;
import static com.vidsearch.videosearchapp.ui.utils.Constants.KEY_IMAGES_FOLDER_NAME;
import static com.vidsearch.videosearchapp.ui.utils.Constants.KEY_NUM_IN_IMAGES_FOLDER;

public class ImagesFolderProgressActivity extends AppCompatActivity {
    private ActivityImagesFolderProgressBinding mBinding;
    private int mImageThumbSize;
    private ImageResizer mImageResizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();

        mBinding = ActivityImagesFolderProgressBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        setSupportActionBar(mBinding.imagesFolderToolbar);
        String folderName = intent.getStringExtra(KEY_IMAGES_FOLDER_NAME);
        getSupportActionBar().setTitle(folderName + getResources().getString(R.string.folder));

        mBinding.totalImagesProcessed.setText(Integer.toString(intent.getIntExtra(KEY_NUM_IN_IMAGES_FOLDER, 0)));
        mBinding.currentImageProcessed.setText(Integer.toString(1));
        mBinding.imageProcessedImg.setScaleType(ImageView.ScaleType.CENTER_CROP);

        mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.images_folder_thumbnail_size);
        mImageResizer = new ImageResizer(
                this,
                mImageThumbSize, mImageThumbSize, Constants.IMAGES_FOLDERS_KEY_PREFIX);

        long folderId = intent.getLongExtra(KEY_IMAGES_FOLDER_ID, 0);
        mImageResizer.findFacesInImages(folderId, folderName, mBinding.imageProcessedImg, mBinding.currentImageProcessed, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mImageResizer.setExitTasksEarly(false);
    }

    @Override
    public void onPause() {
        super.onPause();
        mImageResizer.setExitTasksEarly(true);
    }
}
