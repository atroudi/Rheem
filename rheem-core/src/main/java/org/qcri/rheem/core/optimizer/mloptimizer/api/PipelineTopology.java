package org.qcri.rheem.core.optimizer.mloptimizer.api;

import java.util.Stack;

/**
 * Created by migiwara on 07/07/17.
 */
public class PipelineTopology extends TopologyBase implements Topology {

    /**
     * Input topology slot for the {@link PipelineTopology}
     */
    //static private InputTopologySlot[] inputTopologySlots;

    /**
     * Output topology slot for the {@link PipelineTopology}
     */
    //static private OutputTopologySlot[] outputTopologySlots;

    /**
     * Number of Nodes inside the {@link PipelineTopology}
     */
    //private int nodeNumber;

    public PipelineTopology(){
        this.inputTopologySlots = new InputTopologySlot[1];
        this.outputTopologySlots = new OutputTopologySlot[1];
        this.inputTopologySlots[0] = new InputTopologySlot("in", this);
        this.outputTopologySlots[0] = new OutputTopologySlot("out", this);
    }

    public PipelineTopology(int topologyNumber){
        this.inputTopologySlots = new InputTopologySlot[1];
        this.outputTopologySlots = new OutputTopologySlot[1];
        this.inputTopologySlots[0] = new InputTopologySlot("in", this);
        this.outputTopologySlots[0] = new OutputTopologySlot("out", this);
        this.topologyNumber=topologyNumber;
    }

    public PipelineTopology(InputTopologySlot inputTopology, OutputTopologySlot outputTopology, int nodeNumber) {
        this.inputTopologySlots[0] = new InputTopologySlot<>("in", this);
        this.outputTopologySlots[0] = new OutputTopologySlot<>("out", this);
        this.nodeNumber = nodeNumber;
    }


    /**
     * create a copy of current topology
     * @return
     */
    public Topology createCopy(int topologyNumber){


        PipelineTopology newTopology = new PipelineTopology(topologyNumber);

        // Clone the input topologies
        InputTopologySlot[] tmpInputTopologySlots = new InputTopologySlot[1];
        OutputTopologySlot[] tmpOutTopologySlots = new OutputTopologySlot[1];



        for(InputTopologySlot in:this.inputTopologySlots){
            tmpInputTopologySlots[0] = in.clone();

            if (this.inputTopologySlots[0].getOccupant() != null){
                // input1 topology copy
                Topology previousTopology = this.inputTopologySlots[0].getOccupant().getOwner().createCopy(topologyNumber-1);

                // Add the input tmpInputTopologySlots[counter] to the output of the previous topology tmpNewTopology
                //previousTopology.getOutput(0).connectTo(tmpInputTopologySlots[0]);
                previousTopology.getOutput(0).connectTo((InputTopologySlot)newTopology.getInput(0));

                // connect the input1Copy topology with the new junctureCopy input1
                tmpInputTopologySlots[0].setOccupant(previousTopology.getOutput(0));

            }
        }

        for(OutputTopologySlot out:this.outputTopologySlots){
            // Initialize outputs
            tmpOutTopologySlots[0] = new OutputTopologySlot("in", newTopology);
            tmpOutTopologySlots[0].setOccupiedSlots(out.getOccupiedSlots());
        }

        newTopology.setInputTopologySlots(tmpInputTopologySlots);
        //newTopology.setOutputTopologySlots(tmpOutTopologySlots);


        // Clone the nodes
        newTopology.setNodes((Stack) this.getNodes().clone());

        //Clone the nodenumber
        newTopology.setNodeNumber(this.nodeNumber);
        newTopology.setName(this.getName());
        return newTopology;
    }
}
