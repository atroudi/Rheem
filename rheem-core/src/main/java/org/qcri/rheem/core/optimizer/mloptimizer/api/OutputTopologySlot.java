package org.qcri.rheem.core.optimizer.mloptimizer.api;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by migiwara on 07/07/17.
 */
public class OutputTopologySlot<T> extends TopologySlot<T> {

    public List<InputTopologySlot<T>> getOccupiedSlots() {
        //List<InputTopologySlot<T>> newList = new ArrayList<>();
        return occupiedSlots;
    }

    public void setOccupiedSlots(List<InputTopologySlot<T>> occupiedSlots) {
        this.occupiedSlots = occupiedSlots;
    }

    public void setOccupiedSlot(Integer slotNumber, InputTopologySlot<T> occupiedSlot) {
        this.occupiedSlots.add(slotNumber, occupiedSlot);
    }

    /**
     * Output slot of another Topology that is connected to this output slot.
     */
    private List<InputTopologySlot<T>> occupiedSlots = new LinkedList<>() ;

    protected OutputTopologySlot(String name, Topology owner) {
        super(name, owner);
    }

    /**
     * Connect this output slot to an input slot. The input slot must not be occupied already.
     *
     * @param inputSlot the input slot to connect to
     */
    public void connectTo(InputTopologySlot<T> inputSlot) {
        if (inputSlot.getOccupant() != null) {
            throw new IllegalStateException("Cannot connect: input slot is already occupied");
        }

        this.occupiedSlots.add(inputSlot);
        inputSlot.setOccupant(this);
    }

    public OutputTopologySlot<T> clone() {
        OutputTopologySlot newOutputTopologySlot = new OutputTopologySlot(this.getName(),this.getOwner());
        return newOutputTopologySlot;
    }
}
