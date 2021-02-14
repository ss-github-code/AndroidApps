package com.vidsearch.videosearchapp.mtcnn.util;

public class MatrixUtil {
    /*
     * Transpose a batch matrix of 3D data
     */
    public static float[][][][] transposeBatch3DData(float[][][][] in) {
        int batchSize = in.length;
        int h = in[0].length;
        int w = in[0][0].length;
        int channel = in[0][0][0].length;
        float[][][][] out = new float[batchSize][w][h][channel];
        for (int i = 0; i < batchSize; i++) {
            for (int j = 0; j < h; j++) {
                for (int k = 0; k < w; k++) {
                    out[i][k][j] = in[i][j][k] ;
                }
            }
        }
        return out;
    }
}
