package com.vidsearch.videosearchapp.viewmodel;

import android.app.Application;

import com.vidsearch.videosearchapp.db.VideoSearchDatabase;
import com.vidsearch.videosearchapp.db.entity.SelectedFaceEntity;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

public class SelectedFacesListViewModel extends AndroidViewModel {
    private static volatile SelectedFacesListViewModel sSelectedFacesListVM;
    private VideoSearchDatabase mDatabase;

    private SelectedFacesListViewModel(@NonNull Application application) {
        super(application);
        mDatabase = VideoSearchDatabase.getVideoSearchDatabase(application.getApplicationContext());
    }

    public static SelectedFacesListViewModel getInstance(@NonNull Application application) {
        if (sSelectedFacesListVM == null) {
            sSelectedFacesListVM = new SelectedFacesListViewModel(application);
        }
        return sSelectedFacesListVM;
    }

    public LiveData<List<SelectedFaceEntity>> getSelectedFaces() {
        return mDatabase.getSelectedFacesDao().getSelectedFaces();
    }

    public List<SelectedFaceEntity> getSelectedFacesSync() {
        return mDatabase.getSelectedFacesDao().getSelectedFacesSync();
    }

    public SelectedFaceEntity getSelectedFace(long picId, int faceNum) {
        return mDatabase.getSelectedFacesDao().getSelectedFaceSync(picId, faceNum);
    }

    public void insertSync(SelectedFaceEntity entity) {
        mDatabase.getSelectedFacesDao().insert(entity);
    }

    public void deleteSync(int faceId) {
        mDatabase.getSelectedFacesDao().deleteByFaceId(faceId);
    }
}
