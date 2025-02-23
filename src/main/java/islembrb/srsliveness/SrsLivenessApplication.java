package islembrb.srsliveness;

import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.translate.TranslateException;
import jakarta.annotation.Resource;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

@SpringBootApplication
@RestController
@CrossOrigin("*")
public class SrsLivenessApplication {
    @Resource
    private Supplier<Predictor<Image, DetectedObjects>> predictorSupplier;
    @Resource
    private SilentService silentService;
    @Resource
    private ImageFactory imageFactory;
    private final ThreadLocal<Predictor<Image, DetectedObjects>> threadLocalPredictor = ThreadLocal.withInitial(() -> {
        try {
            return predictorSupplier.get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Predictor", e);
        }
    });

    public static void main(String[] args) {
        SpringApplication.run(SrsLivenessApplication.class, args);
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @GetMapping("/process")
    public SseEmitter process(@RequestParam String streamUrl) {
        return null;
    }


}
