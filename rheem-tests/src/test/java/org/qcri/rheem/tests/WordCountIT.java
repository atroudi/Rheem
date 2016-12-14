package org.qcri.rheem.tests;

import org.junit.Assert;
import org.junit.Test;
import org.qcri.rheem.basic.data.Record;
import org.qcri.rheem.basic.data.Tuple2;
import org.qcri.rheem.basic.operators.*;
import org.qcri.rheem.core.api.Job;
import org.qcri.rheem.core.api.RheemContext;
import org.qcri.rheem.core.function.FlatMapDescriptor;
import org.qcri.rheem.core.function.PredicateDescriptor;
import org.qcri.rheem.core.function.ReduceDescriptor;
import org.qcri.rheem.core.function.TransformationDescriptor;
import org.qcri.rheem.core.optimizer.costs.DefaultLoadEstimator;
import org.qcri.rheem.core.optimizer.costs.LoadEstimator;
import org.qcri.rheem.core.plan.rheemplan.RheemPlan;
import org.qcri.rheem.core.platform.Platform;
import org.qcri.rheem.core.types.BasicDataUnitType;
import org.qcri.rheem.core.types.DataSetType;
import org.qcri.rheem.core.types.DataUnitType;
import org.qcri.rheem.core.util.Counter;
import org.qcri.rheem.core.util.ReflectionUtils;
import org.qcri.rheem.core.util.Tuple;
import org.qcri.rheem.java.Java;
import org.qcri.rheem.java.operators.JavaLocalCallbackSink;
import org.qcri.rheem.java.operators.JavaReduceByOperator;
import org.qcri.rheem.spark.Spark;
import org.qcri.rheem.spark.operators.SparkFlatMapOperator;
import org.qcri.rheem.spark.operators.SparkMapOperator;
import org.qcri.rheem.spark.operators.SparkTextFileSource;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Word count integration test. Besides going through different {@link Platform} combinations, each test addresses a different
 * way of specifying the target {@link Platform}s.
 */
public class WordCountIT {

    @Test
    public void testOnJava() throws URISyntaxException, IOException {
        // Assignment mode: RheemContext.

        // Instantiate Rheem and activate the backend.
        RheemContext rheemContext = new RheemContext().with(Java.basicPlugin());

        TextFileSource textFileSource = new TextFileSource(RheemPlans.FILE_SOME_LINES_TXT.toString());


        // for each line (input) output an iterator of the words
        FlatMapOperator<String, String> flatMapOperator = new FlatMapOperator<>(
                new FlatMapDescriptor<>(line -> Arrays.asList((String[]) line.split(" ")),
                        String.class,
                        String.class
                )
        );
        flatMapOperator.getFunctionDescriptor().setLoadEstimators(
                new DefaultLoadEstimator(1, 1, 0.9d, (inCards, outCards) -> inCards[0] * 670),
                LoadEstimator.createFallback(1, 1)
        );


        // for each word transform it to lowercase and output a key-value pair (word, 1)
        MapOperator<String, Tuple2<String, Integer>> mapOperator = new MapOperator<>(
                new TransformationDescriptor<>(word -> new Tuple2<String, Integer>(word.toLowerCase(), 1),
                        DataUnitType.<String>createBasic(String.class),
                        DataUnitType.<Tuple2<String, Integer>>createBasicUnchecked(Tuple2.class)
                ), DataSetType.createDefault(String.class),
                DataSetType.createDefaultUnchecked(Tuple2.class)
        );
        mapOperator.getFunctionDescriptor().setLoadEstimators(
                new DefaultLoadEstimator(1, 1, 0.9d, (inCards, outCards) -> inCards[0] * 245),
                LoadEstimator.createFallback(1, 1)
        );


        // groupby the key (word) and add up the values (frequency)
        ReduceByOperator<Tuple2<String, Integer>, String> reduceByOperator = new ReduceByOperator<>(
                new TransformationDescriptor<>(pair -> pair.field0,
                        DataUnitType.createBasicUnchecked(Tuple2.class),
                        DataUnitType.createBasic(String.class)), new ReduceDescriptor<>(
                ((a, b) -> {
                    a.field1 += b.field1;
                    return a;
                }), DataUnitType.createGroupedUnchecked(Tuple2.class),
                DataUnitType.createBasicUnchecked(Tuple2.class)
        ), DataSetType.createDefaultUnchecked(Tuple2.class)
        );
        reduceByOperator.getKeyDescriptor().setLoadEstimators(
                new DefaultLoadEstimator(1, 1, 0.9d, (inCards, outCards) -> inCards[0] * 50),
                LoadEstimator.createFallback(1, 1)
        );
        reduceByOperator.getReduceDescriptor().setLoadEstimators(
                new DefaultLoadEstimator(1, 1, 0.9d, (inCards, outCards) -> inCards[0] * 350 + 500000),
                LoadEstimator.createFallback(1, 1)
        );


        // write results to a sink
        List<Tuple2> results = new ArrayList<>();
        LocalCallbackSink<Tuple2> sink = LocalCallbackSink.createCollectingSink(results, DataSetType.createDefault(Tuple2.class));

        // Build Rheem plan by connecting operators
        textFileSource.connectTo(0, flatMapOperator, 0);
        flatMapOperator.connectTo(0, mapOperator, 0);
        mapOperator.connectTo(0, reduceByOperator, 0);
        reduceByOperator.connectTo(0, sink, 0);

        // Have Rheem execute the plan.
        rheemContext.execute(new RheemPlan(sink));

        // Verify the plan result.
        Counter<String> counter = new Counter<>();
        List<Tuple2> correctResults = new ArrayList<>();
        final List<String> lines = Files.lines(Paths.get(RheemPlans.FILE_SOME_LINES_TXT)).collect(Collectors.toList());
        lines.stream()
                .flatMap(line -> Arrays.stream(line.split("\\s+")))
                .map(String::toLowerCase)
                .forEach(counter::increment);

        for (Map.Entry<String, Integer> countEntry : counter) {
            correctResults.add(new Tuple2<>(countEntry.getKey(), countEntry.getValue()));
        }
        Assert.assertTrue(results.size() == correctResults.size() && results.containsAll(correctResults) && correctResults.containsAll(results));
    }

    @Test
    public void testOnSpark() throws URISyntaxException, IOException {
        // Assignment mode: Job.

        int threshold=2;  //filtering threshold

        RheemContext rheemContext = new RheemContext();

        rheemContext.register(Java.basicPlugin());
        rheemContext.register(Spark.basicPlugin());


        String inputTextLocation = "C:\\Users\\FLVBSLV\\Documents\\hello_world.txt";
        String translationTextLocation = "C:\\Users\\FLVBSLV\\Documents\\translation.txt";

        URI inputTestUri = new File(inputTextLocation).toURI();
        URI translationTextUri = new File(translationTextLocation).toURI();

        TextFileSource textFileSource = new TextFileSource( inputTestUri.toString());
        TextFileSource translationText = new TextFileSource(translationTextUri.toString());
        translationText.addTargetPlatform(Spark.platform());

        // for each line (input) output an iterator of the words
        FlatMapOperator<String, String> flatMapOperator = new FlatMapOperator<>(
                new FlatMapDescriptor<>(line -> Arrays.asList((String[]) line.split(" ")),
                        String.class,
                        String.class
                )
        );

        FlatMapOperator<String, String> translationFlatMapOperator = new FlatMapOperator<>(
                new FlatMapDescriptor<>(line -> Arrays.asList((String[]) line.split("\\\\.")),
                        String.class,
                        String.class
                )
        );
        translationFlatMapOperator.addTargetPlatform(Spark.platform());


        // for each word transform it to lowercase and output a key-value pair (word, 1)
        MapOperator<String, Tuple2<String, Integer>> mapOperator = new MapOperator<>(
                new TransformationDescriptor<>(word -> new Tuple2<String, Integer>(word.toLowerCase(), 1),
                        DataUnitType.createBasic(String.class),
                        DataUnitType.createBasicUnchecked(Tuple2.class)
                ), DataSetType.createDefault(String.class),
                DataSetType.createDefaultUnchecked(Tuple2.class)
        );

        MapOperator<String, Tuple2<String, String>> translationMapOperator = new MapOperator<>(
                new TransformationDescriptor<>(word -> {
                    String[] parts = word.split(" ");
                    Tuple2<String, String> result= new Tuple2<>(parts[0],parts[1]);
                    return result;
                },
                        DataUnitType.createBasic(String.class),
                        DataUnitType.createBasicUnchecked(Tuple2.class)
                ), DataSetType.createDefault(String.class),
                DataSetType.createDefaultUnchecked(Tuple2.class)
        );

        translationMapOperator.addTargetPlatform(Spark.platform());
        // groupby the key (word) and add up the values (frequency)
        ReduceByOperator<Tuple2<String, Integer>, String> reduceByOperator = new ReduceByOperator<>(
                new TransformationDescriptor<>(pair -> pair.field0,
                        DataUnitType.createBasicUnchecked(Tuple2.class),
                        DataUnitType.createBasic(String.class)), new ReduceDescriptor<>(
                ((a, b) -> {
                    a.field1 += b.field1;
                    return a;
                }), DataUnitType.createGroupedUnchecked(Tuple2.class),
                DataUnitType.createBasicUnchecked(Tuple2.class)
        ), DataSetType.createDefaultUnchecked(Tuple2.class)
        );

        // Add a filter operator to filter elements with frequency lower than 10

        FilterOperator<Tuple2<String, Integer>> filterOperator = new FilterOperator<Tuple2<String, Integer>>(
                (PredicateDescriptor.SerializablePredicate<Tuple2<String, Integer>>) tuple ->tuple.getField1()>= threshold, ReflectionUtils.specify(Tuple2.class)
        );

        // Add a second filter operator to filter elements with frequency lower than 10

        FilterOperator<Tuple2<String, Integer>> filterOperator2 = new FilterOperator<Tuple2<String, Integer>>(
                (PredicateDescriptor.SerializablePredicate<Tuple2<String, Integer>>) tuple ->tuple.getField1()<= threshold+1, ReflectionUtils.specify(Tuple2.class)
        );

        // Add a first join operator
        JoinOperator<Tuple2<String, Integer>, Tuple2<String, String>, String> firstJoin =
                new JoinOperator<>(
                        Tuple2::getField0,
                        Tuple2::getField0,
                        ReflectionUtils.specify(Tuple2.class),
                        ReflectionUtils.specify(Tuple2.class),
                        String.class
                );

        // Add a join operator
        JoinOperator<Tuple2<String, Integer>, Tuple2<String, String>, String> textJoin =
                new JoinOperator<>(
                        Tuple2::getField0,
                        Tuple2::getField0,
                        ReflectionUtils.specify(Tuple2.class),
                        ReflectionUtils.specify(Tuple2.class),
                        String.class
                );

        // write results to a sink
        List<Tuple2> results = new ArrayList<>();
        LocalCallbackSink<Tuple2> sink = LocalCallbackSink.createCollectingSink(results, DataSetType.createDefault(Tuple2.class));
        LocalCallbackSink<Tuple2> sink2 = LocalCallbackSink.createCollectingSink(results, DataSetType.createDefault(Tuple2.class));


        // Build Rheem plan by connecting operators
        //translation file transformation operators
        translationText.connectTo(0,translationFlatMapOperator,0);
        translationFlatMapOperator.connectTo(0,translationMapOperator,0);
        translationMapOperator.connectTo(0, textJoin, 1);
        //translationMapOperator.connectTo(0, firstJoin, 1);
        //input text transformation operators
        textFileSource.connectTo(0, flatMapOperator, 0);
        flatMapOperator.connectTo(0, mapOperator, 0);
        mapOperator.connectTo(0, reduceByOperator, 0);
        reduceByOperator.connectTo(0, filterOperator2, 0);
        reduceByOperator.connectTo(0,filterOperator,0);
        filterOperator.connectTo(0,textJoin,0);
        filterOperator2.connectTo(0,firstJoin,0);
        filterOperator.connectTo(0,firstJoin,1);

        firstJoin.connectTo(0,sink2,0);
        //filterOperator.connectTo(0,sink,0);
        // connect two transformation paths in a join operator
        textJoin.connectTo(0,sink,0);
        RheemPlan rheemPlan = new RheemPlan(sink,sink2);


        // Have Rheem execute the plan.
        rheemContext.execute(new RheemPlan(sink,sink2));

        // Verify the plan result.
        Counter<String> counter = new Counter<>();
        List<Tuple2> correctResults = new ArrayList<>();
        final List<String> lines = Files.lines(Paths.get(translationTextLocation)).collect(Collectors.toList());
        lines.stream()
                //.flatMap(line -> Arrays.stream(line.split("\\s+")))
                .map(String::toLowerCase)
                .forEach(counter::increment);

        for (Map.Entry<String, Integer> countEntry : counter) {
            correctResults.add(new Tuple2<>(countEntry.getKey(), countEntry.getValue()));
        }
        System.out.print(correctResults.toString());
        List<String> test_lines= new ArrayList();
        test_lines.add("hello world!");
        test_lines.add("bye world!");

        // Print the output sink
        System.out.print('\n');
        System.out.print(results.toString()+'\n');
        // Print the size of the output sink
        System.out.print(results.size());
        System.out.print('\n');

        //Assert.assertTrue(results.size() == correctResults.size() && results.containsAll(correctResults) && correctResults.containsAll(results));

    }

    @Test
    public void testOnSparkToJava() throws URISyntaxException, IOException {
        // Assignment mode: ExecutionOperators.

        // Instantiate Rheem and activate the backend.
        RheemContext rheemContext = new RheemContext();
        rheemContext.register(Spark.basicPlugin());
        rheemContext.register(Java.basicPlugin());

        TextFileSource textFileSource = new SparkTextFileSource(RheemPlans.FILE_SOME_LINES_TXT.toString());

        // for each line (input) output an iterator of the words
        FlatMapOperator<String, String> flatMapOperator = new SparkFlatMapOperator<>(
                DataSetType.createDefault(String.class),
                DataSetType.createDefault(String.class),
                new FlatMapDescriptor<>(line -> Arrays.asList(line.split(" ")),
                        String.class,
                        String.class
                ));


        // for each word transform it to lowercase and output a key-value pair (word, 1)
        MapOperator<String, Tuple2<String, Integer>> mapOperator = new SparkMapOperator<>(
                DataSetType.createDefault(String.class),
                DataSetType.createDefaultUnchecked(Tuple2.class),
                new TransformationDescriptor<>(word -> new Tuple2<>(word.toLowerCase(), 1),
                        DataUnitType.createBasic(String.class),
                        DataUnitType.createBasicUnchecked(Tuple2.class)
                ));


        // groupby the key (word) and add up the values (frequency)
        ReduceByOperator<Tuple2<String, Integer>, String> reduceByOperator = new JavaReduceByOperator<>(
                DataSetType.createDefaultUnchecked(Tuple2.class),
                new TransformationDescriptor<>(pair -> pair.field0,
                        DataUnitType.createBasicUnchecked(Tuple2.class),
                        DataUnitType.createBasic(String.class)),
                new ReduceDescriptor<>(
                        ((a, b) -> {
                            a.field1 += b.field1;
                            return a;
                        }), DataUnitType.createGroupedUnchecked(Tuple2.class),
                        DataUnitType.createBasicUnchecked(Tuple2.class)
                )
        );


        // write results to a sink
        List<Tuple2> results = new ArrayList<>();
        LocalCallbackSink<Tuple2> sink = new JavaLocalCallbackSink<>(results::add, DataSetType.createDefault(Tuple2.class));

        // Build Rheem plan by connecting operators
        textFileSource.connectTo(0, flatMapOperator, 0);
        flatMapOperator.connectTo(0, mapOperator, 0);
        mapOperator.connectTo(0, reduceByOperator, 0);
        reduceByOperator.connectTo(0, sink, 0);
        RheemPlan rheemPlan = new RheemPlan(sink);

        // Have Rheem execute the plan.
        rheemContext.execute(rheemPlan);

        // Verify the plan result.
        Counter<String> counter = new Counter<>();
        List<Tuple2> correctResults = new ArrayList<>();
        final List<String> lines = Files.lines(Paths.get(RheemPlans.FILE_SOME_LINES_TXT)).collect(Collectors.toList());
        lines.stream()
                .flatMap(line -> Arrays.stream(line.split("\\s+")))
                .map(String::toLowerCase)
                .forEach(counter::increment);

        for (Map.Entry<String, Integer> countEntry : counter) {
            correctResults.add(new Tuple2<>(countEntry.getKey(), countEntry.getValue()));
        }
        Assert.assertTrue(results.size() == correctResults.size() && results.containsAll(correctResults) && correctResults.containsAll(results));
    }

    @Test
    public void testOnJavaToSpark() throws URISyntaxException, IOException {
        // Assignment mode: Constraints.

        TextFileSource textFileSource = new TextFileSource(RheemPlans.FILE_SOME_LINES_TXT.toString());
        textFileSource.addTargetPlatform(Java.platform());

        // for each line (input) output an iterator of the words
        FlatMapOperator<String, String> flatMapOperator = new FlatMapOperator<>(
                new FlatMapDescriptor<>(line -> Arrays.asList((String[]) line.split(" ")),
                        String.class,
                        String.class
                )
        );
        flatMapOperator.addTargetPlatform(Java.platform());
        flatMapOperator.setName("Split words");


        // for each word transform it to lowercase and output a key-value pair (word, 1)
        MapOperator<String, Tuple2<String, Integer>> mapOperator = new MapOperator<>(
                new TransformationDescriptor<>(word -> new Tuple2<>(word.toLowerCase(), 1),
                        DataUnitType.createBasic(String.class),
                        DataUnitType.createBasicUnchecked(Tuple2.class)
                ), DataSetType.createDefault(String.class),
                DataSetType.createDefaultUnchecked(Tuple2.class)
        );
        mapOperator.addTargetPlatform(Spark.platform());
        mapOperator.setName("Create counters");


        // groupby the key (word) and add up the values (frequency)
        ReduceByOperator<Tuple2<String, Integer>, String> reduceByOperator = new ReduceByOperator<>(
                new TransformationDescriptor<>(pair -> pair.field0,
                        DataUnitType.createBasicUnchecked(Tuple2.class),
                        DataUnitType.createBasic(String.class)), new ReduceDescriptor<>(
                ((a, b) -> {
                    a.field1 += b.field1;
                    return a;
                }), DataUnitType.createGroupedUnchecked(Tuple2.class),
                DataUnitType.createBasicUnchecked(Tuple2.class)
        ), DataSetType.createDefaultUnchecked(Tuple2.class)
        );
        reduceByOperator.addTargetPlatform(Spark.platform());
        reduceByOperator.setName("Add counters");


        // write results to a sink
        List<Tuple2> results = new ArrayList<>();
        LocalCallbackSink<Tuple2> sink = LocalCallbackSink.createCollectingSink(results, DataSetType.createDefault(Tuple2.class));
        sink.addTargetPlatform(Spark.platform());

        // Build Rheem plan by connecting operators
        textFileSource.connectTo(0, flatMapOperator, 0);
        flatMapOperator.connectTo(0, mapOperator, 0);
        mapOperator.connectTo(0, reduceByOperator, 0);
        reduceByOperator.connectTo(0, sink, 0);
        RheemPlan rheemPlan = new RheemPlan(sink);

        // Have Rheem execute the plan.
        RheemContext rheemContext = new RheemContext();
        rheemContext.register(Java.basicPlugin());
        rheemContext.register(Spark.basicPlugin());
        rheemContext.execute(rheemPlan);

        // Verify the plan result.
        Counter<String> counter = new Counter<>();
        List<Tuple2> correctResults = new ArrayList<>();
        final List<String> lines = Files.lines(Paths.get(RheemPlans.FILE_SOME_LINES_TXT)).collect(Collectors.toList());
        lines.stream()
                .flatMap(line -> Arrays.stream(line.split("\\s+")))
                .map(String::toLowerCase)
                .forEach(counter::increment);

        for (Map.Entry<String, Integer> countEntry : counter) {
            correctResults.add(new Tuple2<>(countEntry.getKey(), countEntry.getValue()));
        }
        Assert.assertEquals(new HashSet<>(correctResults), new HashSet<>(results));
    }

    @Test
    public void testOnJavaAndSpark() throws URISyntaxException, IOException {
        // Assignment mode: none.

        TextFileSource textFileSource = new TextFileSource(RheemPlans.FILE_SOME_LINES_TXT.toString());
        textFileSource.addTargetPlatform(Java.platform());
        textFileSource.addTargetPlatform(Spark.platform());

        // for each line (input) output an iterator of the words
        FlatMapOperator<String, String> flatMapOperator = new FlatMapOperator<>(
                new FlatMapDescriptor<>(line -> Arrays.asList((String[]) line.split(" ")),
                        String.class,
                        String.class
                )
        );


        // for each word transform it to lowercase and output a key-value pair (word, 1)
        MapOperator<String, Tuple2<String, Integer>> mapOperator = new MapOperator<>(
                new TransformationDescriptor<>(word -> new Tuple2<String, Integer>(word.toLowerCase(), 1),
                        DataUnitType.createBasic(String.class),
                        DataUnitType.createBasicUnchecked(Tuple2.class)
                ), DataSetType.createDefault(String.class),
                DataSetType.createDefaultUnchecked(Tuple2.class)
        );


        // groupby the key (word) and add up the values (frequency)
        ReduceByOperator<Tuple2<String, Integer>, String> reduceByOperator = new ReduceByOperator<>(
                new TransformationDescriptor<>(pair -> pair.field0,
                        DataUnitType.createBasicUnchecked(Tuple2.class),
                        DataUnitType.createBasic(String.class)), new ReduceDescriptor<>(
                ((a, b) -> {
                    a.field1 += b.field1;
                    return a;
                }), DataUnitType.createGroupedUnchecked(Tuple2.class),
                DataUnitType.createBasicUnchecked(Tuple2.class)
        ), DataSetType.createDefaultUnchecked(Tuple2.class)
        );


        // write results to a sink
        List<Tuple2> results = new ArrayList<>();
        LocalCallbackSink<Tuple2> sink = LocalCallbackSink.createCollectingSink(results, DataSetType.createDefault(Tuple2.class));

        // Build Rheem plan by connecting operators
        textFileSource.connectTo(0, flatMapOperator, 0);
        flatMapOperator.connectTo(0, mapOperator, 0);
        mapOperator.connectTo(0, reduceByOperator, 0);
        reduceByOperator.connectTo(0, sink, 0);
        RheemPlan rheemPlan = new RheemPlan(sink);

        // Have Rheem execute the plan.
        RheemContext rheemContext = new RheemContext();
        rheemContext.register(Java.basicPlugin());
        rheemContext.register(Spark.basicPlugin());
        rheemContext.execute(rheemPlan);

        // Verify the plan result.
        Counter<String> counter = new Counter<>();
        List<Tuple2> correctResults = new ArrayList<>();
        final List<String> lines = Files.lines(Paths.get(RheemPlans.FILE_SOME_LINES_TXT)).collect(Collectors.toList());
        lines.stream()
                .flatMap(line -> Arrays.stream(line.split("\\s+")))
                .map(String::toLowerCase)
                .forEach(counter::increment);

        for (Map.Entry<String, Integer> countEntry : counter) {
            correctResults.add(new Tuple2<>(countEntry.getKey(), countEntry.getValue()));
        }
        Assert.assertTrue(results.size() == correctResults.size() && results.containsAll(correctResults) && correctResults.containsAll(results));
    }


}
