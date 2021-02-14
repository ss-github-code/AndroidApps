package com.vidsearch.videosearchapp.db.dao;

import com.vidsearch.videosearchapp.db.entity.ImagesFolderEntity;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface ImagesFolderDao {
    @Query("SELECT * FROM images_folder_table WHERE folderId = :folderId")
    LiveData<ImagesFolderEntity> getImagesFolderEntity(long folderId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ImagesFolderEntity imagesFolderEntity);
}
