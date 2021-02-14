package com.vidsearch.videosearchapp.mobilefacenet;

import android.content.res.AssetManager;
import android.graphics.Bitmap;

import com.vidsearch.videosearchapp.mobilefacenet.util.MiscUtil;
import com.vidsearch.videosearchapp.mtcnn.util.BitmapUtil;
import com.vidsearch.videosearchapp.mtcnn.util.ModelFile;

import org.tensorflow.lite.Interpreter;

import java.io.IOException;

public class MobileFaceNet {
    private static final String MODEL_FILE_FACENET = "MobileFaceNet.tflite";

    private static final int INPUT_IMAGE_SIZE = 112; // width and height of the input image
    private Interpreter interpreter;

    public MobileFaceNet(AssetManager assetManager) throws IOException {
        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(4);
        interpreter = new Interpreter(ModelFile.loadModelFile(assetManager, MODEL_FILE_FACENET), options);
    }

    public float compare(Bitmap bitmap1, Bitmap bitmap2) {
        // Resize the face to 112x112, because the shape of the placeholder that needs feed data is (2, 112, 112, 3)
        Bitmap bitmapScale1 = Bitmap.createScaledBitmap(bitmap1, INPUT_IMAGE_SIZE, INPUT_IMAGE_SIZE, true);
        Bitmap bitmapScale2 = Bitmap.createScaledBitmap(bitmap2, INPUT_IMAGE_SIZE, INPUT_IMAGE_SIZE, true);

        float[][][][] datasets = getTwoImageDatasets(bitmapScale1, bitmapScale2);
        float[][] embeddings = new float[2][192];
        interpreter.run(datasets, embeddings);
        MiscUtil.l2Normalize(embeddings, 1e-10);
        return evaluate(embeddings);
    }

    /*
     * Convert two pictures into normalized data
     */
    private float[][][][] getTwoImageDatasets(Bitmap bitmap1, Bitmap bitmap2) {
        Bitmap[] bitmaps = { bitmap1, bitmap2 };
        int[] ddims = { bitmaps.length, INPUT_IMAGE_SIZE, INPUT_IMAGE_SIZE, 3 };
        float[][][][] datasets = new float[ddims[0]][ddims[1]][ddims[2]][ddims[3]];

        for (int i = 0; i < ddims[0]; i++) {
            Bitmap bitmap = bitmaps[i];
            datasets[i] = BitmapUtil.normalizeRGBImage(bitmap);
        }
        return datasets;
    }

    /*
     * Calculate the simalarity of two pictures, using l2 loss
     */
    private float evaluate(float[][] embeddings) {
        float[] embeddings1 = embeddings[0];
        float[] embeddings2 = embeddings[1];
        float dist = 0;
        for (int i = 0; i < 192; i++) {
            dist += Math.pow(embeddings1[i] - embeddings2[i], 2);
        }

        float same = 0;
        for (int i = 0; i < 400; i++) {
            float threshold = 0.01f * (i + 1);
            if (dist < threshold) {
                same += 1.0/400;
            }
        }
        return same;
    }
}
