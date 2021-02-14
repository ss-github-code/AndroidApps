package com.vidsearch.videosearchapp.viewmodel;

import android.app.Application;

import com.vidsearch.videosearchapp.db.VideoSearchDatabase;
import com.vidsearch.videosearchapp.db.entity.FaceEntity;
import com.vidsearch.videosearchapp.db.entity.ImageFacesEntity;
import com.vidsearch.videosearchapp.db.entity.ImagesFolderEntity;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

public class ImagesFoldersListViewModel extends AndroidViewModel {
    private static volatile ImagesFoldersListViewModel sImagesFoldersListViewModel;
    private VideoSearchDatabase mDatabase;
    private ImagesFoldersListViewModel(@NonNull Application application) {
        super(application);
        mDatabase = VideoSearchDatabase.getVideoSearchDatabase(application.getApplicationContext());
    }

    public static ImagesFoldersListViewModel getInstance(@NonNull Application application) {
        if (sImagesFoldersListViewModel == null) {
            sImagesFoldersListViewModel = new ImagesFoldersListViewModel(application);
        }
        return sImagesFoldersListViewModel;
    }

    public LiveData<ImagesFolderEntity> getImagesFolder(long folderId) {
        return mDatabase.getImagesFolderDao().getImagesFolderEntity(folderId);
    }

    public void update(ImagesFolderEntity imagesFolderEntity,
                       List<ImageFacesEntity> listImagesWithFaces,
                       List<FaceEntity> listFaces) {
        mDatabase.runInTransaction(() -> {
            mDatabase.getImagesFolderDao().insert(imagesFolderEntity);
            mDatabase.getImageFacesDao().insertAll(listImagesWithFaces);
            mDatabase.getFacesDao().insertAll(listFaces);
        });
    }
}
