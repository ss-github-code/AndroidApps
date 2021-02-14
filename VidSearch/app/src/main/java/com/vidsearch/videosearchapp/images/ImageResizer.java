package com.vidsearch.videosearchapp.images;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.util.Log;

import java.io.IOException;

import androidx.fragment.app.FragmentActivity;

/*
 A subclass of ImageWorker that resizes images from resources given a target width and height.
 It is useful when the input images might be too large to simply load directly into memory.
 */
public class ImageResizer extends ImageWorker {
    private static final String TAG = "ImageResizer";
    private final int mImageWidth;
    private final int mImageHeight;

    public ImageResizer(FragmentActivity fragmentActivity, int imageWidth, int imageHeight, char keyPrefix) {
        super(fragmentActivity, keyPrefix);
        mImageWidth = imageWidth;
        mImageHeight = imageHeight;
    }

    public ImageResizer(Activity activity, int imageWidth, int imageHeight, char keyPrefix) {
        super(activity, keyPrefix);
        mImageWidth = imageWidth;
        mImageHeight = imageHeight;
    }

    @Override
    protected Bitmap processBitmap(String filename, int orientation) {
        return decodeBitmapFromFile(filename, orientation, false, mImageWidth, mImageHeight, null);
    }

    @Override
    protected Bitmap processBitmap(String filename, int orientation, boolean fullSize) {
        return decodeBitmapFromFile(filename, orientation, fullSize, mImageWidth, mImageHeight, null);
    }

    @Override
    protected Bitmap processBitmap(Bitmap fullBitmap, ScaleInfo scaleInfo) {
        final int srcWidth = fullBitmap.getWidth();
        final int srcHeight = fullBitmap.getHeight();

        fillScaleInfo(scaleInfo, srcWidth, srcHeight, mImageWidth, mImageHeight);
        final Matrix m = new Matrix();
        m.setScale(scaleInfo.mScale, scaleInfo.mScale);

        final int srcCroppedW = Math.round(mImageWidth/scaleInfo.mScale);
        final int srcCroppedH = Math.round(mImageHeight/scaleInfo.mScale);

        // assume CENTER_CROP
        int srcX = srcWidth/2 - srcCroppedW/2; // (int) (srcWidth * horizontalCenterPercent - srcCroppedW / 2);
        int srcY = srcHeight/2 - srcCroppedH/2;// (int) (srcHeight * verticalCenterPercent - srcCroppedH / 2);

        // Nudge srcX and srcY to be within the bounds of src
        srcX = Math.max(Math.min(srcX, srcWidth - srcCroppedW), 0);
        srcY = Math.max(Math.min(srcY, srcHeight - srcCroppedH), 0);
        //Log.d(TAG, "Src: (" + srcWidth + ", " + srcHeight + ")" + " : " + srcX + ", " + srcY);
        //Log.d(TAG, "cropped: " + srcCroppedW + ", " + srcCroppedH);

        final Bitmap cropped = Bitmap.createBitmap(fullBitmap, srcX, srcY, srcCroppedW, srcCroppedH, m, true /* filter */);
        return cropped;
    }

    public static Bitmap decodeBitmapFromFile(String fileName, int orientation, boolean fullSize, int reqWidth, int reqHeight, ScaleInfo scaleInfo) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(fileName, options);

        // calculate size
        options.inSampleSize = (fullSize == true) ? 1 : calculateInSampleSize(options, reqWidth, reqHeight);

        // try to use inBitmap; if inBitmap is set, BitmapFactory.decodeFile will attempt to reuse this
        addInBitmapOptions(options);
        if (scaleInfo != null)
            fillScaleInfo(scaleInfo, options.outWidth, options.outHeight, reqWidth, reqHeight);

        // decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        Bitmap bitmap = BitmapFactory.decodeFile(fileName, options);
        if (orientation != 0) {
            Matrix m = new Matrix();
            m.postRotate(orientation);
            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
            bitmap.recycle();
            return rotatedBitmap;
        }
        return bitmap;
    }

    public static void fillScaleInfo(ScaleInfo scaleInfo, int srcWidth, int srcHeight, int reqWidth, int reqHeight) {
        scaleInfo.mScale = Math.max((float) reqWidth / srcWidth, (float)reqHeight / srcHeight);

        final int srcCroppedW = Math.round(reqWidth/scaleInfo.mScale);
        final int srcCroppedH = Math.round(reqHeight/scaleInfo.mScale);

        // assume CENTER_CROP
        int srcX = srcWidth/2 - srcCroppedW/2; // (int) (srcWidth * horizontalCenterPercent - srcCroppedW / 2);
        int srcY = srcHeight/2 - srcCroppedH/2;// (int) (srcHeight * verticalCenterPercent - srcCroppedH / 2);

        // Nudge srcX and srcY to be within the bounds of src
        srcX = Math.max(Math.min(srcX, srcWidth - srcCroppedW), 0);
        srcY = Math.max(Math.min(srcY, srcHeight - srcCroppedH), 0);
        //Log.d(TAG, "Src: (" + srcWidth + ", " + srcHeight + ")" + " : " + srcX + ", " + srcY);
        //Log.d(TAG, "cropped: " + srcCroppedW + ", " + srcCroppedH);
        scaleInfo.mScaledSrcX = Math.round(srcX*scaleInfo.mScale);
        scaleInfo.mScaledSrcY = Math.round(srcY*scaleInfo.mScale);
        scaleInfo.mScaledWidth = reqWidth;
        scaleInfo.mScaledHeight = reqHeight;
    }

    public static ScaleInfo getScaleInfoForBitmap(String fileName, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        ScaleInfo scaleInfo = new ScaleInfo();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(fileName, options);
        fillScaleInfo(scaleInfo, options.outWidth, options.outHeight, reqWidth, reqWidth);
        return scaleInfo;
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            // calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width
            while ((halfHeight / inSampleSize) > reqHeight &&
                    (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }
        // a panorama may have a much larger width than height. in these cases the total pixels
        // might still be too large to fit comfortably in memory.
        long totalPixels = width * height / (inSampleSize * inSampleSize);
        // anything more than 2x the requested pixels, sample down further
        final long totalReqPixelsCap = reqWidth * reqHeight * 2;
        while (totalPixels > totalReqPixelsCap) {
            inSampleSize *= 2; // Note: the decoder uses a final value based on powers of 2
            totalPixels /= 2;
        }
        return inSampleSize;
    }

    private static void addInBitmapOptions(BitmapFactory.Options options) {
        options.inMutable = true; // inBitmap only works with mutable bitmaps
        // try to find a bitmap to use for inBitmap
        Bitmap inBitmap = sImageMemCache.getBitmapFromReusableSet(options);
        if (inBitmap != null) {
            options.inBitmap = inBitmap;
        }
    }
}
