package islembrb.srsliveness;

import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.DetectedObjects;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.bytedeco.opencv.opencv_core.Mat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.util.Base64;
import java.util.function.Supplier;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imdecode;

@Service
public class SocketIOService {
    @Resource
    private SilentService silentService;
    @Resource
    private Supplier<Predictor<Image, DetectedObjects>> predictorSupplier;

    @Resource
    private ImageFactory imageFactory;


    // Use ThreadLocal to ensure each thread has its own Predictor
    private final ThreadLocal<Predictor<Image, DetectedObjects>> threadLocalPredictor = ThreadLocal.withInitial(() -> {
        try {
            return predictorSupplier.get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Predictor", e);
        }
    });


    @Autowired
    private SocketIOServer socketIOServer;

    @PostConstruct
    public void start() {
        socketIOServer.addEventListener("frame", byte[].class, new DataListener<byte[]>() {
            @Override
            public void onData(SocketIOClient client, byte[] frameData, com.corundumstudio.socketio.AckRequest ackRequest) {
                // Process the incoming binary frame
                String processedFrame = processFrame(frameData);

                // Send the processed frame back to the client
                if (processedFrame != null) {
                    client.sendEvent("processedFrame", processedFrame);
                }
            }
        });

        socketIOServer.start();
    }

    private String processFrame(byte[] imageBytes) {
        try {
            // Convert the byte array to a Mat object
//            Mat frame = new Mat(imageBytes);
//            boolean b = silentService.analyzeFace(frame);
            Image image = imageFactory.fromInputStream(new ByteArrayInputStream(imageBytes));
            DetectedObjects detectedObjects = threadLocalPredictor.get().predict(image);
            // Convert the Mat object to a byte array
            image.drawBoundingBoxes(detectedObjects);
            // Encode to base64 and add the prefix for sending back to the client
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            image.save(outputStream, "png");

            String processedBase64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());
            return "data:image/jpeg;base64," + processedBase64;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}