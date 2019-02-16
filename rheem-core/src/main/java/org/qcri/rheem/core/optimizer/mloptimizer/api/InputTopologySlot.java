package org.qcri.rheem.core.optimizer.mloptimizer.api;

/**
 * Created by migiwara on 07/07/17.
 */
public class InputTopologySlot<T> extends TopologySlot<T> {

    /**
     * Output slot of another topology that is connected to this input slot.
     */
    private OutputTopologySlot<T> occupant = null;


    /**
     * Constructor of the instance.
     * @param name {@link InputTopologySlot}'s name
     * @param owner {@link InputTopologySlot}'s owner
     */

    protected InputTopologySlot(String name, Topology owner) {
        super(name, owner);
    }

    /**
     * Connects the given {@link OutputTopologySlot}. Consider using the interface of the {@link OutputTopologySlot} instead to
     * keep consistency of connections in the plan.
     *
     * @param outputSlot the output slot to connect to
     * @return this instance
     * @see OutputTopologySlot#connectTo(InputTopologySlot)
     */
    InputTopologySlot<T> setOccupant(OutputTopologySlot<T> outputSlot) {
        this.occupant = outputSlot;
        return this;
    }

    public OutputTopologySlot<T> getOccupant() {
        //assert !this.getOccupant().equals(null);
        //boolean bool = this.occupant==null;
        return this.occupant;
    }

    public InputTopologySlot<T> clone() {
        InputTopologySlot newInputTopologySlot = new InputTopologySlot(this.getName(),this.getOwner());
        return newInputTopologySlot;
    }

    public Boolean hasOccupant(){
        return !(this.occupant == null);
    }

}
