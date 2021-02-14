package com.vidsearch.videosearchapp.db.entity;

import java.io.Serializable;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "image_faces_table", foreignKeys = {
        @ForeignKey(entity = ImagesFolderEntity.class,
                parentColumns = "folderId", childColumns = "folderId",
                onDelete = ForeignKey.CASCADE)},
        indices = {@Index(value = "folderId")}
)
public class ImageFacesEntity implements Serializable {
    @PrimaryKey
    @ColumnInfo(name = "picId")
    private long mPicId;
    @ColumnInfo(name = "folderId")
    private long mFolderId;
    @ColumnInfo(name = "facesDetected")
    private int mNumFacesDetected;

    public long getPicId() {
        return mPicId;
    }
    public long getFolderId() {
        return mFolderId;
    }
    public int getNumFacesDetected() {
        return mNumFacesDetected;
    }

    public ImageFacesEntity(long picId, long folderId, int numFacesDetected) {
        mPicId = picId;
        mFolderId = folderId;
        mNumFacesDetected = numFacesDetected;
    }
}
