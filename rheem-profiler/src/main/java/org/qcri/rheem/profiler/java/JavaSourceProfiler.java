package org.qcri.rheem.profiler.java;

import org.apache.commons.lang3.Validate;
import org.qcri.rheem.core.plan.rheemplan.Operator;
import org.qcri.rheem.java.channels.JavaChannelInstance;
import org.qcri.rheem.java.operators.JavaExecutionOperator;
import org.qcri.rheem.java.operators.JavaTextFileSource;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * Created by migiwara on 11/06/17.
 */
public abstract class JavaSourceProfiler extends JavaOperatorProfiler{

    private JavaChannelInstance outputChannelInstance;

    public String getFileUrl() {
        return fileUrl;
    }

    private String fileUrl;

    public JavaSourceProfiler(Supplier<JavaExecutionOperator> operatorGenerator, Supplier<?>... dataQuantumGenerators) {
        super(operatorGenerator, dataQuantumGenerators);
    }

    @Override
    public void prepare(long dataQuantaSize,long... inputCardinalities) {
        Validate.isTrue(inputCardinalities.length == 1);

        try {
            //old setup
            //this.setUpSourceData(inputCardinalities[0]);
            this.prepareInput(0,dataQuantaSize,inputCardinalities[0]);
        } catch (Exception e) {
            LoggerFactory.getLogger(this.getClass()).error(
                    String.format("Failed to set up source data for input cardinality %d.", inputCardinalities[0]),
                    e
            );
        }

        super.prepare(dataQuantaSize, inputCardinalities);

        // Channel creation separation between operators that requires Collection channels vs Stream channels for the evaluation.
        List operatorsWithCollectionInput = new ArrayList<Class<Operator>>();

        // List of operators requiring collection channels.
        operatorsWithCollectionInput.addAll(Arrays.asList(JavaTextFileSource.class));

        if (operatorsWithCollectionInput.contains(this.executionOperator.getClass())) {
            this.outputChannelInstance = createChannelInstance();
        } else {
            this.outputChannelInstance = createCollectionChannelInstance();
        }
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    @Override
    protected long executeOperator() {
        this.evaluate(
                new JavaChannelInstance[]{},
                new JavaChannelInstance[]{this.outputChannelInstance}
        );
        return this.outputChannelInstance.provideStream().count();
    }

    public abstract void setUpSourceData(long cardinality) throws Exception;

    public abstract void clearSourceData();


}
