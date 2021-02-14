package com.vidsearch.videosearchapp.images;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.vidsearch.videosearchapp.R;
import com.vidsearch.videosearchapp.mtcnn.MTCNN;
import com.vidsearch.videosearchapp.viewmodel.FacesListViewModel;
import com.vidsearch.videosearchapp.viewmodel.ImagesFacesListViewModel;

import java.io.IOException;

import androidx.fragment.app.FragmentActivity;

public abstract class Worker {
    protected static Context sAppContext;
    protected static Resources sResources;
    protected static Bitmap sLoadingBitmap = null;
    protected static MTCNN sMTCNN = null;

    protected static FacesListViewModel sFacesListViewModel;

    protected boolean mExitTasksEarly = false;

    private void initialize(Application app, Context context, AssetManager assetManager, Resources resources) {
        sAppContext = context;
        sResources = resources;
        sLoadingBitmap = BitmapFactory.decodeResource(resources, R.drawable.empty_photo);
        try {
            sMTCNN = new MTCNN(assetManager);
        } catch (IOException e) {
        }
        sFacesListViewModel = FacesListViewModel.getInstance(app);
    }

    public Worker(Activity activity) {
        if (sLoadingBitmap == null) {
            initialize(activity.getApplication(), activity.getApplicationContext(), activity.getAssets(), activity.getResources());
        }
    }

    public Worker(FragmentActivity fragmentActivity) {
        if (sLoadingBitmap == null) {
            initialize(fragmentActivity.getApplication(), fragmentActivity.getApplicationContext(), fragmentActivity.getAssets(), fragmentActivity.getResources());
        }
    }

    public void setExitTasksEarly(boolean exitTasksEarly) {
        mExitTasksEarly = exitTasksEarly;
    }
}
