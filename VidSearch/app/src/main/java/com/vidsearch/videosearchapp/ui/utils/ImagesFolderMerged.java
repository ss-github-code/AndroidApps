package com.vidsearch.videosearchapp.ui.utils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.vidsearch.videosearchapp.db.entity.FaceEntity;
import com.vidsearch.videosearchapp.db.entity.ImageFacesEntity;
import com.vidsearch.videosearchapp.images.ImageWorker;

import java.util.ArrayList;
import java.util.List;

public class ImagesFolderMerged {
    private static final String TAG = "ImagesFolderMerged";
    private long mFolderId;
    private static ImagesFolderMerged sInstance = null;
    private static List<ImageMS> sImageMSList = null;
    private static List<ImageWFaces> sImageWFacesList = null;

    public class ImageMS { // from MediaStore
        private String mData;
        private long mPicId;
        private int mOrientation;

        public long getPicId() {
            return mPicId;
        }
        public String getData() {
            return mData;
        }
        public int getOrientation() {
            return mOrientation;
        }
        public ImageMS(String data, long picId, int orientation) {
            this.mData = data;
            this.mPicId = picId;
            this.mOrientation = orientation;
        }
    }

    public static class FaceInfo { // Used for display
        private int mFaceId;
        private Rect mImageR;
        private Rect mDispayR;
        public FaceInfo(int faceId, Rect imageR) {
            mFaceId = faceId;
            mImageR = imageR;
        }
        public int getFaceId() {
            return mFaceId;
        }
        public Rect getImageRect() {
            return mImageR;
        }
        public Rect getDisplayRect() {
            return mDispayR;
        }
    }

    public static class ImageWFaces { // Merged
        private long mPicId;
        private String mPath;
        private int mOrientation;
        private ImageFacesEntity mImageFacesEntity;
        private List<FaceInfo> mFaceDetectedInfoList;

        public ImageWFaces(long picId, String path, int orientation, ImageFacesEntity imageFacesEntity) {
            mPicId = picId;
            mPath = path;
            mOrientation = orientation;
            mImageFacesEntity = imageFacesEntity;
            mFaceDetectedInfoList = new ArrayList<>(imageFacesEntity.getNumFacesDetected());
        }

        public long getPicId() {
            return mPicId;
        }
        public String getPath() {
            return mPath;
        }
        public int getOrientation() {
            return mOrientation;
        }
        public int getNumFacesDetected() {
            return mImageFacesEntity.getNumFacesDetected();
        }
        public List<FaceInfo> getFaceInfoList() {
            return mFaceDetectedInfoList;
        }
        public ImageFacesEntity getImageFacesEntity() {
            return mImageFacesEntity;
        }

        public int addFaceRect(FaceEntity faceEntity) {
            Rect r = new Rect();

            r.left = faceEntity.getTlX(); r.top = faceEntity.getTlY();
            r.right = faceEntity.getBrX(); r.bottom = faceEntity.getBrY();

            FaceInfo faceInfo = new FaceInfo(faceEntity.getFaceId(), r);
            mFaceDetectedInfoList.add(faceInfo);
            return mFaceDetectedInfoList.size()-1;
        }

        public void addFaceDisplayRect(int facePosition, ImageWorker.ScaleInfo scaleInfo) {
            FaceInfo faceInfo = mFaceDetectedInfoList.get(facePosition);
            Rect r = faceInfo.mImageR;
            Rect displayR = new Rect();

            displayR.left = Math.max(Math.round(r.left*scaleInfo.mScale) - scaleInfo.mScaledSrcX, 0);
            displayR.top = Math.max(Math.round(r.top*scaleInfo.mScale) - scaleInfo.mScaledSrcY, 0);
            displayR.right = Math.max(Math.round(r.right*scaleInfo.mScale) - scaleInfo.mScaledSrcX, 0);
            displayR.right = Math.min(displayR.right, scaleInfo.mScaledWidth);
            displayR.bottom = Math.max(Math.round(r.bottom*scaleInfo.mScale) - scaleInfo.mScaledSrcY, 0);
            displayR.bottom = Math.min(displayR.bottom, scaleInfo.mScaledHeight);
            faceInfo.mDispayR = displayR;

            //Log.d(TAG, "Scale: " + scaleInfo.mScale + "; scaledSrcY: " + scaleInfo.mScaledSrcY +", " + scaleInfo.mScaledSrcX);
            //Log.d(TAG, "displayR: " + displayR + ", W: " + scaleInfo.mScaledWidth + ", H: " + scaleInfo.mScaledHeight);
        }
    }

    private ImagesFolderMerged(long folderID) {
        mFolderId = folderID;
    }

    public static ImagesFolderMerged getInstance(Context context, long folderId) {
        if (sInstance == null) {
            sInstance = new ImagesFolderMerged(folderId);
            sImageMSList = sInstance.getImagesInMSFolder(context);
        } else if (sInstance.mFolderId != folderId) {
            sInstance.mFolderId = folderId;
            sImageMSList = sInstance.getImagesInMSFolder(context);
            sImageWFacesList = null;
        }
        return sInstance;
    }

    public List<ImagesFolderMerged.ImageMS> getImageMSList() {
        return sInstance.sImageMSList;
    }

    public List<ImageWFaces> getImageWFacesList() {
        return sInstance.sImageWFacesList;
    }

    public int addImageWFaces(int position, ImageFacesEntity imageFacesEntity) {
        if (sImageWFacesList == null) {
            sImageWFacesList = new ArrayList<>();
        }
        ImageMS imageMS = sImageMSList.get(position);
        ImageWFaces imageWFaces = new ImageWFaces(imageMS.mPicId, imageMS.mData, imageMS.mOrientation, imageFacesEntity);
        sImageWFacesList.add(imageWFaces);
        return sImageWFacesList.size()-1; // returns the index of the newly created entry
    }

    public void addFaceRect(int index, FaceEntity faceEntity) {
        ImageWFaces imageWFaces = sImageWFacesList.get(index);
        Rect r = new Rect();

        r.left = faceEntity.getTlX(); r.top = faceEntity.getTlY();
        r.right = faceEntity.getBrX(); r.bottom = faceEntity.getBrY();

        FaceInfo faceInfo = new FaceInfo(faceEntity.getFaceId(), r);
        imageWFaces.mFaceDetectedInfoList.add(faceInfo);
    }

    private List<ImagesFolderMerged.ImageMS> getImagesInMSFolder(Context context) {
        List<ImageMS> imagesInFolderList = new ArrayList<>();

        Uri allImagesuri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.ORIENTATION,
                MediaStore.Images.ImageColumns._ID};
        String[] selectionArgs = {Long.toString(mFolderId)};
        Cursor cursor = context.getContentResolver().query(allImagesuri, projection, MediaStore.Images.Media.BUCKET_ID+"=?", selectionArgs, null);
        try {
            if (cursor != null) {
                cursor.moveToFirst();
            }
            do {
                String data = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
                long picId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
                int orientation = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.ORIENTATION));

                ImageMS imgMS = new ImageMS(data, picId, orientation);
                imagesInFolderList.add(imgMS);
            } while (cursor.moveToNext());
            cursor.close();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        return imagesInFolderList;
    }
}
