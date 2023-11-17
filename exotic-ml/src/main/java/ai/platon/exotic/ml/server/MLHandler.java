package ai.platon.exotic.ml.server;

import ai.platon.exotic.ml.RandomForestClassifier;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MLHandler implements HttpHandler {

    private RandomForestClassifier classifier;

    public void setClassifier(RandomForestClassifier classifier) {
        this.classifier = classifier;
    }

    public MLHandler() {

    }

    @Override
    public void handle(HttpExchange he) throws IOException {
        var headers = he.getRequestHeaders();
        Set<Map.Entry<String, List<String>>> entries = headers.entrySet();
        StringBuilder response = new StringBuilder();
//        for (Map.Entry<String, List<String>> entry : entries) {
//            response.append(entry.toString()).append("\n");
//        }

        try(var isr = new InputStreamReader(he.getRequestBody());
            BufferedReader reader = new BufferedReader(isr)
        ) {
            reader.lines().map(this::predict).forEach(result -> response.append(result).append('\n'));
        }

        he.sendResponseHeaders(200, response.length());
        OutputStream os = he.getResponseBody();

        os.write(response.toString().getBytes());
        os.close();
    }

    private String predict(String features) {
        if (classifier != null) {
            var vector = Arrays.stream(features.split(" "))
                    .mapToDouble(NumberUtils::toDouble)
                    .toArray();
            var result = classifier.predict(vector);
            return result + " " + features;
        }

        // negative predict result means an error
        return "-1 " + features;
    }
}
