package com.vidsearch.videosearchapp.ui;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import com.vidsearch.videosearchapp.R;
import com.vidsearch.videosearchapp.databinding.ActivityVideosFolderProgressBinding;
import com.vidsearch.videosearchapp.images.VideoWorker;
import com.vidsearch.videosearchapp.ui.utils.VideosFoldersMS;

import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

public class VideosFolderProgressActivity extends AppCompatActivity {
    private ActivityVideosFolderProgressBinding mBinding;
    private VideoWorker mVideoWorker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        mBinding = ActivityVideosFolderProgressBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        setSupportActionBar(mBinding.videosFolderToolbar);
        getSupportActionBar().setTitle(getResources().getString(R.string.search_videos));

        List<VideosFoldersMS.VideosFolderMS> videosFoldersMSList = VideosFoldersMS.getVideosFolders(this);
        int countVideos = 0;
        for (int i = 0; i < videosFoldersMSList.size(); i++) {
            VideosFoldersMS.VideosFolderMS vidsFoldersMS = videosFoldersMSList.get(i);
            if (vidsFoldersMS.getVidsFolderName().equals("Camera"))
                countVideos += videosFoldersMSList.get(i).getVidsCount();
        }
        if (countVideos == 0) {
            setResult(0);
            finish();
        } else {
            mBinding.currentVideoProcessed.setText(Integer.toString(1));
            mBinding.totalVideosProcessed.setText(Integer.toString(countVideos));
            setResult(countVideos);

            Drawable drawable = getDrawable(R.drawable.empty_photo);
            mBinding.videoImg.setImageDrawable(drawable);
            mBinding.videoFaceImg.setImageDrawable(drawable);
            mBinding.faceImg.setImageDrawable(drawable);

            mVideoWorker = new VideoWorker(this);
            mVideoWorker.matchFacesInVideosTask("", videosFoldersMSList,
                    mBinding.currentVideoProcessed,
                    mBinding.videoImg, mBinding.videoTimestamp,
                    mBinding.videoFaceImg, mBinding.faceImg, mBinding.compareScore,
                    mBinding.bestScoreId, mBinding.bestScore);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mVideoWorker != null) {
            mVideoWorker.setExitTasksEarly(true);
        }
    }
}
