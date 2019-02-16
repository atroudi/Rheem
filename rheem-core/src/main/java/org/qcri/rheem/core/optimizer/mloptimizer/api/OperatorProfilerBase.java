package org.qcri.rheem.core.optimizer.mloptimizer.api;

import org.qcri.rheem.core.api.Configuration;
import org.qcri.rheem.core.api.exception.RheemException;
import org.qcri.rheem.core.plan.rheemplan.ExecutionOperator;
import org.qcri.rheem.core.plan.rheemplan.Operator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * Basic implementation of {@link OperatorProfiler}
 */
public class OperatorProfilerBase extends OperatorProfiler {


    private static List<Tuple2> createdData = new ArrayList<>();

    private String fileUrl;

    Operator rheemOperator;
    /**
     *
     * @param operator
     */
    public OperatorProfilerBase(Operator operator){
        this.rheemOperator = operator;
    }


    public OperatorProfilerBase(Supplier<ExecutionOperator> executionOperatorGenerator,
                                 Configuration configuration,
                                 Supplier<?>... dataQuantumGenerators) {
        this.executionOperatorGenerator = executionOperatorGenerator;
        this.executionOperator = this.executionOperatorGenerator.get();
        this.dataQuantumGenerators = Arrays.asList(dataQuantumGenerators);
        this.fileUrl = configuration.getStringProperty("rheem.profiler.logs.syntheticDataURL.prefix");
    }

    @Override
    protected void prepareInput(int inputIndex, long dataQuantaSize, long inputCardinality) {

        assert inputIndex == 0;
        File file = new File(this.fileUrl+"-"+dataQuantaSize+"-"+inputCardinality+".txt");

        //System.out.printf("[PROFILING] Input data already exist and read: %b \n",file.canRead());
        this.logger.info(String.format("[PROFILING] can read input url: %b \n",file.canRead()));

        Tuple2 newData = new Tuple2(inputCardinality,dataQuantaSize);
        // check if input data is already created
        if(createdData.contains(newData)||file.exists()||true)
            return;

        // add new data
        createdData.add(newData);
        try {
            final File parentFile = file.getParentFile();
            if (!parentFile.exists() && !file.getParentFile().mkdirs()) {
                throw new RheemException("Could not initialize log repository.");
            }
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false), "UTF-8"));

            final Supplier<?> supplier = this.dataQuantumGenerators.get(0);

            for (long i = 0; i < inputCardinality; i++) {
                String tmp= supplier.get().toString();
                writer.write(tmp);
                writer.write('\n');
            }
            writer.flush();
            writer.close();
        } catch (RheemException e) {
            throw e;
        } catch (Exception e) {
            throw new RheemException(String.format("Cannot write to %s.", this.fileUrl), e);
        }
    }


    public Operator getRheemOperator() {
        return rheemOperator;
    }

    public void setRheemOperator(Operator rheemOperator) {
        this.rheemOperator = rheemOperator;
    }
}
