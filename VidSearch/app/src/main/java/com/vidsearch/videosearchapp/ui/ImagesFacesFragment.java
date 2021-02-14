package com.vidsearch.videosearchapp.ui;

import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.vidsearch.videosearchapp.R;
import com.vidsearch.videosearchapp.databinding.ImageFacesItemBinding;
import com.vidsearch.videosearchapp.databinding.ImagesFacesFragmentBinding;
import com.vidsearch.videosearchapp.images.CustomAsyncTask;
import com.vidsearch.videosearchapp.images.ImageResizer;
import com.vidsearch.videosearchapp.ui.utils.RecyclerClickListener;
import com.vidsearch.videosearchapp.ui.utils.Constants;
import com.vidsearch.videosearchapp.ui.utils.FacesImageTask;
import com.vidsearch.videosearchapp.ui.utils.ImagesFolderMerged;
import com.vidsearch.videosearchapp.ui.utils.RecyclerTouchListener;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ImagesFacesFragment extends Fragment {
    private final static String TAG = "ImagesFacesFragment";

    private ImagesFacesAdapter mImagesFacesAdapter;
    private ImageResizer mImageResizer;

    private List<ImagesFolderMerged.ImageWFaces> mImageWFacesList = null;
    /*
     * Empty constructor
     */
    public ImagesFacesFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mImagesFacesAdapter = new ImagesFacesAdapter();
        int imageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_faces_thumbnail_size);
        mImageResizer = new ImageResizer(
                getActivity(),
                imageThumbSize, imageThumbSize, Constants.IMAGES_FACES_KEY_PREFIX);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        ImagesFacesFragmentBinding binding = ImagesFacesFragmentBinding.inflate(inflater, container, false);
        binding.imagesFacesRecycler.setAdapter(mImagesFacesAdapter);
        binding.imagesFacesRecycler.setLayoutManager(new GridLayoutManager(getContext(), 1));

        Bundle args = getArguments();
        long folderId = args.getLong(Constants.KEY_IMAGES_FOLDER_ID);

        ImagesFolderMerged imagesFolderMerged = ImagesFolderMerged.getInstance(getActivity().getApplicationContext(), folderId);
        mImageWFacesList = imagesFolderMerged.getImageWFacesList();

        binding.imagesFacesRecycler.addOnItemTouchListener(new RecyclerTouchListener(getActivity(),
                binding.imagesFacesRecycler, new RecyclerClickListener() {
            @Override
            public void onClick(View view, final int position, int x, int y) {
                //Values are passing to activity & to fragment as well
                y -= view.getY();
                ImagesFolderMerged.ImageWFaces imageWFaces = mImageWFacesList.get(position);
                List<ImagesFolderMerged.FaceInfo> displayFaceInfoList = imageWFaces.getFaceInfoList();
                //Log.d(TAG, "x: " + x + ", " + y);
                for (int i = 0; i < displayFaceInfoList.size(); i++) {
                    ImagesFolderMerged.FaceInfo faceInfo = displayFaceInfoList.get(i);
                    Rect displayR = faceInfo.getDisplayRect();
                    if (displayR.contains(x, y)) {
                        FacesImageTask task = new FacesImageTask("", getActivity(),
                                folderId, imageWFaces.getPicId(), faceInfo.getFaceId(), position, i, null, null);
                        // custom version of AsyncTask
                        task.executeOnExecutor(CustomAsyncTask.DUAL_THREAD_EXECUTOR);
                        break;
                    }
                }
            }

            @Override
            public void onLongClick(View view, int position) {
                //Toast.makeText(getActivity(), "Long press on position :"+position,
                //        Toast.LENGTH_LONG).show();
            }
        }));
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        mImageResizer.setExitTasksEarly(false);
        mImagesFacesAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        super.onPause();
        mImageResizer.setExitTasksEarly(true);
    }

    /*
     * The main adapter that backs the recycler view
     */
    private class ImagesFacesAdapter extends RecyclerView.Adapter<ImagesFacesViewHolder> {
        ImagesFacesAdapter() {
        }

        @NonNull
        @Override
        public ImagesFacesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            ImageFacesItemBinding binding = ImageFacesItemBinding.inflate(inflater, parent, false);
            return new ImagesFacesViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ImagesFacesViewHolder holder, int position) {
            ImagesFolderMerged.ImageWFaces imageWFaces = mImageWFacesList.get(position);
            mImageResizer.loadImageWithFaces(imageWFaces, holder.mImageView);
        }

        @Override
        public int getItemCount() {
            return (mImageWFacesList == null) ? 0 : mImageWFacesList.size();
        }
    }

    private static class ImagesFacesViewHolder extends RecyclerView.ViewHolder {
        private ImageView mImageView;

        public ImagesFacesViewHolder(@NonNull ImageFacesItemBinding binding) {
            super(binding.getRoot());
            mImageView = binding.imageFacesImg;
            mImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        }
    }
}
