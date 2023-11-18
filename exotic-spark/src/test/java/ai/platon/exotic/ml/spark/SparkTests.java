package ai.platon.exotic.ml.spark;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SparkTests {
    private int numClasses = 7;
    private Path datasetPath = Paths.get("../data/dom/amazon.dataset.6.labels.txt");
    private RandomForest classifier = new RandomForest(numClasses, datasetPath);

    @Test
    public void testTrain() throws IOException {
        var modelPath = Files.createTempDirectory("pulsar-ml");
        Files.createDirectories(modelPath);

        classifier.train();
        classifier.predict();

        FileUtils.deleteDirectory(modelPath.toFile());
    }

    @Test
    public void testParseLibSVMRecord() {
        var record = "1 1:523.7 2:636.7 3:23.6 4:17.3 5:5 11:15 12:4559 33:5 34:5 35:5 64:23.6 65:17.3 70:523.7 71:636.7 72:23.6 73:17.3 74:5 75:1 78:3 80:14 81:4558 82:409.21 90:523.7 91:636.7 95:1 102:5 103:5 104:5 105:1 109:4558 122:23.6 124:17.3 133:23.6 134:17.3 139:523.7 140:636.7 141:119.6 142:17.3 149:14 150:4557 208:523.7 209:636.7 210:119.6 211:17.3 218:14 219:4560";
        var point = ExoticMLUtils.parseLibSVMRecord(record, 261);
        var vector = point.getValue();
        System.out.println(point.getValue().toString());
        assertEquals(261, vector.size());
        var arr = vector.toArray();
        assertEquals(523.7, vector.toArray()[0]);
    }
}
