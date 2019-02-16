package org.qcri.rheem.profiler.java;

import org.qcri.rheem.core.api.Configuration;
import org.qcri.rheem.core.optimizer.DefaultOptimizationContext;
import org.qcri.rheem.core.optimizer.OptimizationContext;
import org.qcri.rheem.core.plan.executionplan.Channel;
import org.qcri.rheem.core.platform.ChannelDescriptor;
import org.qcri.rheem.core.platform.ChannelInstance;
import org.qcri.rheem.core.util.RheemArrays;
import org.qcri.rheem.core.util.RheemCollections;
import org.qcri.rheem.java.channels.CollectionChannel;
import org.qcri.rheem.java.channels.StreamChannel;
import org.qcri.rheem.java.execution.JavaExecutor;
import org.qcri.rheem.java.operators.JavaExecutionOperator;
import org.qcri.rheem.core.optimizer.mloptimizer.api.OperatorProfiler;
import org.qcri.rheem.profiler.util.ProfilingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * Created by migiwara on 11/06/17.
 */
public abstract class JavaOperatorProfiler extends OperatorProfiler {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    public int cpuMhz;

    //protected Supplier<JavaExecutionOperator> executionOperatorGenerator;

    //protected JavaExecutionOperator executionOperator;

    protected JavaExecutor executor;

    //protected List<Supplier<?>> dataQuantumGenerators;

    private List<Long> inputCardinalities;

    private long dataQuantaSize;

    private int udfComplexity;
    public JavaOperatorProfiler(){
        super();
    }

    public JavaOperatorProfiler(Supplier<JavaExecutionOperator> operatorGenerator,
                            Supplier<?>... dataQuantumGenerators) {
        super(operatorGenerator, dataQuantumGenerators);
        this.executionOperatorGenerator = operatorGenerator;
        this.executionOperator = operatorGenerator.get();
        this.dataQuantumGenerators = Arrays.asList(dataQuantumGenerators);

        // No need for fake job  in the case of plan generation
        // Should be enabled for single executionOperator profiling
        //this.executor = ProfilingUtils.fakeJavaExecutor();
        this.cpuMhz = Integer.parseInt(System.getProperty("rheem.java.cpu.mhz", "2700"));
    }


    public void prepare( long dataQuantaSize,long... inputCardinalities) {
        this.executionOperator = this.executionOperatorGenerator.get();
        this.inputCardinalities = RheemArrays.asList(inputCardinalities);
        this.dataQuantaSize = dataQuantaSize;
        //this.dataQuantaSize = udfComplexity;
    }


    /**
     * Executes and profiles the profiling task. Requires that this instance is prepared.
     */
    public OperatorProfiler.Result run() {
        final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        threadMXBean.setThreadCpuTimeEnabled(true);
        ProfilingUtils.sleep(1000);
        long startCpuTime = threadMXBean.getCurrentThreadCpuTime();
        final long outputCardinality = this.executeOperator();
        long endCpuTime = threadMXBean.getCurrentThreadCpuTime();

        long cpuCycles = this.calculateCpuCycles(startCpuTime, endCpuTime);
        return new OperatorProfiler.Result(
                this.inputCardinalities,
                outputCardinality,
                this.provideDiskBytes(),
                this.provideNetworkBytes(),
                cpuCycles,
                this.dataQuantaSize,1,4);
    }

    private long calculateCpuCycles(long startNanos, long endNanos) {
        long passedNanos = endNanos - startNanos;
        double cyclesPerNano = (this.cpuMhz * 1e6) / 1e9;
        return Math.round(cyclesPerNano * passedNanos);
    }

    protected long provideNetworkBytes() {
        return 0L;
    }

    protected long provideDiskBytes() {
        return 0L;
    }

    /**
     * Executes the profiling task. Requires that this instance is prepared.
     */
    protected abstract long executeOperator();

    protected static StreamChannel.Instance createChannelInstance(final Collection<?> collection) {
        final StreamChannel.Instance channelInstance = createChannelInstance();
        channelInstance.accept(collection);
        return channelInstance;
    }

    protected static StreamChannel.Instance createChannelInstance() {
        final ChannelDescriptor channelDescriptor = StreamChannel.DESCRIPTOR;
        final Channel channel = channelDescriptor.createChannel(null, new Configuration());
        return (StreamChannel.Instance) channel.createInstance(null, null, -1);
    }

    protected static CollectionChannel.Instance createCollectionChannelInstance(final Collection<?> collection) {
        final CollectionChannel.Instance channelInstance = createCollectionChannelInstance();
        channelInstance.accept(collection);
        return channelInstance;
    }

    protected static CollectionChannel.Instance createCollectionChannelInstance() {
        final ChannelDescriptor channelDescriptor = CollectionChannel.DESCRIPTOR;
        final Channel channel = channelDescriptor.createChannel(null, new Configuration());
        return (CollectionChannel.Instance) channel.createInstance(null, null, -1);
    }

//    public JavaExecutionOperator getExecutionOperator() {
//        return this.JavaOperator;
//    }



    /**
     * Utility method to invoke
     * {@link JavaExecutionOperator#evaluate(ChannelInstance[], ChannelInstance[], JavaExecutor, OptimizationContext.OperatorContext)}.
     */
    protected void evaluate(ChannelInstance[] inputs,
                            ChannelInstance[] outputs) {
        OptimizationContext optimizationContext = new DefaultOptimizationContext(this.executor.getJob());
        final OptimizationContext.OperatorContext operatorContext = optimizationContext.addOneTimeOperator(executionOperator);
        JavaExecutionOperator javaOperator = (JavaExecutionOperator) executionOperator;
        javaOperator.evaluate(inputs, outputs, this.executor, operatorContext);
    }

    /**
     * The result of a single profiling run.
     */
    public static class Result {

        private final List<Long> inputCardinalities;

        private final long outputCardinality;

        private final long diskBytes, networkBytes;

        private final long cpuCycles;

        private final long dataQuantaSize;

        private int udfComplexity;

        public Result(List<Long> inputCardinalities, long outputCardinality, long diskBytes, long networkBytes,
                      long cpuCycles, long dataQuantaSize) {
            this.inputCardinalities = inputCardinalities;
            this.outputCardinality = outputCardinality;
            this.diskBytes = diskBytes;
            this.networkBytes = networkBytes;
            this.cpuCycles = cpuCycles;
            this.dataQuantaSize = dataQuantaSize;
            //this.udfComplexity = udfComplexity;
        }

        public List<Long> getInputCardinalities() {
            return this.inputCardinalities;
        }

        public long getOutputCardinality() {
            return this.outputCardinality;
        }

        public long getDiskBytes() {
            return this.diskBytes;
        }

        public long getNetworkBytes() {
            return this.networkBytes;
        }

        public long getCpuCycles() {
            return this.cpuCycles;
        }

        public void setUdfComplexity(int udfComplexity) {
            this.udfComplexity = udfComplexity;
        }

        @Override
        public String toString() {
            return "Result{" +
                    "inputCardinalities=" + this.inputCardinalities +
                    ", outputCardinality=" + this.outputCardinality +
                    ", diskBytes=" + this.diskBytes +
                    ", networkBytes=" + this.networkBytes +
                    ", cpuCycles=" + this.cpuCycles +
                    '}';
        }

        public String getCsvHeader() {
            return String.join(",", RheemCollections.map(this.inputCardinalities, (index, card) -> "input_card_" + index)) + "," +
                    "output_card," +
                    "disk," +
                    "network," +
                    "cpu," +
                    "dataQuanta_size," +
                    "UDF_complexity";
        }

        public String toCsvString() {
            return String.join(",", RheemCollections.map(this.inputCardinalities, Object::toString)) + ","
                    + this.outputCardinality + ","
                    + this.diskBytes + ","
                    + this.networkBytes + ","
                    + this.cpuCycles + ","
                    + this.dataQuantaSize + ","
                    + this.udfComplexity;
        }
    }

}
