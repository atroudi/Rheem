package org.qcri.rheem.profiler.log;

import org.json.JSONObject;
import org.qcri.rheem.core.api.Configuration;
import org.qcri.rheem.core.api.exception.RheemException;
import org.qcri.rheem.core.optimizer.costs.*;
import org.qcri.rheem.core.plan.rheemplan.ExecutionOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;

/**
 * Utility to create {@link DynamicLoadProfileEstimator}s.
 */
public class DynamicLoadProfileEstimators {

    private static final Logger logger = LoggerFactory.getLogger(DynamicLoadProfileEstimators.class);


    /**
     * Creates a {@link DynamicLoadProfileEstimator} according to the given {@link LoadProfileEstimator} and its
     * nested {@link LoadProfileEstimator}s.
     *
     * @param loadProfileEstimator that should be turned into a {@link DynamicLoadProfileEstimator}
     * @param configuration        provides templates for the {@link DynamicLoadProfileEstimator}s
     * @param optimizationSpace    provides {@link Variable}s for the {@link DynamicLoadProfileEstimator}s
     * @return the {@link DynamicLoadProfileEstimator}
     */
    public static DynamicLoadProfileEstimator createEstimatorFor(LoadProfileEstimator loadProfileEstimator,
                                                                 Configuration configuration,
                                                                 OptimizationSpace optimizationSpace) {

        DynamicLoadProfileEstimator mainEstimator;
        String templateKey = loadProfileEstimator.getTemplateKey();
        final String template;

        if (loadProfileEstimator instanceof DynamicLoadProfileEstimator) {
            mainEstimator = (DynamicLoadProfileEstimator) loadProfileEstimator;
        } else if (templateKey != null && (template = configuration.getStringProperty(templateKey, null)) != null) {
            mainEstimator = createFromTemplate(loadProfileEstimator.getConfigurationKey(), template, optimizationSpace);
        } else {
            mainEstimator = wrap(loadProfileEstimator);
        }

        for (LoadProfileEstimator nestedEstimator : loadProfileEstimator.getNestedEstimators()) {
            mainEstimator.nest(createEstimatorFor(nestedEstimator, configuration, optimizationSpace));
        }

        return mainEstimator;
    }


//    /**
//     * Let this class try to find a suitable {@link DynamicLoadProfileEstimator} for the given {@link ExecutionOperator}.
//     *
//     * @param executionOperator          the {@link ExecutionOperator} for that should be estimated
//     * @param optimizationSpace context for {@link Variable}s
//     * @param configuration     provides configuration values
//     * @return the {@link DynamicLoadProfileEstimator}
//     */
//    public static DynamicLoadProfileEstimator createSuitableEstimator(ExecutionOperator executionOperator,
//                                                                      OptimizationSpace optimizationSpace,
//                                                                      Configuration configuration) {
//
//        // JavaExecutionOperators.
//
//        // Map-like operators.
//        if (executionOperator instanceof JavaMapOperator) {
//            return createLinearEstimator(executionOperator, new int[]{0}, new int[0], false, optimizationSpace);
//        } else if (executionOperator instanceof JavaFilterOperator) {
//            return createLinearEstimator(executionOperator, new int[]{0}, new int[]{}, false, optimizationSpace);
//        } else if (executionOperator instanceof JavaFlatMapOperator) {
//            return createLinearEstimator(executionOperator, new int[]{0}, new int[]{}, false, optimizationSpace);
//        } else if (executionOperator instanceof JavaMapPartitionsOperator) {
//            return createLinearEstimator(executionOperator, new int[]{0}, new int[]{}, false, optimizationSpace);
//        } else if (executionOperator instanceof JavaRandomSampleOperator) {
//            return createLinearEstimator(executionOperator, new int[]{0}, new int[]{}, false, optimizationSpace);
//        } else if (executionOperator instanceof JavaReservoirSampleOperator) {
//            return createLinearEstimator(executionOperator, new int[]{0}, new int[]{}, false, optimizationSpace);
//
//            // Reduce-like operators
//        } else if (executionOperator instanceof JavaReduceByOperator) {
//            return createQuadraticEstimator(executionOperator, new int[]{0}, new int[]{}, false, optimizationSpace);
//        } else if (executionOperator instanceof JavaMaterializedGroupByOperator) {
//            return createLinearEstimator(executionOperator, new int[]{0}, new int[0], false, optimizationSpace);
//        } else if (executionOperator instanceof JavaDistinctOperator) {
//            return createLinearEstimator(executionOperator, new int[]{0}, new int[]{}, false, optimizationSpace);
//        } else if (executionOperator instanceof JavaSortOperator) {
//            return createLinearEstimator(executionOperator, new int[]{0}, new int[0], false, optimizationSpace);
//        } else if (executionOperator instanceof JavaGlobalReduceOperator) {
//            return createLinearEstimator(executionOperator, new int[]{0}, new int[0], false, optimizationSpace);
//        } else if (executionOperator instanceof JavaGlobalMaterializedGroupOperator) {
//            return createLinearEstimator(executionOperator, new int[]{0}, new int[0], false, optimizationSpace);
//        } else if (executionOperator instanceof JavaCountOperator) {
//            return createLinearEstimator(executionOperator, new int[]{0}, new int[0], true, optimizationSpace);
//
//            // Binary operators.
//        } else if (executionOperator instanceof JavaIntersectOperator) {
//            return createLinearEstimator(executionOperator, new int[]{0, 1}, new int[0], false, optimizationSpace);
//        } else if (executionOperator instanceof JavaUnionAllOperator) {
//            return createLinearEstimator(executionOperator, new int[0], new int[0], true, optimizationSpace);
//        } else if (executionOperator instanceof JavaCartesianOperator) {
//            return createLinearEstimator(executionOperator, new int[0], new int[]{0}, false, optimizationSpace);
//        } else if (executionOperator instanceof JavaJoinOperator) {
//            return createLinearEstimator(executionOperator, new int[]{0, 1}, new int[]{0}, false, optimizationSpace);
//
//            // Loop operators.
//        } else if (executionOperator instanceof JavaLoopOperator) {
//            return createLoopEstimator(executionOperator, optimizationSpace);
//        } else if (executionOperator instanceof JavaDoWhileOperator) {
//            return createLoopEstimator(executionOperator, optimizationSpace);
//        } else if (executionOperator instanceof JavaRepeatOperator) {
//            return createLoopEstimator(executionOperator, optimizationSpace);
//
//            // Sources and sinks.
//        } else if (executionOperator instanceof JavaCollectionSource) {
//            return createLinearEstimator(executionOperator, new int[0], new int[0], true, optimizationSpace);
//        } else if (executionOperator instanceof JavaLocalCallbackSink) {
//            return createLinearEstimator(executionOperator, new int[]{0}, new int[0], false, optimizationSpace);
//        } else if (executionOperator instanceof JavaTextFileSource) {
//            return createLinearEstimator(executionOperator, new int[]{}, new int[]{0}, true, optimizationSpace);
//        } else if (executionOperator instanceof JavaTextFileSink) {
//            return createLinearEstimator(executionOperator, new int[]{0}, new int[0], true, optimizationSpace);
//        } else if (executionOperator instanceof JavaObjectFileSource) {
//            return createLinearEstimator(executionOperator, new int[]{}, new int[]{0}, true, optimizationSpace);
//        } else if (executionOperator instanceof JavaObjectFileSink) {
//            return createLinearEstimator(executionOperator, new int[]{0}, new int[0], true, optimizationSpace);
//        } else if (executionOperator instanceof JavaTsvFileSource) {
//            return createLinearEstimator(executionOperator, new int[]{}, new int[]{0}, true, optimizationSpace);
//        } else if (executionOperator instanceof JavaTsvFileSink) {
//            return createLinearEstimator(executionOperator, new int[]{0}, new int[0], true, optimizationSpace);
//
//            // Graph operators.
//        } else if (executionOperator instanceof JavaPageRankOperator) {
//            return createLinearEstimator(executionOperator, new int[]{0}, new int[]{0}, false, optimizationSpace);
//
//            // Conversion operators.
//        } else if (executionOperator instanceof JavaCollectOperator) {
//            return createLinearEstimator(executionOperator, new int[]{0}, new int[0], false, optimizationSpace);
//        } else if (executionOperator instanceof SqlToStreamOperator) {
//            return createLinearEstimator(executionOperator, new int[]{}, new int[]{0}, true, optimizationSpace);
//        }
//
//        // SparkExecutionOperators.
//
//        // Map-like operators.
//        else if (executionOperator instanceof SparkMapOperator) {
//            return createLinearEstimator(executionOperator, new int[]{0}, new int[0], false, optimizationSpace);
//        } else if (executionOperator instanceof SparkFilterOperator) {
//            return createLinearEstimator(executionOperator, new int[]{0}, new int[]{}, false, optimizationSpace);
//        } else if (executionOperator instanceof SparkFlatMapOperator) {
//            return createLinearEstimator(executionOperator, new int[]{0}, new int[]{}, false, optimizationSpace);
//        } else if (executionOperator instanceof SparkMapPartitionsOperator) {
//            return createLinearEstimator(executionOperator, new int[]{0}, new int[]{}, false, optimizationSpace);
//        } else if (executionOperator instanceof SparkBernoulliSampleOperator) {
//            return createLinearEstimator(executionOperator, new int[]{0}, new int[]{}, false, optimizationSpace);
//        } else if (executionOperator instanceof SparkRandomPartitionSampleOperator) {
//            return createLinearEstimator(executionOperator, new int[]{0}, new int[]{}, false, optimizationSpace);
//        } else if (executionOperator instanceof SparkShufflePartitionSampleOperator) {
//            return createLinearEstimator(executionOperator, new int[]{0}, new int[]{}, false, optimizationSpace);
//        } else if (executionOperator instanceof ZipWithIdOperator) {
//            return createLinearEstimator(executionOperator, new int[]{0}, new int[]{}, false, optimizationSpace);
//
//            // Reduce-like operators
//        } else if (executionOperator instanceof SparkReduceByOperator) {
//            return createLinearEstimator(executionOperator, new int[]{0}, new int[]{}, false, optimizationSpace);
//        } else if (executionOperator instanceof SparkMaterializedGroupByOperator) {
//            return createLinearEstimator(executionOperator, new int[]{0}, new int[0], false, optimizationSpace);
//        } else if (executionOperator instanceof SparkDistinctOperator) {
//            return createLinearEstimator(executionOperator, new int[]{0}, new int[]{}, false, optimizationSpace);
//        } else if (executionOperator instanceof SparkSortOperator) {
//            return createLinearEstimator(executionOperator, new int[]{0}, new int[0], false, optimizationSpace);
//        } else if (executionOperator instanceof SparkGlobalReduceOperator) {
//            return createLinearEstimator(executionOperator, new int[]{0}, new int[0], true, optimizationSpace);
//        } else if (executionOperator instanceof SparkGlobalMaterializedGroupOperator) {
//            return createLinearEstimator(executionOperator, new int[]{0}, new int[0], true, optimizationSpace);
//        } else if (executionOperator instanceof SparkCountOperator) {
//            return createLinearEstimator(executionOperator, new int[]{0}, new int[0], true, optimizationSpace);
//
//            // Binary operators.
//        } else if (executionOperator instanceof SparkIntersectOperator) {
//            return createLinearEstimator(executionOperator, new int[]{0, 1}, new int[0], false, optimizationSpace);
//        } else if (executionOperator instanceof SparkUnionAllOperator) {
//            return createLinearEstimator(executionOperator, new int[0], new int[0], true, optimizationSpace);
//        } else if (executionOperator instanceof SparkCartesianOperator) {
//            return createLinearEstimator(executionOperator, new int[0], new int[]{0}, false, optimizationSpace);
//        } else if (executionOperator instanceof SparkJoinOperator) {
//            return createLinearEstimator(executionOperator, new int[]{0, 1}, new int[]{0}, false, optimizationSpace);
//
//            // Loop operators.
//        } else if (executionOperator instanceof SparkLoopOperator) {
//            return createLoopEstimator(executionOperator, optimizationSpace);
//        } else if (executionOperator instanceof SparkDoWhileOperator) {
//            return createLoopEstimator(executionOperator, optimizationSpace);
//        } else if (executionOperator instanceof SparkRepeatOperator) {
//            return createLoopEstimator(executionOperator, optimizationSpace);
//
//            // Sources and sinks.
//        } else if (executionOperator instanceof SparkCollectionSource) {
//            return createLinearEstimator(executionOperator, new int[0], new int[0], true, optimizationSpace);
//        } else if (executionOperator instanceof SparkLocalCallbackSink) {
//            return createLinearEstimator(executionOperator, new int[]{0}, new int[0], false, optimizationSpace);
//        } else if (executionOperator instanceof SparkTextFileSource) {
//            return createLinearEstimator(executionOperator, new int[]{}, new int[]{0}, true, optimizationSpace);
//        } else if (executionOperator instanceof SparkTextFileSink) {
//            return createLinearEstimator(executionOperator, new int[]{0}, new int[0], true, optimizationSpace);
//        } else if (executionOperator instanceof SparkObjectFileSource) {
//            return createLinearEstimator(executionOperator, new int[]{}, new int[]{0}, true, optimizationSpace);
//        } else if (executionOperator instanceof SparkObjectFileSink) {
//            return createLinearEstimator(executionOperator, new int[]{0}, new int[0], true, optimizationSpace);
//        } else if (executionOperator instanceof SparkTsvFileSource) {
//            return createLinearEstimator(executionOperator, new int[]{}, new int[]{0}, true, optimizationSpace);
//        } else if (executionOperator instanceof SparkTsvFileSink) {
//            return createLinearEstimator(executionOperator, new int[]{0}, new int[0], true, optimizationSpace);
//
//            // Graph operators.
//        } else if (executionOperator instanceof SparkPageRankOperator) {
//            return createQuadraticEstimator(executionOperator, new int[]{0}, new int[]{0}, true, optimizationSpace);
//
//            // Conversion operators.
//        } else if (executionOperator instanceof SparkCollectOperator) {
//            return createLinearEstimator(executionOperator, new int[]{0}, new int[0], true, optimizationSpace);
//        } else if (executionOperator instanceof SparkCacheOperator) {
//            return createLinearEstimator(executionOperator, new int[]{}, new int[]{0}, true, optimizationSpace);
//        } else if (executionOperator instanceof SparkBroadcastOperator) {
//            return createLinearEstimator(executionOperator, new int[]{0}, new int[]{}, true, optimizationSpace);
//        }
//
//        // JdbcExecutionOperators.
//        else if (executionOperator instanceof JdbcTableSource) {
//            return createLinearEstimator(executionOperator, new int[]{}, new int[]{0}, true, optimizationSpace);
//        } else if (executionOperator instanceof JdbcFilterOperator) {
//            return createLinearEstimator(executionOperator, new int[]{}, new int[]{0}, false, optimizationSpace);
//        } else if (executionOperator instanceof JdbcProjectionOperator) {
//            return createLinearEstimator(executionOperator, new int[]{}, new int[]{0}, false, optimizationSpace);
//
//            // GraphChiExecutionOperators.
//        } else if (executionOperator instanceof GraphChiPageRankOperator) {
//            return createLinearEstimator(executionOperator, new int[]{0}, new int[]{0}, true, optimizationSpace);
//        }
//
//        // Otherwise, use heuristics.
//        System.out.printf("Creating load profile estimator for %s heuristically.\n", executionOperator);
//
//        return createSuitableEstimatorHeuristically(executionOperator, optimizationSpace, configuration);
//    }

//    private static DynamicLoadProfileEstimator createSuitableEstimatorHeuristically(
//            ExecutionOperator executionOperator,
//            OptimizationSpace optimizationSpace,
//            Configuration configuration) {
//        // First check if the configuration already provides an estimator.
//        final String key = RheemCollections.getSingle(executionOperator.getLoadProfileEstimatorConfigurationKeys());
//        final String juelSpec = configuration.getProperties().provideLocally(key);
//        if (juelSpec != null) {
//            return wrap(LoadProfileEstimators.createFromJuelSpecification(juelSpec));
//        }
//
//        // Special treatment of such unary operators that have a static number of output data quanta.
//        boolean isMapLike = (executionOperator.getNumInputs() == 1 && executionOperator.getNumOutputs() == 1) &&
//                (executionOperator instanceof MapOperator || executionOperator instanceof SortOperator
//                        || executionOperator instanceof SqlToStreamOperator || executionOperator instanceof JavaCollectOperator
//                        || executionOperator instanceof SparkCacheOperator || executionOperator instanceof SparkBroadcastOperator
//                        || executionOperator instanceof SparkCollectOperator || executionOperator instanceof ZipWithIdOperator);
//        boolean isGlobalReduction = (executionOperator.getNumInputs() == 1 && executionOperator.getNumOutputs() == 1) &&
//                (executionOperator instanceof CountOperator || executionOperator instanceof GlobalReduceOperator
//                        || executionOperator instanceof GlobalMaterializedGroupOperator);
//        if (isMapLike || isGlobalReduction) {
//            return createLinearEstimator(executionOperator, new int[]{0}, new int[0], true, optimizationSpace);
//        }
//
//        // Special treatment of such binay executionOperator that have a static number of output data quanta.
//        if (executionOperator instanceof UnionAllOperator || executionOperator instanceof CartesianOperator) {
//            return createLinearEstimator(executionOperator, new int[0], new int[]{0}, true, optimizationSpace);
//        }
//
//        // Special treatment of loop head operators.
//        if (executionOperator.isLoopHead()) {
//            return createLoopEstimator(executionOperator, optimizationSpace);
//        }
//
//        return createLinearEstimator(executionOperator, true, optimizationSpace);
//    }

//    /**
//     * Create a {@link DynamicLoadProfileEstimator} that is linear in the input and output (including some offset).
//     *
//     * @param executionOperator          the {@link ExecutionOperator} for that should be estimated
//     * @param optimizationSpace context for {@link Variable}s
//     * @param isWithOffset      whether to include an offset
//     * @return the {@link DynamicLoadProfileEstimator}
//     */
//    public static DynamicLoadProfileEstimator createLinearEstimator(ExecutionOperator executionOperator,
//                                                                    boolean isWithOffset,
//                                                                    OptimizationSpace optimizationSpace) {
//        return createLinearEstimator(
//                executionOperator,
//                RheemArrays.range(executionOperator.getNumInputs()),
//                RheemArrays.range(executionOperator.getNumOutputs()),
//                isWithOffset,
//                optimizationSpace
//        );
//    }

//    /**
//     * Create a {@link DynamicLoadProfileEstimator} that is linear in the input and output (including some offset).
//     *
//     * @param executionOperator          the {@link ExecutionOperator} for that should be estimated
//     * @param inputIndices      indices of {@link InputSlot}s for which {@link Variable}s should be created
//     * @param outputIndices     indices of {@link OutputSlot}s for which {@link Variable}s should be created
//     * @param isWithOffset      whether to include an offset
//     * @param optimizationSpace context for {@link Variable}s
//     * @return the {@link DynamicLoadProfileEstimator}
//     */
//    public static DynamicLoadProfileEstimator createLinearEstimator(ExecutionOperator executionOperator,
//                                                                    int[] inputIndices,
//                                                                    int[] outputIndices,
//                                                                    boolean isWithOffset,
//                                                                    OptimizationSpace optimizationSpace) {
//        // Create variables.
//        Variable[] inVars = new Variable[inputIndices.length];
//        for (int i = 0; i < inputIndices.length; i++) {
//            int index = inputIndices[i];
//            inVars[i] = optimizationSpace.getOrCreateVariable(
//                    RheemCollections.getSingle(executionOperator.getLoadProfileEstimatorConfigurationKeys()) + "->" + executionOperator.getInput(index).getName()
//            );
//        }
//        Variable[] outVars = new Variable[outputIndices.length];
//        for (int i = 0; i < outputIndices.length; i++) {
//            int index = outputIndices[i];
//            outVars[i] = optimizationSpace.getOrCreateVariable(
//                    RheemCollections.getSingle(executionOperator.getLoadProfileEstimatorConfigurationKeys()) + "->" + executionOperator.getOutput(index).getName()
//            );
//        }
//        Variable offsetVar = isWithOffset ? optimizationSpace.getOrCreateVariable(
//                RheemCollections.getSingle(executionOperator.getLoadProfileEstimatorConfigurationKeys()) + "->offset"
//        ) : null;
//
//        // Create the estimation function.
//        final DynamicLoadEstimator.SinglePointEstimator singlePointEstimator = (individual, in, out) -> {
//            double accu = isWithOffset ? offsetVar.getValue(individual) : 0d;
//            for (int i = 0; i < inputIndices.length; i++) {
//                accu += inVars[i].getValue(individual) * in[inputIndices[i]];
//            }
//            for (int i = 0; i < outputIndices.length; i++) {
//                accu += outVars[i].getValue(individual) * out[outputIndices[i]];
//            }
//            return accu;
//        };
//
//        // Create the JUEL template.
//        StringBuilder sb = new StringBuilder().append("${");
//        for (int i = 0; i < inputIndices.length; i++) {
//            sb.append("%s*in").append(inputIndices[i]).append(" + ");
//        }
//        for (int i = 0; i < outputIndices.length; i++) {
//            sb.append("%s*out").append(outputIndices[i]).append(" + ");
//        }
//        if (isWithOffset) {
//            sb.append("%s}");
//        } else {
//            sb.setLength(sb.length() - " + ".length());
//            sb.append("}");
//        }
//        String juelTemplate = sb.toString();
//
//        // Gather the employed variables.
//        Collection<Variable> employedVariables = new LinkedList<>();
//        employedVariables.addAll(Arrays.asList(inVars));
//        employedVariables.addAll(Arrays.asList(outVars));
//        if (isWithOffset) employedVariables.add(offsetVar);
//
//        // Assemble the estimator.
//        return new DynamicLoadProfileEstimator(
//                RheemCollections.getSingle(executionOperator.getLoadProfileEstimatorConfigurationKeys()),
//                executionOperator.getNumInputs(),
//                executionOperator.getNumOutputs(),
//                new DynamicLoadEstimator(singlePointEstimator, juelTemplate, employedVariables)
//        );
//    }

//    /**
//     * Create a {@link DynamicLoadProfileEstimator} that is linear in the input and output (including some offset).
//     *
//     * @param executionOperator          the {@link ExecutionOperator} for that should be estimated
//     * @param inputIndices      indices of {@link InputSlot}s for which {@link Variable}s should be created
//     * @param outputIndices     indices of {@link OutputSlot}s for which {@link Variable}s should be created
//     * @param isWithOffset      whether to include an offset
//     * @param optimizationSpace context for {@link Variable}s
//     * @return the {@link DynamicLoadProfileEstimator}
//     */
//    public static DynamicLoadProfileEstimator createQuadraticEstimator(ExecutionOperator executionOperator,
//                                                                       int[] inputIndices,
//                                                                       int[] outputIndices,
//                                                                       boolean isWithOffset,
//                                                                       OptimizationSpace optimizationSpace) {
//        // Create variables.
//        Variable[] linearInVars = new Variable[inputIndices.length];
//        Variable[] quadraticInVars = new Variable[inputIndices.length];
//        for (int i = 0; i < inputIndices.length; i++) {
//            int index = inputIndices[i];
//            linearInVars[i] = optimizationSpace.getOrCreateVariable(
//                    RheemCollections.getSingle(executionOperator.getLoadProfileEstimatorConfigurationKeys()) + "->" + executionOperator.getInput(index).getName()
//            );
//            quadraticInVars[i] = optimizationSpace.getOrCreateVariable(
//                    RheemCollections.getSingle(executionOperator.getLoadProfileEstimatorConfigurationKeys()) + "->" + executionOperator.getInput(index).getName() + "^2"
//            );
//        }
//        Variable[] linearOutVars = new Variable[outputIndices.length];
//        Variable[] quadraticOutVars = new Variable[outputIndices.length];
//        for (int i = 0; i < outputIndices.length; i++) {
//            int index = outputIndices[i];
//            linearOutVars[i] = optimizationSpace.getOrCreateVariable(
//                    RheemCollections.getSingle(executionOperator.getLoadProfileEstimatorConfigurationKeys()) + "->" + executionOperator.getOutput(index).getName()
//            );
//            quadraticOutVars[i] = optimizationSpace.getOrCreateVariable(
//                    RheemCollections.getSingle(executionOperator.getLoadProfileEstimatorConfigurationKeys()) + "->" + executionOperator.getOutput(index).getName() + "^2"
//            );
//        }
//
//        Variable offsetVar = isWithOffset ? optimizationSpace.getOrCreateVariable(
//                RheemCollections.getSingle(executionOperator.getLoadProfileEstimatorConfigurationKeys()) + "->offset"
//        ) : null;
//
//        // Create the estimation function.
//        final DynamicLoadEstimator.SinglePointEstimator singlePointEstimator = (individual, in, out) -> {
//            double accu = isWithOffset ? offsetVar.getValue(individual) : 0d;
//            for (int i = 0; i < inputIndices.length; i++) {
//                accu += linearInVars[i].getValue(individual) * in[inputIndices[i]]
//                        + quadraticInVars[i].getValue(individual) * in[inputIndices[i]] * in[inputIndices[i]];
//            }
//            for (int i = 0; i < outputIndices.length; i++) {
//                accu += linearOutVars[i].getValue(individual) * out[outputIndices[i]]
//                        + quadraticOutVars[i].getValue(individual) * out[outputIndices[i]] * out[outputIndices[i]];
//            }
//            return accu;
//        };
//
//        // Create the JUEL template.
//        StringBuilder sb = new StringBuilder().append("${");
//        for (int i = 0; i < inputIndices.length; i++) {
//            sb.append("%s*in").append(inputIndices[i]).append(" + ")
//                    .append("%s*in").append(inputIndices[i]).append("*in").append(inputIndices[i]).append(" + ");
//        }
//        for (int i = 0; i < outputIndices.length; i++) {
//            sb.append("%s*out").append(outputIndices[i]).append(" + ")
//                    .append("%s*out").append(inputIndices[i]).append("*out").append(inputIndices[i]).append(" + ");
//
//        }
//        if (isWithOffset) {
//            sb.append("%s}");
//        } else {
//            sb.setLength(sb.length() - " + ".length());
//            sb.append("}");
//        }
//        String juelTemplate = sb.toString();
//
//        // Gather the employed variables.
//        Collection<Variable> employedVariables = new LinkedList<>();
//        for (int i = 0; i < linearInVars.length; i++) {
//            employedVariables.add(linearInVars[i]);
//            employedVariables.add(quadraticInVars[i]);
//        }
//        for (int i = 0; i < linearOutVars.length; i++) {
//            employedVariables.add(linearOutVars[i]);
//            employedVariables.add(quadraticOutVars[i]);
//        }
//        employedVariables.addAll(Arrays.asList(linearInVars));
//
//        // Assemble the estimator.
//        return new DynamicLoadProfileEstimator(
//                RheemCollections.getSingle(executionOperator.getLoadProfileEstimatorConfigurationKeys()),
//                executionOperator.getNumInputs(),
//                executionOperator.getNumOutputs(),
//                new DynamicLoadEstimator(singlePointEstimator, juelTemplate, employedVariables)
//        );
//    }

//    /**
//     * Create a {@link DynamicLoadProfileEstimator} that is specifically adapted to {@link LoopHeadOperator}s.
//     *
//     * @param executionOperator          the {@link ExecutionOperator} for that should be estimated
//     * @param optimizationSpace context for {@link Variable}s
//     * @return the {@link DynamicLoadProfileEstimator}
//     */
//    public static DynamicLoadProfileEstimator createLoopEstimator(ExecutionOperator executionOperator,
//                                                                  OptimizationSpace optimizationSpace) {
//        assert executionOperator.isLoopHead();
//
//        int[] mainInputIndices;
//        int[] convergenceInputIndices;
//
//        if (executionOperator instanceof LoopOperator) {
//            mainInputIndices = new int[]{LoopOperator.INITIAL_INPUT_INDEX, LoopOperator.ITERATION_INPUT_INDEX};
//            convergenceInputIndices = new int[]{LoopOperator.INITIAL_CONVERGENCE_INPUT_INDEX, LoopOperator.ITERATION_CONVERGENCE_INPUT_INDEX};
//        } else if (executionOperator instanceof DoWhileOperator) {
//            mainInputIndices = new int[]{DoWhileOperator.INITIAL_INPUT_INDEX, DoWhileOperator.ITERATION_INPUT_INDEX};
//            convergenceInputIndices = new int[]{DoWhileOperator.CONVERGENCE_INPUT_INDEX};
//        } else if (executionOperator instanceof RepeatOperator) {
//            mainInputIndices = new int[]{RepeatOperator.INITIAL_INPUT_INDEX, RepeatOperator.ITERATION_INPUT_INDEX};
//            convergenceInputIndices = new int[0];
//        } else {
//            throw new IllegalArgumentException("Unsupported loop executionOperator: " + executionOperator);
//        }
//
//        // Create variables.
//        Variable mainVar = optimizationSpace.getOrCreateVariable(
//                RheemCollections.getSingle(executionOperator.getLoadProfileEstimatorConfigurationKeys()) + "->main"
//        );
//        Variable convergenceVar = convergenceInputIndices.length > 0 ?
//                optimizationSpace.getOrCreateVariable(
//                        RheemCollections.getSingle(executionOperator.getLoadProfileEstimatorConfigurationKeys()) + "->convergence"
//                ) :
//                null;
//        Variable offsetVar = optimizationSpace.getOrCreateVariable(
//                RheemCollections.getSingle(executionOperator.getLoadProfileEstimatorConfigurationKeys()) + "->offset"
//        );
//
//        // Create the estimation function.
//        final DynamicLoadEstimator.SinglePointEstimator singlePointEstimator = (individual, in, out) -> {
//            double accu = offsetVar.getValue(individual);
//            for (int inputIndex : mainInputIndices) {
//                accu += in[inputIndex] * mainVar.getValue(individual);
//            }
//            for (int inputIndex : convergenceInputIndices) {
//                accu += in[inputIndex] * convergenceVar.getValue(individual);
//            }
//            return accu;
//        };
//
//        // Create the JUEL template.
//        StringBuilder sb = new StringBuilder().append("${%s*(");
//        String separator = "";
//        for (int i = 0; i < mainInputIndices.length; i++) {
//            sb.append(separator).append("in").append(mainInputIndices[i]);
//            separator = " + ";
//        }
//        sb.append(") + ");
//        if (convergenceVar != null) {
//            sb.append("%s*(");
//            separator = "";
//            for (int i = 0; i < convergenceInputIndices.length; i++) {
//                sb.append(separator).append("in").append(convergenceInputIndices[i]);
//                separator = " + ";
//            }
//            sb.append(") + ");
//        }
//        sb.append("%s}");
//        String juelTemplate = sb.toString();
//
//        // Gather the employed variables.
//        Collection<Variable> employedVariables = new LinkedList<>();
//        employedVariables.add(mainVar);
//        if (convergenceVar != null) employedVariables.add(convergenceVar);
//        employedVariables.add(offsetVar);
//
//        // Assemble the estimator.
//        return new DynamicLoadProfileEstimator(
//                executionOperator.getLoadProfileEstimatorConfigurationKey(),
//                executionOperator.getNumInputs(),
//                executionOperator.getNumOutputs(),
//                new DynamicLoadEstimator(singlePointEstimator, juelTemplate, employedVariables)
//        );
//    }

    /**
     * Exposes a {@link LoadProfileEstimator} for {@link ExecutionOperator}s as a {@link DynamicLoadProfileEstimator}.
     *
     * @param loadProfileEstimator the {@link LoadProfileEstimator} or {@code null}
     * @return the {@link DynamicLoadProfileEstimator} or {@code null} if {@code loadProfileEstimator} is {@code null}
     */
    public static DynamicLoadProfileEstimator wrap(LoadProfileEstimator loadProfileEstimator) {
        return new DynamicLoadProfileEstimator(null, -1, -1, DynamicLoadEstimator.zeroLoad) {
            @Override
            public LoadProfile estimate(EstimationContext context) {
                return loadProfileEstimator.estimate(context);
            }

            @Override
            public Collection<Variable> getEmployedVariables() {
                return Collections.emptyList();
            }
        };
    }

    /**
     * Exposes a {@link LoadEstimator} for {@link ExecutionOperator}s as a {@link DynamicLoadEstimator} with the
     * caveat that the {@link ExecutionOperator} will not be available in the estimation process.
     *
     * @param loadEstimator the {@link LoadEstimator} or {@code null}
     * @return the {@link DynamicLoadEstimator} or {@code null} if {@code loadEstimator} is {@code null}
     */
    public static DynamicLoadEstimator wrap(LoadEstimator loadEstimator) {
        if (loadEstimator == null) return null;
        return new DynamicLoadEstimator(null, null, Collections.emptySet()) {
            @Override
            public LoadEstimate calculate(EstimationContext estimationContext) {
                return loadEstimator.calculate(estimationContext);
            }
        };
    }

    /**
     * Creates a new instance from a template {@link String}. Valid specifications are as follows:
     * <pre>
     *     {"type":&lt;*org.qcri.rheem.core.util.mathex.mathex*&gt;,
     *      "cpu":&lt;mathematical expression&gt;,
     *      "ram":&lt;mathematical expression&gt;,
     *      "disk":&lt;mathematical expression&gt;,
     *      "network":&lt;mathematical expression&gt;,
     *      "in":&lt;#inputs&gt;,
     *      "out":&lt;#outputs&gt;,
     *      "overhead":&lt;overhead in milliseconds&gt;,
     *      "ru":&lt;resource utilization mathematical expression&gt;
     *      }
     * </pre>
     * The JUEL expressions accept as parameters {@code in0}, {@code in1} a.s.o. for the input cardinalities and
     * {@code out0}, {@code out1} a.s.o. for the output cardinalities.
     *
     * @param configKey         the {@link Configuration} from that the {@code spec} was retrieved or else {@code null}
     * @param specification     a specification that adheres to above format
     * @param optimizationSpace maintains {@link Variable}s imposed by the {@code spec}
     * @return the new instance
     */
    public static DynamicLoadProfileEstimator createFromTemplate(String configKey,
                                                                 String specification,
                                                                 OptimizationSpace optimizationSpace) {
        try {
            final JSONObject spec = new JSONObject(specification);
            if (!spec.has("type") || "mathex".equalsIgnoreCase(spec.getString("type"))) {
                return createFromMathExTemplate(configKey, spec, optimizationSpace);
            } else if ("juel".equalsIgnoreCase(spec.getString("type"))) {
                throw new IllegalStateException("JUEL templates not supported");
            } else {
                throw new RheemException(String.format("Unknown specification type: %s", spec.get("type")));
            }
        } catch (Exception e) {
            throw new RheemException(String.format("Could not initialize from specification \"%s\".", specification), e);
        }
    }

    /**
     * Creates a {@link DynamicLoadProfileEstimator} from a template.
     *
     * @param configKey         the {@link Configuration} key of the template
     * @param spec              the template
     * @param optimizationSpace maintains {@link Variable}s imposed by the template
     * @return the {@link DynamicLoadEstimator}
     */
    private static DynamicLoadProfileEstimator createFromMathExTemplate(String configKey,
                                                                        JSONObject spec,
                                                                        OptimizationSpace optimizationSpace) {
        int numInputs = spec.getInt("in");
        int numOutputs = spec.getInt("out");

        DynamicLoadEstimator cpuEstimator =
                DynamicLoadEstimator.createFor(configKey, "cpu", spec.getString("cpu"), optimizationSpace);
//        DynamicLoadEstimator ramEstimator =
//                DynamicLoadEstimator.createFor(configKey, "ram", spec.getString("ram"), optimizationSpace);
        DynamicLoadEstimator diskEstimator = !spec.has("disk") ?
                DynamicLoadEstimator.zeroLoad :
                DynamicLoadEstimator.createFor(configKey, "disk", spec.getString("disk"), optimizationSpace);
        DynamicLoadEstimator networkEstimator = !spec.has("network") ?
                DynamicLoadEstimator.zeroLoad :
                DynamicLoadEstimator.createFor(configKey, "network", spec.getString("network"), optimizationSpace);

        if (spec.has("overhead")) {
            logger.warn("Overhead specification in {} will be ignored.", configKey);
        }
        if (spec.has("ru")) {
            logger.warn("Resource utilization specification will be ignored.", configKey);
        }

        return new DynamicLoadProfileEstimator(configKey, numInputs, numOutputs, cpuEstimator, diskEstimator, networkEstimator);

    }


}
