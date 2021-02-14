package com.vidsearch.videosearchapp.db.entity;

import java.io.Serializable;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "selected_faces_table", foreignKeys = {
        @ForeignKey(entity = FaceEntity.class,
                    parentColumns = "faceId", childColumns = "faceId",
                    onDelete = ForeignKey.CASCADE)}
)
public class SelectedFaceEntity implements Serializable {
    @PrimaryKey
    @ColumnInfo(name = "faceId")
    private int mFaceId;
    @ColumnInfo(name = "picId")
    private long mPicId;
    @ColumnInfo(name = "faceNum")
    private int mFaceNum;
    @ColumnInfo(name = "name")
    private String mName;
    @ColumnInfo(name = "isFamily")
    private boolean mIsFamily;

    public int getFaceId() {
        return mFaceId;
    }
    public long getPicId() {
        return mPicId;
    }
    public int getFaceNum() {
        return mFaceNum;
    }
    public String getName() {
        return mName;
    }
    public void setName(String name) {
        mName = name;
    }
    public void setIsFamily(boolean isFamily) {
        mIsFamily = isFamily;
    }
    public boolean getIsFamily() {
        return mIsFamily;
    }
    public SelectedFaceEntity(int faceId, long picId, int faceNum, String name, boolean isFamily) {
        this.mFaceId = faceId;
        this.mPicId = picId;
        this.mFaceNum = faceNum;
        this.mName = name;
        this.mIsFamily = isFamily;
    }
}
