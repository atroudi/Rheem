package org.qcri.rheem.profiler.java;

import de.hpi.isg.profiledb.instrumentation.StopWatch;
import de.hpi.isg.profiledb.store.model.Experiment;
import de.hpi.isg.profiledb.store.model.Subject;
import de.hpi.isg.profiledb.store.model.TimeMeasurement;
import org.qcri.rheem.basic.data.Tuple2;
import org.qcri.rheem.core.optimizer.costs.LoadProfileEstimator;
import org.qcri.rheem.core.util.RheemCollections;
import org.qcri.rheem.java.operators.JavaExecutionOperator;
import org.qcri.rheem.core.optimizer.mloptimizer.api.OperatorProfiler;
import org.qcri.rheem.profiler.generators.DataGenerators;
import org.qcri.rheem.profiler.util.ProfilingUtils;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Utility to support finding reasonable {@link LoadProfileEstimator}s for {@link JavaExecutionOperator}s.
 */
public class JavaProfilerRunner {

    private static final int GC_RUNS = 1;

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.printf("Usage: java %s <executionOperator to profile> <cardinality>[,<cardinality>] <dataQuantaSize>[,<dataQuantaSize>]\n", JavaProfilerRunner.class);
            System.exit(1);
        }

        // Read the input Operators, Cardinalities and DataQuantaSizes
        String inputOperator = args[0];
        List<Integer> cardinalityList = Arrays.stream(args[1].split(",")).map(Integer::valueOf).collect(Collectors.toList());
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

        // Loop through all operators
        for (String operator:operators) {
            // Initiate the result list
            List<OperatorProfiler.Result> allResults = null;
            // Loop through all cardinalities
            for (int card : cardinalityList){
                List<Integer> cardinalities = new ArrayList<>(Arrays.asList(card));
                // Loop with different dataQuatas size
                for (int UdfComplexity : UdfsComplexity) {
                    // Loop with different UDFs
                    for (int dataQuata : dataQuantas){
                        List<OperatorProfiler.Result> results ;
                        System.out.println();
                        System.out.println("*****************************************************");
                        System.out.println("Starting profiling of " + operator + " executionOperator: ");
                        switch (operator) {
                            case "textsource":
                                results = profile(JavaOperatorProfilers.createJavaTextFileSourceProfiler(dataQuata), cardinalities, dataQuata);
                                break;
                            case "collectionsource":
                                results = profile(JavaOperatorProfilers.createJavaCollectionSourceProfiler(dataQuata), cardinalities, dataQuata);
                                break;
                            case "map":
                                results = profile(JavaOperatorProfilers.createJavaMapProfiler(dataQuata, UdfComplexity), cardinalities, dataQuata);
                                break;
                            case "filter":
                                results = profile(JavaOperatorProfilers.createJavaFilterProfiler(dataQuata, UdfComplexity), cardinalities, dataQuata);
                                break;
                            case "flatmap":
                                results = profile(JavaOperatorProfilers.createJavaFlatMapProfiler(dataQuata, UdfComplexity), cardinalities, dataQuata);
                                break;
                            case "reduce":
                                results = profile(JavaOperatorProfilers.createJavaReduceByProfiler(dataQuata, UdfComplexity), cardinalities, dataQuata);
                                break;
                            case "globalreduce":
                                results = profile(JavaOperatorProfilers.createJavaGlobalReduceProfiler(dataQuata, UdfComplexity), cardinalities, dataQuata);
                                break;
                            case "distinct":
                            case "distinct-string":
                                results = profile(JavaOperatorProfilers.createJavaDistinctProfiler(dataQuata), cardinalities, dataQuata);
                                break;
                            case "distinct-integer":
                                results = profile(JavaOperatorProfilers.createJavaDistinctProfiler(
                                        DataGenerators.createReservoirBasedIntegerListSupplier(new ArrayList<List<Integer>>(),0.0,new Random(),dataQuata),
                                        List.class
                                ), cardinalities, dataQuata);
                                break;
                            case "sort":
                            case "sort-string":
                                results = profile(JavaOperatorProfilers.createJavaSortProfiler(dataQuata,UdfComplexity), cardinalities, dataQuata);
                                break;
                            case "sort-integer":
                                results = profile(JavaOperatorProfilers.createJavaSortProfiler(
                                        DataGenerators.createReservoirBasedIntegerListSupplier(new ArrayList<List<Integer>>(),0.0,new Random(),dataQuata),
                                        List.class
                                ), cardinalities, dataQuata);
                                break;
                            case "count":
                                results = profile(JavaOperatorProfilers.createJavaCountProfiler(dataQuata), cardinalities, dataQuata);
                                break;
                            case "groupby":
                                results = profile(JavaOperatorProfilers.createJavaMaterializedGroupByProfiler(dataQuata, UdfComplexity), cardinalities, dataQuata);
                                break;
                            case "join":
                                results = profile(JavaOperatorProfilers.createJavaJoinProfiler(dataQuata,UdfComplexity), cardinalities, inputRatio, dataQuata);
                                break;
                            case "union":
                                results = profile(JavaOperatorProfilers.createJavaUnionProfiler(dataQuata), cardinalities, inputRatio, dataQuata);
                                break;
                            case "cartesian":
                                results = profile(JavaOperatorProfilers.createJavaCartesianProfiler(dataQuata), cardinalities, inputRatio, dataQuata);
                                break;
                            case "callbacksink":
                                results = profile(JavaOperatorProfilers.createJavaLocalCallbackSinkProfiler(dataQuata), cardinalities, dataQuata);
                                break;
                            case "collect":
                                results = profile(JavaOperatorProfilers.createCollectingJavaLocalCallbackSinkProfiler(dataQuata), cardinalities, dataQuata);
                                break;
                            case "word-count-split": {
                                final Supplier<String> randomStringSupplier = DataGenerators.createRandomStringSupplier(2 + dataQuata, 10 + dataQuata, new Random(42));
                                results = profile(
                                        JavaOperatorProfilers.createJavaFlatMapProfiler(
                                                () -> String.format("%s %s %s %s %s %s %s %s %s",
                                                        randomStringSupplier.get(), randomStringSupplier.get(),
                                                        randomStringSupplier.get(), randomStringSupplier.get(),
                                                        randomStringSupplier.get(), randomStringSupplier.get(),
                                                        randomStringSupplier.get(), randomStringSupplier.get(),
                                                        randomStringSupplier.get()),
                                                str -> Arrays.asList(str.split(" ")),
                                                String.class,
                                                String.class
                                        ),
                                        cardinalities, dataQuata);
                                break;
                            }
                            case "word-count-canonicalize": {
                                final Supplier<String> randomStringSupplier = DataGenerators.createRandomStringSupplier(2 + dataQuata, 10 + dataQuata, new Random(42));
                                results = profile(
                                        JavaOperatorProfilers.createJavaMapProfiler(
                                                randomStringSupplier,
                                                word -> new Tuple2<>(word.toLowerCase(), 1),
                                                String.class,
                                                Tuple2.class
                                        ),
                                        cardinalities,dataQuata
                                );
                                break;
                            }
                            case "word-count-count": {
                                final Supplier<String> stringSupplier = DataGenerators.createReservoirBasedStringSupplier(new ArrayList<>(), 0.7, new Random(42), 2 + dataQuata, 10 + dataQuata);
                                results = profile(
                                        JavaOperatorProfilers.createJavaReduceByProfiler(
                                                () -> new Tuple2<>(stringSupplier.get(), 1),
                                                pair -> pair.field0,
                                                (p1, p2) -> {
                                                    p1.field1 += p2.field1;
                                                    return p1;
                                                },
                                                cast(Tuple2.class),
                                                String.class
                                        ),
                                        cardinalities,dataQuata
                                );
                                break;
                            }
                            default:
                                System.out.println("Unknown executionOperator: " + operator);
                                return;
                        }
                        results.stream().forEach(result->result.setUdfComplexity(UdfComplexity));

                        // Collect all profiling results
                        if (allResults == null){
                            allResults=results;
                        }else{
                            for (OperatorProfiler.Result el:results)
                                allResults.add(el);
                        }
                    }
                }
            }

            System.out.println();
            System.out.println("Profiling results of " + operator + " executionOperator: ");
            System.out.println(RheemCollections.getAny(allResults).getCsvHeader());
            allResults.forEach(result -> System.out.println(result.toCsvString()));
        }
    }

    private static StopWatch createStopWatch() {
        Experiment experiment = new Experiment("rheem-profiler", new Subject("Rheem", "0.1"));
        return new StopWatch(experiment);
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> cast(Class<?> cls) {
        return (Class<T>) cls;
    }

    private static List<OperatorProfiler.Result> profile(JavaUnaryOperatorProfiler unaryProfiler,
                                                         Collection<Integer> cardinalities,
                                                         long dataQuantaSize) {
        return cardinalities.stream()
                .map(cardinality -> {
            // Make a first white run to be sure the cache has change with random generation
            //unaryProfiler.setDataQuantumGenerators( DataGenerators.createReservoirBasedStringSupplier(new ArrayList<>(), 0.01, new Random(), 4 + (int)dataQuantaSize,
            //        10 + (int)dataQuantaSize));
            profile(unaryProfiler, cardinality,dataQuantaSize);

            //unaryProfiler.setDataQuantumGenerators( DataGenerators.createReservoirBasedStringSupplier(new ArrayList<>(), 0.01, new Random(42), 4 + (int)dataQuantaSize,
            //        10 + (int)dataQuantaSize));
            return profile(unaryProfiler, cardinality, dataQuantaSize);
        }).collect(Collectors.toList());
    }


    private static OperatorProfiler.Result profile(JavaUnaryOperatorProfiler unaryProfiler, int cardinality, long dataQuantaSize) {
        System.out.println("Running garbage collector...");
        for (int i = 0; i < GC_RUNS; i++) {
            System.gc();
        }
        ProfilingUtils.sleep(1000);

        System.out.printf("Profiling %s with %d data quanta.\n", unaryProfiler, cardinality);
        final StopWatch stopWatch = createStopWatch();

        System.out.println("Prepare...");
        final TimeMeasurement preparation = stopWatch.start("Preparation");
        unaryProfiler.prepare(dataQuantaSize,cardinality);
        preparation.stop();

        System.out.println("Execute...");
        final TimeMeasurement execution = stopWatch.start("Execution");
        final OperatorProfiler.Result result = unaryProfiler.run();
        execution.stop();

        System.out.println("Measurement:");
        System.out.println(result);
        System.out.println(stopWatch.toPrettyString());
        System.out.println();

        return result;
    }

    private static List<OperatorProfiler.Result> profile(JavaSourceProfiler sourceProfiler, Collection<Integer> cardinalities, long dataQuantaSize) {
        return cardinalities.stream()
                .map(cardinality -> {
                    // Make a first white run to be sure the cache has changed
                    profile(sourceProfiler, cardinality, dataQuantaSize);
                    return profile(sourceProfiler, cardinality, dataQuantaSize);
                })
                .collect(Collectors.toList());
    }

    private static OperatorProfiler.Result profile(JavaSourceProfiler sourceProfiler, int cardinality, long dataQuantaSize) {
        System.out.println("Running garbage collector...");
        for (int i = 0; i < GC_RUNS; i++) {
            System.gc();
        }
        ProfilingUtils.sleep(1000);

        System.out.printf("Profiling %s with %d data quanta.\n", sourceProfiler, cardinality);
        final StopWatch stopWatch = createStopWatch();

        System.out.println("Prepare...");
        final TimeMeasurement preparation = stopWatch.start("Preparation");
        sourceProfiler.prepare(dataQuantaSize, cardinality);
        preparation.stop();

        System.out.println("Execute...");
        final TimeMeasurement execution = stopWatch.start("Execution");
        final OperatorProfiler.Result result = sourceProfiler.run();
        execution.stop();

        System.out.println("Measurement:");
        System.out.println(result);
        System.out.println(stopWatch.toPrettyString());
        System.out.println();

        return result;
    }



   /* private static List<OperatorProfiler.Result> profile(UnaryOperatorProfiler unaryOperatorProfiler,
                                                         Collection<Integer> cardinalities,
                                                         long dataQuantaSize) {
        return cardinalities.stream()
                .map(cardinality -> {
                    profile(unaryOperatorProfiler, cardinality, dataQuantaSize);
                    return profile(unaryOperatorProfiler, cardinality, dataQuantaSize);
                })
                .collect(Collectors.toList());
    }

    private static OperatorProfiler.Result profile(UnaryOperatorProfiler unaryOperatorProfiler, int cardinality, long dataQuantaSize) {
        System.out.println("Running garbage collector...");
        for (int i = 0; i < GC_RUNS; i++) {
            System.gc();
        }
        ProfilingUtils.sleep(1000);

        System.out.printf("Profiling %s with %d data quanta.\n", unaryOperatorProfiler, cardinality);
        final StopWatch stopWatch = createStopWatch();

        System.out.println("Prepare...");
        final TimeMeasurement preparation = stopWatch.start("Preparation");
        unaryOperatorProfiler.prepare(dataQuantaSize, cardinality);
        preparation.stop();

        System.out.println("Execute...");
        final TimeMeasurement execution = stopWatch.start("Execution");
        final OperatorProfiler.Result result = unaryOperatorProfiler.run();
        execution.stop();

        System.out.println("Measurement:");
        System.out.println(result);
        System.out.println(stopWatch.toPrettyString());
        System.out.println();

        return result;
    }
*/
    private static List<OperatorProfiler.Result> profile(JavaBinaryOperatorProfiler javaBinaryOperatorProfiler,
                                                         Collection<Integer> cardinalities0,
                                                         Collection<Integer> inputRatio,
                                                         long dataQuantaSize) {
        return cardinalities0.stream()
                .flatMap(cardinality0 ->
                        inputRatio.stream()
                                .map(
                                        ratio -> {
                                            // Make a first white run to be sure the cache has changed
                                            profile(javaBinaryOperatorProfiler, cardinality0, cardinality0/ratio, dataQuantaSize);
                                            return profile(javaBinaryOperatorProfiler, cardinality0, cardinality0/ratio, dataQuantaSize);
                                        }
                                )
                )
                .collect(Collectors.toList());
    }

    private static OperatorProfiler.Result profile(JavaBinaryOperatorProfiler javaBinaryOperatorProfiler,
                                                   int cardinality0,
                                                   int cardinality1,
                                                   long dataQuantaSize) {
        System.out.println("Running garbage collector...");
        for (int i = 0; i < GC_RUNS; i++) {
            System.gc();
        }
        ProfilingUtils.sleep(1000);

        System.out.printf("Profiling %s with %dx%d data quanta.\n", javaBinaryOperatorProfiler.getExecutionOperator(), cardinality0, cardinality1);
        final StopWatch stopWatch = createStopWatch();

        System.out.println("Prepare...");
        final TimeMeasurement preparation = stopWatch.start("Preparation");
        javaBinaryOperatorProfiler.prepare(dataQuantaSize, cardinality0, cardinality1);
        preparation.stop();

        System.out.println("Execute...");
        final TimeMeasurement execution = stopWatch.start("Execution");
        final OperatorProfiler.Result result = javaBinaryOperatorProfiler.run();
        execution.stop();

        System.out.println("Measurement:");
        System.out.println(result);
        System.out.println(stopWatch.toPrettyString());
        System.out.println();

        return result;
    }

    private static List<OperatorProfiler.Result> profile(JavaSinkProfiler sinkProfiler, Collection<Integer> cardinalities, long dataQuantaSize) {
        return cardinalities.stream()
                .map(cardinality -> {
                    // Make a first white run to be sure the cache has changed
                    profile(sinkProfiler, cardinality, dataQuantaSize);
                    return profile(sinkProfiler, cardinality, dataQuantaSize);
                })
                .collect(Collectors.toList());
    }

    private static OperatorProfiler.Result profile(JavaSinkProfiler sinkProfiler, long cardinality, long dataQuantaSize) {
        System.out.println("Running garbage collector...");
        for (int i = 0; i < GC_RUNS; i++) {
            System.gc();
        }
        ProfilingUtils.sleep(1000);

        System.out.printf("Profiling %s with %d data quanta.\n", sinkProfiler, cardinality);
        final StopWatch stopWatch = createStopWatch();

        System.out.println("Prepare...");
        final TimeMeasurement preparation = stopWatch.start("Preparation");
        sinkProfiler.prepare(dataQuantaSize, cardinality);
        preparation.stop();

        System.out.println("Execute...");
        final TimeMeasurement execution = stopWatch.start("Execution");
        final OperatorProfiler.Result result = sinkProfiler.run();
        execution.stop();

        System.out.println("Measurement:");
        System.out.println(result);
        System.out.println(stopWatch.toPrettyString());
        System.out.println();

        return result;
    }

}