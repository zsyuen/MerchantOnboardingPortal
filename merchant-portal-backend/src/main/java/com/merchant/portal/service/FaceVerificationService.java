package com.merchant.portal.service;

import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.transform.Normalize;
import ai.djl.modality.cv.transform.Resize;
import ai.djl.modality.cv.transform.ToTensor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.Pipeline;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import ai.djl.translate.Batchifier;
import ai.djl.translate.TranslateException;
import ai.djl.training.util.ProgressBar;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Service
public class FaceVerificationService {

    private static final double THRESHOLD_HIGH = 0.60;
    private static final double THRESHOLD_MEDIUM = 0.40;

    public double compareFaces(byte[] idCardBytes, byte[] selfieBytes) throws IOException, ModelNotFoundException, MalformedModelException, TranslateException {

        Image img1 = ImageFactory.getInstance().fromInputStream(new ByteArrayInputStream(idCardBytes));
        Image img2 = ImageFactory.getInstance().fromInputStream(new ByteArrayInputStream(selfieBytes));

        // 1. Define Preprocessing Pipeline
        Pipeline pipeline = new Pipeline();
        pipeline.add(new Resize(160, 160));
        pipeline.add(new ToTensor());
        pipeline.add(new Normalize(
                new float[]{0.5f, 0.5f, 0.5f},
                new float[]{0.5f, 0.5f, 0.5f}
        ));

        // 2. Load Local Model
        String modelUrl = "src/main/resources/models/face_feature.pt";

        Criteria<Image, float[]> criteria = Criteria.builder()
                .setTypes(Image.class, float[].class)
                .optModelUrls(modelUrl)
                .optEngine("PyTorch")
                .optTranslator(new FaceFeatureTranslator(pipeline)) // This uses the class below
                .optProgress(new ProgressBar())
                .build();

        try (ZooModel<Image, float[]> model = criteria.loadModel();
             Predictor<Image, float[]> predictor = model.newPredictor()) {

            float[] embedding1 = predictor.predict(img1);
            float[] embedding2 = predictor.predict(img2);

            return calculateCosineSimilarity(embedding1, embedding2);
        }
    }

    public String getConfidenceLevel(double score) {
        if (score >= THRESHOLD_HIGH) return "High";
        if (score >= THRESHOLD_MEDIUM) return "Medium";
        return "Low";
    }

    private double calculateCosineSimilarity(float[] vectorA, float[] vectorB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public static class FaceFeatureTranslator implements Translator<Image, float[]> {
        private Pipeline pipeline;

        public FaceFeatureTranslator(Pipeline pipeline) {
            this.pipeline = pipeline;
        }

        @Override
        public ai.djl.ndarray.NDList processInput(TranslatorContext ctx, Image input) {
            // 1. Convert the Image object to an NDArray
            ai.djl.ndarray.NDArray array = input.toNDArray(ctx.getNDManager());

            // 2. Wrap the array in an NDList
            ai.djl.ndarray.NDList list = new ai.djl.ndarray.NDList(array);

            // 3. Transform using the pipeline (Pass only the list)
            return pipeline.transform(list);
        }

        @Override
        public float[] processOutput(TranslatorContext ctx, ai.djl.ndarray.NDList list) {
            return list.get(0).toFloatArray();
        }

        @Override
        public Batchifier getBatchifier() {
            return Batchifier.STACK;
        }
    }
}