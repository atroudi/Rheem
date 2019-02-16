package org.qcri.rheem.core.optimizer.mloptimizer.api;

import org.qcri.rheem.core.types.DataSetType;

/**
 * Created by migiwara on 07/07/17.
 */
abstract public class TopologySlot<T> {
    /**
     * Identifies this slot within its executionOperator.
     */
    private final String name;

    public Topology getOwner() {
        return owner;
    }

    public void setOwner(Topology topology) {
        this.owner = topology;
    }


    /**
     * The executionOperator that is being decorated by this slot.
     */
    private Topology owner;

    /**
     * <i>Lazy initialized.</i> The index of this instance within its {@link #owner}.
     */
    protected int index = -1;

    /**
     * Type of data passed through this slot, expressed as a {@link DataSetType} so as to define not only the types of
     * elements that are passed but also capture their structure (e.g., flat, grouped, sorted, ...).
     */
    private DataSetType<T> type;

    protected TopologySlot(String name, Topology owner) {
        assert owner != null;
        this.name = name;
        this.owner = owner;
    }

    public String getName() {
        return this.name;
    }


}
