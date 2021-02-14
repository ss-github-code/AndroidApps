package com.vidsearch.videosearchapp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.vidsearch.videosearchapp.R;
import com.vidsearch.videosearchapp.databinding.NoImagesFragmentBinding;
import com.vidsearch.videosearchapp.ui.utils.Constants;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class NoImagesFragment extends Fragment {
    /*
     * Empty constructor
     */
    public NoImagesFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        NoImagesFragmentBinding binding = NoImagesFragmentBinding.inflate(inflater, container, false);
        Bundle args = getArguments();
        if (args != null) {
            String folderName = args.getString(Constants.KEY_IMAGES_FOLDER_NAME);
            binding.noImages.setText(getResources().getText(R.string.no_faces_found_text));
        }
        return binding.getRoot();
    }
}
