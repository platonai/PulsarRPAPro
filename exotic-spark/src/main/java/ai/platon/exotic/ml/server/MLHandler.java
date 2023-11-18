package ai.platon.exotic.ml.server;

import ai.platon.exotic.ml.spark.ExoticMLUtils;
import ai.platon.exotic.ml.spark.RandomForest;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Paths;

public class MLHandler implements HttpHandler {
    private Logger logger = LoggerFactory.getLogger(MLHandler.class);

    private int numClasses = 7;
    private int numFeatures = 276;

    private RandomForest classifier;

    public void setClassifier(RandomForest classifier) {
        this.classifier = classifier;
    }

    public MLHandler() {
        var datasetPath = Paths.get("data/dom/amazon.dataset.6.labels.txt");
        classifier = new RandomForest(numClasses, datasetPath);
    }

    @Override
    public void handle(HttpExchange he) throws IOException {
        StringBuilder response = new StringBuilder();

        try (var isr = new InputStreamReader(he.getRequestBody());
             BufferedReader reader = new BufferedReader(isr)
        ) {
            reader.lines().map(this::predict).forEach(result -> response.append(result).append('\n'));
        }

        he.sendResponseHeaders(200, response.length());
        try (OutputStream os = he.getResponseBody()) {
            os.write(response.toString().getBytes());
        } finally {
            System.out.printf("Response : %s%n", response);
        }
    }

    private String predict(String libsvmRecord) {
        var record = ExoticMLUtils.parseLibSVMRecord(libsvmRecord, numFeatures);

        if (classifier != null) {
            try {
                var pred = classifier.predict(record.getValue());
                return pred.getKey().intValue() + " " + StringUtils.substringAfter(libsvmRecord, " ");
            } catch (Exception e) {
                e.printStackTrace();
                logger.warn(e.getMessage());
            }
        }

        // Negative predict result means an error
        return ExoticMLUtils.encodeToLibSVMRecord(record.getValue().toArray(), -1).toString();
    }
}
