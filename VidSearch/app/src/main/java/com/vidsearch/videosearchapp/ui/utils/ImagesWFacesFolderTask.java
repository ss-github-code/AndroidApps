package com.vidsearch.videosearchapp.ui.utils;

import com.vidsearch.videosearchapp.db.entity.FaceEntity;
import com.vidsearch.videosearchapp.db.entity.ImageFacesEntity;
import com.vidsearch.videosearchapp.images.CustomAsyncTask;
import com.vidsearch.videosearchapp.images.ImageResizer;
import com.vidsearch.videosearchapp.images.ImageWorker;
import com.vidsearch.videosearchapp.ui.ImagesFoldersActivity;
import com.vidsearch.videosearchapp.viewmodel.FacesListViewModel;
import com.vidsearch.videosearchapp.viewmodel.ImagesFacesListViewModel;

import java.lang.ref.WeakReference;
import java.util.List;

import androidx.fragment.app.FragmentActivity;

public class ImagesWFacesFolderTask extends CustomAsyncTask<Void, Void, Void> {
    private boolean mExitTasksEarly = false;

    private long mFolderId;
    private String mFolderName;
    ImagesFolderMerged mImagesFolderMerged;
    private int mThumbnailSize;

    private WeakReference<FragmentActivity> mFragmentActivityRef;
    private ImagesFacesListViewModel mImagesFacesListVM;
    private FacesListViewModel mFacesListVM;

    public ImagesWFacesFolderTask(String key, FragmentActivity fragmentActivity, long folderId, String folderName, int thumbNailSize) {
        super(key);
        mFolderId = folderId;
        mFolderName = folderName;
        mThumbnailSize = thumbNailSize;

        mFragmentActivityRef = new WeakReference<>(fragmentActivity);
        mImagesFolderMerged = ImagesFolderMerged.getInstance(fragmentActivity.getApplication(), folderId);
        mImagesFacesListVM = ImagesFacesListViewModel.getInstance(fragmentActivity.getApplication());
        mFacesListVM = FacesListViewModel.getInstance(fragmentActivity.getApplication());
    }

    @Override
    protected Void doInBackground(Void... voids) {
        List<ImagesFolderMerged.ImageWFaces> imageWFacesList = mImagesFolderMerged.getImageWFacesList();
        if (imageWFacesList != null) {
            return null;
        }
        List<ImagesFolderMerged.ImageMS> imageMSList = mImagesFolderMerged.getImageMSList();

        for (int i = 0; i < imageMSList.size(); i++) {
            ImagesFolderMerged.ImageMS imageMS = imageMSList.get(i);
            ImageFacesEntity imageFacesEntity = mImagesFacesListVM.getImageFaces(mFolderId, imageMS.getPicId());
            if (imageFacesEntity != null) {
                ImageWorker.ScaleInfo scaleInfo = ImageResizer.getScaleInfoForBitmap(imageMS.getData(), mThumbnailSize, mThumbnailSize);
                int idx = mImagesFolderMerged.addImageWFaces(i, imageFacesEntity);
                ImagesFolderMerged.ImageWFaces imageWFaces = mImagesFolderMerged.getImageWFacesList().get(idx);

                List<FaceEntity> facesInImage = mFacesListVM.getFacesInImage(imageFacesEntity.getPicId());
                for (int j = 0; j < facesInImage.size(); j++) {
                    FaceEntity faceEntity = facesInImage.get(j);
                    mImagesFolderMerged.addFaceRect(idx, faceEntity);
                    imageWFaces.addFaceDisplayRect(j, scaleInfo);
                }
            }
            if (mExitTasksEarly)
                break;
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void v) {
        if (isCancelled() || mExitTasksEarly) {
            return;
        }
        FragmentActivity fragmentActivity = mFragmentActivityRef.get();
        if (fragmentActivity != null) {
            ((ImagesFoldersActivity)fragmentActivity)
                    .showImagesFolder(mFolderId, mFolderName);
        }
    }

    public void setExitTasksEarly(boolean exitTasksEarly) {
        mExitTasksEarly = exitTasksEarly;
    }
}
