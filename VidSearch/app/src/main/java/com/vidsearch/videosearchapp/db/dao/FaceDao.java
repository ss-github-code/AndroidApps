package com.vidsearch.videosearchapp.db.dao;

import com.vidsearch.videosearchapp.db.entity.FaceEntity;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface FaceDao {
    @Query("SELECT * FROM face_table WHERE picId = :picId")
    List<FaceEntity> getFacesInPic(long picId);

    @Query("SELECT * FROM face_table WHERE picId = :picId AND faceId = :faceId")
    FaceEntity getFaceInPic(long picId, int faceId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<FaceEntity> listFaceEntity);
}
