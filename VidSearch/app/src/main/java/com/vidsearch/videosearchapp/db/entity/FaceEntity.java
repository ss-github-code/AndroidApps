package com.vidsearch.videosearchapp.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "face_table", foreignKeys = {
        @ForeignKey(entity = ImageFacesEntity.class,
                    parentColumns = "picId", childColumns = "picId",
                    onDelete = ForeignKey.CASCADE)},
        indices = {@Index(value = "picId")})
public class FaceEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "faceId")
    private int mFaceId;
    @ColumnInfo(name = "picId")
    private long mPicId;
    @ColumnInfo(name = "topLeftX")
    private int mTlX;
    @ColumnInfo(name = "topLeftY")
    private int mTlY;
    @ColumnInfo(name = "botRightX")
    private int mBrX;
    @ColumnInfo(name = "botRightY")
    private int mBrY;

    public int getFaceId() {
        return mFaceId;
    }
    public void setFaceId(int faceId) {
        mFaceId = faceId;
    }
    public long getPicId() {
        return mPicId;
    }
    public int getTlX() {
        return mTlX;
    }
    public int getTlY() {
        return mTlY;
    }
    public int getBrX() {
        return mBrX;
    }
    public int getBrY() {
        return mBrY;
    }

    public FaceEntity(long picId, int tlX, int tlY, int brX, int brY) {
        mPicId = picId;
        mTlX = tlX;
        mTlY = tlY;
        mBrX = brX;
        mBrY = brY;
    }
}
