package org.qcri.rheem.profiler.spark;

import org.apache.spark.api.java.JavaRDD;
import org.qcri.rheem.core.api.Configuration;
import org.qcri.rheem.core.plan.executionplan.Channel;
import org.qcri.rheem.core.plan.rheemplan.Operator;
import org.qcri.rheem.core.platform.ChannelDescriptor;
import org.qcri.rheem.core.platform.ChannelInstance;
import org.qcri.rheem.java.channels.CollectionChannel;
import org.qcri.rheem.profiler.util.ProfilingUtils;
import org.qcri.rheem.spark.channels.RddChannel;
import org.qcri.rheem.spark.operators.SparkCountOperator;
import org.qcri.rheem.spark.operators.SparkExecutionOperator;
import org.qcri.rheem.spark.operators.SparkGlobalReduceOperator;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * {@link SparkOperatorProfiler} implementation for {@link SparkExecutionOperator}s with one input and one output.
 */
public class SparkUnaryOperatorProfiler extends SparkOperatorProfiler {

    private JavaRDD<?> inputRdd;

    // Channel creation separation between operators that requires Collection channels vs Stream channels for the evaluation.
    final private List<Class<? extends Operator>> operatorsWithCollectionOutput =  Arrays.asList(SparkCountOperator.class,SparkGlobalReduceOperator.class);

    public SparkUnaryOperatorProfiler(Supplier<SparkExecutionOperator> operatorGenerator,
                                      Configuration configuration,
                                      Supplier<?> dataQuantumGenerator) {
        super(operatorGenerator, configuration, dataQuantumGenerator);
    }

    @Override
    protected void prepareInput(int inputIndex, long dataQuantaSize, long inputCardinality) {
        assert inputIndex == 0;
        this.inputRdd = this.prepareInputRdd(inputCardinality, inputIndex);
    }

    @Override
    protected Result executeOperator() {
        final RddChannel.Instance inputChannelInstance = createChannelInstance(this.inputRdd, this.sparkExecutor);
         //RddChannel.Instance outputChannelInstance = createChannelInstance(this.sparkExecutor);

        if (operatorsWithCollectionOutput.isEmpty())
            operatorsWithCollectionOutput.addAll(Arrays.asList(SparkCountOperator.class,SparkGlobalReduceOperator.class));

        RddChannel.Instance outputChannelInstance = createChannelInstance(this.sparkExecutor);

        // Check if the executionOperator needs execution with output collection channel
        if (operatorsWithCollectionOutput.contains(this.executionOperator.getClass())){
            // Create an output collection channel
            final ChannelDescriptor channelDescriptor = CollectionChannel.DESCRIPTOR;
            final Channel channel = channelDescriptor.createChannel(null, new Configuration());
            final CollectionChannel.Instance channelInstance = (CollectionChannel.Instance) channel.createInstance(null, null, -1);

            channelInstance.accept(this.inputCardinalities);
            CollectionChannel.Instance outputCollectionChannelInstance = channelInstance;
            // Execute Operator
            return executeOperatorWithCollectionOutput(inputChannelInstance,outputCollectionChannelInstance);
        }

        // Let the executionOperator execute.
        ProfilingUtils.sleep(this.executionPaddingTime); // Pad measurement with some idle time.
        final long startTime = System.currentTimeMillis();
        this.evaluate(
                (SparkExecutionOperator) this.executionOperator,
                new ChannelInstance[]{inputChannelInstance},
                new ChannelInstance[]{outputChannelInstance}
        );

        // Force the execution of the executionOperator.
        outputChannelInstance.provideRdd().foreach(dataQuantum -> {
        });
        final long endTime = System.currentTimeMillis();
        ProfilingUtils.sleep(this.executionPaddingTime); // Pad measurement with some idle time.

        // Yet another run to count the output cardinality.
        final long outputCardinality = outputChannelInstance.provideRdd().count();

        // Gather and assemble all result metrics.
        return new Result(
                this.inputCardinalities,
                outputCardinality,
                endTime - startTime,
                this.provideDiskBytes(startTime, endTime),
                this.provideNetworkBytes(startTime, endTime),
                this.provideCpuCycles(startTime, endTime),
                this.numMachines,
                this.numCoresPerMachine
        );
    }

    protected Result executeOperatorWithCollectionOutput(RddChannel.Instance inputChannelInstance,CollectionChannel.Instance outputCollectionChannelInstance) {
        // Let the executionOperator execute.
        ProfilingUtils.sleep(this.executionPaddingTime); // Pad measurement with some idle time.
        final long startTime = System.currentTimeMillis();
        this.evaluate(
                (SparkExecutionOperator) this.executionOperator,
                new ChannelInstance[]{inputChannelInstance},
                new ChannelInstance[]{outputCollectionChannelInstance}
        );

        final long endTime = System.currentTimeMillis();
        ProfilingUtils.sleep(this.executionPaddingTime); // Pad measurement with some idle time.

        final long outputCardinality = outputCollectionChannelInstance.provideStream().count();

        // Gather and assemble all result metrics.
        return new Result(
                this.inputCardinalities,
                outputCardinality,
                endTime - startTime,
                this.provideDiskBytes(startTime, endTime),
                this.provideNetworkBytes(startTime, endTime),
                this.provideCpuCycles(startTime, endTime),
                this.numMachines,
                this.numCoresPerMachine
        );
    }
}
