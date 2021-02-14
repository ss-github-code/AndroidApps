package com.vidsearch.videosearchapp.db;

import android.content.Context;

import com.vidsearch.videosearchapp.db.converter.DateConverter;
import com.vidsearch.videosearchapp.db.dao.FaceDao;
import com.vidsearch.videosearchapp.db.dao.ImageFacesDao;
import com.vidsearch.videosearchapp.db.dao.ImagesFolderDao;
import com.vidsearch.videosearchapp.db.dao.SelectedFacesDao;
import com.vidsearch.videosearchapp.db.entity.FaceEntity;
import com.vidsearch.videosearchapp.db.entity.ImageFacesEntity;
import com.vidsearch.videosearchapp.db.entity.ImagesFolderEntity;
import com.vidsearch.videosearchapp.db.entity.SelectedFaceEntity;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(entities = {FaceEntity.class, ImageFacesEntity.class, ImagesFolderEntity.class, SelectedFaceEntity.class}, version = 1, exportSchema = false)
@TypeConverters(DateConverter.class)
public abstract class VideoSearchDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "video_search_db";

    public abstract FaceDao getFacesDao();
    public abstract ImageFacesDao getImageFacesDao();
    public abstract ImagesFolderDao getImagesFolderDao();
    public abstract SelectedFacesDao getSelectedFacesDao();
    //static final ExecutorService sDbWriteExecutor = Executors.newFixedThreadPool(4);

    private static volatile VideoSearchDatabase sVideoSearchDatabase;

    public static VideoSearchDatabase getVideoSearchDatabase(final Context context) {
        if (sVideoSearchDatabase == null) {
            synchronized (VideoSearchDatabase.class) {
                if (sVideoSearchDatabase == null) {
                    sVideoSearchDatabase = Room.databaseBuilder(context.getApplicationContext(),
                            VideoSearchDatabase.class, DATABASE_NAME)
                            .build();
                }
            }
        }
        return sVideoSearchDatabase;
    }

}
