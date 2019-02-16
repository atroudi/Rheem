package org.qcri.rheem.profiler.spark;

import de.hpi.isg.profiledb.instrumentation.StopWatch;
import de.hpi.isg.profiledb.store.model.Experiment;
import de.hpi.isg.profiledb.store.model.Subject;
import de.hpi.isg.profiledb.store.model.TimeMeasurement;
import org.qcri.rheem.core.api.Configuration;
import org.qcri.rheem.core.util.RheemArrays;
import org.qcri.rheem.core.util.RheemCollections;
import org.qcri.rheem.core.optimizer.mloptimizer.api.OperatorProfiler;
import org.qcri.rheem.profiler.generators.DataGenerators;
import org.qcri.rheem.spark.platform.SparkPlatform;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Starts a profiling run of Spark.
 */
public class SparkProfilerRunner implements Serializable {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.printf("Usage: java %s <executionOperator to profile> [<cardinality n>[,<cardinality n>]*]+ \n", SparkProfilerRunner.class);
            System.exit(1);
        }

        // Read the input Operators, Cardinalities and DataQuantaSizes
        String inputOperator = args[0];
        List<Long> cardinalityList = Arrays.stream(args[1].split(",")).map(Long::valueOf).collect(Collectors.toList());
        List<Integer> dataQuantas = Arrays.stream(args[2].split(",")).map(Integer::valueOf).collect(Collectors.toList());
        List<Integer> UdfsComplexity = Arrays.stream(args[3].split(",")).map(Integer::valueOf).collect(Collectors.toList());
        List<Integer> inputRatio;
        if (args.length == 5) {
            inputRatio = Arrays.stream(args[4].split(",")).map(Integer::valueOf).collect(Collectors.toList());
        } else {
            inputRatio = new ArrayList<>(Arrays.asList(10));
        }


        List<String> operators = new ArrayList<>();

        // Profile all operators
        if (Objects.equals(inputOperator, "all"))
            operators = new ArrayList<String>(Arrays.asList("textsource","collectionsource","map","filter","flatmap","reduce","globalreduce","distinct","distinct-string",
                    "distinct-integer","sort","sort-string","sort-integer","count","groupby","join","union","cartesian","callbacksink","collect",
                    "word-count-split","word-count-canonicalize","word-count-count"));
        else {
            assert operators != null;
            operators.add(inputOperator);
        }

        //String executionOperator = args[0];
        /*List<List<Long>> allCardinalities2 = new LinkedList<>();
        for (int i = 1; i < args.length; i++) {
            List<Long> cardinalities = Arrays.stream(args[i].split(",")).map(Long::valueOf).collect(Collectors.toList());
            allCardinalities2.add(cardinalities);
        }*/
        //List<SparkOperatorProfiler.Result> results;

        for (String operator:operators) {
            // Initiate the result list
            List<OperatorProfiler.Result> allResults = null;
            // Loop through all cardinalities
            for (Long card : cardinalityList) {
                List<Long> cardinalities = new ArrayList<>(Arrays.asList(card));
                List<List<Long>> allCardinalities = new ArrayList<>(Arrays.asList(cardinalities));
                // Loop with different dataQuatas size
                for (int UdfComplexity : UdfsComplexity) {
                    // Loop with different UDFs
                    for (int dataQuata : dataQuantas) {
                        List<OperatorProfiler.Result> results;
                        System.out.println();
                        System.out.println("*****************************************************");
                        System.out.println("Starting profiling of " + operator + " executionOperator: ");

                        switch (operator) {
                            case "textsource":
                                results = profile(SparkOperatorProfilers.createSparkTextFileSourceProfiler(dataQuata), allCardinalities);
                                break;
                            case "collectionsource":
                                results = profile(SparkOperatorProfilers.createSparkCollectionSourceProfiler(dataQuata), allCardinalities);
                                break;
                            case "map":
                                results = profile(SparkOperatorProfilers.createSparkMapProfiler(dataQuata, UdfComplexity), allCardinalities);
                                break;
                            case "filter":
                                results = profile(SparkOperatorProfilers.createSparkFilterProfiler(dataQuata, UdfComplexity), allCardinalities);
                                break;
                            case "flatmap":
                                results = profile(SparkOperatorProfilers.createSparkFlatMapProfiler(dataQuata, UdfComplexity), allCardinalities);
                                break;
                            case "reduce":
                                results = profile(SparkOperatorProfilers.createSparkReduceByProfiler(dataQuata, UdfComplexity), allCardinalities);
                                break;
                            case "globalreduce":
                                results = profile(SparkOperatorProfilers.createSparkGlobalReduceProfiler(dataQuata, UdfComplexity), allCardinalities);
                                break;
                            case "distinct":
                            case "distinct-string":
                                results = profile(SparkOperatorProfilers.createSparkDistinctProfiler(dataQuata), allCardinalities);
                                break;
                            case "distinct-integer":
                                results = profile(SparkOperatorProfilers.createSparkDistinctProfiler(
                                        DataGenerators.createReservoirBasedIntegerSupplier(new ArrayList<>(), 0.7d, new Random(42)),
                                        Integer.class,
                                        new Configuration()
                                ), allCardinalities);
                                break;
                            case "sort":
                            case "sort-string":
                                results = profile(SparkOperatorProfilers.createSparkSortProfiler(dataQuata), allCardinalities);
                                break;
                            case "sort-integer":
                                results = profile(SparkOperatorProfilers.createSparkSortProfiler(
                                        DataGenerators.createReservoirBasedIntegerSupplier(new ArrayList<>(), 0.7d, new Random(42)),
                                        Integer.class,
                                        new Configuration()
                                ), allCardinalities);
                                break;
                            case "count":
                                results = profile(SparkOperatorProfilers.createSparkCountProfiler(dataQuata), allCardinalities);
                                break;
                            case "groupby":
                                results = profile(SparkOperatorProfilers.createSparkMaterializedGroupByProfiler(dataQuata, UdfComplexity), allCardinalities);
                                break;
                            case "join":
                                //allCardinalities.get(0).add(allCardinalities.get(0).get(0)/
                                allCardinalities.add(Arrays.asList(allCardinalities.get(0).get(0)/inputRatio.get(0)));
                                results = profile(SparkOperatorProfilers.createSparkJoinProfiler(dataQuata, UdfComplexity), allCardinalities);
                                break;
                            case "union":
                                allCardinalities.add(Arrays.asList(allCardinalities.get(0).get(0)/inputRatio.get(0)));
                                results = profile(SparkOperatorProfilers.createSparkUnionProfiler(dataQuata), allCardinalities);
                                break;
                            case "cartesian":
                                allCardinalities.add(Arrays.asList(allCardinalities.get(0).get(0)/inputRatio.get(0)));
                                results = profile(SparkOperatorProfilers.createSparkCartesianProfiler(dataQuata), allCardinalities);
                                break;
                            case "callbacksink":
                                results = profile(SparkOperatorProfilers.createSparkLocalCallbackSinkProfiler(dataQuata), allCardinalities);
                                break;
//            case "word-count-split": {
//                final Supplier<String> randomStringSupplier = DataGenerators.createRandomStringSupplier(2, 10, new Random(42));
//                results = profile(
//                        org.qcri.rheem.profiler.java.SparkOperatorProfilers.createJavaFlatMapProfiler(
//                                () -> String.format("%s %s %s %s %s %s %s %s %s",
//                                        randomStringSupplier.get(), randomStringSupplier.get(),
//                                        randomStringSupplier.get(), randomStringSupplier.get(),
//                                        randomStringSupplier.get(), randomStringSupplier.get(),
//                                        randomStringSupplier.get(), randomStringSupplier.get(),
//                                        randomStringSupplier.get()),
//                                str -> Arrays.asList(str.split(" ")),
//                                String.class,
//                                String.class
//                        ),
//                        cardinalities);
//                break;
//            }
//            case "word-count-canonicalize": {
//                final Supplier<String> randomStringSupplier = DataGenerators.createRandomStringSupplier(2, 10, new Random(42));
//                results = profile(
//                        org.qcri.rheem.profiler.java.SparkOperatorProfilers.createJavaMapProfiler(
//                                randomStringSupplier,
//                                word -> new Tuple2<>(word.toLowerCase(), 1),
//                                String.class,
//                                Tuple2.class
//                        ),
//                        cardinalities
//                );
//                break;
//            }
//            case "word-count-count": {
//                final Supplier<String> stringSupplier = DataGenerators.createReservoirBasedStringSupplier(new ArrayList<>(), 0.7, new Random(42), 2, 10);
//                results = profile(
//                        org.qcri.rheem.profiler.java.SparkOperatorProfilers.createJavaReduceByProfiler(
//                                () -> new Tuple2<>(stringSupplier.get(), 1),
//                                pair -> pair.field0,
//                                (p1, p2) -> {
//                                    p1.field1 += p2.field1;
//                                    return p1;
//                                },
//                                cast(Tuple2.class),
//                                String.class
//                        ),
//                        cardinalities
//                );
//                break;
//            }
                            default:
                                System.out.println("Unknown executionOperator: " + operator);
                                return;
                        }
                        results.stream().forEach(result->result.setUdfComplexity(UdfComplexity));
                        results.stream().forEach(result->result.setDataQuantaSize(dataQuata));
                        // Collect all profiling results
                        if (allResults == null){
                            allResults=results;
                        }else{
                            for (OperatorProfiler.Result el:results)
                                allResults.add(el);
                        }

                        System.out.println("# Intermidiate results");
                        System.out.println(RheemCollections.getAny(allResults).getCsvHeader());
                        allResults.forEach(result -> System.out.println(result.toCsvString()));

                    }
                }
            }

            System.out.println();
            System.out.println(RheemCollections.getAny(allResults).getCsvHeader());
            allResults.forEach(result -> System.out.println(result.toCsvString()));
        }
    }

    private static StopWatch createStopWatch() {
        Experiment experiment = new Experiment("rheem-profiler", new Subject("Rheem", "0.1"));
        return new StopWatch(experiment);
    }

    /**
     * Run the {@code opProfiler} with all combinations that can be derived from {@code allCardinalities}.
     */
    private static List<OperatorProfiler.Result> profile(OperatorProfiler opProfiler,
                                                              List<List<Long>> allCardinalities) {

        return StreamSupport.stream(RheemCollections.streamedCrossProduct(allCardinalities).spliterator(), false)
                .map(cardinalities -> profile(opProfiler, RheemArrays.toArray(cardinalities)))
                .collect(Collectors.toList());


    }

    /**
     * Run the {@code opProfiler} with the given {@code cardinalities}.
     */
    private static OperatorProfiler.Result profile(OperatorProfiler opProfiler, long... cardinalities) {
        System.out.printf("Profiling %s with %s data quanta.\n", opProfiler, RheemArrays.asList(cardinalities));
        final StopWatch stopWatch = createStopWatch();
        OperatorProfiler.Result result = null;
        OperatorProfiler.Result averageResult = null;
        List<OperatorProfiler.Result> allResult = new ArrayList<>();

        try {
            // Execute 3 runs
            for(int i=1;i<=3;i++){
                System.out.println("Prepare Run"+i+"...");
                final TimeMeasurement preparation = stopWatch.start("Preparation");
                SparkPlatform.getInstance().warmUp(new Configuration());
                opProfiler.prepare(1,cardinalities);
                preparation.stop();


            // Execute 3 runs
            //for(int i=1;i<=3;i++){
                System.out.println("Execute Run"+i+"...");
                final TimeMeasurement execution = stopWatch.start("Execution");
                result = opProfiler.run();
                allResult.add(result);
                execution.stop();

                System.out.println("Measurement Run "+i+":");
                if (result != null) System.out.println(result);
                System.out.println(stopWatch.toPrettyString());
                System.out.println();
            }
            averageResult = SparkOperatorProfiler.averageResult(allResult);
        } finally {
            System.out.println("Clean up...");
            final TimeMeasurement cleanUp = stopWatch.start("Clean up");
            opProfiler.cleanUp();
            cleanUp.stop();

            System.out.println("Average Measurement:");
            if (result != null) System.out.println(averageResult);
            System.out.println(stopWatch.toPrettyString());
            System.out.println();
        }


        return averageResult;
    }

}
