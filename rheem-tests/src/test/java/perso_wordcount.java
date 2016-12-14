/**
 * Created by FLVBSLV on 12/4/2016.
 */

//package com.github.sekruse.wordcount.java;
/*import java.util.Collection;

import org.qcri.rheem.basic.data.Tuple2;
import org.qcri.rheem.core.api.Configuration;
import org.qcri.rheem.core.plugin.Plugin;

import com.github.sekruse.wordcount.java.WordCount;
*/

import org.junit.Test;
import org.qcri.rheem.api.JavaPlanBuilder;
import org.qcri.rheem.basic.data.Tuple2;
import org.qcri.rheem.core.api.Configuration;
import org.qcri.rheem.core.api.RheemContext;
import org.qcri.rheem.core.optimizer.ProbabilisticDoubleInterval;
import org.qcri.rheem.core.optimizer.cardinality.DefaultCardinalityEstimator;
import org.qcri.rheem.core.plugin.Plugin;
import org.qcri.rheem.java.Java;
import org.qcri.rheem.spark.Spark;
import java.util.Collection;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;


public class perso_wordcount  {

    @Test
    public void testPerso() throws MalformedURLException {

        String inputPath = "C:\\Users\\FLVBSLV\\Documents\\hello_world.txt";

        URL inputUrl = new File(inputPath).toURI().toURL();

        //System.out.println(inputPath);
        //System.out.println(inputPath);

        //String workingDirectory = System.getProperty("user.dir");
        //String absolutePath=workingDirectory + File.separator +"hello.txt";

        // Get a plan builder.
        RheemContext rheemContext = new RheemContext(new Configuration())
                .withPlugin(Java.basicPlugin())
                .withPlugin(Spark.basicPlugin());

        JavaPlanBuilder planBuilder = new JavaPlanBuilder(rheemContext)
                .withJobName(String.format("WordCount (%s)", inputUrl))
                .withUdfJarOf(new JavaPlanBuilder(rheemContext).getClass());

        Collection<String> data = new ArrayList();
        //data.add("hello world");
        //data.add("bye world");
        // Start building the RheemPlan.
        Collection<Tuple2<String, Integer>> wordcounts = planBuilder
                // Read the text file.
                //.loadCollection(data).withName("myData")
                .readTextFile(inputUrl.toString()).withName("Load file")
                .flatMap(line -> Arrays.asList(line.split("\\W+")))
                // Split each line by non-word characters.
                .flatMap(line -> Arrays.asList(line.split(" ")))
                .withSelectivity(10, 100, 0.9)
                .withName("Split words")

                // Filter empty tokens.
                .filter(token -> !token.isEmpty())
                .withSelectivity(0.99, 0.99, 0.99)
                .withName("Filter empty words")

                // Attach counter to each word.
                .map(word -> new Tuple2<>(word.toLowerCase(), 1)).withName("To lower case, add counter")

                // Sum up counters for every word.
                .reduceByKey(
                        Tuple2::getField0,
                        (t1, t2) -> new Tuple2<>(t1.getField0(), t1.getField1() + t2.getField1())
                )
                .withCardinalityEstimator(new DefaultCardinalityEstimator(0.9, 1, false, in -> Math.round(0.01 * in[0])))
                .withName("Add counters")

                // Execute the plan and collect the results.
                .collect();
            System.out.print(Arrays.toString(wordcounts.toArray()));
    }
    public perso_wordcount() throws MalformedURLException {
    }

			//System.out.println(Arrays.toString(wordcounts.toArray()));
		/*
		*/
    /**
     * {@link Configuration} to control Rheem.
     * {@link Plugin}s that Rheem will use.
     */
		/*String configurationFileUrl="C:/Users/FLVBSLV/Documents/rheem-examples-master/wordcount/src/main/resources/rheem.properties";

		Configuration configuration = new Configuration(configurationFileUrl);
		//Plugin plugins = new P();


		WordCount wordCount = new WordCount(configuration, null);
		Collection<Tuple2<String, Integer>> output=wordCount.execute(inputUrl);*/

}
