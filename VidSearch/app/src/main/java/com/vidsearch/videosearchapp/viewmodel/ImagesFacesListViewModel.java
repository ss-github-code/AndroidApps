package com.vidsearch.videosearchapp.viewmodel;

import android.app.Application;

import com.vidsearch.videosearchapp.db.VideoSearchDatabase;
import com.vidsearch.videosearchapp.db.entity.ImageFacesEntity;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

public class ImagesFacesListViewModel extends AndroidViewModel {
    private static volatile ImagesFacesListViewModel sImagesFacesListViewModel;
    private VideoSearchDatabase mDatabase;
    private ImagesFacesListViewModel(@NonNull Application application) {
        super(application);
        mDatabase = VideoSearchDatabase.getVideoSearchDatabase(application.getApplicationContext());
    }

    public static ImagesFacesListViewModel getInstance(@NonNull Application application) {
        if (sImagesFacesListViewModel == null) {
            sImagesFacesListViewModel = new ImagesFacesListViewModel(application);
        }
        return sImagesFacesListViewModel;
    }

    public ImageFacesEntity getImageFaces(long folderId, long picId) {
        return mDatabase.getImageFacesDao().getImageSync(folderId, picId);
    }

    public ImageFacesEntity getImageFaces(long picId) {
        return mDatabase.getImageFacesDao().getImageSync(picId);
    }
}
