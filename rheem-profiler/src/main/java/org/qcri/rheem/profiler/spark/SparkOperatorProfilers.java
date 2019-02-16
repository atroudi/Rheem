package org.qcri.rheem.profiler.spark;

import org.qcri.rheem.core.api.Configuration;
import org.qcri.rheem.core.function.*;
import org.qcri.rheem.core.types.DataSetType;
import org.qcri.rheem.profiler.generators.DataGenerators;
import org.qcri.rheem.profiler.generators.UdfGenerators;
import org.qcri.rheem.spark.operators.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Utilities to create {@link SparkOperatorProfiler} instances.
 */
public class SparkOperatorProfilers {

    /**
     * Create a default {@link SparkTextFileSource} profiler.
     */
    public static SparkTextFileSourceProfiler createSparkTextFileSourceProfiler(int dataQuataSize) {
        return createSparkTextFileSourceProfiler(
                DataGenerators.createRandomStringSupplier(20 + dataQuataSize, 40 + dataQuataSize, new Random()),
                new Configuration()
        );
    }

    /**
     * Create a custom {@link SparkTextFileSource} profiler.
     */
    public static SparkTextFileSourceProfiler createSparkTextFileSourceProfiler(Supplier<String> dataGenerator,
                                                                                Configuration configuration) {
        return new SparkTextFileSourceProfiler(configuration, dataGenerator);
    }

    /**
     * Create a default {@link SparkCollectionSource} profiler.
     */
    public static SparkCollectionSourceProfiler createSparkCollectionSourceProfiler(int dataQuataSize, Type type) {
        if(type==String.class)
            return createSparkCollectionSourceProfiler(
                    DataGenerators.createRandomStringSupplier(20 + dataQuataSize, 40 + dataQuataSize, new Random(42)),
                    new Configuration(),String.class
            );
        else
            return createSparkCollectionSourceProfiler(
                    DataGenerators.createRandomIntegerSupplier(20 + dataQuataSize, 40 + dataQuataSize, new Random(42)),
                    new Configuration(),Integer.class
            );
    }

    /**
     * Create a default {@link SparkCollectionSource} profiler.
     */
    public static SparkTextFileSourceProfiler createSparkCollectionSourceProfiler(int dataQuataSize) {
        return createSparkCollectionSourceProfiler(
                DataGenerators.createRandomStringSupplier(20 + dataQuataSize, 40 + dataQuataSize, new Random(42)),
                new Configuration()
        );
    }

    /**
     * Create a custom {@link SparkTextFileSource} profiler.
     */
    public static <Out> SparkCollectionSourceProfiler createSparkCollectionSourceProfiler(Supplier<Out> dataGenerator,
                                                                                     Configuration configuration,
                                                                                     Class<Out> outClass) {
        return new SparkCollectionSourceProfiler(configuration, dataGenerator,DataSetType.createDefault(outClass));
    }

    /**
     * Create a custom {@link SparkTextFileSource} profiler.
     */
    public static SparkTextFileSourceProfiler createSparkCollectionSourceProfiler(Supplier<String> dataGenerator,
                                                                                  Configuration configuration) {
        return new SparkTextFileSourceProfiler(configuration, dataGenerator);
    }

    /**
     * Creates a default {@link SparkFlatMapOperator} profiler.
     */
    public static SparkUnaryOperatorProfiler createSparkFlatMapProfiler(int dataQuataSize, int UdfComplexity) {
        if (dataQuataSize==1){
            return createSparkFlatMapProfiler(
                    DataGenerators.createRandomIntegerSupplier(new Random(42)),
                    integer -> new ArrayList<>(Arrays.asList(UdfGenerators.mapIntUDF(1,dataQuataSize).apply(integer))),
                    Integer.class, Integer.class,
                    new Configuration()
            );
        } else {
            return new SparkUnaryOperatorProfiler(
                    () -> new SparkFlatMapOperator<>(
                            DataSetType.createDefault(List.class),
                            DataSetType.createGrouped(List.class),
                            new FlatMapDescriptor<List,Integer>(
                                    integerList -> (ArrayList<Integer>) integerList.stream().map(el->(Integer)UdfGenerators.mapIntUDF(UdfComplexity,dataQuataSize).apply((Integer) el))
                                            .collect(Collectors.toList()),
                                    List.class,
                                    Integer.class
                            )),
                    new Configuration(),
                    DataGenerators.createReservoirBasedIntegerListSupplier(new ArrayList<List<Integer>>(),0.0,new Random(),dataQuataSize)
            );
        }
    }


    /**
     * Creates a custom {@link SparkFlatMapOperator} profiler.
     */
    public static <In, Out> SparkUnaryOperatorProfiler createSparkFlatMapProfiler(Supplier<In> dataGenerator,
                                                                                  FunctionDescriptor.SerializableFunction<In, Iterable<Out>> udf,
                                                                                  Class<In> inClass,
                                                                                  Class<Out> outClass,
                                                                                  Configuration configuration) {
        return new SparkUnaryOperatorProfiler(
                () -> new SparkFlatMapOperator<>(
                        DataSetType.createDefault(inClass),
                        DataSetType.createGrouped(outClass),
                        new FlatMapDescriptor<>(udf, inClass, outClass)
                ),
                configuration,
                dataGenerator
        );
    }

    /**
     * Creates a default {@link SparkMapOperator} profiler.
     */
    public static SparkUnaryOperatorProfiler createSparkMapProfiler(int dataQuataSize, int UdfComplexity) {
        if (dataQuataSize==1){
            return createSparkMapProfiler(
                    DataGenerators.createRandomIntegerSupplier(new Random()),
                    i -> i,
                    Integer.class, Integer.class,
                    new Configuration()
            );
        } else {
            return new SparkUnaryOperatorProfiler(
                    () -> new SparkMapOperator<>(
                            DataSetType.createDefault(List.class),
                            DataSetType.createDefault(List.class),
                            new TransformationDescriptor<List,List>(i -> UdfGenerators.mapIntListUDF(UdfComplexity,i.size()).apply(i),List.class, List.class)
                    ),
                    new Configuration(),
                    DataGenerators.createReservoirBasedIntegerListSupplier(new ArrayList<List<Integer>>(),0.0,new Random(),dataQuataSize)
            );
        }

    }


    /**
     * Creates a custom {@link SparkMapOperator} profiler.
     */
    public static <In, Out> SparkUnaryOperatorProfiler createSparkMapProfiler(Supplier<In> dataGenerator,
                                                                              FunctionDescriptor.SerializableFunction<In, Out> udf,
                                                                              Class<In> inClass,
                                                                              Class<Out> outClass,
                                                                              Configuration configuration) {
        return new SparkUnaryOperatorProfiler(
                () -> new SparkMapOperator<>(
                        DataSetType.createDefault(inClass),
                        DataSetType.createDefault(outClass),
                        new TransformationDescriptor<>(udf, inClass, outClass)
                ),
                configuration,
                dataGenerator
        );
    }

    /**
     * Creates a default {@link SparkFilterOperator} profiler.
     */
    public static SparkUnaryOperatorProfiler createSparkFilterProfiler(int dataQuataSize, int UdfComplexity) {
        if(dataQuataSize==1){
            return createSparkFilterProfiler(
                    DataGenerators.createRandomIntegerSupplier(new Random()),
                    UdfGenerators.filterIntUDF(UdfComplexity,dataQuataSize),
                    Integer.class,
                    new Configuration()
            );
        } else {
            return new SparkUnaryOperatorProfiler(
                    () -> new SparkFilterOperator<>(
                            DataSetType.createDefault(List.class),
                            new PredicateDescriptor<>(UdfGenerators.filterIntListUDF(UdfComplexity,(int)dataQuataSize), List.class)
                    ),
                    new Configuration(),
                    DataGenerators.createReservoirBasedIntegerListSupplier(new ArrayList<List<Integer>>(), 0.00, new Random(), (int) dataQuataSize)
            );
        }
    }


    /**
     * Creates a custom {@link SparkMapOperator} profiler.
     */
    public static <Type> SparkUnaryOperatorProfiler createSparkFilterProfiler(Supplier<Type> dataGenerator,
                                                                              PredicateDescriptor.SerializablePredicate<Type> udf,
                                                                              Class<Type> inOutClass,
                                                                              Configuration configuration) {
        return new SparkUnaryOperatorProfiler(
                () -> new SparkFilterOperator<>(
                        DataSetType.createDefault(inOutClass),
                        new PredicateDescriptor<>(udf, inOutClass)
                ),
                configuration,
                dataGenerator
        );
    }


    /**
     * Creates a default {@link SparkReduceByOperator} profiler.
     */
    public static SparkUnaryOperatorProfiler createSparkReduceByProfiler(int dataQuataSize, int UdfComplexity) {
        return createSparkReduceByProfiler(
                DataGenerators.createReservoirBasedStringSupplier(new ArrayList<>(), 0.7, new Random(), 4 + dataQuataSize, 20 + dataQuataSize),
                String::new,
                (s1, s2) -> { return UdfGenerators.mapStringUDF(UdfComplexity,dataQuataSize).apply(s1);},
                String.class,
                String.class,
                new Configuration()
        );
    }

    /**
     * Creates a custom {@link SparkReduceByOperator} profiler.
     */
    public static <In, Key> SparkUnaryOperatorProfiler createSparkReduceByProfiler(Supplier<In> dataGenerator,
                                                                                   FunctionDescriptor.SerializableFunction<In, Key> keyUdf,
                                                                                   FunctionDescriptor.SerializableBinaryOperator<In> udf,
                                                                                   Class<In> inOutClass,
                                                                                   Class<Key> keyClass,
                                                                                   Configuration configuration) {
        return new SparkUnaryOperatorProfiler(
                () -> new SparkReduceByOperator<>(
                        DataSetType.createDefault(inOutClass),
                        new TransformationDescriptor<>(keyUdf, inOutClass, keyClass),
                        new ReduceDescriptor<>(udf, inOutClass)
                ),
                configuration,
                dataGenerator
        );
    }

    /**
     * Creates a default {@link SparkGlobalReduceOperator} profiler.
     */
    public static SparkUnaryOperatorProfiler createSparkGlobalReduceProfiler(int dataQuataSize, int UdfComplexity) {
        return createSparkGlobalReduceProfiler(
                DataGenerators.createReservoirBasedStringSupplier(new ArrayList<>(), 0.7, new Random(), 4, 20),
                (s1, s2) -> {return UdfGenerators.mapStringUDF(UdfComplexity,dataQuataSize).apply(s1);},
                String.class,
                new Configuration()
        );
    }

    /**
     * Creates a custom {@link SparkGlobalReduceOperator} profiler.
     */
    public static <Type> SparkUnaryOperatorProfiler createSparkGlobalReduceProfiler(Supplier<Type> dataGenerator,
                                                                                    FunctionDescriptor.SerializableBinaryOperator<Type> udf,
                                                                                    Class<Type> inOutClass,
                                                                                    Configuration configuration) {
        return new SparkUnaryOperatorProfiler(
                () -> new SparkGlobalReduceOperator<>(
                        DataSetType.createDefault(inOutClass),
                        new ReduceDescriptor<>(udf, inOutClass)
                ),
                configuration,
                dataGenerator
        );
    }

    /**
     * Creates a default {@link SparkDistinctOperator} profiler.
     */
    public static SparkUnaryOperatorProfiler createSparkDistinctProfiler(int dataQuataSize) {
        return createSparkDistinctProfiler(
                DataGenerators.createReservoirBasedStringSupplier(new ArrayList<>(), 0.7, new Random(), 4 + dataQuataSize, 20 + dataQuataSize),
                String.class,
                new Configuration()
        );
    }

    /**
     * Creates a custom {@link SparkGlobalReduceOperator} profiler.
     */
    public static <Type> SparkUnaryOperatorProfiler createSparkDistinctProfiler(Supplier<Type> dataGenerator,
                                                                                Class<Type> inOutClass,
                                                                                Configuration configuration) {
        return new SparkUnaryOperatorProfiler(
                () -> new SparkDistinctOperator<>(DataSetType.createDefault(inOutClass)),
                configuration,
                dataGenerator
        );
    }

    /**
     * Creates a default {@link SparkSortOperator} profiler.
     */
    public static SparkUnaryOperatorProfiler createSparkSortProfiler(int dataQuataSize) {
        return createSparkSortProfiler(
                DataGenerators.createReservoirBasedStringSupplier(new ArrayList<>(), 0.7, new Random(), 4 + dataQuataSize, 20 + dataQuataSize),
                String.class,
                new Configuration()
        );
    }

    /**
     * Creates a custom {@link SparkSortOperator} profiler.
     */
    public static <Type> SparkUnaryOperatorProfiler createSparkSortProfiler(Supplier<Type> dataGenerator,
                                                                            Class<Type> inOutClass,
                                                                            Configuration configuration) {
        return new SparkUnaryOperatorProfiler(
                () -> new SparkSortOperator<>(new TransformationDescriptor<>(in->in, inOutClass, inOutClass),
                        DataSetType.createDefault(inOutClass)),
                configuration,
                dataGenerator
        );
    }

    /**
     * Creates a default {@link SparkCountOperator} profiler.
     */
    public static SparkUnaryOperatorProfiler createSparkCountProfiler(int dataQuataSize) {
        return createSparkCountProfiler(
                DataGenerators.createReservoirBasedStringSupplier(new ArrayList<>(), 0.7, new Random(), 4 + dataQuataSize, 20 + dataQuataSize),
                String.class,
                new Configuration()
        );
    }

    /**
     * Creates a custom {@link SparkCountOperator} profiler.
     */
    public static <In> SparkUnaryOperatorProfiler createSparkCountProfiler(Supplier<In> dataGenerator,
                                                                           Class<In> inClass,
                                                                           Configuration configuration) {
        return new SparkUnaryOperatorProfiler(
                () -> new SparkCountOperator<>(DataSetType.createDefault(inClass)),
                configuration,
                dataGenerator
        );
    }

    /**
     * Creates a default {@link SparkMaterializedGroupByOperator} profiler.
     */
    public static SparkUnaryOperatorProfiler createSparkMaterializedGroupByProfiler(int dataQuataSize, int UdfComplexity) {
        return createSparkMaterializedGroupByProfiler(
                DataGenerators.createReservoirBasedStringSupplier(new ArrayList<>(), 0.7, new Random(), 4 + dataQuataSize, 20 + dataQuataSize),
                s->{
                    return UdfGenerators.mapStringUDF(UdfComplexity,dataQuataSize).apply(s);
                },
                String.class,
                String.class,
                new Configuration()
        );
    }

    /**
     * Creates a custom {@link SparkMaterializedGroupByOperator} profiler.
     */
    public static <In, Key> SparkUnaryOperatorProfiler createSparkMaterializedGroupByProfiler(Supplier<In> dataGenerator,
                                                                                              FunctionDescriptor.SerializableFunction<In, Key> keyUdf,
                                                                                              Class<In> inClass,
                                                                                              Class<Key> keyClass,
                                                                                              Configuration configuration) {
        return new SparkUnaryOperatorProfiler(
                () -> new SparkMaterializedGroupByOperator<>(
                        new TransformationDescriptor<>(keyUdf, inClass, keyClass),
                        DataSetType.createDefault(inClass),
                        DataSetType.createGrouped(inClass)
                ),
                configuration,
                dataGenerator
        );
    }

    /**
     * Creates a default {@link SparkJoinOperator} profiler.
     */
    public static BinaryOperatorProfiler createSparkJoinProfiler(int dataQuataSize, int UdfComplexity) {
        // NB: If we generate the Strings from within Spark, we will have two different reservoirs for each input.
        final DataGenerators.Generator<String> stringGenerator = DataGenerators.createReservoirBasedStringSupplier(new ArrayList<>(), 0.7, new Random(), 4 + dataQuataSize, 20 + dataQuataSize);
        return createSparkJoinProfiler(
                stringGenerator, String.class, s -> {return UdfGenerators.mapStringUDF(UdfComplexity,dataQuataSize).apply(s);},
                stringGenerator, String.class, String::new,
                String.class, new Configuration()
        );
    }

    /**
     * Creates a custom {@link SparkJoinOperator} profiler.
     */
    public static <In0, In1, Key> BinaryOperatorProfiler createSparkJoinProfiler(
            Supplier<In0> dataGenerator0,
            Class<In0> inClass0,
            FunctionDescriptor.SerializableFunction<In0, Key> keyUdf0,
            Supplier<In1> dataGenerator1,
            Class<In1> inClass1,
            FunctionDescriptor.SerializableFunction<In1, Key> keyUdf1,
            Class<Key> keyClass,
            Configuration configuration) {
        return new BinaryOperatorProfiler(
                () -> new SparkJoinOperator<>(
                        DataSetType.createDefault(inClass0),
                        DataSetType.createDefault(inClass1),
                        new TransformationDescriptor<>(keyUdf0, inClass0, keyClass),
                        new TransformationDescriptor<>(keyUdf1, inClass1, keyClass)
                ),
                configuration,
                dataGenerator0,
                dataGenerator1
        );
    }

    /**
     * Creates a default {@link SparkUnionAllOperator} profiler.
     */
    public static BinaryOperatorProfiler createSparkUnionProfiler(int dataQuataSize) {
        if (dataQuataSize==1) {
            return createSparkUnionProfiler(
                    DataGenerators.createRandomStringSupplier(10 + dataQuataSize,20 + dataQuataSize,new Random(42)),
                    DataGenerators.createRandomStringSupplier(dataQuataSize+ 10,dataQuataSize+ 20,new Random(23)),
                    String.class, new Configuration()
            );
        } else {
            return new BinaryOperatorProfiler(
                    () -> new SparkUnionAllOperator<>(DataSetType.createDefault(List.class)),
                    new Configuration(),
                    DataGenerators.createReservoirBasedIntegerListSupplier(new ArrayList<List<Integer>>(), 0.70, new Random(), (int) dataQuataSize),
                    DataGenerators.createReservoirBasedIntegerListSupplier(new ArrayList<List<Integer>>(), 0.70, new Random(), (int) dataQuataSize)
            );
        }

    }

    /**
     * Creates a default {@link SparkUnionAllOperator} profiler.
     */
    public static BinaryOperatorProfiler createSparkIntegerUnionProfiler(int dataQuataSize) {
        if (dataQuataSize==1) {
            return createSparkUnionProfiler(
                    DataGenerators.createRandomIntegerSupplier(new Random(42)),
                    DataGenerators.createRandomIntegerSupplier(new Random(23)),
                    Integer.class, new Configuration()
            );
        } else {
            return new BinaryOperatorProfiler(
                    () -> new SparkUnionAllOperator<>(DataSetType.createDefault(List.class)),
                    new Configuration(),
                    DataGenerators.createReservoirBasedIntegerListSupplier(new ArrayList<List<Integer>>(), 0.70, new Random(), (int) dataQuataSize),
                    DataGenerators.createReservoirBasedIntegerListSupplier(new ArrayList<List<Integer>>(), 0.70, new Random(), (int) dataQuataSize)
            );
        }

    }

    /**
     * Creates a custom {@link SparkUnionAllOperator} profiler.
     */
    public static <Type> BinaryOperatorProfiler createSparkUnionProfiler(
            Supplier<Type> dataGenerator0,
            Supplier<Type> dataGenerator1,
            Class<Type> typeClass,
            Configuration configuration) {
        return new BinaryOperatorProfiler(
                () -> new SparkUnionAllOperator<>(DataSetType.createDefault(typeClass)),
                configuration,
                dataGenerator0,
                dataGenerator1
        );
    }

    /**
     * Creates a default {@link SparkCartesianOperator} profiler.
     */
    public static BinaryOperatorProfiler createSparkCartesianProfiler(int dataQuataSize) {
        if (dataQuataSize==1) {
            return createSparkCartesianProfiler(
                    DataGenerators.createRandomIntegerSupplier(new Random(42)),
                    DataGenerators.createRandomIntegerSupplier(new Random(23)),
                    Integer.class, Integer.class, new Configuration()
            );
        } else {
            return new BinaryOperatorProfiler(
                    () -> new SparkCartesianOperator<>(DataSetType.createDefault(List.class), DataSetType.createDefault(List.class)),
                    new Configuration(),
                    DataGenerators.createReservoirBasedIntegerListSupplier(new ArrayList<List<Integer>>(), 0.10, new Random(), (int) dataQuataSize),
                    DataGenerators.createReservoirBasedIntegerListSupplier(new ArrayList<List<Integer>>(), 0.10, new Random(), (int) dataQuataSize)
            );
        }

    }

    /**
     * Creates a custom {@link SparkCartesianOperator} profiler.
     */
    public static <In0, In1> BinaryOperatorProfiler createSparkCartesianProfiler(
            Supplier<In0> dataGenerator0,
            Supplier<In1> dataGenerator1,
            Class<In0> inClass0,
            Class<In1> inClass1,
            Configuration configuration) {
        return new BinaryOperatorProfiler(
                () -> new SparkCartesianOperator<>(DataSetType.createDefault(inClass0), DataSetType.createDefault(inClass1)),
                configuration,
                dataGenerator0,
                dataGenerator1
        );
    }

    /**
     * Creates a default {@link SparkLocalCallbackSink} profiler.
     */
    public static SparkSinkProfiler createSparkLocalCallbackSinkProfiler(int dataQuataSize) {
        if (dataQuataSize==1){
            return createSparkLocalCallbackSinkProfiler(
                    DataGenerators.createRandomIntegerSupplier(new Random(42)),
                    Integer.class,
                    new Configuration()
            );
        } else {
            return new SparkSinkProfiler(
                    () -> new SparkLocalCallbackSink<>(dataQuantum -> { }, DataSetType.createDefault(List.class)),
                    new Configuration(),
                    DataGenerators.createReservoirBasedIntegerListSupplier(new ArrayList<List<Integer>>(), 0.10, new Random(), (int) dataQuataSize)
            );
        }

    }

    /**
     * Creates a custom {@link SparkLocalCallbackSink} profiler.
     */
    public static <Type> SparkSinkProfiler createSparkLocalCallbackSinkProfiler(
            Supplier<Type> dataGenerator,
            Class<Type> typeClass,
            Configuration configuration) {
        return new SparkSinkProfiler(
                () -> new SparkLocalCallbackSink<>(dataQuantum -> { }, DataSetType.createDefault(typeClass)),
                configuration,
                dataGenerator
        );
    }


}
