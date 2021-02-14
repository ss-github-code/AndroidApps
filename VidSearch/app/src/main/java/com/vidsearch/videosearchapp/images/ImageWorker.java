package com.vidsearch.videosearchapp.images;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.vidsearch.videosearchapp.db.entity.FaceEntity;
import com.vidsearch.videosearchapp.db.entity.ImageFacesEntity;
import com.vidsearch.videosearchapp.db.entity.ImagesFolderEntity;
import com.vidsearch.videosearchapp.db.entity.SelectedFaceEntity;
import com.vidsearch.videosearchapp.mtcnn.util.Box;
import com.vidsearch.videosearchapp.ui.SelectedImageFragment;
import com.vidsearch.videosearchapp.ui.utils.Constants;
import com.vidsearch.videosearchapp.ui.utils.ImagesFolderMerged;
import com.vidsearch.videosearchapp.viewmodel.FacesListViewModel;
import com.vidsearch.videosearchapp.viewmodel.ImagesFacesListViewModel;
import com.vidsearch.videosearchapp.viewmodel.ImagesFoldersListViewModel;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

public abstract class ImageWorker extends Worker {
    private static final String TAG = "ImageWorker";
    private static final float PERCENT_MEM = 0.25f;

    private static ImagesFoldersListViewModel sImagesFoldersListViewModel;
    protected static ImagesFacesListViewModel sImagesFacesListViewModel;

    protected static ImageMemCache sImageMemCache;
    private static Paint sPaint;

    protected final char mKeyPrefix;

    public static class ScaleInfo {
        public float mScale;
        public int mScaledSrcX;
        public int mScaledSrcY;
        public int mScaledWidth;
        public int mScaledHeight;
    }

    /* Subclasses should override this define any processing work that must happen to produce
     * the final bitmap. This will be executed in a background thread. For example, you could
     * resize a large bitmap here.
     */
    protected abstract Bitmap processBitmap(String filename, int orientation);
    protected abstract Bitmap processBitmap(String filename, int orientation, boolean fullSize);
    protected abstract Bitmap processBitmap(Bitmap fullBitmap, ScaleInfo scaleInfo);

    private static void initialize(Application app) {
        if (sImageMemCache == null) {
            sImageMemCache = ImageMemCache.getInstance(PERCENT_MEM);

            sImagesFoldersListViewModel = ImagesFoldersListViewModel.getInstance(app);
            sImagesFacesListViewModel = ImagesFacesListViewModel.getInstance(app);

            sPaint = new Paint();
            sPaint.setColor(Color.GREEN);
            sPaint.setStrokeWidth(20);
            sPaint.setStyle(Paint.Style.STROKE);
        }
    }

    protected ImageWorker(Activity activity, char keyPrefix) {
        super(activity);
        mKeyPrefix = keyPrefix;
        initialize(activity.getApplication());
    }

    protected ImageWorker(FragmentActivity fragmentActivity, char keyPrefix) {
        super(fragmentActivity);
        mKeyPrefix = keyPrefix;
        initialize(fragmentActivity.getApplication());
    }

    // ImagesFoldersMSFragment.ImagesFolderMSAdapter
    public void loadFolderImage(String key, int orientation, ImageView imageView) {
        BitmapDrawable value = sImageMemCache.getBitmapFromMemCache(mKeyPrefix + key);

        if (value != null) {
            // Bitmap found in memory cache
            imageView.setImageDrawable(value);
        } else if (cancelPotentialWork(key, imageView)) {
            final BitmapLoaderTask task = new BitmapLoaderTask(key, orientation, imageView);
            final CustomAsyncDrawable asyncDrawable = new CustomAsyncDrawable(sResources, sLoadingBitmap, task);
            imageView.setImageDrawable(asyncDrawable);

            // custom version of AsyncTask
            task.executeOnExecutor(CustomAsyncTask.DUAL_THREAD_EXECUTOR);
        }
    }

    // ImagesFolderProgressActivity
    public void findFacesInImages(long folderId, String folderName, ImageView imageView, TextView countTextView, Activity activity) {
        final BitmapFaceDetectorTask task = new BitmapFaceDetectorTask(folderId, folderName, imageView, countTextView, activity);

        // custom version of AsyncTask
        task.executeOnExecutor(CustomAsyncTask.DUAL_THREAD_EXECUTOR);
    }

    // FaceNameDialogActivity
    public void loadFaceImage(ImagesFolderMerged.ImageWFaces imageWFaces, int selectedFaceNum,
                              ImageView imageView, Rect faceImageRect/*could be null*/) {
        String picPath = imageWFaces.getPath();
        //long picId = imageWFaces.getPicId();
        BitmapDrawable value = sImageMemCache.getBitmapFromMemCache(mKeyPrefix + picPath);
        if (imageWFaces.getFaceInfoList().size() != 0) {

            ImagesFolderMerged.FaceInfo faceInfo = imageWFaces.getFaceInfoList().get(selectedFaceNum);
            if (value != null) {
                Bitmap bitmap = value.getBitmap();
                if (bitmap != null) {
                    Rect displayR = faceInfo.getDisplayRect();
                    BitmapDrawable drawable = new BitmapDrawable(sResources,
                            Bitmap.createBitmap(bitmap,
                                    displayR.left, displayR.top, displayR.right - displayR.left, displayR.bottom - displayR.top));
                    imageView.setImageDrawable(drawable);
                }
            }
        } else { // from the selected images list
            if (value != null) {
                Bitmap bitmap = value.getBitmap();
                if (bitmap != null) {
                    BitmapDrawable drawable = new BitmapDrawable(sResources,
                            Bitmap.createBitmap(bitmap,
                                    faceImageRect.left, faceImageRect.top,
                                    faceImageRect.right - faceImageRect.left,
                                    faceImageRect.bottom - faceImageRect.top));
                    imageView.setImageDrawable(drawable);
                }
            }
        }
    }

    // ImagesFacesFragment.ImagesFacesAdapter
    public void loadImageWithFaces(ImagesFolderMerged.ImageWFaces imageWFaces, ImageView imageView) {
        String picPath = imageWFaces.getPath();
        long picId = imageWFaces.getPicId();
        BitmapDrawable value = sImageMemCache.getBitmapFromMemCache(mKeyPrefix + picPath);
        if (value != null) {
            // Bitmap found in memory cache
            imageView.setImageDrawable(value);
        } else if (cancelPotentialWork(picPath, imageView)) {
            final BitmapFacesLoaderTask task = new BitmapFacesLoaderTask(picId, picPath, imageWFaces, imageView);
            final CustomAsyncDrawable asyncDrawable = new CustomAsyncDrawable(sResources, sLoadingBitmap, task);
            imageView.setImageDrawable(asyncDrawable);

            // custom version of AsyncTask
            task.executeOnExecutor(CustomAsyncTask.DUAL_THREAD_EXECUTOR);
        }
    }

    // SelectedFacesFragment.SelectedFacesAdapter
    public void loadImageWSelectedFace(String key, int orientation, SelectedFaceEntity selectedFaceEntity, ImageView imageView) {
        String uniqueKey = mKeyPrefix + Integer.toString(selectedFaceEntity.getFaceNum()) + key;
        BitmapDrawable value = sImageMemCache.getBitmapFromMemCache(uniqueKey);

        if (value != null) {
            // Bitmap found in memory cache
            //Log.d(TAG, "cache: " + uniqueKey);
            imageView.setImageDrawable(value);
        } else if (cancelPotentialWork(key, imageView)) {
            final BitmapSelectedFaceImageLoaderTask task = new BitmapSelectedFaceImageLoaderTask(key, orientation, selectedFaceEntity, imageView);
            final CustomAsyncDrawable asyncDrawable = new CustomAsyncDrawable(sResources, sLoadingBitmap, task);
            imageView.setImageDrawable(asyncDrawable);

            // custom version of AsyncTask
            task.executeOnExecutor(CustomAsyncTask.DUAL_THREAD_EXECUTOR);
        }
    }

    // SelectedImageFragment::onCreateView
    public void loadSelectedImage(String key, int orientation, SelectedFaceEntity selectedFaceEntity,
                                  Fragment fragment, ImageView imageView) {
        final BitmapSelectedImageLoaderTask task = new BitmapSelectedImageLoaderTask(key, orientation,
                selectedFaceEntity, fragment, imageView);
        final CustomAsyncDrawable asyncDrawable = new CustomAsyncDrawable(sResources, sLoadingBitmap, task);
        imageView.setImageDrawable(asyncDrawable);

        // custom version of AsyncTask
        task.executeOnExecutor(CustomAsyncTask.DUAL_THREAD_EXECUTOR);
    }

    /*
     Returns true if the current work has been canceled or if there was no work in progress
     on this image view. Returns false if the work in progress deals with the same key.
     */
    public static boolean cancelPotentialWork(String key, ImageView imageView) {
        final CustomAsyncTask asyncWorkerTask = getAsyncWorkerTask(imageView);
        if (asyncWorkerTask != null) {
            final String asyncWorkerkey = asyncWorkerTask.mKey;
            if (!asyncWorkerkey.equals(key)) {
                asyncWorkerTask.cancel(true);
                Log.d(TAG, "cancelPotentialWork - " + asyncWorkerkey);
            }
            else {
                return false; // same work is already in progress
            }
        }
        return true;
    }

    private static CustomAsyncTask getAsyncWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof CustomAsyncDrawable) {
                final CustomAsyncDrawable asyncDrawable = (CustomAsyncDrawable) drawable;
                return asyncDrawable.getAsyncWorkerTask();
            }
        }
        return null;
    }

    // loadFolderImage (ImagesFoldersMSFragment.ImagesFolderMSAdapter)
    private class BitmapLoaderTask extends CustomAsyncTask<Void, Void, BitmapDrawable> {
        private final WeakReference<ImageView> mImageViewRef;
        private final int mOrientation;
        public BitmapLoaderTask(String key, int orientation, ImageView imageView) {
            super(key);
            mImageViewRef = new WeakReference<>(imageView);
            mOrientation = orientation;
        }

        @Override
        protected BitmapDrawable doInBackground(Void... params) {
            Bitmap bitmap = null;
            BitmapDrawable drawable = null;

            if (!isCancelled() && getAttachedImageView() != null && !mExitTasksEarly) {
                bitmap = processBitmap(mKey, mOrientation);
            }
            // if the bitmap was processed, add the processed bitmap to the cache for future use.
            if (bitmap != null) {
                drawable = new BitmapDrawable(sResources, bitmap);
                sImageMemCache.addBitmapToMemCache(mKeyPrefix + mKey, drawable);
            }
            return drawable;
        }

        @Override
        protected void onPostExecute(BitmapDrawable drawable) {
            //boolean success = false;
            if (isCancelled() || mExitTasksEarly) {
                drawable = null;
                return;
            }
            final ImageView imageView = getAttachedImageView();
            if (drawable != null && imageView != null) {
                //success = true;
                imageView.setImageDrawable(drawable);
            }
        }

        /*
         Returns the ImageView associated with this task as long as the ImageView's task still
         points to this task as well; returns null otherwise.
         */
        private ImageView getAttachedImageView() {
            final ImageView imageView = mImageViewRef.get();
            final CustomAsyncTask asyncWorkerTask = getAsyncWorkerTask(imageView);
            if (this == asyncWorkerTask)
                return imageView;
            return null;
        }
    }

    /*
     A custom drawable that is attached to an imageView. This contains a reference to the actual
     worker task, so that it can be stopped if a new binding is required and make sure that
     only the last started worker task can bind its result.
     */
    private static class CustomAsyncDrawable extends BitmapDrawable {
        private final WeakReference<CustomAsyncTask> mAsyncWorkerTaskRef;
        public CustomAsyncDrawable(Resources res, Bitmap bitmap, CustomAsyncTask asyncWorkerTask) {
            super(res, bitmap);
            mAsyncWorkerTaskRef = new WeakReference<>(asyncWorkerTask);
        }
        public CustomAsyncTask getAsyncWorkerTask() {
            return mAsyncWorkerTaskRef.get();
        }
    }

    // loadImageWithFaces (ImagesFacesFragment.ImagesFacesAdapter)
    private class BitmapFacesLoaderTask extends CustomAsyncTask<Void, Void, BitmapDrawable> {
        private final WeakReference<ImageView> mImageViewRef;
        private final ImagesFolderMerged.ImageWFaces mImageWFaces;
        private final long mPicId;

        public BitmapFacesLoaderTask(long picId, String picPath, ImagesFolderMerged.ImageWFaces imageWFaces, ImageView imageView) {
            super(picPath);
            mImageViewRef = new WeakReference<>(imageView);
            mImageWFaces = imageWFaces;
            mPicId = picId;
        }

        @Override
        protected BitmapDrawable doInBackground(Void... params) {
            Bitmap fullBitmap = null, bitmap = null;
            BitmapDrawable drawable = null;

            if (!isCancelled() && getAttachedImageView() != null && !mExitTasksEarly) {
                fullBitmap = processBitmap(mKey, mImageWFaces.getOrientation(), true);
                Canvas canvas = new Canvas(fullBitmap);

                //Log.d(TAG, "Full Img: w: " + fullBitmap.getWidth() + ", h: " + fullBitmap.getHeight());
                for (int i = 0; i < mImageWFaces.getNumFacesDetected(); i++) {
                    Rect r = mImageWFaces.getFaceInfoList().get(i).getImageRect();
                    //Log.d(TAG, "Rect: " + r);
                    canvas.drawRect(r, sPaint);
                }
            }

            if (fullBitmap != null && !isCancelled() && getAttachedImageView() != null && !mExitTasksEarly) {
                //Log.d(TAG, "processBitmap: " + mKey);
                ScaleInfo scaleInfo = new ScaleInfo();
                bitmap = processBitmap(fullBitmap, scaleInfo);
                for (int i = 0; i < mImageWFaces.getNumFacesDetected(); i++) {
                    mImageWFaces.addFaceDisplayRect(i, scaleInfo);
                }
            }

            // if the bitmap was processed, add the processed bitmap to the cache for future use.
            if (bitmap != null && !isCancelled() && getAttachedImageView() != null && !mExitTasksEarly) {
                drawable = new BitmapDrawable(sResources, bitmap);
                sImageMemCache.addBitmapToMemCache(mKeyPrefix + mKey, drawable);
            }

            return drawable;
        }

        @Override
        protected void onPostExecute(BitmapDrawable drawable) {
            //boolean success = false;
            if (isCancelled() || mExitTasksEarly) {
                drawable = null;
                return;
            }
            final ImageView imageView = getAttachedImageView();
            if (drawable != null && imageView != null) {
                //success = true;
                imageView.setImageDrawable(drawable);
            }
        }

        /*
         Returns the ImageView associated with this task as long as the ImageView's task still
         points to this task as well; returns null otherwise.
         */
        private ImageView getAttachedImageView() {
            final ImageView imageView = mImageViewRef.get();
            final CustomAsyncTask asyncWorkerTask = getAsyncWorkerTask(imageView);
            if (this == asyncWorkerTask)
                return imageView;
            return null;
        }
    }

    // loadSelectedImage (SelectedImageFragment::onCreateView)
    private class BitmapSelectedImageLoaderTask extends CustomAsyncTask<Void, Void, ImagesFolderMerged.ImageWFaces> {
        private final SelectedFaceEntity mSelectedFaceEntity;
        private final WeakReference<Fragment> mFragmentRef;
        private final WeakReference<ImageView> mImageViewRef;
        private int mOrientation;
        private long mFolderId;
        private BitmapDrawable mDrawable;

        public BitmapSelectedImageLoaderTask(String picPath, int orientation, SelectedFaceEntity selectedFaceEntity,
                                             Fragment fragment, ImageView imageView) {
            super(picPath);
            mFragmentRef = new WeakReference<>(fragment);
            mImageViewRef = new WeakReference<>(imageView);
            mSelectedFaceEntity = selectedFaceEntity;
            mDrawable = null;
            mOrientation = orientation;
        }

        @Override
        protected ImagesFolderMerged.ImageWFaces doInBackground(Void... params) {
            Bitmap fullBitmap = null, bitmap = null;
            ImagesFolderMerged.ImageWFaces imageWFaces = null;

            if (!isCancelled() && getAttachedImageView() != null && !mExitTasksEarly) {
                fullBitmap = processBitmap(mKey, mOrientation, true);
                Canvas canvas = new Canvas(fullBitmap);

                long picId = mSelectedFaceEntity.getPicId();
                ImageFacesEntity imageFacesEntity = sImagesFacesListViewModel.getImageFaces(picId);
                List<FaceEntity> facesEntity = sFacesListViewModel.getFacesInImage(picId);
                mFolderId = imageFacesEntity.getFolderId();
                imageWFaces = new ImagesFolderMerged.ImageWFaces(picId, mKey, mOrientation, imageFacesEntity);
                //Log.d(TAG, "Full Img: w: " + fullBitmap.getWidth() + ", h: " + fullBitmap.getHeight());
                for (int i = 0; i < imageFacesEntity.getNumFacesDetected(); i++) {
                    FaceEntity faceEntity = facesEntity.get(i);
                    imageWFaces.addFaceRect(faceEntity);
                    Rect r = imageWFaces.getFaceInfoList().get(i).getImageRect();
                    //Log.d(TAG, "Rect: " + r);
                    canvas.drawRect(r, sPaint);
                }
            }

            if (fullBitmap != null && !isCancelled() && getAttachedImageView() != null && !mExitTasksEarly) {
                //Log.d(TAG, "processBitmap: " + mKey);
                ScaleInfo scaleInfo = new ScaleInfo();
                bitmap = processBitmap(fullBitmap, scaleInfo);
                for (int i = 0; i < imageWFaces.getNumFacesDetected(); i++) {
                    imageWFaces.addFaceDisplayRect(i, scaleInfo);
                }
            }

            // if the bitmap was processed, add the processed bitmap to the cache for future use.
            if (bitmap != null && !isCancelled() && getAttachedImageView() != null && !mExitTasksEarly) {
                mDrawable = new BitmapDrawable(sResources, bitmap);
                sImageMemCache.addBitmapToMemCache(mKeyPrefix + mKey, mDrawable);
            }

            return imageWFaces;
        }

        @Override
        protected void onPostExecute(ImagesFolderMerged.ImageWFaces imageWFaces) {
            //boolean success = false;
            if (isCancelled() || mExitTasksEarly) {
                mDrawable = null;
                return;
            }
            final ImageView imageView = getAttachedImageView();
            final Fragment fragment = mFragmentRef.get();
            if (mDrawable != null && imageView != null && fragment != null) {
                //success = true;
                imageView.setImageDrawable(mDrawable);
                ((SelectedImageFragment)fragment).setResutFromImageResizer(mFolderId, imageWFaces);
            }
        }

        /*
         Returns the ImageView associated with this task as long as the ImageView's task still
         points to this task as well; returns null otherwise.
         */
        private ImageView getAttachedImageView() {
            final ImageView imageView = mImageViewRef.get();
            final CustomAsyncTask asyncWorkerTask = getAsyncWorkerTask(imageView);
            if (this == asyncWorkerTask)
                return imageView;
            return null;
        }
    }

    // loadImageWSelectedFace (SelectedFacesFragment.SelectedFacesAdapter)
    private class BitmapSelectedFaceImageLoaderTask extends CustomAsyncTask<Void, Void, BitmapDrawable> {
        private final WeakReference<ImageView> mImageViewRef;
        private final SelectedFaceEntity mSelectedFaceEntity;
        private final int mOrientation;

        public BitmapSelectedFaceImageLoaderTask(String picPath, int orientation, SelectedFaceEntity selectedFaceEntity, ImageView imageView) {
            super(picPath);
            mImageViewRef = new WeakReference<>(imageView);
            mSelectedFaceEntity = selectedFaceEntity;
            mOrientation = orientation;
        }

        @Override
        protected BitmapDrawable doInBackground(Void... params) {
            Bitmap fullBitmap = null, bitmap = null;
            BitmapDrawable drawable = null;

            if (!isCancelled() && getAttachedImageView() != null && !mExitTasksEarly) {
                fullBitmap = processBitmap(mKey, mOrientation, true);
                Canvas canvas = new Canvas(fullBitmap);

                //Log.d(TAG, "Full Img: w: " + fullBitmap.getWidth() + ", h: " + fullBitmap.getHeight());
                FaceEntity faceEntity = sFacesListViewModel.getFaceInImage(mSelectedFaceEntity.getPicId(),
                                                                           mSelectedFaceEntity.getFaceId());
                Rect r = new Rect(faceEntity.getTlX(), faceEntity.getTlY(), faceEntity.getBrX(), faceEntity.getBrY());
                //Log.d(TAG, "Rect: " + r);
                canvas.drawRect(r, sPaint);
            }

            if (fullBitmap != null && !isCancelled() && getAttachedImageView() != null && !mExitTasksEarly) {
                //Log.d(TAG, "processBitmap: " + mKey);
                ScaleInfo scaleInfo = new ScaleInfo();
                bitmap = processBitmap(fullBitmap, scaleInfo);
            }

            // if the bitmap was processed, add the processed bitmap to the cache for future use.
            if (bitmap != null && !isCancelled() && getAttachedImageView() != null && !mExitTasksEarly) {
                String uniqueKey = mKeyPrefix + Integer.toString(mSelectedFaceEntity.getFaceNum()) + mKey;
                drawable = new BitmapDrawable(sResources, bitmap);
                sImageMemCache.addBitmapToMemCache(uniqueKey, drawable);
            }
            return drawable;
        }

        @Override
        protected void onPostExecute(BitmapDrawable drawable) {
            //boolean success = false;
            if (isCancelled() || mExitTasksEarly) {
                drawable = null;
                return;
            }
            final ImageView imageView = getAttachedImageView();
            if (drawable != null && imageView != null) {
                //success = true;
                imageView.setImageDrawable(drawable);
            }
        }

        /*
         Returns the ImageView associated with this task as long as the ImageView's task still
         points to this task as well; returns null otherwise.
         */
        private ImageView getAttachedImageView() {
            final ImageView imageView = mImageViewRef.get();
            final CustomAsyncTask asyncWorkerTask = getAsyncWorkerTask(imageView);
            if (this == asyncWorkerTask)
                return imageView;
            return null;
        }
    }

    // ImagesFolderProgressActivity
    private class BitmapFaceDetectorTask extends CustomAsyncTask<Void, BitmapDrawable, Integer> {
        private final long mFolderId;
        private final String mFolderName;
        private final WeakReference<Activity> mActivityRef;
        private final WeakReference<ImageView> mImageViewRef;
        private final WeakReference<TextView> mCountTextViewRef;
        private int mCountImages = 0;
        public BitmapFaceDetectorTask(long folderId, String folderName, ImageView imageView, TextView countTextView, Activity activity) {
            super(""); // key is empty
            mFolderId = folderId;
            mFolderName = folderName;
            mActivityRef = new WeakReference<>(activity);
            mImageViewRef = new WeakReference<>(imageView);
            mCountTextViewRef = new WeakReference<>(countTextView);
        }

        @Override
        protected Integer doInBackground(Void... params) {
            Bitmap bitmap = null;
            BitmapDrawable drawable = null;
            int result = 0;
            ImagesFolderMerged imagesFolderMerged = ImagesFolderMerged.getInstance(sAppContext, mFolderId);
            List<ImagesFolderMerged.ImageMS> listImages = imagesFolderMerged.getImageMSList();
            List<ImageFacesEntity> listImageFacesEntity = new ArrayList<>();
            List<FaceEntity> listFaceEntity = new ArrayList<>();

            for (int i = 0; i < listImages.size(); i++) {
                ImagesFolderMerged.ImageMS imageMS = listImages.get(i);
                String keyString = imageMS.getData();
                mCountImages++;

                if (!mExitTasksEarly) {
                    bitmap = processBitmap(keyString, imageMS.getOrientation());
                }
                // if the bitmap was processed, add the processed bitmap to the cache for future use.
                if (bitmap != null) {
                    drawable = new BitmapDrawable(sResources, bitmap);
                    // sImageMemCache.addBitmapToMemCache(mKeyPrefix + keyString, drawable);
                    // Report progress using onProgressUpdate
                    publishProgress(drawable);
                }

                if (!mExitTasksEarly) {
                    Bitmap fullBitmap = processBitmap(keyString, imageMS.getOrientation(), true);
                    Vector<Box> boxes = sMTCNN.detectFaces(fullBitmap, fullBitmap.getWidth()/4);
                    if (boxes.size() > 0) {
                        long picId = listImages.get(i).getPicId();
                        ImageFacesEntity imageWithFacesEntity =
                                //new ImageFacesEntity(picId, mFolderId, boxes.size(), -1, -1, -1);
                                new ImageFacesEntity(picId, mFolderId, boxes.size());
                        listImageFacesEntity.add(imageWithFacesEntity);
                        /*int idx = */imagesFolderMerged.addImageWFaces(i, imageWithFacesEntity); // add it to the app
                        //Log.d(TAG, "Faces detected: " + keyString + " : " + boxes.size());
                        for (Box b : boxes) {
                            //Log.d(TAG, "box left " + b.left() + " top " + b.top() + " width " + b.width() + " height " + b.height() + "\n");
                            FaceEntity faceEntity = new FaceEntity(picId, b.left(), b.top(), b.left() + b.width(), b.top() + b.height());
                            listFaceEntity.add(faceEntity);
                            //imagesFolderMerged.addFaceRect(idx, faceEntity); // need faceId
                        }
                    }
                }
                else {
                    break;
                }
            } // for (int i = 0; i < mImagesMS.size(); i++)

            if (!mExitTasksEarly && listImageFacesEntity.size() > 0) {
                ImagesFolderEntity folderEntity =
                        new ImagesFolderEntity(mFolderId, new Date(System.currentTimeMillis()));
                sImagesFoldersListViewModel.update(folderEntity, listImageFacesEntity, listFaceEntity);
                for (int i  = 0; i < listImageFacesEntity.size(); i++) {
                    ImageFacesEntity entity = listImageFacesEntity.get(i);
                    int numBoxes = entity.getNumFacesDetected();
                    List<FaceEntity> faceEntities = sFacesListViewModel.getFacesInImage(entity.getPicId());
                    //assert (numBoxes == faceEntities.size());
                    //Log.d(TAG, "num rect: " + numBoxes + ", db: " + faceEntities.size());
                    for (int j = 0; j < faceEntities.size(); j++) {
                        FaceEntity faceEntity = faceEntities.get(j);
                        imagesFolderMerged.addFaceRect(i, faceEntity); // add it to the app along with faceid
                    }
                }
                result = listImageFacesEntity.size();
            }
            return result;
        }

        @Override
        protected void onProgressUpdate(BitmapDrawable[] drawable) {
            if (mExitTasksEarly) {
                drawable[0] = null;
                return;
            }
            final ImageView imageView = mImageViewRef.get();
            if (drawable[0] != null && imageView != null) {
                imageView.setImageDrawable(drawable[0]);
                final TextView textView = mCountTextViewRef.get();
                if (textView != null) {
                    textView.setText(Integer.toString(mCountImages));
                }
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            final Activity activity = mActivityRef.get();
            if (activity != null) {
                //Log.d(TAG, "activity: " + result);
                if (result == 0) {
                    ImagesFolderMerged imagesFolderMerged = ImagesFolderMerged.getInstance(sAppContext, mFolderId);
                    if (imagesFolderMerged.getImageWFacesList() != null) // can be null if no faces were detected
                        imagesFolderMerged.getImageWFacesList().clear(); // clear the partially filled list
                }
                Intent intent = new Intent();
                intent.putExtra(Constants.KEY_IMAGES_FOLDER_ID, mFolderId);
                intent.putExtra(Constants.KEY_IMAGES_FOLDER_NAME, mFolderName);
                activity.setResult(result, intent);
                activity.finish();
            }
        }
    }
}
