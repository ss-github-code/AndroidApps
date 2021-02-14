package com.vidsearch.videosearchapp.mtcnn.util;

import android.graphics.Bitmap;
import android.graphics.Matrix;

public class BitmapUtil {
    /*
     * Resize the bitmap by scaling it
     * @param bitmap: input bitmap to process
     * @param scale: value to resize the image
     * @return Bitmap after transforming the input bitmap.
     */
    public static Bitmap resize(Bitmap bitmap, float scale) {
        Matrix m = new Matrix();
        m.postScale(scale, scale);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
    }

    /*
     * Normalize the pixels in the image
     * @param bitmap: input bitmap to process
     * @return array of normalized RGB data
     */
    public static float[][][] normalizeRGBImage(Bitmap bitmap) {
        int h = bitmap.getHeight();
        int w = bitmap.getWidth();
        float[][][] floatValues = new float[h][w][3];

        float imageMean = 127.5f;
        float imageStd = 128;

        int[] pixels = new int[h * w];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, w, h);
        for (int i = 0; i < h; i++) { // Note that the height is first
            for (int j = 0; j < w; j++) {
                final int val = pixels[i * w + j];
                float r = (((val >> 16) & 0xFF) - imageMean) / imageStd;
                float g = (((val >> 8) & 0xFF) - imageMean) / imageStd;
                float b = ((val & 0xFF) - imageMean) / imageStd;
                float[] arr = {r, g, b};
                floatValues[i][j] = arr;
            }
        }
        return floatValues;
    }
    /**
     * Crop the bitmap as specified by the bounding box and resize to size*size and return normalized cropped image
     * @param bitmap: image to crop from
     * @param box: bounding box of the cropped image
     * @param size: cropped image will be resized to size*size
     * @return an array of normalized RGB data
     */
    public static float[][][] cropAndResize(Bitmap bitmap, Box box, int size) {
        // crop and resize
        Matrix matrix = new Matrix();
        float scaleW = 1.0f * size / box.width();
        float scaleH = 1.0f * size / box.height();
        matrix.postScale(scaleW, scaleH);

        Bitmap cropped = Bitmap.createBitmap(
                bitmap, box.left(), box.top(), box.width(), box.height(), matrix, true);

        return normalizeRGBImage(cropped);
    }

    /*
     * Transpose image matrix
     * @param in: input image matrix
     * @return transposed image matrix
     */
    public static float[][][] transposeImage(float[][][] in) {
        int h = in.length;
        int w = in[0].length;
        int channel = in[0][0].length;
        float[][][] out = new float[w][h][channel];
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                out[j][i] = in[i][j] ;
            }
        }
        return out;
    }
}
