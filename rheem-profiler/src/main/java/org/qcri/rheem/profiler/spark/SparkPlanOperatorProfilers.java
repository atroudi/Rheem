package org.qcri.rheem.profiler.spark;

import org.qcri.rheem.core.api.Configuration;
import org.qcri.rheem.core.function.*;
import org.qcri.rheem.core.types.DataSetType;
import org.qcri.rheem.core.optimizer.mloptimizer.api.OperatorProfiler;
import org.qcri.rheem.profiler.generators.DataGenerators;
import org.qcri.rheem.spark.operators.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Utilities to create {@link SparkOperatorProfiler} instances.
 */
public class SparkPlanOperatorProfilers implements Serializable{

     private static Configuration configuration = new Configuration();

    /**
     * Create a default {@link SparkTextFileSource} profiler.
     */
    public static SparkTextFileSourceProfiler createSparkTextFileSourceProfiler(int dataQuantaScale, DataSetType type) {
        return createSparkTextFileSourceProfiler(
                DataGenerators.generateGenerator(dataQuantaScale,type),
                configuration
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
    public static SparkCollectionSourceProfiler createSparkCollectionSourceProfiler(int dataQuataSize, DataSetType type) {
            return createSparkCollectionSourceProfiler(
                    DataGenerators.generateGenerator(dataQuataSize,type),
                    configuration,type.getDataUnitType().getTypeClass()
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
    public static SparkUnaryOperatorProfiler createSparkFlatMapProfiler(int dataQuataSize, int UdfComplexity, DataSetType type) {

            return new SparkUnaryOperatorProfiler(
                    () -> new SparkFlatMapOperator<>(
                            type,
                            type,
                            new FlatMapDescriptor<>(
                                    DataGenerators.generateUDF(UdfComplexity,dataQuataSize,type,"flatmap"),
                                    type.getDataUnitType().getTypeClass(),
                                    type.getDataUnitType().getTypeClass()
                            )),
                    configuration,
                    DataGenerators.generateGenerator(dataQuataSize,type)
            );

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
    public static SparkUnaryOperatorProfiler createSparkMapProfiler(int dataQuataSize, int UdfComplexity, DataSetType type) {

            return new SparkUnaryOperatorProfiler(
                    () -> new SparkMapOperator<>(
                            type,
                            type,
                            new TransformationDescriptor<>(DataGenerators.generateUDF(UdfComplexity,dataQuataSize,type,"map")
                                    ,type.getDataUnitType().getTypeClass(), type.getDataUnitType().getTypeClass())
                    ),
                    configuration,
                    DataGenerators.createReservoirBasedIntegerListSupplier(new ArrayList<List<Integer>>(),0.0,new Random(),dataQuataSize)
            );


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
    public static SparkUnaryOperatorProfiler createSparkFilterProfiler(int dataQuataSize, int UdfComplexity, DataSetType type) {

        return new SparkUnaryOperatorProfiler(
                () -> new SparkFilterOperator<>(
                        type,
                        new PredicateDescriptor<>(DataGenerators.generatefilterUDF(UdfComplexity,dataQuataSize,type,"filter"), type.getDataUnitType().getTypeClass())
                ),
                configuration,
                DataGenerators.generateGenerator(dataQuataSize,type)
        );

    }

    /**
     * Creates a default {@link SparkFilterOperator} profiler.
     */
    public static OperatorProfiler createFilterProfiler(int dataQuataSize, int UdfComplexity, DataSetType type) {

        return new SparkUnaryOperatorProfiler(
                () -> new SparkFilterOperator<>(
                        type,
                        new PredicateDescriptor<>(DataGenerators.generatefilterUDF(UdfComplexity,dataQuataSize,type,"filter"), type.getDataUnitType().getTypeClass())
                ),
                configuration,
                DataGenerators.generateGenerator(dataQuataSize,type)
        );

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
    public static SparkUnaryOperatorProfiler createSparkReduceByProfiler(int dataQuataSize, int UdfComplexity, DataSetType type) {
        return createSparkReduceByProfiler(
                DataGenerators.generateGenerator(dataQuataSize,type),
                DataGenerators.generateUDF(UdfComplexity,dataQuataSize,type,"filter"),
                DataGenerators.generateBinaryUDF(UdfComplexity,dataQuataSize,type),
                type.getDataUnitType().getTypeClass(),
                type.getDataUnitType().getTypeClass(),
                configuration
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
    public static SparkUnaryOperatorProfiler createSparkGlobalReduceProfiler(int dataQuataSize, int UdfComplexity, DataSetType type) {
        return createSparkGlobalReduceProfiler(
                DataGenerators.generateGenerator(dataQuataSize,type),
                DataGenerators.generateBinaryUDF(UdfComplexity,dataQuataSize,type),
                type.getDataUnitType().getTypeClass(),
                configuration
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
    public static SparkUnaryOperatorProfiler createSparkDistinctProfiler(int dataQuataSize, DataSetType type) {
        return createSparkDistinctProfiler(
                DataGenerators.generateGenerator(dataQuataSize,type),
                type.getDataUnitType().getTypeClass(),
                configuration
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
    public static SparkUnaryOperatorProfiler createSparkSortProfiler(int dataQuataSize, DataSetType type) {
        return createSparkSortProfiler(
                DataGenerators.generateGenerator(dataQuataSize,type),
                type.getDataUnitType().getTypeClass(),
                configuration
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
    public static SparkUnaryOperatorProfiler createSparkCountProfiler(int dataQuataSize, DataSetType type) {
        return createSparkCountProfiler(
                DataGenerators.generateGenerator(dataQuataSize,type),
                type.getDataUnitType().getTypeClass(),
                configuration
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
    public static SparkUnaryOperatorProfiler createSparkMaterializedGroupByProfiler(int dataQuataSize, int UdfComplexity, DataSetType type) {
        return createSparkMaterializedGroupByProfiler(
                DataGenerators.generateGenerator(dataQuataSize,type),
                DataGenerators.generateUDF(UdfComplexity, dataQuataSize,type,"map"),
                type.getDataUnitType().getTypeClass(),
                type.getDataUnitType().getTypeClass(),
                configuration
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
    public static BinaryOperatorProfiler createSparkJoinProfiler(int dataQuataSize, int UdfComplexity, DataSetType type) {
        // NB: If we generate the Strings from within Spark, we will have two different reservoirs for each input.
        return createSparkJoinProfiler(
                DataGenerators.generateGenerator(dataQuataSize,type), type.getDataUnitType().getTypeClass(), DataGenerators.generateUDF(UdfComplexity, dataQuataSize,type,"map"),
                DataGenerators.generateGenerator(dataQuataSize,type), type.getDataUnitType().getTypeClass(), DataGenerators.generateUDF(UdfComplexity, dataQuataSize,type,"map"),
                type.getDataUnitType().getTypeClass(), configuration
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
    public static BinaryOperatorProfiler createSparkUnionProfiler(int dataQuataSize, DataSetType type) {

            return new BinaryOperatorProfiler(
                    () -> new SparkUnionAllOperator<>(type),
                    configuration,
                    DataGenerators.generateGenerator(dataQuataSize,type),
                    DataGenerators.generateGenerator(dataQuataSize,type)
            );

    }


    /**
     * Creates a default {@link SparkCartesianOperator} profiler.
     */
    public static BinaryOperatorProfiler createSparkCartesianProfiler(int dataQuataSize, DataSetType type) {

            return new BinaryOperatorProfiler(
                    () -> new SparkCartesianOperator<>(type, type),
                    configuration,
                    DataGenerators.generateGenerator(dataQuataSize,type),
                    DataGenerators.generateGenerator(dataQuataSize,type)
            );
    }

    /**
     * Creates a default {@link SparkLocalCallbackSink} profiler.
     */
    public static BinaryOperatorProfiler createSparkRepeatProfiler(int dataQuataSize, DataSetType type, int ierations) {

        return new BinaryOperatorProfiler(
                () -> new SparkRepeatOperator<>(ierations, type),
                configuration,
                DataGenerators.generateGenerator(dataQuataSize,type),
                DataGenerators.generateGenerator(dataQuataSize,type)
        );

    }

    public static SparkUnaryOperatorProfiler createSparkRandomSampleProfiler(int dataQuantaSize,DataSetType type, int sampleSize) {
        return new SparkUnaryOperatorProfiler(
                () -> new SparkRandomPartitionSampleOperator(iteration->sampleSize,
                        type,
                        iteration -> 42L),
                configuration,
                DataGenerators.generateGenerator(dataQuantaSize,type)
        );
    }

    public static SparkUnaryOperatorProfiler createSparkShuffleSampleProfiler(int dataQuantaSize,DataSetType type, int sampleSize) {
        return new SparkUnaryOperatorProfiler(
                () -> new SparkShufflePartitionSampleOperator<>(iteration->sampleSize,
                        type,
                        iteration -> 42L),
                configuration,
                DataGenerators.generateGenerator(dataQuantaSize,type)
        );
    }

    public static SparkUnaryOperatorProfiler createSparkBernoulliSampleProfiler(int dataQuantaSize,DataSetType type, int sampleSize) {
        return new SparkUnaryOperatorProfiler(
                () -> new SparkBernoulliSampleOperator<>(iteration->sampleSize,
                        type,
                        iteration -> 42L),
                configuration,
                DataGenerators.generateGenerator(dataQuantaSize,type)
        );
    }

    /**
     * Creates a default {@link SparkLocalCallbackSink} profiler.
     */
    public static SparkSinkProfiler createSparkLocalCallbackSinkProfiler(int dataQuataSize, DataSetType type) {

            return new SparkSinkProfiler(
                    () -> new SparkLocalCallbackSink<>(dataQuantum -> { }, type),
                    configuration,
                    DataGenerators.generateGenerator(dataQuataSize,type)
            );

    }



}
