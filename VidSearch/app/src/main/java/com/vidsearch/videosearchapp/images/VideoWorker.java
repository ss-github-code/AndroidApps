package com.vidsearch.videosearchapp.images;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaCodec;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.vidsearch.videosearchapp.db.entity.FaceEntity;
import com.vidsearch.videosearchapp.db.entity.SelectedFaceEntity;
import com.vidsearch.videosearchapp.mobilefacenet.MobileFaceNet;
import com.vidsearch.videosearchapp.mtcnn.util.Box;
import com.vidsearch.videosearchapp.ui.utils.SelectedFacesMerged;
import com.vidsearch.videosearchapp.ui.utils.VideosFoldersMS;
import com.vidsearch.videosearchapp.video.ExtractMpegFrames;
import com.vidsearch.videosearchapp.viewmodel.SelectedFacesListViewModel;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import androidx.appcompat.app.AppCompatActivity;

public class VideoWorker extends Worker {
    private final static String TAG = "VideoWorker";
    private static MobileFaceNet sMobileFaceNet = null;
    private SelectedFacesListViewModel mSelectedFacesListVM;

    // Temp
    private int mCount = 0;
    private int mBestScoreId = -1;
    private float mBestScore = 0f;

    public VideoWorker(AppCompatActivity activity) {
        super(activity);
        mSelectedFacesListVM = SelectedFacesListViewModel.getInstance(activity.getApplication());
        if (sMobileFaceNet == null) {
            try {
                sMobileFaceNet = new MobileFaceNet(activity.getAssets());
            } catch (IOException e) {
            }
        }
     }

    public void matchFacesInVideosTask(String key, List<VideosFoldersMS.VideosFolderMS> videosFoldersMSList,
                                       TextView currVidCountTextView,
                                       ImageView vidImgView,
                                       TextView vidTSTextView,
                                       ImageView vidFaceImgView,
                                       ImageView faceImgView,
                                       TextView scoreTextView,
                                       TextView bestScoreIdTextView,
                                       TextView bestScoreTextView) {
        final MatchFacesInVideosTask task = new MatchFacesInVideosTask(key, videosFoldersMSList, currVidCountTextView,
                vidImgView, vidTSTextView, vidFaceImgView, faceImgView, scoreTextView, bestScoreIdTextView, bestScoreTextView);

        // custom version of AsyncTask
        task.executeOnExecutor(CustomAsyncTask.DUAL_THREAD_EXECUTOR);
    }

    private class MatchFacesInVideosTask extends CustomAsyncTask<Void, ExtractMpegFrames.DecoderStatus, Void> {
        private List<VideosFoldersMS.VideosFolderMS> mVideosFoldersMSList;
        private WeakReference<TextView> mCurrVidCountTextRef;
        private WeakReference<ImageView> mVidImgViewRef;
        private WeakReference<TextView> mVidTSTextRef;
        private WeakReference<ImageView> mVidFaceViewRef;
        private WeakReference<ImageView> mFaceViewRef;
        private WeakReference<TextView> mScoreTextRef;
        private WeakReference<TextView> mBestScoreIdTextRef;
        private WeakReference<TextView> mBestScoreTextRef;

        private SimpleDateFormat mDateFormatter;

        public MatchFacesInVideosTask(String key, List<VideosFoldersMS.VideosFolderMS> videosFoldersMSList,
                                      TextView currVidCountTextView,
                                      ImageView vidImgView, TextView vidTSTextView,
                                      ImageView vidFaceImgView, ImageView faceImgView, TextView scoreTextView,
                                      TextView bestScoreIdTextView, TextView bestScoreTextView) {
            super(key);
            mVideosFoldersMSList = videosFoldersMSList;
            mCurrVidCountTextRef = new WeakReference<>(currVidCountTextView);
            mVidImgViewRef = new WeakReference<>(vidImgView);
            mVidTSTextRef = new WeakReference<>(vidTSTextView);
            mVidFaceViewRef = new WeakReference<>(vidFaceImgView);
            mFaceViewRef = new WeakReference<>(faceImgView);
            mScoreTextRef = new WeakReference<>(scoreTextView);
            mBestScoreIdTextRef = new WeakReference<>(bestScoreIdTextView);
            mBestScoreTextRef = new WeakReference<>(bestScoreTextView);

            mDateFormatter = new SimpleDateFormat("mm:ss");
        }

        @Override
        protected Void doInBackground(Void... voids) {
            List<SelectedFaceEntity> selectedFaceEntities = mSelectedFacesListVM.getSelectedFacesSync();
            List<Bitmap> croppedFacesBitmaps = new ArrayList<>();

            // Build the list of bitmaps of faces
            for (int i = 0; i < selectedFaceEntities.size(); i++) {
                SelectedFaceEntity selectedFaceEntity = selectedFaceEntities.get(i);
                SelectedFacesMerged.SelectedImageInfo imageInfo = SelectedFacesMerged.getSelectedFacePath(sAppContext, selectedFaceEntity);

                Bitmap fullBitmap = ImageResizer.decodeBitmapFromFile(imageInfo.path, imageInfo.orientation, true, -1, -1, null);
                FaceEntity faceEntity = sFacesListViewModel.getFaceInImage(selectedFaceEntity.getPicId(),
                                                                           selectedFaceEntity.getFaceId());
                Box b = new Box();
                b.box[0] = faceEntity.getTlX(); b.box[1] = faceEntity.getTlY();
                b.box[2] = faceEntity.getBrX(); b.box[3] = faceEntity.getBrY();
                b.toSquareShape();
                b.limitSquare(fullBitmap.getWidth(), fullBitmap.getHeight());

                Bitmap cropped = Bitmap.createBitmap(fullBitmap, b.left(), b.top(), b.width(), b.height());
                croppedFacesBitmaps.add(cropped);
            }


            for (int i = 0; i < mVideosFoldersMSList.size(); i++) {
                VideosFoldersMS.VideosFolderMS vidsFoldersMS = mVideosFoldersMSList.get(i);
                if (vidsFoldersMS.getVidsFolderName().equals("Camera")) {
                    List<VideosFoldersMS.VideoMS> videosMSList = vidsFoldersMS.getVidMSList();
                    for (int j = 0; j < videosMSList.size(); j++) {
                        VideosFoldersMS.VideoMS vidMS = videosMSList.get(j);
                        if (!mExitTasksEarly) {
                            mCount++;
                            mBestScoreId = -1;
                            mBestScore = 0f;
                            processVideo(vidMS, croppedFacesBitmaps);
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                            }
                        } else {
                            break;
                        }
                    }
                    break; // Only Camera folder for now
                }
                if (mExitTasksEarly) {
                    break;
                }
            }
            return null;
        }

        private void processVideo(VideosFoldersMS.VideoMS vidMS, List<Bitmap> croppedFaces) {
            ExtractMpegFrames extractMpegFrames = new ExtractMpegFrames();
            try {
                extractMpegFrames.initResources(vidMS.getData());
                ExtractMpegFrames.DecoderStatus[] decoderStatus = new ExtractMpegFrames.DecoderStatus[3];
                for (int i = 0;i < 3; i++) {
                    decoderStatus[i] = new ExtractMpegFrames.DecoderStatus();
                }
                int idx = 0;
                while (!mExitTasksEarly) {
                    decoderStatus[idx].stage = 0; // extract frame
                    extractMpegFrames.extractFrame(decoderStatus[idx]);
                    if (decoderStatus[idx].isOutputDone)
                        break;

                    if (decoderStatus[idx].doRender &&
                        ((decoderStatus[idx].flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) ==
                                                     MediaCodec.BUFFER_FLAG_KEY_FRAME)) {
                        decoderStatus[idx].bitmapVid = extractMpegFrames.saveFrame();
                        publishProgress(decoderStatus[idx]);
                        Thread.sleep(100);

                        Vector<Box> boxes = sMTCNN.detectFaces(decoderStatus[idx].bitmapVid,
                                                               decoderStatus[idx].bitmapVid.getWidth()/4);
                        if (boxes.size() > 0) {
                            for (int i = 0; i < boxes.size(); i++) {
                                Box b = boxes.elementAt(i);
                                b.toSquareShape();
                                b.limitSquare(decoderStatus[idx].bitmapVid.getWidth(), decoderStatus[idx].bitmapVid.getHeight());
                                decoderStatus[idx].bitmapVidFace = Bitmap.createBitmap(decoderStatus[idx].bitmapVid,
                                        b.left(), b.top(), b.width(), b.height());
                                decoderStatus[idx].stage = 1; // detect faces
                                publishProgress(decoderStatus[idx]);
                                Thread.sleep(100);

                                decoderStatus[idx].stage = 2; // compare faces
                                for (int j = 0; j < croppedFaces.size(); j++) {
                                    Bitmap faceBmp = croppedFaces.get(j);
                                    decoderStatus[idx].score = sMobileFaceNet.compare(faceBmp, decoderStatus[idx].bitmapVidFace);
                                    Log.d(TAG, "j: " + j + ", score: " + decoderStatus[idx].score);
                                    if (decoderStatus[idx].score > mBestScore) {
                                        mBestScoreId = j;
                                        mBestScore = decoderStatus[idx].score;
                                    }
                                    decoderStatus[idx].bitmapFace = faceBmp;
                                    publishProgress(decoderStatus[idx]);
                                    Thread.sleep(100);
                                }
                            }
                        }
                        idx = (idx+1)%3;
                    } // doRender && KEY_FRAME
                } // while (!mExitTasksEarly)
            } catch (IOException e) {
                e.printStackTrace();
            } catch (RuntimeException | InterruptedException e) {
            } finally {
                extractMpegFrames.freeResources();
            }
        }

        @Override
        protected void onProgressUpdate(ExtractMpegFrames.DecoderStatus[] decoderStatus) {
            if (mExitTasksEarly) {
                return;
            }
            if (decoderStatus[0].bitmapVid != null) {
                if (decoderStatus[0].stage == 0) {
                    final ImageView vidImgView = mVidImgViewRef.get();
                    //Log.d(TAG, "stage 0: " + vidImgView);
                    if (vidImgView != null) {
                        BitmapDrawable drawable = new BitmapDrawable(sResources, decoderStatus[0].bitmapVid);
                        vidImgView.setImageDrawable(drawable);
                    }
                    final TextView vidTSTextView = mVidTSTextRef.get();
                    if (vidTSTextView != null) {
                        vidTSTextView.setText(mDateFormatter.format(decoderStatus[0].presentationTimeUs / 1000));
                    }
                    final TextView currVidCountTextView = mCurrVidCountTextRef.get();
                    if (currVidCountTextView != null) {
                        currVidCountTextView.setText(Integer.toString(mCount));
                    }
                } else if (decoderStatus[0].stage == 1) {
                    final ImageView vidFaceImgView = mVidFaceViewRef.get();
                    if (vidFaceImgView != null) {
                        BitmapDrawable drawable = new BitmapDrawable(sResources, decoderStatus[0].bitmapVidFace);
                        vidFaceImgView.setImageDrawable(drawable);
                    }
                } else if (decoderStatus[0].stage == 2) {
                    final ImageView faceImgView = mFaceViewRef.get();
                    if (faceImgView != null) {
                        BitmapDrawable drawable = new BitmapDrawable(sResources, decoderStatus[0].bitmapFace);
                        faceImgView.setImageDrawable(drawable);
                    }
                    final TextView scoreTextView = mScoreTextRef.get();
                    if (scoreTextView != null) {
                        scoreTextView.setText(Float.toString(decoderStatus[0].score));
                    }
                    final TextView bestScoreIdTextView = mBestScoreIdTextRef.get();
                    if (bestScoreIdTextView != null) {
                        bestScoreIdTextView.setText(Integer.toString(mBestScoreId));
                    }
                    final TextView bestScoreTextView = mBestScoreTextRef.get();
                    if (bestScoreTextView != null) {
                        bestScoreTextView.setText(Float.toString(mBestScore));
                    }
                }
            } // if (decoderStatus[0].bitmapVid != null)
        }
    }
}
