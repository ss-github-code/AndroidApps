package com.vidsearch.videosearchapp.ui.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.vidsearch.videosearchapp.db.entity.FaceEntity;
import com.vidsearch.videosearchapp.db.entity.SelectedFaceEntity;

public class SelectedFacesMerged {
    public static class SelectedImageInfo {
        public String path;
        public int orientation;
    }

    public static SelectedImageInfo getSelectedFacePath(Context context, SelectedFaceEntity selectedFaceEntity) {
        SelectedImageInfo selectedImageInfo = new SelectedImageInfo();
        selectedImageInfo.path = "";
        Uri allImagesuri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.ORIENTATION,
                MediaStore.Images.ImageColumns._ID};
        String[] selectionArgs = {Long.toString(selectedFaceEntity.getPicId())};
        Cursor cursor = context.getContentResolver().query(allImagesuri, projection, MediaStore.Images.Media._ID+"=?", selectionArgs, null);
        try {
            if (cursor != null) {
                cursor.moveToFirst();
                selectedImageInfo.path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
                selectedImageInfo.orientation = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.ORIENTATION));
                cursor.close();
            }
        } catch (IllegalArgumentException e) {
        }
        return selectedImageInfo;
    }
}
