package com.merchant.portal.service;

import ai.djl.Application;
import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.BoundingBox;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Rectangle;
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

import java.io.IOException;
import java.nio.file.Path;

@Service
public class FaceVerificationService {

    private static final double THRESHOLD_HIGH = 0.60;
    private static final double THRESHOLD_MEDIUM = 0.40;

    public double compareFaces(Path idCardPath, Path selfiePath) throws IOException, ModelNotFoundException, MalformedModelException, TranslateException {

        Image img1 = ImageFactory.getInstance().fromFile(idCardPath);
        Image img2 = ImageFactory.getInstance().fromFile(selfiePath);

        // Step 1: Extract faces from both images
        Image faceFromId = extractFace(img1);
        Image faceFromSelfie = extractFace(img2);

        if (faceFromId == null || faceFromSelfie == null) {
            throw new IllegalArgumentException("Could not detect a face in one or both of the provided images.");
        }

        // Step 2: Define Preprocessing Pipeline for the FaceNet model
        Pipeline pipeline = new Pipeline();
        pipeline.add(new Resize(160, 160));
        pipeline.add(new ToTensor());
        pipeline.add(new Normalize(
                new float[]{0.5f, 0.5f, 0.5f},
                new float[]{0.5f, 0.5f, 0.5f}
        ));

        // Step 3: Load Local Feature Extraction Model
        String modelUrl = "src/main/resources/models/face_feature.pt";

        Criteria<Image, float[]> criteria = Criteria.builder()
                .setTypes(Image.class, float[].class)
                .optModelUrls(modelUrl)
                .optEngine("PyTorch")
                .optTranslator(new FaceFeatureTranslator(pipeline))
                .optProgress(new ProgressBar())
                .build();

        try (ZooModel<Image, float[]> model = criteria.loadModel();
             Predictor<Image, float[]> predictor = model.newPredictor()) {

            // Pass the CROPPED faces instead of the full images
            float[] embedding1 = predictor.predict(faceFromId);
            float[] embedding2 = predictor.predict(faceFromSelfie);

            return calculateCosineSimilarity(embedding1, embedding2);
        }
    }

    /**
     * Detects a face in the image and returns a cropped Image containing only the face.
     * Uses DJL's RetinaFace model downloaded at runtime from the DJL model hub.
     */
    private Image extractFace(Image img) throws ModelNotFoundException, MalformedModelException, IOException, TranslateException {
        // Application.CV.OBJECT_DETECTION is the correct constant (FACE_DETECTION does not exist).
        // optGroupId + optArtifactId tell DJL to fetch the RetinaFace model from its CDN at
        // runtime (cached in ~/.djl.ai/), independently of Maven.
        Criteria<Image, DetectedObjects> criteria = Criteria.builder()
                .optApplication(Application.CV.OBJECT_DETECTION)
                .setTypes(Image.class, DetectedObjects.class)
                .optGroupId("ai.djl.mxnet")
                .optArtifactId("retinaface")
                .optProgress(new ProgressBar())
                .build();

        try (ZooModel<Image, DetectedObjects> model = criteria.loadModel();
             Predictor<Image, DetectedObjects> predictor = model.newPredictor()) {

            DetectedObjects detection = predictor.predict(img);

            if (detection.getNumberOfObjects() == 0) {
                return null;
            }

            // item() is generic: assign to DetectedObjects.DetectedObject to access getBoundingBox()
            DetectedObjects.DetectedObject face = detection.item(0);
            BoundingBox bbox = face.getBoundingBox();
            Rectangle rect = bbox.getBounds();

            int width = img.getWidth();
            int height = img.getHeight();

            // rect coordinates are relative (0.0–1.0), convert to absolute pixels
            int x = (int) (rect.getX() * width);
            int y = (int) (rect.getY() * height);
            int w = (int) (rect.getWidth() * width);
            int h = (int) (rect.getHeight() * height);

            // Add padding so the full face is captured
            int padding = 20;
            x = Math.max(0, x - padding);
            y = Math.max(0, y - padding);
            w = Math.min(width - x, w + (padding * 2));
            h = Math.min(height - y, h + (padding * 2));

            return img.getSubImage(x, y, w, h);
        }
    }

    /**
     * Returns a human-readable confidence level based on the cosine similarity score.
     */
    public String getConfidenceLevel(double similarity) {
        if (similarity >= THRESHOLD_HIGH) {
            return "High";
        } else if (similarity >= THRESHOLD_MEDIUM) {
            return "Medium";
        } else {
            return "Low";
        }
    }

    /**
     * Computes cosine similarity between two embedding vectors.
     * Returns a value between -1 and 1 (1 = identical, 0 = unrelated).
     */
    private double calculateCosineSimilarity(float[] vec1, float[] vec2) {
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * Inner translator that preprocesses a DJL Image into a float[] embedding
     * using the provided preprocessing Pipeline (Resize → ToTensor → Normalize).
     */
    private static final class FaceFeatureTranslator implements Translator<Image, float[]> {

        private final Pipeline pipeline;

        FaceFeatureTranslator(Pipeline pipeline) {
            this.pipeline = pipeline;
        }

        @Override
        public ai.djl.ndarray.NDList processInput(TranslatorContext ctx, Image input) {
            ai.djl.ndarray.NDArray array = input.toNDArray(ctx.getNDManager(), Image.Flag.COLOR);
            return pipeline.transform(new ai.djl.ndarray.NDList(array));
        }

        @Override
        public float[] processOutput(TranslatorContext ctx, ai.djl.ndarray.NDList list) {
            ai.djl.ndarray.NDArray array = list.singletonOrThrow();
            // Flatten to a 1-D float array (the embedding vector)
            return array.toFloatArray();
        }
    }
}