package com.vidsearch.videosearchapp.db.dao;

import com.vidsearch.videosearchapp.db.entity.ImageFacesEntity;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface ImageFacesDao {
    @Query("SELECT * FROM image_faces_table WHERE folderId = :folderId")
    List<ImageFacesEntity> getImagesSync(long folderId);

    @Query("SELECT * FROM image_faces_table WHERE folderId = :folderId AND picId = :picId")
    ImageFacesEntity getImageSync(long folderId, long picId);

    @Query("SELECT * FROM image_faces_table WHERE picId = :picId")
    ImageFacesEntity getImageSync(long picId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ImageFacesEntity> listImageFaces);

    @Update
    void update(ImageFacesEntity imageFacesEntity);
}
