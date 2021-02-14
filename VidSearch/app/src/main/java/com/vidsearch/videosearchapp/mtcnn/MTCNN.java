package com.vidsearch.videosearchapp.mtcnn;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Point;

import com.vidsearch.videosearchapp.mtcnn.util.BitmapUtil;
import com.vidsearch.videosearchapp.mtcnn.util.Box;
import com.vidsearch.videosearchapp.mtcnn.util.MatrixUtil;
import com.vidsearch.videosearchapp.mtcnn.util.ModelFile;

import org.tensorflow.lite.Interpreter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/*
 * MTCNN (Multi-task Cascaded Convolutional Neural Network)
 */
public class MTCNN {
    // Parameters
    private float factor = 0.709f;
    private float pNetThreshold = 0.6f;
    private float rNetThreshold = 0.7f;
    private float oNetThreshold = 0.7f;

    // 3 CNNs (p-net, r-net and o-net)
    private static final String MODEL_FILE_PNET = "pnet.tflite";
    private static final String MODEL_FILE_RNET = "rnet.tflite";
    private static final String MODEL_FILE_ONET = "onet.tflite";

    private Interpreter pInterpreter;
    private Interpreter rInterpreter;
    private Interpreter oInterpreter;

    public MTCNN(AssetManager assetManager) throws IOException {
        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(4);
        pInterpreter = new Interpreter(ModelFile.loadModelFile(assetManager, MODEL_FILE_PNET), options);
        rInterpreter = new Interpreter(ModelFile.loadModelFile(assetManager, MODEL_FILE_RNET), options);
        oInterpreter = new Interpreter(ModelFile.loadModelFile(assetManager, MODEL_FILE_ONET), options);
    }

    /*
     * Face detection
     * @param bitmap: input bitmap to process
     * @param minFaceSize: smallest size of face (pixels) - the larger the value, the faster the detection
     * @return array of bounding Box
     */
    public Vector<Box> detectFaces(Bitmap bitmap, int minFaceSize) {
        Vector<Box> boxes;
        try {
            //1: PNet generate candidate boxes
            boxes = pNet(bitmap, minFaceSize);
            square_limit(boxes, bitmap.getWidth(), bitmap.getHeight());

            //2: RNet
            boxes = rNet(bitmap, boxes);
            square_limit(boxes, bitmap.getWidth(), bitmap.getHeight());

            //3: ONet
            boxes = oNet(bitmap, boxes);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            boxes = new Vector<>();
        }
        return boxes;
    }

    private void square_limit(Vector<Box> boxes, int w, int h) {
        // Reshape each box to square
        for (int i = 0; i < boxes.size(); i++) {
            boxes.get(i).toSquareShape();
            boxes.get(i).limitSquare(w, h);
        }
    }

    /*
     * Stage 1:
     * Create an image pyramid in order to detect faces of different sizes
     * NMS (non-max suppression) only executes after regression
     * (1) For each scale , use NMS with threshold=0.5
     * (2) For all candidates , use NMS with threshold=0.7
     * (3) Calibrate Bounding Box
     * @param bitmap: input bitmap to process
     * @param minFaceSize: smallest face size (pixels)
     * @return array of bounding box to pass to stage 2
     */
    private Vector<Box> pNet(Bitmap bitmap, int minFaceSize) {
        int whMin = Math.min(bitmap.getWidth(), bitmap.getHeight());
        // currentFaceSize=minSize/(factor^k) k=0,1,2... until it exceeds whMin
        float currentFaceSize = minFaceSize;
        Vector<Box> totalBoxes = new Vector<>();

        //1: Generate images for the image pyramid and feed to Pnet
        while (currentFaceSize <= whMin) {
            float scale = 12.0f / currentFaceSize;

            // (1)Image Resize
            Bitmap bm = BitmapUtil.resize(bitmap, scale);
            int w = bm.getWidth();
            int h = bm.getHeight();

            // (2)Run CNN
            int outW = (int) (Math.ceil(w * 0.5 - 5) + 0.5);
            int outH = (int) (Math.ceil(h * 0.5 - 5) + 0.5);
            float[][][][] prob1 = new float[1][outW][outH][2];
            float[][][][] conv4_2_BiasAdd = new float[1][outW][outH][4];

            pNetForward(bm, prob1, conv4_2_BiasAdd);
            prob1 = MatrixUtil.transposeBatch3DData(prob1);
            conv4_2_BiasAdd = MatrixUtil.transposeBatch3DData(conv4_2_BiasAdd);

            // (3)Data analysis
            Vector<Box> curBoxes = new Vector<>();
            pNetGenerateBoxes(prob1, conv4_2_BiasAdd, scale, curBoxes);

            // (4)nms 0.5
            nms(curBoxes, 0.5f, "Union");

            // (5)add to totalBoxes
            for (int i = 0; i < curBoxes.size(); i++)
                if (!curBoxes.get(i).deleted)
                    totalBoxes.addElement(curBoxes.get(i));

            // next face size: proportional increase
            currentFaceSize /= factor;
        }

        // 2. NMS 0.7
        nms(totalBoxes, 0.7f, "Union");

        // 3. BBR
        BoundingBoxReggression(totalBoxes);

        return removeDeletedBoxes(totalBoxes);
    }

    /*
     * pnet Forward propagation
     *
     * @param bitmap: scaled bitmap
     * @param prob1: batch matrix of weights
     * @param conv4_2_BiasAdd: batch matrix of biases
     * @return
     */
    private void pNetForward(Bitmap bitmap, float[][][][] prob1, float[][][][] conv4_2_BiasAdd) {
        float[][][] img = BitmapUtil.normalizeRGBImage(bitmap);
        float[][][][] pNetIn = new float[1][][][];
        pNetIn[0] = img;
        pNetIn = MatrixUtil.transposeBatch3DData(pNetIn);

        Map<Integer, Object> outputs = new HashMap<>();
        outputs.put(pInterpreter.getOutputIndex("pnet/prob1"), prob1);
        outputs.put(pInterpreter.getOutputIndex("pnet/conv4-2/BiasAdd"), conv4_2_BiasAdd);

        pInterpreter.runForMultipleInputsOutputs(new Object[]{pNetIn}, outputs);
    }

    private int pNetGenerateBoxes(float[][][][] prob1,
                                  float[][][][] conv4_2_BiasAdd,
                                  float scale,
                                  Vector<Box> boxes) {
        int h = prob1[0].length;
        int w = prob1[0][0].length;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float score = prob1[0][y][x][1];
                // only accept prob >threadshold (0.6)
                if (score > pNetThreshold) {
                    Box box = new Box();
                    // core
                    box.score = score;
                    // box
                    box.box[0] = Math.round(x * 2 / scale);
                    box.box[1] = Math.round(y * 2 / scale);
                    box.box[2] = Math.round((x * 2 + 11) / scale);
                    box.box[3] = Math.round((y * 2 + 11) / scale);
                    // bbr
                    for (int i = 0; i < 4; i++) {
                        box.bbr[i] = conv4_2_BiasAdd[0][y][x][i];
                    }
                    // add
                    boxes.addElement(box);
                }
            }
        }
        return 0;
    }

    /**
     * nms，non-max suppression: ineligible box's delete is set to true
     *
     * @param boxes: vector of boxes detected
     * @param threshold: iou >= threshold
     * @param method: "union" or "min"
     */
    private void nms(Vector<Box> boxes, float threshold, String method) {
        // NMS.Pairwise comparison
        for (int i = 0; i < boxes.size(); i++) {
            Box box = boxes.get(i);
            if (!box.deleted) {
                for (int j = i + 1; j < boxes.size(); j++) {
                    Box box2 = boxes.get(j);
                    if (!box2.deleted) {
                        // Intersect over Union (IoU)
                        // x1 = max(x), y1 = max(y); x2 = min(x), y2 = min(y)
                        int x1 = Math.max(box.box[0], box2.box[0]);
                        int y1 = Math.max(box.box[1], box2.box[1]);
                        int x2 = Math.min(box.box[2], box2.box[2]);
                        int y2 = Math.min(box.box[3], box2.box[3]);
                        if (x2 < x1 || y2 < y1) continue;
                        // if width and height of overlap are both positive
                        int areaIoU = (x2 - x1 + 1) * (y2 - y1 + 1);
                        float iou = 0f;
                        if (method.equals("Union"))
                            // IoU = Overlapping region/Combined region
                            iou = 1.0f * areaIoU / (box.area() + box2.area() - areaIoU);
                        else if (method.equals("Min"))
                            iou = 1.0f * areaIoU / (Math.min(box.area(), box2.area()));
                        if (iou >= threshold) {
                            // if iou is >= threshold, delete the box with the smaller score
                            if (box.score > box2.score)
                                box2.deleted = true;
                            else
                                box.deleted = true;
                        }
                    }
                } // for (int j = i + 1; j < boxes.size(); j++)
            }
        } // for (int i = 0; i < boxes.size(); i++)
    }

    private void BoundingBoxReggression(Vector<Box> boxes) {
        for (int i = 0; i < boxes.size(); i++)
            boxes.get(i).calibrate();
    }

    /*
     * Stage 2: Refine Net
     * @param bitmap: bitmap to process
     * @param boxes: array of bounding boxes from pNet
     * @return array of bounding boxes to pass on to stage 3. rNet is similar to pNet; it includes
     * the coordinates of new, more accurate bounding boxes.
     */
    private Vector<Box> rNet(Bitmap bitmap, Vector<Box> boxes) {
        // RNet input: boxes from pNet
        int num = boxes.size();
        float[][][][] rNetIn = new float[num][24][24][3];
        for (int i = 0; i < num; i++) {
            // resize to 24x24 and normalize
            float[][][] curCrop = BitmapUtil.cropAndResize(bitmap, boxes.get(i), 24);
            curCrop = BitmapUtil.transposeImage(curCrop);
            rNetIn[i] = curCrop;
        }

        // Run RNet
        rNetForward(rNetIn, boxes);

        // RNet threshold to get rid of boxes with low confidence
        for (int i = 0; i < num; i++) {
            if (boxes.get(i).score < rNetThreshold) {
                boxes.get(i).deleted = true;
            }
        }

        // NMS to further eliminate redundant boxes
        nms(boxes, 0.7f, "Union");
        // BBR
        BoundingBoxReggression(boxes);
        return removeDeletedBoxes(boxes);
    }

    /**
     * Run the RNet CNN and write score and bias into boxes
     * @param rNetIn
     * @param boxes
     */
    private void rNetForward(float[][][][] rNetIn, Vector<Box> boxes) {
        int num = rNetIn.length;
        if (num == 0) return;
        float[][] prob1 = new float[num][2];
        float[][] conv5_2_conv5_2 = new float[num][4];

        Map<Integer, Object> outputs = new HashMap<>();
        outputs.put(rInterpreter.getOutputIndex("rnet/prob1"), prob1);
        outputs.put(rInterpreter.getOutputIndex("rnet/conv5-2/conv5-2"), conv5_2_conv5_2);
        rInterpreter.runForMultipleInputsOutputs(new Object[]{rNetIn}, outputs);

        // Save the score and bias
        for (int i = 0; i < num; i++) {
            boxes.get(i).score = prob1[i][1];
            for (int j = 0; j < 4; j++) {
                boxes.get(i).bbr[j] = conv5_2_conv5_2[i][j];
            }
        }
    }

    /**
     * Stage 3: ONet
     * @param bitmap: image to process
     * @param boxes: from RNet
     * @return array of bounding boxes, coordinates of the 5 facial landmarks in each box and the score
     */
    private Vector<Box> oNet(Bitmap bitmap, Vector<Box> boxes) {
        // ONet input: boxes from RNet
        int num = boxes.size();
        float[][][][] oNetIn = new float[num][48][48][3];
        for (int i = 0; i < num; i++) {
            // resize to 48x48 and normalize
            float[][][] curCrop = BitmapUtil.cropAndResize(bitmap, boxes.get(i), 48);
            curCrop = BitmapUtil.transposeImage(curCrop);
            oNetIn[i] = curCrop;
        }

        // Run ONet
        oNetForward(oNetIn, boxes);
        // ONet threshold
        for (int i = 0; i < num; i++) {
            if (boxes.get(i).score < oNetThreshold) {
                boxes.get(i).deleted = true;
            }
        }
        BoundingBoxReggression(boxes);
        // Nms
        nms(boxes, 0.7f, "Min");
        return removeDeletedBoxes(boxes);
    }

    /**
     * Runs ONet CNN, writes score and bias into boxes
     * @param oNetIn
     * @param boxes
     */
    private void oNetForward(float[][][][] oNetIn, Vector<Box> boxes) {
        int num = oNetIn.length;
        if (num == 0) return;
        float[][] prob1 = new float[num][2];
        float[][] conv6_2_conv6_2 = new float[num][4];
        float[][] conv6_3_conv6_3 = new float[num][10];

        Map<Integer, Object> outputs = new HashMap<>();
        outputs.put(oInterpreter.getOutputIndex("onet/prob1"), prob1);
        outputs.put(oInterpreter.getOutputIndex("onet/conv6-2/conv6-2"), conv6_2_conv6_2);
        outputs.put(oInterpreter.getOutputIndex("onet/conv6-3/conv6-3"), conv6_3_conv6_3);
        oInterpreter.runForMultipleInputsOutputs(new Object[]{oNetIn}, outputs);

        // Save score, bias, 5 landmarks
        for (int i = 0; i < num; i++) {
            Box b = boxes.get(i);
            // prob
            b.score = prob1[i][1];
            // bias
            for (int j = 0; j < 4; j++) {
                b.bbr[j] = conv6_2_conv6_2[i][j];
            }

            int w = b.width();
            int h = b.height();
            int left = b.left();
            int top = b.top();
            // landmark
            for (int j = 0; j < 5; j++) {
                int x = Math.round(left + (conv6_3_conv6_3[i][j] * w));
                int y = Math.round(top + (conv6_3_conv6_3[i][j + 5] * h));
                b.landmark[j] = new Point(x, y);
            }
        }
    }

    /*
     * Remove the deleted boxes
     * @param boxes: array of boxes
     * @return array of boxes that have not been deleted
     */
    public static Vector<Box> removeDeletedBoxes(Vector<Box> boxes) {
        Vector<Box> b = new Vector<>();
        for (int i = 0; i < boxes.size(); i++) {
            if (!boxes.get(i).deleted) {
                b.addElement(boxes.get(i));
            }
        }
        return b;
    }
}
