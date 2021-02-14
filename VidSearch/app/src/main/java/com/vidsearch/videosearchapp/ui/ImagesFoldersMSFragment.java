package com.vidsearch.videosearchapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.vidsearch.videosearchapp.R;
import com.vidsearch.videosearchapp.databinding.ImagesFolderItemBinding;
import com.vidsearch.videosearchapp.databinding.ImagesFoldersFragmentBinding;
import com.vidsearch.videosearchapp.db.entity.ImagesFolderEntity;
import com.vidsearch.videosearchapp.images.CustomAsyncTask;
import com.vidsearch.videosearchapp.images.ImageResizer;
import com.vidsearch.videosearchapp.ui.utils.Constants;
import com.vidsearch.videosearchapp.ui.utils.ImagesWFacesFolderTask;
import com.vidsearch.videosearchapp.ui.utils.ImagesFoldersMS;
import com.vidsearch.videosearchapp.viewmodel.ImagesFoldersListViewModel;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.vidsearch.videosearchapp.ui.utils.Constants.KEY_IMAGES_FOLDER_ID;
import static com.vidsearch.videosearchapp.ui.utils.Constants.KEY_IMAGES_FOLDER_NAME;
import static com.vidsearch.videosearchapp.ui.utils.Constants.KEY_NUM_IN_IMAGES_FOLDER;

public class ImagesFoldersMSFragment extends Fragment {
    private final static String TAG = "ImagesFoldersMSFragment" ;

    private ImagesFolderMSAdapter mImagesFolderMSAdapter;
    private ImageResizer mImageResizer;
    private ImagesWFacesFolderTask mImagesWFacesFolderTask;
    private boolean mStartedProgressActivity;

    /*
     * Empty constructor
     */
    public ImagesFoldersMSFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mImagesFolderMSAdapter = new ImagesFolderMSAdapter();
        int imageThumbSize = getResources().getDimensionPixelSize(R.dimen.images_folder_thumbnail_size);
        mImageResizer = new ImageResizer(
                getActivity(),
                imageThumbSize, imageThumbSize, Constants.IMAGES_FOLDERS_KEY_PREFIX);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        ImagesFoldersFragmentBinding binding = ImagesFoldersFragmentBinding.inflate(inflater, container, false);
        binding.imagesFoldersRecycler.setAdapter(mImagesFolderMSAdapter);
        binding.imagesFoldersRecycler.setLayoutManager(new GridLayoutManager(getContext(), 1));
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        mImageResizer.setExitTasksEarly(false);
        mImagesFolderMSAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        super.onPause();
        mImageResizer.setExitTasksEarly(true);
        if (mImagesWFacesFolderTask != null) {
            mImagesWFacesFolderTask.setExitTasksEarly(true);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null && requestCode == Constants.FACE_DETECTION_PROGRESS_ACTIVITY_REQ_CODE) {
            long folderId = data.getLongExtra(KEY_IMAGES_FOLDER_ID, 0);
            String folderName = data.getStringExtra(KEY_IMAGES_FOLDER_NAME);
            Log.d(TAG, "Result from face detection: " + resultCode);
            if (resultCode > 0)
                ((ImagesFoldersActivity)requireActivity()).showImagesFolder(folderId, folderName);
            else if (resultCode == 0) {
                ((ImagesFoldersActivity)requireActivity()).showNoImagesFragment(folderName);
            }
            mStartedProgressActivity = false;
        }
    }

    /*
     * The main adapter that backs the recycler view
     */
    private class ImagesFolderMSAdapter extends RecyclerView.Adapter<ImagesFolderViewHolder> {
        private List<ImagesFoldersMS.ImagesFolderMS> mImagesFoldersMS;

        ImagesFolderMSAdapter() {
            mImagesFoldersMS = ImagesFoldersMS.getImagesFolders(getContext());
        }

        @NonNull
        @Override
        public ImagesFolderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            ImagesFolderItemBinding binding = ImagesFolderItemBinding.inflate(inflater, parent, false);
            return new ImagesFolderViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ImagesFolderViewHolder holder, int position) {
            final ImagesFoldersMS.ImagesFolderMS imgsFolder = mImagesFoldersMS.get(position);
            String text = "(" + imgsFolder.getPicsCount() + ") " + imgsFolder.getFolderName();
            //Log.v("ImagesFoldersMSFragment", text);
            mImageResizer.loadFolderImage(imgsFolder.getData(), imgsFolder.getOrientation(), holder.mFolderImageView);
            holder.mFolderTextView.setText(text);
            holder.mFolderImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "onClick folder id: " + imgsFolder.getFolderId());
                    ImagesFoldersListViewModel viewModel = ImagesFoldersListViewModel.getInstance(getActivity().getApplication());
                    viewModel.getImagesFolder(imgsFolder.getFolderId()).observe(getViewLifecycleOwner(), new Observer<ImagesFolderEntity>() {
                        @Override
                        public void onChanged(ImagesFolderEntity imagesFolderEntity) {
                            // Log.d(TAG, "imagesFolder: " + imagesFolderEntity);
                            if (imagesFolderEntity == null) {
                                Intent intent = new Intent(getActivity(), ImagesFolderProgressActivity.class);
                                intent.putExtra(KEY_IMAGES_FOLDER_NAME, imgsFolder.getFolderName());
                                intent.putExtra(KEY_IMAGES_FOLDER_ID, imgsFolder.getFolderId());
                                intent.putExtra(KEY_NUM_IN_IMAGES_FOLDER, imgsFolder.getPicsCount());
                                mStartedProgressActivity = true;
                                startActivityForResult(intent, Constants.FACE_DETECTION_PROGRESS_ACTIVITY_REQ_CODE);
                            }
                            else {
                                // TODO: check to see if there are new images in the folder that have dates later than
                                // the last scan date.
                                if (!mStartedProgressActivity) {
                                    mImagesWFacesFolderTask = new ImagesWFacesFolderTask("", getActivity(),
                                            imgsFolder.getFolderId(), imgsFolder.getFolderName(),
                                            getResources().getDimensionPixelSize(R.dimen.image_faces_thumbnail_size));
                                    // custom version of AsyncTask
                                    mImagesWFacesFolderTask.executeOnExecutor(CustomAsyncTask.DUAL_THREAD_EXECUTOR);
                                }
                            }
                        }
                    });
                }
            });
        }

        @Override
        public int getItemCount() {
            return mImagesFoldersMS.size();
        }
    }

    private static class ImagesFolderViewHolder extends RecyclerView.ViewHolder {
        private ImageView mFolderImageView;
        private TextView mFolderTextView;

        public ImagesFolderViewHolder(@NonNull ImagesFolderItemBinding binding) {
            super(binding.getRoot());
            mFolderImageView = binding.imagesFolderImg;
            mFolderImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            mFolderTextView = binding.imagesFolderText;
        }
    }
}
