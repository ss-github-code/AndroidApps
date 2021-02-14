package com.vidsearch.videosearchapp.ui;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;

import com.vidsearch.videosearchapp.R;
import com.vidsearch.videosearchapp.databinding.FaceNameDialogBinding;
import com.vidsearch.videosearchapp.db.entity.ImageFacesEntity;
import com.vidsearch.videosearchapp.db.entity.SelectedFaceEntity;
import com.vidsearch.videosearchapp.images.CustomAsyncTask;
import com.vidsearch.videosearchapp.images.ImageResizer;
import com.vidsearch.videosearchapp.ui.utils.Constants;
import com.vidsearch.videosearchapp.ui.utils.FacesImageTask;
import com.vidsearch.videosearchapp.ui.utils.ImagesFolderMerged;
import com.vidsearch.videosearchapp.ui.utils.SelectedFacesMerged;

public class FaceNameDialogActivity extends Activity {
    private FaceNameDialogBinding mBinding;
    private SelectedFaceEntity mSelectedFaceEntity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = FaceNameDialogBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        //mBinding.faceImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        int thumbnailSize = getResources().getDimensionPixelSize(R.dimen.face_thumbnail_size);
        Intent intent = getIntent();
        SelectedFaceEntity selectedFaceEntity = (SelectedFaceEntity)intent.getSerializableExtra(Constants.KEY_FACE_ENTITY);
        long folderId = intent.getLongExtra(Constants.KEY_IMAGES_FOLDER_ID, 0);
        boolean fromDb = intent.getBooleanExtra(Constants.KEY_FACE_ENTITY_FROM_DB, false);
        int posInMergedList = intent.getIntExtra(Constants.KEY_POS_IN_MERGED_LIST, 0);

        if (posInMergedList != -1) {
            ImagesFolderMerged imagesFolderMerged = ImagesFolderMerged.getInstance(this, folderId);
            List<ImagesFolderMerged.ImageWFaces> imageWFacesList = imagesFolderMerged.getImageWFacesList();
            ImagesFolderMerged.ImageWFaces imageWFaces = imageWFacesList.get(posInMergedList);

            ImageResizer imgResizer = new ImageResizer(this, thumbnailSize, thumbnailSize, Constants.IMAGES_FACES_KEY_PREFIX);
            imgResizer.loadFaceImage(imageWFaces, selectedFaceEntity.getFaceNum(), mBinding.faceImageView, null);
        }
        else {
            ImageFacesEntity imageFacesEntity = (ImageFacesEntity)intent.getSerializableExtra(Constants.KEY_IMAGE_FACES_ENTITY);
            SelectedFacesMerged.SelectedImageInfo selectedImageInfo = SelectedFacesMerged.getSelectedFacePath(this, selectedFaceEntity);
            ImagesFolderMerged.ImageWFaces imageWFaces =
                    new ImagesFolderMerged.ImageWFaces(selectedFaceEntity.getPicId(),
                            selectedImageInfo.path, selectedImageInfo.orientation, imageFacesEntity);
            Rect displayR = new Rect(intent.getIntExtra(Constants.KEY_DISPLAY_RECT_L, 0),
                                     intent.getIntExtra(Constants.KEY_DISPLAY_RECT_T, 0),
                                     intent.getIntExtra(Constants.KEY_DISPLAY_RECT_R, 0),
                                     intent.getIntExtra(Constants.KEY_DISPLAY_RECT_B, 0));
            ImageResizer imgResizer = new ImageResizer(this, thumbnailSize, thumbnailSize, Constants.IMAGE_SELECTED_FACES_KEY_PREFIX);
            imgResizer.loadFaceImage(imageWFaces, selectedFaceEntity.getFaceNum(), mBinding.faceImageView, displayR);
        }

        if (!fromDb) {
            mBinding.deleteButton.setVisibility(View.GONE);
        }

        String faceName = selectedFaceEntity.getName();
        mBinding.faceName.setText(faceName);
        mBinding.familyCheck.setChecked(selectedFaceEntity.getIsFamily());
        mSelectedFaceEntity = selectedFaceEntity;

        if (faceName.isEmpty()) {
            mBinding.saveButton.setEnabled(false);
        }

        mBinding.faceName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(charSequence.toString().trim().length()==0){
                    mBinding.saveButton.setEnabled(false);
                } else {
                    mBinding.saveButton.setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        mBinding.saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSelectedFaceEntity.setName(mBinding.faceName.getText().toString());
                mSelectedFaceEntity.setIsFamily(mBinding.familyCheck.isChecked());
                FacesImageTask task = new FacesImageTask("",
                        FaceNameDialogActivity.this,
                        mSelectedFaceEntity, FacesImageTask.FACES_IMAGE_TASK_MODE.MODE_SAVE);
                // custom version of AsyncTask
                task.executeOnExecutor(CustomAsyncTask.DUAL_THREAD_EXECUTOR);
            }
        });
        mBinding.cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        mBinding.deleteButton.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {
                 FacesImageTask task = new FacesImageTask("",
                         FaceNameDialogActivity.this, mSelectedFaceEntity,
                         FacesImageTask.FACES_IMAGE_TASK_MODE.MODE_DELETE);
                 // custom version of AsyncTask
                 task.executeOnExecutor(CustomAsyncTask.DUAL_THREAD_EXECUTOR);
             }
         });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
