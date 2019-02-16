package org.qcri.rheem.core.optimizer.mloptimizer;

import org.qcri.rheem.core.api.Configuration;
import org.qcri.rheem.core.api.exception.RheemException;
import org.qcri.rheem.core.optimizer.mloptimizer.api.Tuple2;
import org.qcri.rheem.core.util.Tuple;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * Compute feature vectors estimates
 */

public class MLestimation {

    private static Configuration configuration = new Configuration();

    public static Tuple2<double[],Double> getBestVector(List<double[]> featureVectors){

        // reinitialize the estimates
        // Load output estimates
        List<Double>  estimates = retreiveEstimates();

        // pick best (minimum) estimate
        int minIndex;

        final ListIterator<Double> itr = estimates.listIterator();
        Double min = itr.next(); // first element as the current minimum
        minIndex = itr.previousIndex();
        while (itr.hasNext()) {
            final Double curr = itr.next();
            if (curr.compareTo(min) < 0) {
                min = curr;
                minIndex = itr.previousIndex();
            }
        }
        Tuple2 result = new Tuple2();
        try{
            result = new Tuple2(featureVectors.get(minIndex),min);
        } catch (Exception e){
            e.getStackTrace();
        }
        return result;
    }

    // Load estimated times
    public static List<Double> retreiveEstimates() {

        List<Double>  estimates = new ArrayList<>() ;
        // The name of the file to open.
        String fileName = configuration.getStringProperty("rheem.core.optimizer.mloptimizer.loadEstimatesLocation");

        // This will reference one line at a time
        String line = null;

        try {
            // FileReader reads text files in the default encoding.
            FileReader fileReader =
                    new FileReader(fileName);

            // Always wrap FileReader in BufferedReader.
            BufferedReader bufferedReader =
                    new BufferedReader(fileReader);

            while((line = bufferedReader.readLine()) != null) {
                estimates.add(Double.valueOf(line));
                //System.out.println(line);
                //System.out.println(line);
            }

            // Always close files.
            bufferedReader.close();
        }
        catch(FileNotFoundException ex) {
            throw new RheemException("could not find estimation ML model output!");
        }
        catch(IOException ex) {
            throw new RheemException("could not read estimates from loaded ML model!");
            // Or we could just do this:
            // ex.printStackTrace();
        }
        return estimates;
    }
}
