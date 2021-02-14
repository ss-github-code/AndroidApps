package com.vidsearch.videosearchapp.ui.utils;

import android.content.Context;
import android.database.Cursor;
import android.media.MediaCodec;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImagesFoldersMS {
    public static class ImagesFolderMS {
        private final String mFolderName;
        private final long mFolderId;
        private String mData;
        private long mPicId;
        private int mPicsCount;
        private int mOrientation;

        private ImagesFolderMS(String folderName, long folderId, String data, long picId, int orientation) {
            mFolderName = folderName;
            mFolderId = folderId;
            mData = data;
            mPicId = picId;
            mOrientation = orientation;
            mPicsCount = 1;
        }

        public int getPicsCount() {
            return mPicsCount;
        }
        public String getFolderName() {
            return mFolderName;
        }
        public long getFolderId() {
            return mFolderId;
        }
        public long getPicId() {
            return mPicId;
        }
        public String getData() {
            return mData;
        }
        public int getOrientation() {
            return mOrientation;
        }
    }
    private static List<ImagesFolderMS> mImagesFoldersListMS = null;

    public static List<ImagesFolderMS> getImagesFolders(Context context) {
        if (mImagesFoldersListMS == null) {
            mImagesFoldersListMS = new ArrayList<>();
            Map<Long, ImagesFolderMS> folderIdsWithCount = new HashMap<>();

            Uri allImagesuri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            String[] projection = {
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                    MediaStore.Images.Media.BUCKET_ID,
                    MediaStore.Images.Media.ORIENTATION,
                    MediaStore.Images.Media._ID};
            Cursor cursor = context.getContentResolver().query(allImagesuri, projection, null, null, null);
            try {
                if (cursor != null) {
                    cursor.moveToFirst();
                }
                do {
                    String data = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
                    long bucketId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID));
                    long picId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
                    int orientation = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.ORIENTATION));

                    if (folderIdsWithCount.containsKey(bucketId) == false) {
                        String folderName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME));
                        ImagesFolderMS imgsFolder = new ImagesFolderMS(folderName, bucketId, data, picId, orientation);
                        mImagesFoldersListMS.add(imgsFolder);
                        folderIdsWithCount.put(bucketId, imgsFolder);
                        //Log.d("Images", folderName + ", " + data);
                    } else {
                        ImagesFolderMS imgsFolder = folderIdsWithCount.get(bucketId);
                        imgsFolder.mPicsCount++;
                        imgsFolder.mPicId = picId;
                        imgsFolder.mData = data;
                        imgsFolder.mOrientation = orientation;
                    }
                } while (cursor.moveToNext());
                cursor.close();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
        return mImagesFoldersListMS;
    }
}
