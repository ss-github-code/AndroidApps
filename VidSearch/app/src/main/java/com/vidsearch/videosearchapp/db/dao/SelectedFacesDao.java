package com.vidsearch.videosearchapp.db.dao;

import com.vidsearch.videosearchapp.db.entity.SelectedFaceEntity;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface SelectedFacesDao {
    @Query("SELECT * FROM selected_faces_table")
    List<SelectedFaceEntity> getSelectedFacesSync();

    @Query("SELECT * FROM selected_faces_table")
    LiveData<List<SelectedFaceEntity>> getSelectedFaces();

    @Query("SELECT * FROM selected_faces_table WHERE picId = :picId AND faceNum = :faceNum")
    SelectedFaceEntity getSelectedFaceSync(long picId, int faceNum);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SelectedFaceEntity selectedFacesDb);

    @Query("DELETE FROM selected_faces_table WHERE faceId = :faceId")
    void deleteByFaceId(int faceId);
}
