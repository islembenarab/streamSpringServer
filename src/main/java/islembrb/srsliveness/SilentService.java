package islembrb.srsliveness;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import org.springframework.stereotype.Component;

import java.nio.FloatBuffer;
import java.util.Collections;

import static org.bytedeco.opencv.global.opencv_imgproc.resize;

@Component
public class SilentService {
    private final OrtSession session1;
    private final OrtSession session2;
    private final OrtEnvironment env;
    private volatile boolean initialized = false;


    public SilentService() throws OrtException {
        try {
            env = OrtEnvironment.getEnvironment();
            session1 = env.createSession("src/main/resources/2.7_80x80_MiniFASNetV2.onnx", new OrtSession.SessionOptions());
            session2 = env.createSession("src/main/resources/4_0_0_80x80_MiniFASNetV1SE.onnx", new OrtSession.SessionOptions());
            initialized = true;
            System.out.println("OrtEnvironment and sessions initialized successfully.");
        } catch (Exception e) {
            System.err.println("Failed to initialize OrtEnvironment or sessions: " + e.getMessage());
            e.printStackTrace();
            throw e; // Rethrow to prevent the service from starting in an invalid state
        }
    }

    public boolean analyzeFace(Mat face) throws OrtException {
        if (!initialized) {
            throw new IllegalStateException("Service not properly initialized");
        }

        OnnxTensor input1 = preprocess(face, 80, 80);
        OnnxTensor input2 = preprocess(face, 80, 80);

        try {
            // Run inference for both sessions
            float[][] output1 = (float[][]) session1.run(Collections.singletonMap("input", input1)).get(0).getValue();
            float[][] output2 = (float[][]) session2.run(Collections.singletonMap("input", input2)).get(0).getValue();

            // Flatten the 2D outputs to 1D
            float[] flattenedOutput1 = output1[0];
            float[] flattenedOutput2 = output2[0];

            // Combine outputs and return the result
            return combineOutputs(flattenedOutput1, flattenedOutput2).isReal;
        } finally {
            // Clean up tensors
            input1.close();
            input2.close();
        }
    }

    private OnnxTensor preprocess(Mat face, int width, int height) throws OrtException {
        if (env == null) {
            throw new IllegalStateException("OrtEnvironment not initialized");
        }

        Mat resizedFace = new Mat();
        resize(face, resizedFace, new Size(width, height));
        resizedFace.convertTo(resizedFace, org.bytedeco.opencv.global.opencv_core.CV_32F);

        // Ensure the image has 3 channels (RGB)
        if (resizedFace.channels() != 3) {
            throw new IllegalArgumentException("Input image must have 3 channels (RGB)");
        }

        // Change layout from HWC to CHW
        float[] chwData = new float[(int) (resizedFace.total() * resizedFace.channels())];
        int index = 0;
        for (int c = 0; c < resizedFace.channels(); c++) {
            for (int row = 0; row < resizedFace.rows(); row++) {
                for (int col = 0; col < resizedFace.cols(); col++) {
                    chwData[index++] = resizedFace.ptr(row, col).getFloat(c);
                }
            }
        }

        // Add batch dimension and create tensor
        long[] shape = {1, resizedFace.channels(), resizedFace.rows(), resizedFace.cols()};
        return OnnxTensor.createTensor(env, FloatBuffer.wrap(chwData), shape);
    }
    private static class PredictionResult {
        boolean isReal;
        float score;

        public PredictionResult(boolean isReal, float score) {
            this.isReal = isReal;
            this.score = score;
        }
    }

    private PredictionResult combineOutputs(float[] output1, float[] output2) {
        // Apply softmax to the outputs
        float[] firstResult = softmax(output1);
        float[] secondResult = softmax(output2);

        // Combine the predictions
        float[] prediction = new float[3];
        for (int i = 0; i < 3; i++) {
            prediction[i] = firstResult[i] + secondResult[i];
        }

        // Determine the label with the highest score
        int label = 0;
        for (int i = 1; i < prediction.length; i++) {
            if (prediction[i] > prediction[label]) {
                label = i;
            }
        }

        // Check if the label corresponds to "real" (assuming label 1 means real)
        boolean isReal = (label == 1);

        // Calculate the confidence score
        float score = prediction[label] / 2.0f;

        return new PredictionResult(isReal, score);
    }

    private float[] softmax(float[] logits) {
        float maxLogit = Float.NEGATIVE_INFINITY;
        for (float logit : logits) {
            if (logit > maxLogit) {
                maxLogit = logit;
            }
        }

        float sumExp = 0.0f;
        float[] expLogits = new float[logits.length];
        for (int i = 0; i < logits.length; i++) {
            expLogits[i] = (float) Math.exp(logits[i] - maxLogit);
            sumExp += expLogits[i];
        }

        for (int i = 0; i < expLogits.length; i++) {
            expLogits[i] /= sumExp;
        }

        return expLogits;
    }
}