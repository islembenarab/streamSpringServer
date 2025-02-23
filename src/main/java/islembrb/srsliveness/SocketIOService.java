package islembrb.srsliveness;

import ai.djl.modality.cv.ImageFactory;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.ByteArrayInputStream;
import java.util.Base64;

@Service
public class SocketIOService {
    @Resource
    private SilentService silentService;
    @Resource
    private ImageFactory imageFactory;

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
            // Convert byte array to OpenCV Mat
//            Mat mat = Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.IMREAD_COLOR);
//
//            // Example processing (uncomment if needed)
//            // Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
//
//            // Encode the processed Mat back to a byte array
//            MatOfByte matOfByte = new MatOfByte();
//            Imgcodecs.imencode(".jpg", mat, matOfByte);
//            byte[] processedBytes = matOfByte.toArray();

            // Encode to base64 and add the prefix for sending back to the client
            String processedBase64 = Base64.getEncoder().encodeToString(imageBytes);
            return "data:image/jpeg;base64," + processedBase64;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}