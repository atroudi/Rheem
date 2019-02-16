package org.qcri.rheem.profiler.generators;

import org.qcri.rheem.core.api.Configuration;
import org.qcri.rheem.core.function.FlatMapDescriptor;
import org.qcri.rheem.core.function.PredicateDescriptor;
import org.qcri.rheem.core.function.ReduceDescriptor;
import org.qcri.rheem.core.function.TransformationDescriptor;
import org.qcri.rheem.core.plan.rheemplan.ExecutionOperator;
import org.qcri.rheem.core.types.DataSetType;
import org.qcri.rheem.flink.operators.*;
import org.qcri.rheem.profiler.core.ProfilingPlanBuilder;
import org.qcri.rheem.core.optimizer.mloptimizer.api.OperatorProfiler;
import org.qcri.rheem.core.optimizer.mloptimizer.api.OperatorProfilerBase;
import org.qcri.rheem.profiler.java.JavaOperatorProfilers;
import org.qcri.rheem.profiler.spark.SparkPlanOperatorProfilers;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Created by VettaEx1 on 1/2/2018.
 */
public class ProfilingOperatorGenerator {


    private ProfilingPlanBuilder profilingPlanBuilder;

    public ProfilingOperatorGenerator(ProfilingPlanBuilder profilingPlanBuilder) {
        this.profilingPlanBuilder = profilingPlanBuilder;
    }

    /**
     * Get the {@link OperatorProfiler} of the input string
     *
     * @param operator
     * @return
     */
    public static OperatorProfiler getProfilingOperator(String operator, DataSetType type, String plateform,
                                                        int dataQuantaScale, int UdfComplexity) {
        String fileUrl = ProfilingPlanBuilder.configuration.getStringProperty("rheem.profiler.logs.syntheticDataURL.prefix");
        switch (plateform) {
            case "flink":
                switch (operator) {
                    case "textsource":
                        return new OperatorProfilerBase(
                                (Supplier<ExecutionOperator> & Serializable) () -> {
                                    FlinkTextFileSource op = new FlinkTextFileSource("file:///" + fileUrl);
                                    op.setName("FinkTextFileSource");
                                    return op;
                                },
                                ProfilingPlanBuilder.configuration,
                                DataGenerators.generateGenerator(1, type)
                        );
                    case "collectionsource":
                        return new OperatorProfilerBase(
                                (Supplier<ExecutionOperator> & Serializable) () -> {
                                    FlinkCollectionSource op = new FlinkCollectionSource(type);
                                    op.setName("FinkCollectionFileSource");
                                    return op;
                                },
                                ProfilingPlanBuilder.configuration,
                                DataGenerators.generateGenerator(1, type)
                        );
                    case "map":
                        return new OperatorProfilerBase(
                                (Supplier<ExecutionOperator> & Serializable) () -> {
                                    FlinkMapOperator op = new FlinkMapOperator(
                                            type,
                                            type,
                                            new TransformationDescriptor<>(
                                                    DataGenerators.generateUDF(UdfComplexity, dataQuantaScale, type, "map"),
                                                    type.getDataUnitType().getTypeClass(),
                                                    type.getDataUnitType().getTypeClass()
                                            )
                                    );
                                    op.setName("FinkMap");
                                    return op;
                                },
                                ProfilingPlanBuilder.configuration,
                                DataGenerators.generateGenerator(1, type)
                        );

                    case "filter":
                        return new OperatorProfilerBase(
                                (Supplier<ExecutionOperator> & Serializable) () -> {
                                    FlinkFilterOperator op = new FlinkFilterOperator(type, new PredicateDescriptor<>(
                                            DataGenerators.generatefilterUDF(UdfComplexity, dataQuantaScale, type, "filter"),
                                            type.getDataUnitType().getTypeClass()));
                                    op.setName("FinkFilter");
                                    return op;
                                },
                                ProfilingPlanBuilder.configuration,
                                DataGenerators.generateGenerator(1, type)
                        );

                    case "flatmap":
                        return new OperatorProfilerBase(
                                (Supplier<ExecutionOperator> & Serializable) () -> {
                                    FlinkFlatMapOperator op = new FlinkFlatMapOperator(type,
                                            type,
                                            new FlatMapDescriptor<>(
                                                    DataGenerators.generateUDF(UdfComplexity, dataQuantaScale, type, "flatmap"),
                                                    type.getDataUnitType().getTypeClass(),
                                                    type.getDataUnitType().getTypeClass()
                                            ));
                                    op.setName("FinkFlatMap");
                                    return op;
                                },
                                ProfilingPlanBuilder.configuration,
                                DataGenerators.generateGenerator(1, type)
                        );

                    case "reduce":
                        return new OperatorProfilerBase(
                                (Supplier<ExecutionOperator> & Serializable) () -> {
                                    FlinkReduceByOperator op = new FlinkReduceByOperator(
                                            type,
                                            new TransformationDescriptor<>(DataGenerators.generateUDF(
                                                    UdfComplexity, dataQuantaScale, type, "filter"), type.getDataUnitType().getTypeClass(), type.getDataUnitType().getTypeClass()),
                                            new ReduceDescriptor<>(DataGenerators.generateBinaryUDF(UdfComplexity, dataQuantaScale, type), type.getDataUnitType().getTypeClass())
                                    );
                                    op.setName("FinkReduce");
                                    return op;
                                },
                                ProfilingPlanBuilder.configuration,
                                DataGenerators.generateGenerator(1, type)
                        );

                    case "globalreduce":
                        return new OperatorProfilerBase(
                                (Supplier<ExecutionOperator> & Serializable) () -> {
                                    FlinkGlobalReduceOperator op = new FlinkGlobalReduceOperator(
                                            type,
                                            new ReduceDescriptor<>(DataGenerators.generateBinaryUDF(UdfComplexity, dataQuantaScale, type), type.getDataUnitType().getTypeClass())
                                    );
                                    op.setName("FinkGlobalReduce");
                                    return op;
                                },
                                ProfilingPlanBuilder.configuration,
                                DataGenerators.generateGenerator(1, type)
                        );

                    case "distinct":
                        return new OperatorProfilerBase(
                                (Supplier<ExecutionOperator> & Serializable) () -> {
                                    FlinkDistinctOperator op = new FlinkDistinctOperator(
                                            type
                                    );
                                    op.setName("FinkDistinct");
                                    return op;
                                },
                                ProfilingPlanBuilder.configuration,
                                DataGenerators.generateGenerator(1, type)
                        );

                    case "sort":
                        return new OperatorProfilerBase(
                                (Supplier<ExecutionOperator> & Serializable) () -> {
                                    FlinkSortOperator op = new FlinkSortOperator(
                                            new TransformationDescriptor<>(in -> in, type.getDataUnitType().getTypeClass(), type.getDataUnitType().getTypeClass()),
                                            type
                                    );
                                    op.setName("FinkSort");
                                    return op;
                                },
                                ProfilingPlanBuilder.configuration,
                                DataGenerators.generateGenerator(1, type)
                        );

                    case "count":
                        return new OperatorProfilerBase(
                                (Supplier<ExecutionOperator> & Serializable) () -> {
                                    FlinkCountOperator op = new FlinkCountOperator(
                                            type
                                    );
                                    op.setName("FinkCount");
                                    return op;
                                },
                                ProfilingPlanBuilder.configuration,
                                DataGenerators.generateGenerator(1, type)
                        );

                    case "groupby":
                        return new OperatorProfilerBase(
                                (Supplier<ExecutionOperator> & Serializable) () -> {
                                    FlinkGroupByOperator op = new FlinkGroupByOperator(
                                            new TransformationDescriptor<>(
                                                    DataGenerators.generateUDF(UdfComplexity, dataQuantaScale, type, "map"),
                                                    type.getDataUnitType().getTypeClass(),
                                                    type.getDataUnitType().getTypeClass()
                                            ),
                                            type,
                                            type
                                    );
                                    op.setName("FinkGroupBy");
                                    return op;
                                },
                                ProfilingPlanBuilder.configuration,
                                DataGenerators.generateGenerator(1, type)
                        );

                    case "join":
                        return new OperatorProfilerBase(
                                (Supplier<ExecutionOperator> & Serializable) () -> {
                                    FlinkJoinOperator op = new FlinkJoinOperator(
                                            type,
                                            type,
                                            new TransformationDescriptor<>(
                                                    DataGenerators.generateUDF(UdfComplexity, dataQuantaScale, type, "map"),
                                                    type.getDataUnitType().getTypeClass(),
                                                    type.getDataUnitType().getTypeClass()
                                            ),
                                            new TransformationDescriptor<>(
                                                    DataGenerators.generateUDF(UdfComplexity, dataQuantaScale, type, "map"),
                                                    type.getDataUnitType().getTypeClass(),
                                                    type.getDataUnitType().getTypeClass()
                                            )
                                    );
                                    op.setName("FinkJoin");
                                    return op;
                                },
                                ProfilingPlanBuilder.configuration,
                                DataGenerators.generateGenerator(1, type)
                        );

                    case "union":
                        return new OperatorProfilerBase(
                                (Supplier<ExecutionOperator> & Serializable) () -> {
                                    FlinkUnionAllOperator op = new FlinkUnionAllOperator(
                                            type
                                    );
                                    op.setName("FinkUnion");
                                    return op;
                                },
                                ProfilingPlanBuilder.configuration,
                                DataGenerators.generateGenerator(1, type)
                        );

                    case "cartesian":
                        return new OperatorProfilerBase(
                                (Supplier<ExecutionOperator> & Serializable) () -> {
                                    FlinkCartesianOperator op = new FlinkCartesianOperator(
                                            type,
                                            type
                                    );
                                    op.setName("FinkCartesian");
                                    return op;
                                },
                                ProfilingPlanBuilder.configuration,
                                DataGenerators.generateGenerator(1, type)
                        );

                    case "callbacksink":
                        return new OperatorProfilerBase(
                                (Supplier<ExecutionOperator> & Serializable) () -> {
                                    FlinkLocalCallbackSink op = new FlinkLocalCallbackSink(dataQuantum -> {
                                    }, type);

                                    List<String> collector = new LinkedList<>();

                                    // set a collector
                                    //op.setCollector(collector);

                                    op.setName("FinkCallBackSink");
                                    return op;
                                },
                                ProfilingPlanBuilder.configuration,
                                DataGenerators.generateGenerator(1, type)
                        );

                    case "repeat":
                        return new OperatorProfilerBase(
                                (Supplier<ExecutionOperator> & Serializable) () -> {
                                    FlinkRepeatOperator op = new FlinkRepeatOperator(
                                            ProfilingPlanBuilder.profilingConfig.getIterations().get(0),
                                            type
                                    );
                                    op.setName("FinkRepeat");
                                    return op;
                                },
                                ProfilingPlanBuilder.configuration,
                                DataGenerators.generateGenerator(1, type)
                        );

                    case "randomsample":
                    case "shufflesample":
                    case "bernoullisample":
                        return new OperatorProfilerBase(
                                (Supplier<ExecutionOperator> & Serializable) () -> {
                                    FlinkSampleOperator op = new FlinkSampleOperator(
                                            iteration -> ProfilingPlanBuilder.profilingConfig.getSampleSize(),
                                            type,
                                            iteration -> 42L
                                    );
                                    op.setName("FinkSample");
                                    return op;
                                },
                                ProfilingPlanBuilder.configuration,
                                DataGenerators.generateGenerator(1, type)
                        );

                    default:
                        ProfilingPlanBuilder.logger.error("Unknown executionOperator: " + operator);
                        return new OperatorProfilerBase(
                                (Supplier<ExecutionOperator> & Serializable) () -> {
                                    FlinkLocalCallbackSink op = new FlinkLocalCallbackSink(dataQuantum -> {
                                    }, type);

                                    List<String> collector = new LinkedList<>();

                                    // set a collector
                                    op.setCollector(collector);
                                    op.setName("FinkCallBackSink");
                                    return op;
                                },
                                ProfilingPlanBuilder.configuration,
                                DataGenerators.generateGenerator(1, type)
                        );
                }
            case "spark":
                switch (operator) {
                    case "textsource":
                        return SparkPlanOperatorProfilers.createSparkTextFileSourceProfiler(1, type);
                    case "collectionsource":
                        return SparkPlanOperatorProfilers.createSparkCollectionSourceProfiler(1000, type);
                    case "map":
                        return SparkPlanOperatorProfilers.createSparkMapProfiler(1, UdfComplexity, type);

                    case "filter":
                        return (SparkPlanOperatorProfilers.createSparkFilterProfiler(1, UdfComplexity, type));

                    case "flatmap":
                        return (SparkPlanOperatorProfilers.createSparkFlatMapProfiler(1, UdfComplexity, type));

                    case "reduce":
                        return (SparkPlanOperatorProfilers.createSparkReduceByProfiler(1, UdfComplexity, type));

                    case "globalreduce":
                        return (SparkPlanOperatorProfilers.createSparkGlobalReduceProfiler(1000, UdfComplexity, type));

                    case "distinct":
                        return (SparkPlanOperatorProfilers.createSparkDistinctProfiler(1, type));

                    case "sort":
                        return (SparkPlanOperatorProfilers.createSparkSortProfiler(1, type));

                    case "count":
                        return (SparkPlanOperatorProfilers.createSparkCountProfiler(1, type));

                    case "groupby":
                        return (SparkPlanOperatorProfilers.createSparkMaterializedGroupByProfiler(1, UdfComplexity, type));

                    case "join":
                        return (SparkPlanOperatorProfilers.createSparkJoinProfiler(1, UdfComplexity, type));

                    case "union":
                        return (SparkPlanOperatorProfilers.createSparkUnionProfiler(1, type));

                    case "cartesian":
                        return (SparkPlanOperatorProfilers.createSparkCartesianProfiler(1, type));

                    case "callbacksink":
                        return (SparkPlanOperatorProfilers.createSparkLocalCallbackSinkProfiler(1, type));
                    case "repeat":
                        return (SparkPlanOperatorProfilers.createSparkRepeatProfiler(1, type, ProfilingPlanBuilder.profilingConfig.getIterations().get(0)));
                    case "randomsample":
                        return (SparkPlanOperatorProfilers.createSparkRandomSampleProfiler(1, type, ProfilingPlanBuilder.profilingConfig.getSampleSize()));
                    case "shufflesample":
                        return (SparkPlanOperatorProfilers.createSparkShuffleSampleProfiler(1, type, ProfilingPlanBuilder.profilingConfig.getSampleSize()));
                    case "bernoullisample":
                        return (SparkPlanOperatorProfilers.createSparkBernoulliSampleProfiler(1, type, ProfilingPlanBuilder.profilingConfig.getSampleSize()));

                    default:
                        ProfilingPlanBuilder.logger.error("Unknown executionOperator: " + operator);
                        return (SparkPlanOperatorProfilers.createSparkLocalCallbackSinkProfiler(1, type));
                }
            case "java":
                switch (operator) {
                    case "textsource":
                        return (JavaOperatorProfilers.createJavaTextFileSourceProfiler(1, type));

                    case "collectionsource":
                        return (JavaOperatorProfilers.createJavaCollectionSourceProfiler(1000, type));

                    case "map":
                        return (JavaOperatorProfilers.createJavaMapProfiler(1, UdfComplexity, type));

                    case "filter":
                        return (JavaOperatorProfilers.createJavaFilterProfiler(1, UdfComplexity, type));
                    case "flatmap":
                        return (JavaOperatorProfilers.createJavaFlatMapProfiler(1, UdfComplexity, type));

                    case "reduce":
                        return (JavaOperatorProfilers.createJavaReduceByProfiler(1, UdfComplexity, type));

                    case "globalreduce":
                        return (JavaOperatorProfilers.createJavaGlobalReduceProfiler(1, UdfComplexity, type));

                    case "distinct":
                        return (JavaOperatorProfilers.createJavaDistinctProfiler(1, type));

                    case "sort":
                        return (JavaOperatorProfilers.createJavaSortProfiler(1, UdfComplexity, type));

                    case "count":
                        return (JavaOperatorProfilers.createJavaCountProfiler(1, type));

                    case "groupby":
                        return (JavaOperatorProfilers.createJavaMaterializedGroupByProfiler(1, UdfComplexity, type));

                    case "join":
                        return (JavaOperatorProfilers.createJavaJoinProfiler(1, UdfComplexity, type));

                    case "union":
                        return (JavaOperatorProfilers.createJavaUnionProfiler(1, type));

                    case "cartesian":
                        return (JavaOperatorProfilers.createJavaCartesianProfiler(1, type));
                    case "randomsample":
                        return (JavaOperatorProfilers.createJavaRandomSampleProfiler(1, type, ProfilingPlanBuilder.profilingConfig.getSampleSize()));
                    case "shufflesample":
                        return (JavaOperatorProfilers.createJavaReservoirSampleProfiler(1, type, ProfilingPlanBuilder.profilingConfig.getSampleSize()));
                    case "bernoullisample":
                        return (JavaOperatorProfilers.createJavaReservoirSampleProfiler(1, type, ProfilingPlanBuilder.profilingConfig.getSampleSize()));
                    case "repeat":
                        return (JavaOperatorProfilers.createJavaRepeatProfiler(1, type, ProfilingPlanBuilder.profilingConfig.getIterations().get(0)));

                    case "callbacksink":
                        return (JavaOperatorProfilers.createJavaLocalCallbackSinkProfiler(1, type));

                    case "collect":
                        return (JavaOperatorProfilers.createCollectingJavaLocalCallbackSinkProfiler(1, type));

                    default:
                        ProfilingPlanBuilder.logger.error("Unknown executionOperator: " + operator);
                        return (JavaOperatorProfilers.createJavaLocalCallbackSinkProfiler(1));
                }
            default:
                ProfilingPlanBuilder.logger.error("Unknown executionOperator: " + operator);
                return (JavaOperatorProfilers.createJavaLocalCallbackSinkProfiler(1));
        }
    }
}
