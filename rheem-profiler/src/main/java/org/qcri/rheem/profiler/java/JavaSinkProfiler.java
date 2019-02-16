package org.qcri.rheem.profiler.java;

import org.apache.commons.lang3.Validate;
import org.qcri.rheem.java.channels.JavaChannelInstance;
import org.qcri.rheem.java.operators.JavaExecutionOperator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

/**
 * Created by migiwara on 11/06/17.
 */
public class JavaSinkProfiler extends JavaOperatorProfiler {
    private JavaChannelInstance inputChannelInstance;

    public JavaSinkProfiler(Supplier<JavaExecutionOperator> operatorGenerator, Supplier<?>... dataQuantumGenerators) {
        super(operatorGenerator, dataQuantumGenerators);
    }

    @Override
    public void prepare(long dataQuantaSize, long... inputCardinalities) {
        Validate.isTrue(inputCardinalities.length == 1);

        super.prepare(dataQuantaSize, inputCardinalities);
        int inputCardinality = (int) inputCardinalities[0];

        // Create input data.
        Collection<Object> dataQuanta = new ArrayList<>(inputCardinality);
        final Supplier<?> supplier = this.dataQuantumGenerators.get(0);
        for (int i = 0; i < inputCardinality; i++) {
            dataQuanta.add(supplier.get());
        }
        this.inputChannelInstance = createChannelInstance(dataQuanta);
    }

    @Override
    protected void prepareInput(int inputIndex, long dataQuantaSize, long inputCardinality) {

    }

    @Override
    protected long executeOperator() {
        this.evaluate(
                new JavaChannelInstance[]{this.inputChannelInstance},
                new JavaChannelInstance[]{}
        );
        return 0L;
    }
}
