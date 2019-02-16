package org.qcri.rheem.core.optimizer.mloptimizer.api;

import org.qcri.rheem.core.api.exception.RheemException;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Contains generic methods and variable shared among {@link Topology}s
 */
public class TopologyBase implements Topology {

    /**
     * The below variable saves the the layer of the topology (i.e. for how many number of nodes "in termes of pipelines pipelines" is connected to)
     * PS: first nodeNumber implementation was having this purpose too as well as the number of number of nodes that is
     * updated after instantiation
     */
    protected int topologyNumber;

    /**
     * Number of nodes in the topology
     */
    protected int nodeNumber = -1;

    /**
     * Nodes inside a Topology
     */
    private Stack<Tuple2<String,OperatorProfiler>> nodes = new Stack<>();

    /**
     * Optional name. Helpful for debugging.
     */
    private String name;

    /**
     * platforms associated to the topology (it will contain the same order of platforms as the topology inner node connection)
     * PS1: The last element of the stack will be used to know the Topology's output platform
     * PS2: The Stack ordering structure will help identify the platform switch number used if the max platform switch constraint is enabled
     */
    private Stack<String> platformStack = new Stack<>();


    /**
     * True when the topology is a part of a loop body
     */
    private Boolean isLoopBody = false;

    @Override
    public void setInputTopologySlots(InputTopologySlot[] inputTopologySlots) {
        this.inputTopologySlots = inputTopologySlots;
    }

    @Override
    public void setOutputTopologySlots(OutputTopologySlot[] outputTopologySlots) {
        this.outputTopologySlots = outputTopologySlots;
    }

    public void setOutputTopologySlot(OutputTopologySlot outputTopologySlot, Integer index) {
        this.outputTopologySlots[index] = outputTopologySlot;
    }
    /**
     * Input Slots associated with the topology instance
     */
    protected InputTopologySlot[] inputTopologySlots;

    /**
     * Output Slots associated with the topology instance
     */
    protected OutputTopologySlot[] outputTopologySlots;


    public boolean resetInputSlots(Integer slot){
        //if (this instanceof PipelineTopology)
        if (slot<=inputTopologySlots.length)
            this.inputTopologySlots[slot] = new InputTopologySlot("in", this);
        else
            throw new RheemException("out of index topology slot!");
        /*if (this instanceof JunctureTopology)
            this.inputTopologySlots = new InputTopologySlot[2];
        if (this instanceof LoopTopology)
            this.inputTopologySlots = new InputTopologySlot[2];*/

        return true;
    }

    public boolean resetOutputSlots(Integer slot){
        if (slot<=outputTopologySlots.length)
            this.outputTopologySlots[slot] = new OutputTopologySlot("out",this);
        else
            throw new RheemException("out of index topology slot!");
        /*if (this instanceof PipelineTopology)
            this.outputTopologySlots = new OutputTopologySlot[1];
        if (this instanceof JunctureTopology)
            this.outputTopologySlots = new OutputTopologySlot[1];
        if (this instanceof LoopTopology)
            this.outputTopologySlots = new OutputTopologySlot[2];*/
        return true;
    }

    /**
     * true if the topology is a source topology
     *//*
    private boolean isSource = false;

    *//**
     * true if the topology is a sink topology
     *//*
    private boolean isSink = false;*/
    /*
    public Topology(){
        Nodes = new LinkedHashMap();
        nodeNumber = 0;
    }

    public Topology(int nodeNumber, LinkedHashMap nodes) {
        this.nodeNumber = nodeNumber;
        Nodes = nodes;
    }*/

    public int getTopologyNumber() {
        return topologyNumber;
    }

    public int getNodeNumber() {
        return nodeNumber;
    }

    public void setNodeNumber(int nodeNumber) {
        this.nodeNumber = nodeNumber;
    }

    public Stack<Tuple2<String,OperatorProfiler>> getNodes() {
        return this.nodes;
    }

    // Replace
    public void setNodes(Stack nodes) {
        // reset  nodes platforms
        platformStack.clear();
        Stack<Tuple2<String,OperatorProfiler>> newNodes =nodes;
        // Not sure whether the order of platform is ensured below
        newNodes.stream().forEach(n->this.addPlatform(n.getField1().getExecutionOperator().getPlatform().getName()));
        // replace with new nodes
        this.nodes = nodes;
    }

    public List<Topology> getPredecessors(){
        InputTopologySlot[] inputSlots = this.getAllInputs();
        List<Topology> predecessors = new ArrayList<>();
        for(InputTopologySlot input:inputSlots){
            // check if there's predecessor
            if (input.hasOccupant()){
                OutputTopologySlot output = input.getOccupant();
                predecessors.add(output.getOwner());
            }
        }
        return predecessors;
    }

    public List<Topology> getSuccessors(){
        OutputTopologySlot[] outputSlots = this.getAllOutputs();
        List<Topology> successors = new ArrayList<>();
        for(OutputTopologySlot output:outputSlots){
            // check if there's predecessor
            if (!output.getOccupiedSlots().isEmpty()){
                InputTopologySlot input = (InputTopologySlot) output.getOccupiedSlots().get(0);
                successors.add(input.getOwner());
            }
        }
        return successors;
    }

    public Topology getLeftTopNode(){
        if(!this.getPredecessors().isEmpty())
            return this.getPredecessors().get(0).getLeftTopNode();
        else
            return this;
    }


    @Override
    public InputTopologySlot<?>[] getAllInputs() {
        return this.inputTopologySlots;
    }

    @Override
    public OutputTopologySlot<?>[] getAllOutputs() {
        return this.outputTopologySlots;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }


    /**
     * Connects the pipeline topology to another successive {@link Topology}
     */
    public void connectTo(int thisOutputIndex, Topology that, int thatInputIndex){
        // create an input slot for the topology to connect To
        //that.setInput(thatInputIndex,new InputTopologySlot<>("in", that));
        final InputTopologySlot inputSlot = that.getInput(thatInputIndex);
        // create output slot for current Topology
        //outputTopologySlots[thisOutputIndex] = new OutputTopologySlot<>("out", this);
        final OutputTopologySlot outputSlot = this.getOutput(thisOutputIndex);

        outputSlot.connectTo(inputSlot);
    }


    /**
     * create a copy of current topology
     * @return
     */
    public Topology createCopy(int topologyNumber){

        Topology copiedTopology = new TopologyBase();

        // Initialize copied Topology
        if (this instanceof PipelineTopology){
             copiedTopology = new PipelineTopology(topologyNumber);
        } else if (this instanceof JunctureTopology){
             copiedTopology = new JunctureTopology(topologyNumber);
        } else if (this instanceof JunctureTopology){
             copiedTopology = new LoopTopology(topologyNumber);
        }

        // Clone the input topologies
        InputTopologySlot[] tmpInputTopologySlots = new InputTopologySlot[2];
        OutputTopologySlot[] tmpOutTopologySlots = new OutputTopologySlot[2];

        Integer counter=0;

        // Clone input slots
        for(InputTopologySlot in:this.inputTopologySlots){
            tmpInputTopologySlots[counter]=in.clone();

            if ((this.inputTopologySlots[counter].getOccupant() != null)){
                // case of cloning iteration input (iteration last node ) should be treated in a non recursive way to prevent infinity looping
                // input1 topology copy
                Topology previousTopology = in.getOccupant().getOwner().createCopy(topologyNumber-1);

                // Add the input tmpInputTopologySlots[counter] to the output of the previous topology tmpNewTopology
                previousTopology.getOutput(0).connectTo(tmpInputTopologySlots[counter]);

                // connect the input1Copy topology with the new junctureCopy input1
                // TODO: To be modified with the duplicate topology
                tmpInputTopologySlots[counter].setOccupant(previousTopology.getOutput(0));



            }
            counter++;
        }

        // Add tmpInputTopologySlots
        copiedTopology.setInputTopologySlots(tmpInputTopologySlots);
        //newTopology.setOutputTopologySlot(tmpOutTopologySlot,1);

        // Clone the nodes
        copiedTopology.setNodes((Stack) this.getNodes().clone());

        //Clone the nodenumber
        copiedTopology.setNodeNumber(this.nodeNumber);
        copiedTopology.setName(this.getName());

        //clone platforms
        Topology finalCopiedTopology = copiedTopology;
        this.getPlatforms().forEach(p -> {
            finalCopiedTopology.addPlatform((String)p);
        });
        return finalCopiedTopology;
    }


    public boolean isLoopBody() {
        return isLoopBody;
    }

    public void setBooleanBody(Boolean booleanBody) {
        isLoopBody = booleanBody;
    }

    public void addPlatform(String platform){ this.platformStack.add(platform);}

    public List getPlatforms(){return this.platformStack;}

    /*@Override
    public boolean isSink() {
        return isSink;
    }

    public void setSink(boolean sink) {
        isSink = sink;
    }

    @Override
    public boolean isSource() {
        return isSource;
    }

    public void setSource(boolean source) {
        isSource = source;
    }*/

}
