package org.qcri.rheem.core.optimizer.mloptimizer.api;

import org.qcri.rheem.core.api.exception.RheemException;
import org.qcri.rheem.core.plan.rheemplan.ExecutionOperator;
import org.qcri.rheem.core.plan.rheemplan.Operator;
import org.qcri.rheem.core.platform.Executor;
import org.qcri.rheem.core.platform.Platform;
import org.qcri.rheem.core.util.RheemCollections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * Allows to instrument an {@link ExecutionOperator}.
 */
public abstract class OperatorProfiler {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    public int cpuMhz;

    protected Supplier<? extends ExecutionOperator> executionOperatorGenerator;

    protected Supplier<? extends Operator> operatorGenerator;

    public Operator getOperator() {
        if(this.operator !=null)
            return operator;
        else {
            throw new RheemException("no profiling Execution Opertor nor Operator found!");
        }
    }

    public ExecutionOperator getExecutionOperator() {
        return executionOperator;
    }

    public void setExecutionOperator(ExecutionOperator executionOperator) {
        this.executionOperator = executionOperator;
    }

    protected ExecutionOperator executionOperator;

    protected Operator operator;

    protected Executor executor;

    protected List<Supplier<?>> dataQuantumGenerators;

    protected Platform platform;

    public int getUDFcomplexity() {
        return UDFcomplexity;
    }

    public void setUDFcomplexity(int UDFcomplexity) {
        this.UDFcomplexity = UDFcomplexity;
    }

    public void setDataQuantumGenerators(Supplier<?> dataQuantumGenerators) {
        this.dataQuantumGenerators = Arrays.asList(dataQuantumGenerators);
    }
    private int UDFcomplexity;

    public OperatorProfiler(){
    }

    public OperatorProfiler(Supplier<? extends ExecutionOperator> operatorGenerator,
                            Supplier<?>... dataQuantumGenerators) {
        this.platform = operatorGenerator.get().getPlatform();
        this.executionOperatorGenerator = operatorGenerator;
        this.dataQuantumGenerators = Arrays.asList(dataQuantumGenerators);
        // Assign the executionOperator
        this.executionOperator =this.executionOperatorGenerator.get();
        //this.executor = ProfilingUtils.fakeJavaExecutor();
        this.cpuMhz = Integer.parseInt(System.getProperty("rheem.java.cpu.mhz", "2700"));
    }

    public void prepare( long dq,long... inputCardinalities) {
        this.executionOperator = this.executionOperatorGenerator.get();
        //this.inputCardinalities = RheemArrays.asList(inputCardinalities);
        //this.sparkExecutor = ProfilingUtils.fakeSparkExecutor(ReflectionUtils.getDeclaringJar(SparkOperatorProfiler.class));
        for (int inputIndex = 0; inputIndex < inputCardinalities.length; inputIndex++) {
            long inputCardinality = inputCardinalities[inputIndex];
            this.prepareInput(inputIndex, dq, inputCardinality);
        }
    }


    //public void prepare( long... inputCardinalities) {
    //}

    public OperatorProfiler.Result run() {
        List<Long> inputCard = null;
        return  new Result( inputCard,0,0,0,0,0,
                0,0);
    }

    public void cleanUp() {
    }

    public static OperatorProfiler.Result averageResult(List<OperatorProfiler.Result> allResults){
        long diskBytes=0;
        long networkBytes=0;
        long cpuCycles=0;
        long wallclockMillis=0;
        long outputCardinality=0;
        for(OperatorProfiler.Result result:allResults){
            diskBytes+=result.getDiskBytes();
            networkBytes+=result.getNetworkBytes();
            cpuCycles+=result.getCpuCycles();
            wallclockMillis+=result.getWallclockMillis();
            outputCardinality+=result.getOutputCardinality();
        }
        diskBytes=diskBytes/allResults.size();
        networkBytes=networkBytes/allResults.size();
        cpuCycles=cpuCycles/allResults.size();
        wallclockMillis=wallclockMillis/allResults.size();
        outputCardinality=outputCardinality/allResults.size();

        return new OperatorProfiler.Result(allResults.get(0).getInputCardinalities(),outputCardinality,wallclockMillis,diskBytes,networkBytes,cpuCycles,allResults.get(0).getNumMachines(),
                allResults.get(0).getNumCoresPerMachine());
    }

    /**
     * The result of a single profiling run.
     */
    public static class Result {

        private final List<Long> inputCardinalities;

        public int getNumMachines() {
            return numMachines;
        }

        public int getNumCoresPerMachine() {
            return numCoresPerMachine;
        }

        private final int numMachines, numCoresPerMachine;

        private final long outputCardinality;

        private final long diskBytes, networkBytes;

        private final long cpuCycles;

        private final long wallclockMillis;

        private long dataQuantaSize;

        public long getWallclockMillis() {
            return wallclockMillis;
        }

        private int udfComplexity;

        public Result(List<Long> inputCardinalities, long outputCardinality,
                      long wallclockMillis, long diskBytes, long networkBytes, long cpuCycles,
                      int numMachines, int numCoresPerMachine) {
            this.inputCardinalities = inputCardinalities;
            this.outputCardinality = outputCardinality;
            this.wallclockMillis = wallclockMillis;
            this.diskBytes = diskBytes;
            this.networkBytes = networkBytes;
            this.cpuCycles = cpuCycles;
            this.numMachines = numMachines;
            this.numCoresPerMachine = numCoresPerMachine;
        }

        public void setDataQuantaSize(long dataQuantaSize) {
            this.dataQuantaSize = dataQuantaSize;
        }

        public void setUdfComplexity(int udfComplexity) {
            this.udfComplexity = udfComplexity;
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

        @Override
        public String toString() {
            return "Result{" +
                    "inputCardinalities=" + inputCardinalities +
                    ", outputCardinality=" + outputCardinality +
                    ", dataQuantaSize=" + dataQuantaSize+
                    ", udfComplexity=" + udfComplexity +
                    ", numMachines=" + numMachines +
                    ", numCoresPerMachine=" + numCoresPerMachine +
                    ", wallclockMillis=" + wallclockMillis +
                    ", cpuCycles=" + cpuCycles +
                    ", diskBytes=" + diskBytes +
                    ", networkBytes=" + networkBytes +
                    '}';
        }

        public String getCsvHeader() {
            return String.join(",", RheemCollections.map(this.inputCardinalities, (index, card) -> "input_card_" + index)) + "," +
                    "output_card," +
                    "dataQuantaSize," +
                    "udfComplexity," +
                    "wallclock," +
                    "disk," +
                    "network," +
                    "cpu," +
                    "machines," +
                    "cores_per_machine";
        }

        public String toCsvString() {
            return String.join(",", RheemCollections.map(this.inputCardinalities, Object::toString)) + ","
                    + this.outputCardinality + ","
                    + this.dataQuantaSize + ","
                    + this.udfComplexity + ","
                    + this.wallclockMillis + ","
                    + this.diskBytes + ","
                    + this.networkBytes + ","
                    + this.cpuCycles + ","
                    + this.numMachines + ","
                    + this.numCoresPerMachine;
        }
    }

    protected abstract void prepareInput(int inputIndex, long dataQuantaSize ,long inputCardinality);


}
