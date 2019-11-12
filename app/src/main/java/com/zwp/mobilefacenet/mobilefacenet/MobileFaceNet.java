package com.zwp.mobilefacenet.mobilefacenet;

import android.content.res.AssetManager;
import android.graphics.Bitmap;

import com.zwp.mobilefacenet.MyUtil;

import org.tensorflow.lite.Interpreter;

import java.io.IOException;

/**
 * 人脸比对
 */
public class MobileFaceNet {
    private static final String MODEL_FILE = "MobileFaceNet.tflite";

    public static final int INPUT_IMAGE_SIZE = 112; // 需要feed数据的placeholder的图片宽高
    private static final int EMBEDDING_SIZE = 192; // 输出的embedding维度
    public static final float THRESHOLD = 0.92f; // 设置一个阙值，大于这个值认为是同一个人

    private Interpreter interpreter;

    public MobileFaceNet(AssetManager assetManager) throws IOException {
        interpreter = new Interpreter(MyUtil.loadModelFile(assetManager, MODEL_FILE), new Interpreter.Options());
    }

    public float compare(Bitmap bitmap1, Bitmap bitmap2) {
        // 将人脸resize为112X112大小的，因为下面需要feed数据的placeholder的形状是(2, 112, 112, 3)
        bitmap1 = Bitmap.createScaledBitmap(bitmap1, INPUT_IMAGE_SIZE, INPUT_IMAGE_SIZE, true);
        bitmap2 = Bitmap.createScaledBitmap(bitmap2, INPUT_IMAGE_SIZE, INPUT_IMAGE_SIZE, true);

        float[][][][] datasets = getTwoImageDatasets(bitmap1, bitmap2);
        float[][] embeddings = new float[2][EMBEDDING_SIZE];
        interpreter.run(datasets, embeddings);
        return evaluate(embeddings);
    }

    /**
     * 计算两张图片的相似度
     * @param embeddings
     * @return
     */
    private float evaluate(float[][] embeddings) {
        float[] embeddings1 = embeddings[0];
        float[] embeddings2 = embeddings[1];
        float dist = 0;
        for (int i = 0; i < EMBEDDING_SIZE; i++) {
            dist += Math.pow(embeddings1[i] - embeddings2[i], 2);
        }
        float same = 0;
        for (int i = 0; i < 400; i++) {
            float threshold = 100 * (i + 1);
            if (dist < threshold) {
                same += 1.0 / 400;
            }
        }
        return same;
    }

    /**
     * 转换两张图片为归一化后的数据
     * @param bitmap1
     * @param bitmap2
     * @return
     */
    private float[][][][] getTwoImageDatasets(Bitmap bitmap1, Bitmap bitmap2) {
        Bitmap[] bitmaps = {bitmap1, bitmap2};

        int[] ddims = {bitmaps.length, INPUT_IMAGE_SIZE, INPUT_IMAGE_SIZE, 3};
        float[][][][] datasets = new float[ddims[0]][ddims[1]][ddims[2]][ddims[3]];

        for (int i = 0; i < ddims[0]; i++) {
            Bitmap bitmap = bitmaps[i];
            datasets[i] = MyUtil.normalizeImage(bitmap);
        }
        return datasets;
    }
}
