package ai.platon.exotic.ml.server;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MLServer {
    public static int ML_SERVER_PORT = 8382;

    public MLServer() {}

    public void start() throws IOException {
        Executor threadPoolExecutor = Executors.newFixedThreadPool(3);
        HttpServer server = HttpServer.create(new InetSocketAddress(ML_SERVER_PORT), 0);
        server.createContext("/api/ml/predict", new MLHandler());
        server.createContext("/", new HelpHandler());
        server.setExecutor(threadPoolExecutor);
        server.start();
    }

    public static void main(String[] args) throws IOException {
        new MLServer().start();
    }
}