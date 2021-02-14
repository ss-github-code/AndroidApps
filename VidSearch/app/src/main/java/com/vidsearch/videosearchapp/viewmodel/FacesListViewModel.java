package com.vidsearch.videosearchapp.viewmodel;

import android.app.Application;

import com.vidsearch.videosearchapp.db.VideoSearchDatabase;
import com.vidsearch.videosearchapp.db.entity.FaceEntity;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

public class FacesListViewModel extends AndroidViewModel {
    private static volatile FacesListViewModel sFacesListViewModel;
    private VideoSearchDatabase mDatabase;
    private FacesListViewModel(@NonNull Application application) {
        super(application);
        mDatabase = VideoSearchDatabase.getVideoSearchDatabase(application.getApplicationContext());
    }

    public static FacesListViewModel getInstance(@NonNull Application application) {
        if (sFacesListViewModel == null) {
            sFacesListViewModel = new FacesListViewModel(application);
        }
        return sFacesListViewModel;
    }

    public FaceEntity getFaceInImage(long picId, int faceId) {
        return mDatabase.getFacesDao().getFaceInPic(picId, faceId);
    }

    public List<FaceEntity> getFacesInImage(long picId) {
        return mDatabase.getFacesDao().getFacesInPic(picId);
    }
}
