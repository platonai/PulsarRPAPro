package ai.platon.exotic.ml.spark;

import org.apache.spark.mllib.linalg.Vector;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Map;

public class ExoticMLUtils {

    /**
     * Creates a sparse vector from a record in string format.
     *
     * @param size   vector size.
     * @param record record in libSVM format
     */
    public static Map.Entry<Double, Vector> parseLibSVMRecord(String record, int size) {
        var SPACE = " ";
        var items = record.split(SPACE);
        var numFeatures = items.length - 1;
        var label = Double.parseDouble(items[0]);
        var indices = new int[numFeatures];
        var values = new double[numFeatures];

        final int[] i = {0};
        Arrays.stream(items).skip(1)
                .map(f -> f.trim().split(":"))
                .forEach(parts -> {
                    indices[i[0]] = Integer.parseInt(parts[0]) - 1; // Convert 1-based indices to 0-based.
                    values[i[0]] = Double.parseDouble(parts[1]);
                    ++i[0];
                });

        var vector = org.apache.spark.mllib.linalg.Vectors.sparse(size, indices, values);
        return Map.entry(label, vector);
    }

    /**
     * Encode to libsvm record,
     *
     * @param features the feature vector
     * @param label    the label, negative value means not specified
     */
    public static StringBuilder encodeToLibSVMRecord(double[] features, int label) {
        var space = ' ';
        var record = new StringBuilder();
        record.append(label).append(space);
        for (var i = 0; i < features.length; ++i) {
            var value = features[i];
            if (value != 0.0) {
                record.append(space);
                // i + 1 to convert zero-based index to 1-based index
                record.append(i + 1).append(':').append(value);
            }
        }
        return record;
    }
}
