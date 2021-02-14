package com.vidsearch.videosearchapp.ui;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.vidsearch.videosearchapp.R;
import com.vidsearch.videosearchapp.databinding.SelectedImageFragmentBinding;
import com.vidsearch.videosearchapp.db.entity.SelectedFaceEntity;
import com.vidsearch.videosearchapp.images.CustomAsyncTask;
import com.vidsearch.videosearchapp.images.ImageResizer;
import com.vidsearch.videosearchapp.ui.utils.Constants;
import com.vidsearch.videosearchapp.ui.utils.FacesImageTask;
import com.vidsearch.videosearchapp.ui.utils.ImagesFolderMerged;
import com.vidsearch.videosearchapp.ui.utils.SelectedFacesMerged;

import java.util.List;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SelectedImageFragment extends Fragment {
    private final static String TAG = "SelectedImageFragment";
    private long mFolderId;
    private ImageResizer mImageResizer;
    private ImagesFolderMerged.ImageWFaces mImageWFaces = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int imageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_faces_thumbnail_size);
        mImageResizer = new ImageResizer(
                getActivity(),
                imageThumbSize, imageThumbSize, Constants.IMAGE_SELECTED_FACES_KEY_PREFIX);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        SelectedImageFragmentBinding binding = SelectedImageFragmentBinding.inflate(inflater, container, false);

        Bundle args = getArguments();
        SelectedFaceEntity selectedFaceEntity = (SelectedFaceEntity)args.getSerializable(Constants.KEY_FACE_ENTITY);
        SelectedFacesMerged.SelectedImageInfo selectedImageInfo = SelectedFacesMerged.getSelectedFacePath(getContext(), selectedFaceEntity);

        mImageResizer.setExitTasksEarly(false); // selected image fragments can be stacked on top of another
        mImageResizer.loadSelectedImage(selectedImageInfo.path, selectedImageInfo.orientation, selectedFaceEntity, this, binding.selectedImageFacesImg);
        binding.selectedImageFacesImg.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (mImageWFaces != null) {
                    List<ImagesFolderMerged.FaceInfo> displayFaceInfoList = mImageWFaces.getFaceInfoList();
                    int x = (int)motionEvent.getX();
                    int y = (int)motionEvent.getY();
                    //Log.d(TAG, "x: " + x + ", " + y);
                    for (int i = 0; i < displayFaceInfoList.size(); i++) {
                        ImagesFolderMerged.FaceInfo faceInfo = displayFaceInfoList.get(i);
                        Rect displayR = faceInfo.getDisplayRect();
                        if (displayR.contains(x, y)) {
                            FacesImageTask task = new FacesImageTask("", getActivity(),
                                    mFolderId, mImageWFaces.getPicId(), faceInfo.getFaceId(),
                                    -1, i, mImageWFaces.getImageFacesEntity(), displayR);
                            // custom version of AsyncTask
                            task.executeOnExecutor(CustomAsyncTask.DUAL_THREAD_EXECUTOR);
                            break;
                        }
                    }
                }
                return false;
            }
        });
        return binding.getRoot();
    }

    public void setResutFromImageResizer(long folderId, ImagesFolderMerged.ImageWFaces imageWFaces) {
        mFolderId = folderId;
        mImageWFaces = imageWFaces;
    }

    @Override
    public void onPause() {
        super.onPause();
        mImageResizer.setExitTasksEarly(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        mImageResizer.setExitTasksEarly(false);
    }
}
