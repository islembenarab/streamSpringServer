package islembrb.srsliveness;

import ai.djl.Application;
import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.onnxruntime.OrtException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

import java.io.IOException;
import java.util.function.Supplier;

@Configuration
public class DjlConfig {
    @Bean
    public ImageFactory imageFactory() {
        return ImageFactory.getInstance();
    }
    @Bean
    public Criteria<Image, DetectedObjects> criteria() {
        return Criteria.builder()
                .setTypes(Image.class, DetectedObjects.class)
                .optApplication(Application.CV.OBJECT_DETECTION)
                .optFilter("size", "300")
                .optFilter("backbone", "resnet50")
                .optFilter("dataset", "coco")
                .optArgument("threshold", 0.1)
                .build();
    }

    @Bean
    public ZooModel<Image, DetectedObjects> model(
            @Qualifier("criteria") Criteria<Image, DetectedObjects> criteria)
            throws MalformedModelException, ModelNotFoundException, IOException {
//        Map<Application, List<Artifact>> applicationListMap = ModelZoo.listModels();
//        applicationListMap.forEach((application, artifacts) -> {
//            System.out.println(application + ":");
//            artifacts.forEach(System.out::println);
//        });
        return ModelZoo.loadModel(criteria);
    }

    @Bean(destroyMethod = "close")
    @Scope(value = "prototype", proxyMode = ScopedProxyMode.INTERFACES)
    public Predictor<Image, DetectedObjects> predictor(ZooModel<Image, DetectedObjects> model) {
        return model.newPredictor();
    }

    /**
     * Inject with @Resource or autowired. Only safe to be used in the try with resources.
     * @param model object for which predictor is expected to be returned
     * @return supplier of predictor for thread-safe inference
     */
    @Bean
    public Supplier<Predictor<Image, DetectedObjects>> predictorProvider(ZooModel<Image, DetectedObjects> model) {
        return model::newPredictor;
    }
    @Bean
    public SilentService silentService() throws OrtException {
        return new SilentService();
    }


}
