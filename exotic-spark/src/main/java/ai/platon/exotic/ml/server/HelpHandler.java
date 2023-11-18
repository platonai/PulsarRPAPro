package ai.platon.exotic.ml.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HelpHandler implements HttpHandler {

    public HelpHandler() {

    }

    @Override
    public void handle(HttpExchange he) throws IOException {
        String response = "Welcome to the predict server!";

        he.sendResponseHeaders(200, response.length());

        try (OutputStream os = he.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
}
