package com.vidsearch.videosearchapp.ui.utils;

import android.view.View;

public interface RecyclerClickListener {
    void onClick(View view, int position, int x, int y);
    void onLongClick(View view, int position);
}