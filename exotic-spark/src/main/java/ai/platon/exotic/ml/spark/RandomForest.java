/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.platon.exotic.ml.spark;

import com.globalmentor.apache.hadoop.fs.BareLocalFileSystem;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.fs.FileSystem;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.tree.model.RandomForestModel;
import org.apache.spark.mllib.util.MLUtils;
import scala.Tuple2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class RandomForest implements AutoCloseable {

    public static Path DEFAULT_MODEL_PATH = Paths.get(System.getProperty("user.home") + "/.pulsar/ml/model/spark/RandomForest");

    private int numClasses;
    private Path datasetPath;
    private Path modelPath;

    private RandomForestModel loadedModel = null;
    private SparkConf sparkConf;
    private JavaSparkContext javaSparkContext;
    private SparkContext sparkContext;

    public RandomForest(int numClasses, Path datasetPath) {
        this(numClasses, datasetPath, DEFAULT_MODEL_PATH);
    }

    public RandomForest(int numClasses, Path datasetPath, Path modelPath) {
        this.numClasses = numClasses;
        this.datasetPath = datasetPath;
        this.modelPath = modelPath;
    }

    public void setNumClasses(int numClasses) {
        this.numClasses = numClasses;
    }

    public void setDatasetPath(Path path) {
        this.datasetPath = path;
    }

    public void setModelPath(Path modelPath) {
        this.modelPath = modelPath;
    }

    public void initialize() {
        if (sparkContext != null) {
            return;
        }

        sparkConf = new SparkConf()
                .set("spark.master", "local")
                .setAppName("RandomForest");
        javaSparkContext = new JavaSparkContext(sparkConf);
        sparkContext = javaSparkContext.sc();

        // Use BareLocalFileSystem, see https://stackoverflow.com/questions/73503205
        javaSparkContext.hadoopConfiguration()
                .setClass("fs.file.impl", BareLocalFileSystem.class, FileSystem.class);
    }

    public void train() throws IOException {
        initialize();

        // Load and parse the data file.
        var data = MLUtils.loadLibSVMFile(sparkContext, datasetPath.toAbsolutePath().toString()).toJavaRDD();
        // Split the data into training and test sets (30% held out for testing)
        var splits = data.randomSplit(new double[]{0.7, 0.3});
        var trainingData = splits[0];
        var testData = splits[1];

        // Train a RandomForest model.
        // Empty categoricalFeaturesInfo indicates all features are continuous.
        Map<Integer, Integer> categoricalFeaturesInfo = new HashMap<>();
        var numTrees = 10;
        var featureSubsetStrategy = "auto"; // Let the algorithm choose.
        var impurity = "gini";
        var maxDepth = 5;
        var maxBins = 32;
        var seed = new Random().nextInt();

        var model = org.apache.spark.mllib.tree.RandomForest.trainClassifier(trainingData, numClasses,
                categoricalFeaturesInfo, numTrees, featureSubsetStrategy, impurity, maxDepth, maxBins,
                seed);

        // Evaluate model on test instances and compute test error
        var predictionAndLabel = testData.mapToPair(p -> new Tuple2<>(model.predict(p.features()), p.label()));
        var testErr =
                predictionAndLabel.filter(pl -> !pl._1().equals(pl._2())).count() / (double) testData.count();
        System.out.println("Test Error: " + testErr);
        System.out.println("Learned classification forest model:\n" + model.toDebugString());

        // Save and load model
        FileUtils.deleteDirectory(modelPath.toFile());
        model.save(sparkContext, modelPath.toAbsolutePath().toString());
    }

    public Map.Entry<Double, Vector> predict(String libsvmRecord, int numFeatures) {
        var record = ExoticMLUtils.parseLibSVMRecord(libsvmRecord, numFeatures);
        return predict(record.getValue());
    }

    public Map.Entry<Double, Vector> predict(double[] vector) {
        return predict(org.apache.spark.mllib.linalg.Vectors.dense(vector));
    }

    public Map.Entry<Double, Vector> predict(Vector vector) {
        initialize();

        if (loadedModel == null) {
            loadedModel = RandomForestModel.load(sparkContext, modelPath.toString());
        }

        var pred = loadedModel.predict(vector);
        return Map.entry(pred, vector);
    }

    public void predict() {
        initialize();

        if (loadedModel == null) {
            loadedModel = RandomForestModel.load(sparkContext, modelPath.toString());
        }

        var data = MLUtils.loadLibSVMFile(sparkContext, datasetPath.toString()).toJavaRDD();
        data.take(20).forEach(point -> {
            var pred = loadedModel.predict(point.features());
            System.out.println(pred + " | " + point);
        });
    }

    @Override
    public void close() throws Exception {
        if (sparkContext != null) {
            sparkContext.stop();
        }
    }

    public static void main(String[] args) throws IOException {
        var numClasses = 7;
//        var datasetPath = Paths.get("data/dom/amazon.dataset.6.labels.txt");
//        var modelPath = Paths.get(System.getProperty("user.home") + "/.pulsar/ml/model/spark/RandomForest");

        var datasetPath = Paths.get("C:\\Users\\pereg\\AppData\\Local\\Temp\\pulsar\\amazon.dataset.libsvm.20M.txt");
        var modelPath = Paths.get(System.getProperty("user.home") + "/.pulsar/ml/model/spark/RandomForest.20M/");

        Files.createDirectories(modelPath);

        try (var classifier = new RandomForest(numClasses, datasetPath, modelPath)) {
            classifier.train();
            classifier.predict();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}