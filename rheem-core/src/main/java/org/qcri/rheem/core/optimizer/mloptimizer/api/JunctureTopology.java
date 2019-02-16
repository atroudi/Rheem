package org.qcri.rheem.core.optimizer.mloptimizer.api;

import java.util.Stack;

/**
 * Created by migiwara on 07/07/17.
 */
public class JunctureTopology extends TopologyBase implements Topology {

    /**
     * Input topology slot for the {@link PipelineTopology}
     */
    private InputTopologySlot inputTopology;

    /**
     * Output topology slot for the {@link PipelineTopology}
     */
    private OutputTopologySlot outputTopology;


    public JunctureTopology(){
        this.inputTopologySlots = new InputTopologySlot[2];
        this.outputTopologySlots = new OutputTopologySlot[1];

        this.inputTopologySlots[0] = new InputTopologySlot<>("in0", this);
        this.inputTopologySlots[1] = new InputTopologySlot<>("in1", this);
        this.outputTopologySlots[0] = new OutputTopologySlot<>("out", this);
    }

    public JunctureTopology(int topologyNumber){
        this.inputTopologySlots = new InputTopologySlot[2];
        this.outputTopologySlots = new OutputTopologySlot[1];

        this.inputTopologySlots[0] = new InputTopologySlot<>("in0", this);
        this.inputTopologySlots[1] = new InputTopologySlot<>("in1", this);
        this.outputTopologySlots[0] = new OutputTopologySlot<>("out", this);
        this.topologyNumber = topologyNumber;
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
        InputTopologySlot[] tmpInputTopologySlots = new InputTopologySlot[2];
        OutputTopologySlot[] tmpOutTopologySlots = new OutputTopologySlot[1];


        // new junctureCopy
        JunctureTopology newTopology = new JunctureTopology(topologyNumber);

        /*
        //for(InputTopologySlot in:this.inputTopologySlots){
        tmpInputTopologySlots[0]=this.inputTopologySlots[0].clone();

        if (this.inputTopologySlots[0].getOccupant() != null){
            // input1 topology copy
            Topology previousTopology = this.inputTopologySlots[0].getOccupant().getOwner().createCopy(topologyNumber-1);
            // connect the input1Copy topology with the new junctureCopy input1
            tmpInputTopologySlots[0].setOccupant(previousTopology.getOutput(0));

            // Add the input tmpInputTopologySlots[counter] to the output of the previous topology tmpNewTopology
            previousTopology.getOutput(0).setOccupiedSlot(0,tmpInputTopologySlots[0]);

        }

        tmpInputTopologySlots[1]=this.inputTopologySlots[1].clone();

        if (this.inputTopologySlots[1].getOccupant() != null){
            // input2 topology copy
            Topology tmpNewTopology = this.inputTopologySlots[1].getOccupant().getOwner().createCopy(topologyNumber-1);
            // connect the input2Copy topology with the new junctureCopy input2
            tmpInputTopologySlots[1].setOccupant(tmpNewTopology.getOutput(0));
        }

        //}

*/

        Integer counter=0;

        for(InputTopologySlot in:this.inputTopologySlots){
            tmpInputTopologySlots[counter]=in.clone();

            if ((this.inputTopologySlots[counter].getOccupant() != null)){
                // case of cloning iteration input (iteration last node ) should be treated in a non recursive way to prevent infinity looping
                // input1 topology copy
                Topology previousTopology = in.getOccupant().getOwner().createCopy(topologyNumber-1);

                // Add the input tmpInputTopologySlots[counter] to the output of the previous topology tmpNewTopology
                //                previousTopology.getOutput(0).setOccupiedSlot(0,tmpInputTopologySlots[counter]);

                previousTopology.getOutput(0).connectTo((InputTopologySlot)newTopology.getInput(counter));

                // connect the input1Copy topology with the new junctureCopy input1
                // TODO: To be modified with the duplicate topology
                tmpInputTopologySlots[counter].setOccupant(previousTopology.getOutput(0));
            }
            counter++;
        }

        counter=0;
        for(OutputTopologySlot out:this.outputTopologySlots){
            tmpOutTopologySlots[counter] = new OutputTopologySlot("in"+counter, newTopology);
            tmpOutTopologySlots[counter].setOccupiedSlots(out.getOccupiedSlots());

            counter++;
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

    /**
     * Connects the pipeline topology to another successive {@link Topology}
     */
    //static public void connectTo(int thisOutputIndex, PipelineTopology that0, PipelineTopology that1, int thatInputIndex0,int thatInputIndex1){
    //    that0.connect
    //}
}
