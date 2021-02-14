package com.vidsearch.videosearchapp.ui.utils;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.util.Log;

import com.vidsearch.videosearchapp.db.entity.ImageFacesEntity;
import com.vidsearch.videosearchapp.db.entity.SelectedFaceEntity;
import com.vidsearch.videosearchapp.images.CustomAsyncTask;
import com.vidsearch.videosearchapp.ui.FaceNameDialogActivity;
import com.vidsearch.videosearchapp.viewmodel.SelectedFacesListViewModel;

import java.lang.ref.WeakReference;
import java.util.List;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentActivity;

public class FacesImageTask extends CustomAsyncTask<Void, Void, SelectedFaceEntity> {

    private final long mFolderId;
    private final long mPicId;
    private final int mFaceId;
    private final int mPosInMergedList; // could be -1
    private final int mSelectedFaceNum;

    private SelectedFacesListViewModel mSelectedFacesListVM;
    private WeakReference<FragmentActivity> mFragmentActivityRef;
    private WeakReference<Activity> mActivityRef;
    private SelectedFaceEntity mSelectedFaceEntity; // save or delete
    private ImageFacesEntity mImageFacesEntity;
    private Rect mDisplayR;

    public enum FACES_IMAGE_TASK_MODE {
        MODE_LOOKUP,
        MODE_SAVE,
        MODE_DELETE,
    }
    private final FACES_IMAGE_TASK_MODE mMode;

    public FacesImageTask(String key, FragmentActivity fragmentActivity, long folderId, long picId,
                          int faceId, int posInMergedList, int selectedFaceNum,
                          ImageFacesEntity imageFacesEntity, Rect displayR) {
        super(key);
        mMode = FACES_IMAGE_TASK_MODE.MODE_LOOKUP;
        mFolderId = folderId;
        mPicId = picId;
        mFaceId = faceId;
        mPosInMergedList = posInMergedList; // could be -1
        mSelectedFaceNum = selectedFaceNum;
        mFragmentActivityRef = new WeakReference<>(fragmentActivity);
        mSelectedFacesListVM = SelectedFacesListViewModel.getInstance(fragmentActivity.getApplication());
        mImageFacesEntity = imageFacesEntity; // is null when posInMergedList is not -1
        mDisplayR = displayR; // is nill when posInMergedList is not -1
    }

    public FacesImageTask(String key, Activity activity, SelectedFaceEntity entity, FACES_IMAGE_TASK_MODE mode) {
        super(key);
        mMode = mode;
        mFolderId = -1; // not used
        mPicId = entity.getPicId();
        mFaceId = entity.getFaceId();
        mPosInMergedList = -1; // not used
        mSelectedFaceNum = entity.getFaceNum();
        mActivityRef = new WeakReference<>(activity);
        mSelectedFacesListVM = SelectedFacesListViewModel.getInstance(activity.getApplication());
        mSelectedFaceEntity = entity;
    }

    @Override
    protected SelectedFaceEntity doInBackground(Void... voids) {
        switch (mMode) {
            case MODE_LOOKUP:
                return mSelectedFacesListVM.getSelectedFace(mPicId, mSelectedFaceNum);
            case MODE_SAVE:
                mSelectedFacesListVM.insertSync(mSelectedFaceEntity);
                return mSelectedFaceEntity;
            case MODE_DELETE:
                mSelectedFacesListVM.deleteSync(mSelectedFaceEntity.getFaceId());
            default:
                return null;
        }
    }

    @Override
    protected void onPostExecute(SelectedFaceEntity selectedFaceEntity) {
        switch (mMode) {
            case MODE_LOOKUP:
                FragmentActivity fragmentActivity = mFragmentActivityRef.get();
                if (fragmentActivity != null) {
                    Intent intent = new Intent(fragmentActivity, FaceNameDialogActivity.class);
                    boolean fromDb = true;
                    if (selectedFaceEntity == null) {
                        selectedFaceEntity = new SelectedFaceEntity(mFaceId, mPicId, mSelectedFaceNum, "", false);
                        fromDb = false;
                    }
                    intent.putExtra(Constants.KEY_FACE_ENTITY, selectedFaceEntity);
                    intent.putExtra(Constants.KEY_IMAGES_FOLDER_ID, mFolderId);
                    intent.putExtra(Constants.KEY_POS_IN_MERGED_LIST, mPosInMergedList);
                    intent.putExtra(Constants.KEY_FACE_ENTITY_FROM_DB, fromDb);
                    if (mPosInMergedList == -1) {
                        intent.putExtra(Constants.KEY_IMAGE_FACES_ENTITY, mImageFacesEntity);
                        intent.putExtra(Constants.KEY_DISPLAY_RECT_L, mDisplayR.left);
                        intent.putExtra(Constants.KEY_DISPLAY_RECT_T, mDisplayR.top);
                        intent.putExtra(Constants.KEY_DISPLAY_RECT_R, mDisplayR.right);
                        intent.putExtra(Constants.KEY_DISPLAY_RECT_B, mDisplayR.bottom);
                    }

                    fragmentActivity.startActivity(intent);
                }
                break;
            case MODE_SAVE:
            case MODE_DELETE:
                Activity activity = mActivityRef.get();
                if (activity != null) {
                    activity.finish();
                }
            default:
                return;
        }
    }
}
