package com.vidsearch.videosearchapp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.vidsearch.videosearchapp.R;
import com.vidsearch.videosearchapp.databinding.SelectedFacesFragmentBinding;
import com.vidsearch.videosearchapp.databinding.SelectedFacesItemBinding;
import com.vidsearch.videosearchapp.db.entity.SelectedFaceEntity;
import com.vidsearch.videosearchapp.images.ImageResizer;
import com.vidsearch.videosearchapp.ui.utils.Constants;
import com.vidsearch.videosearchapp.ui.utils.RecyclerClickListener;
import com.vidsearch.videosearchapp.ui.utils.RecyclerTouchListener;
import com.vidsearch.videosearchapp.ui.utils.SelectedFacesMerged;
import com.vidsearch.videosearchapp.viewmodel.SelectedFacesListViewModel;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class SelectedFacesFragment extends Fragment {
    private final static String TAG = "SelectedFacesFragment";
    private SelectedFacesListViewModel mSelectedFacesListVM;
    private SelectedFacesAdapter mSelectedFacesAdapter;
    private ImageResizer mImageResizer;

    /*
     * Empty constructor
     */
    public SelectedFacesFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSelectedFacesAdapter = new SelectedFacesAdapter();
        int imageThumbSize = getResources().getDimensionPixelSize(R.dimen.selected_face_thumbnail_size);
        mImageResizer = new ImageResizer(
                getActivity(),
                imageThumbSize, imageThumbSize, Constants.IMAGES_SELECTED_FACES_KEY_PREFIX);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        SelectedFacesFragmentBinding binding = SelectedFacesFragmentBinding.inflate(inflater, container, false);
        binding.selectedFacesRecycler.setAdapter(mSelectedFacesAdapter);
        LinearLayoutManager horizontalLayoutManager = new LinearLayoutManager(getContext(),
                LinearLayoutManager.HORIZONTAL, false);
        binding.selectedFacesRecycler.setLayoutManager(horizontalLayoutManager);
        mSelectedFacesListVM = SelectedFacesListViewModel.getInstance(getActivity().getApplication());
        mSelectedFacesListVM.getSelectedFaces().observe(getViewLifecycleOwner(), new Observer<List<SelectedFaceEntity>>() {
            @Override
            public void onChanged(List<SelectedFaceEntity> selectedFaceEntities) {
                mImageResizer.setExitTasksEarly(false); // this fragment is paused when FaceNameDialogActivity is shown
                mSelectedFacesAdapter.setSelectedFaceList(selectedFaceEntities);
            }
        });
        binding.selectedFacesRecycler.addOnItemTouchListener(new RecyclerTouchListener(getActivity(),
                binding.selectedFacesRecycler, new RecyclerClickListener() {
            @Override
            public void onClick(View view, final int position, int x, int y) {
                //Values are passing to activity & to fragment as well
                SelectedFaceEntity entity = mSelectedFacesAdapter.mSelectedFaceList.get(position);
                ((ImagesFoldersActivity)requireActivity()).showSelectedImage(entity);
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
        mSelectedFacesAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        super.onPause();
        mImageResizer.setExitTasksEarly(true);
    }

    /*
     * The main adapter that backs the recycler view
     */
    private class SelectedFacesAdapter extends RecyclerView.Adapter<SelectedFacesViewHolder> {
        private List<SelectedFaceEntity> mSelectedFaceList;

        SelectedFacesAdapter() {
        }

        @NonNull
        @Override
        public SelectedFacesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            SelectedFacesItemBinding binding = SelectedFacesItemBinding.inflate(inflater, parent, false);
            return new SelectedFacesViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull SelectedFacesViewHolder holder, int position) {
            SelectedFaceEntity selectedFaceEntity = mSelectedFaceList.get(position);
            SelectedFacesMerged.SelectedImageInfo selectedImageInfo = SelectedFacesMerged.getSelectedFacePath(getContext(), selectedFaceEntity);
            mImageResizer.loadImageWSelectedFace(selectedImageInfo.path, selectedImageInfo.orientation, selectedFaceEntity, holder.mImageView);
        }

        @Override
        public int getItemCount() {
            int rc = 0;
            if (mSelectedFaceList != null)
                rc = mSelectedFaceList.size();
            ((ImagesFoldersActivity)getActivity()).showVideoSearchButton(rc == 0 ? View.GONE : View.VISIBLE);
            return rc;
        }

        public void setSelectedFaceList(List<SelectedFaceEntity> selectedFaceList) {
            mSelectedFaceList = selectedFaceList;
            notifyDataSetChanged();
        }
    }

    private static class SelectedFacesViewHolder extends RecyclerView.ViewHolder {
        private ImageView mImageView;

        public SelectedFacesViewHolder(@NonNull SelectedFacesItemBinding binding) {
            super(binding.getRoot());
            mImageView = binding.selectedFaceImg;
            mImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        }
    }
}