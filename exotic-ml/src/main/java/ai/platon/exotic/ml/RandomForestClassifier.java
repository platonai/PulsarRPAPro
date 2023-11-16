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

package ai.platon.exotic.ml;

import com.globalmentor.apache.hadoop.fs.BareLocalFileSystem;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.hadoop.fs.FileSystem;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.ml.linalg.Vectors;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.tree.RandomForest;
import org.apache.spark.mllib.tree.model.RandomForestModel;
import org.apache.spark.mllib.util.MLUtils;
import scala.Tuple2;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class RandomForestClassifier implements AutoCloseable {

    private int numClasses = 7;
    private Path datasetPath = Paths.get("data/dom/amazon.dataset.6.labels.txt");
    private Path modelPath = Paths.get("target/tmp/DomRandomForestClassificationModel");

    private RandomForestModel loadedModel = null;
    private SparkConf sparkConf;
    private JavaSparkContext javaSparkContext;
    private SparkContext sparkContext;

    public RandomForestClassifier() {
        numClasses = 7;
        datasetPath = Paths.get("data/dom/amazon.dataset.6.labels.txt");
        modelPath = SystemUtils.getJavaIoTmpDir().toPath().resolve("pulsar/ml/RandomForestClassifier");
    }

    public RandomForestClassifier(int numClasses, Path datasetPath) {
        this.numClasses = numClasses;
        this.datasetPath = datasetPath;
        modelPath = SystemUtils.getJavaIoTmpDir().toPath().resolve("pulsar/ml/RandomForestClassifier");
    }

    public RandomForestClassifier(int numClasses, Path datasetPath, Path modelPath) {
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
                .setAppName("DomRandomForestClassification");
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

        var model = RandomForest.trainClassifier(trainingData, numClasses,
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

    public double predict(double[] features) {
        return predict((Vector) Vectors.dense(features));
    }

    public double predict(Vector features) {
        initialize();

        if (loadedModel == null) {
            loadedModel = RandomForestModel.load(sparkContext, modelPath.toString());
        }

        return loadedModel.predict(features);
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
        sparkContext.stop();
    }

    public static void main(String[] args) throws IOException {
        try (var classifier = new RandomForestClassifier()) {
            classifier.train();
            classifier.predict();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
