package com.vidsearch.videosearchapp.db.entity;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "images_folder_table")
public class ImagesFolderEntity {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "folderId")
    private long mFolderId;

    @ColumnInfo(name = "dateScanned")
    private Date mDateScanned;

    public long getFolderId() {
        return mFolderId;
    }
    public Date getDateScanned() {
        return mDateScanned;
    }
    public ImagesFolderEntity(long folderId, Date dateScanned) {
        mFolderId = folderId;
        mDateScanned = dateScanned;
    }
}
