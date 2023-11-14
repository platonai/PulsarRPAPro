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

package ai.platon.exotic.examples.spark;

import com.globalmentor.apache.hadoop.fs.BareLocalFileSystem;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.fs.FileSystem;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.mllib.tree.RandomForest;
import org.apache.spark.mllib.tree.model.RandomForestModel;
import org.apache.spark.mllib.util.MLUtils;
import scala.Tuple2;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class DomRandomForestClassification {

    private final SparkConf sparkConf = new SparkConf()
            .set("spark.master", "local")
            .setAppName("DomRandomForestClassification");
    private int numClasses = 7;
    private String dataPath = "data/dom/amazon.dataset.6.labels.txt";
    private String modelPath = "target/tmp/DomRandomForestClassificationModel";

    public void setNumClasses(int numClasses) {
        this.numClasses = numClasses;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }

    public void train() throws IOException {
        try (var sparkContext = new JavaSparkContext(sparkConf)) {
            // Use BareLocalFileSystem, see https://stackoverflow.com/questions/73503205
            sparkContext.hadoopConfiguration()
                    .setClass("fs.file.impl", BareLocalFileSystem.class, FileSystem.class);

            // Load and parse the data file.
            var data = MLUtils.loadLibSVMFile(sparkContext.sc(), dataPath).toJavaRDD();
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
            FileUtils.deleteDirectory(new File(modelPath));
            model.save(sparkContext.sc(), modelPath);

            sparkContext.stop();
        }
    }

    public void predict() {
        try (var jsc = new JavaSparkContext(sparkConf)) {
            var sc = jsc.sc();
            // Use BareLocalFileSystem, see https://stackoverflow.com/questions/73503205
            sc.hadoopConfiguration()
                    .setClass("fs.file.impl", BareLocalFileSystem.class, FileSystem.class);
            var sameModel = RandomForestModel.load(sc, modelPath);
            var data = MLUtils.loadLibSVMFile(sc, dataPath).toJavaRDD();
            data.take(20).forEach(point -> {
                var pred = sameModel.predict(point.features());
                System.out.println(pred + " | " + point);
            });
        }
    }

    public static void main(String[] args) throws IOException {
        var classifier = new DomRandomForestClassification();
        classifier.train();
        classifier.predict();
    }
}
