package com.vidsearch.videosearchapp.ui.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VideosFoldersMS {
    public static class VideoMS {
        private long mVidId;
        private String mData;
        public VideoMS(long vidId, String data) {
            mVidId = vidId;
            mData = data;
        }
        public String getData() {
            return mData;
        }
    }
    public static class VideosFolderMS {
        private final long mFolderId;
        private final String mFolderName;
        private int mVidsCount;
        private List<VideosFoldersMS.VideoMS> mVidMSList;

        private VideosFolderMS(long folderId, String folderName) {
            mFolderId = folderId;
            mFolderName = folderName;
            mVidMSList = new ArrayList<>();
            mVidsCount = 1;
        }

        public int getVidsCount() {
            return mVidsCount;
        }
        public List<VideoMS> getVidMSList() {
            return mVidMSList;
        }
        public String getVidsFolderName() {
            return mFolderName;
        }
    }

    private static List<VideosFolderMS> mVideosFoldersListMS = null;

    public static List<VideosFolderMS> getVideosFolders(Context context) {
        if (mVideosFoldersListMS == null) {
            mVideosFoldersListMS = new ArrayList<>();
            Map<Long, VideosFolderMS> folderIdsWithCount = new HashMap<>();

            Uri allImagesuri = android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            String[] projection = {
                    MediaStore.Video.Media.DATA,
                    MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                    MediaStore.Video.Media.BUCKET_ID,
                    MediaStore.Video.Media._ID};
            Cursor cursor = context.getContentResolver().query(allImagesuri, projection, null, null, null);
            try {
                if (cursor != null) {
                    cursor.moveToFirst();
                }
                do {
                    String data = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
                    long bucketId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID));
                    long vidId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID));

                    if (folderIdsWithCount.containsKey(bucketId) == false) {
                        String folderName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME));
                        VideosFolderMS vidsFolderMS = new VideosFolderMS(bucketId, folderName);
                        VideoMS vidMS = new VideoMS(vidId, data);
                        mVideosFoldersListMS.add(vidsFolderMS);
                        vidsFolderMS.mVidMSList.add(vidMS);
                        folderIdsWithCount.put(bucketId, vidsFolderMS);
                    } else {
                        VideosFolderMS vidsFolderMS = folderIdsWithCount.get(bucketId);
                        vidsFolderMS.mVidsCount++;
                        VideoMS vidMS = new VideoMS(vidId, data);
                        vidsFolderMS.mVidMSList.add(vidMS);
                    }
                } while (cursor.moveToNext());
                cursor.close();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
        return mVideosFoldersListMS;
    }
}
