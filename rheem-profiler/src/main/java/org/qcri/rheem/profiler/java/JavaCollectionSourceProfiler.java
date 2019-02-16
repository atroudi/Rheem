package org.qcri.rheem.profiler.java;

import org.qcri.rheem.core.types.DataSetType;
import org.qcri.rheem.java.operators.JavaCollectionSource;
import org.qcri.rheem.core.optimizer.mloptimizer.api.OperatorProfiler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

/**
 * {@link OperatorProfiler} for {@link JavaCollectionSource}s.
 */
public class JavaCollectionSourceProfiler<Type> extends JavaSourceProfiler {

    private Collection<Type> sourceCollection;

    public <T extends Object>JavaCollectionSourceProfiler(Supplier<?> dataQuantumGenerator, ArrayList<T> collection, Class<T> out) {
        super(()->new JavaCollectionSource<>(collection, DataSetType.createDefault(out)), dataQuantumGenerator);
        this.executionOperatorGenerator = this::createOperator; // We can only pass the method reference here.
    }

    private JavaCollectionSource createOperator() {
        final Object exampleDataQuantum = this.dataQuantumGenerators.get(0).get();
        return new JavaCollectionSource(this.sourceCollection, DataSetType.createDefault(exampleDataQuantum.getClass()));
    }


    @Override
    public void setUpSourceData(long cardinality) throws Exception {
        // Create the #sourceCollection.
        final Supplier<?> dataQuantumGenerator = this.dataQuantumGenerators.get(0);
        sourceCollection = new ArrayList<>((int) cardinality);
        for (int i = 0; i < cardinality; i++) {
            sourceCollection.add((Type) dataQuantumGenerator.get());
        }
    }

    public void clearSourceData(){
        sourceCollection.clear();
    }

    @Override
    protected void prepareInput(int inputIndex, long dataQuantaSize, long inputCardinality) {

    }
}
