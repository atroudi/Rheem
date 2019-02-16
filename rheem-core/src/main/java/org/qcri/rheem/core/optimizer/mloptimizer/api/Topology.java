package org.qcri.rheem.core.optimizer.mloptimizer.api;

import org.qcri.rheem.core.plan.rheemplan.InputSlot;
import org.qcri.rheem.core.plan.rheemplan.OutputSlot;

import java.util.List;
import java.util.Stack;

/**
 * Handles the topology of generated profiling plans.
 */
public interface Topology {

    public void setInputTopologySlots(InputTopologySlot[] inputTopologySlots);

    public void setOutputTopologySlots(OutputTopologySlot[] outputTopologySlots) ;

    /**
     * @return the number of {@link InputTopologySlot}s of this instance;
     */
    default int getNumInputs() {
        return this.getAllInputs().length;
    }

    /**
     * @return the number of {@link OutputTopologySlot}s of this instance
     */
    default int getNumOutputs() {
        return this.getAllOutputs().length;
    }

    Topology createCopy(int topologyNumber);

    int getNodeNumber();

    int getTopologyNumber();

    boolean isLoopBody();

    void setBooleanBody(Boolean bool);

    void setNodeNumber(int nodeNumber);

    Stack<Tuple2<String,OperatorProfiler>> getNodes();

    void setNodes(Stack nodes);

    List<Topology> getPredecessors();
    List<Topology> getSuccessors();


        Topology getLeftTopNode();

    boolean resetInputSlots(Integer slot);

    boolean resetOutputSlots(Integer slot);



    /**
         * @return the {@link InputTopologySlot}s of this instance;
         */
    InputTopologySlot<?>[] getAllInputs();

    /**
     * @return the {@link OutputTopologySlot}s of this instance
     */
    OutputTopologySlot<?>[] getAllOutputs();

    /**
     * Retrieve an {@link OutputTopologySlot} of this instance using its index.
     *
     * @param index of the {@link OutputTopologySlot}
     * @return the requested {@link OutputTopologySlot}
     */
    default OutputTopologySlot<?> getOutput(int index) {
        final OutputTopologySlot[] allOutputs = this.getAllOutputs();
        if (index < 0 || index >= allOutputs.length) {
            throw new IllegalArgumentException(String.format("Illegal output index: %d.", index));
        }
        return allOutputs[index];
    }

    /**
     * Retrieve an {@link OutputTopologySlot} of this instance using its index.
     *
     * @param index of the {@link OutputTopologySlot}
     * @return the requested {@link OutputTopologySlot}
     */
    default InputTopologySlot<?> getInput(int index) {
        final InputTopologySlot[] allInputs = this.getAllInputs();
        if (index < 0 || index >= allInputs.length) {
            throw new IllegalArgumentException(String.format("Illegal input index: %d.", index));
        }
        return allInputs[index];
    }

    /**
     * Retrieve an {@link InputTopologySlot} of this instance by its name.
     *
     * @param name of the {@link InputTopologySlot}
     * @return the requested {@link InputTopologySlot}
     */
    default InputTopologySlot<?> getInput(String name) {
        for (InputTopologySlot inputSlot : this.getAllInputs()) {
            if (inputSlot.getName().equals(name)) return inputSlot;
        }
        throw new IllegalArgumentException(String.format("No slot with such name: %s", name));
    }

    /**
     * Sets the {@link InputSlot} of this instance. This method must only be invoked, when the input index is not
     * yet filled.
     *
     * @param index at which the {@link InputSlot} should be placed
     * @param input the new {@link InputSlot}
     */
    default void setInput(int index, InputTopologySlot<?> input) {
        //assert index < this.getNumRegularInputs() && this.getInput(index) == null;
        //assert input.getOwner() == this;
        ((InputTopologySlot[]) this.getAllInputs())[index] = input;
    }

    /**
     * Sets the {@link OutputSlot} of this instance. This method must only be invoked, when the output index is not
     * yet filled.
     *
     * @param index  at which the {@link OutputSlot} should be placed
     * @param output the new {@link OutputSlot}
     */
    default void setOutput(int index, OutputTopologySlot<?> output) {
        //assert index < this.getNumOutputs() && this.getOutput(index) == null;
        //assert output.getOwner() == this;
        ((OutputTopologySlot[]) this.getAllOutputs())[index] = output;
    }

    /**
     * Connects the pipeline topology to another successive {@link Topology}
     */
    default void connectTo(int thisOutputIndex, Topology that, int thatInputIndex){
    }

    /**
     * Tells whether this topology is a sink, i.e., it has no outputs.
     *
     * @return whether this topology is a sink
     */
    default boolean isSink() {
        return (this.getOutput(0).getOwner()==null);
    }

    /**
     * Tells whether this topology is a source, i.e., it has no inputs.
     *
     * @return whether this topology is a source
     */
    //boolean isSource();

    //void setSource(boolean b);
    default boolean isSource() {
        return (this.getInput(0).getOccupant()==null);
    }

    /**
     * Tells whether this topology is a pipeline.
     *
     * @return whether this topology is a pipeline
     */
    default boolean isPipeline() {return this instanceof PipelineTopology;}

    /**
     * Tells whether this topology is a juncture.
     *
     * @return whether this topology is a juncture
     */
    default boolean isJuncture() {return this instanceof JunctureTopology;}

    /**
     * Tells whether this topology is a pipeline.
     *
     * @return whether this topology is a pipeline
     */
    default boolean isLoop() {return this instanceof LoopTopology;}


    /**
     * Provides an instance's name.
     *
     * @return the name of this instance or {@code null} if none
     */
    String getName();

    /**
     * Provide a name for this instance.
     *
     * @param name the name
     */
    void setName(String name);

    void addPlatform(String platform);

}
