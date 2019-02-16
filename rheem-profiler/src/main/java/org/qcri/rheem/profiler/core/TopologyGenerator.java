package org.qcri.rheem.profiler.core;

import org.qcri.rheem.core.optimizer.mloptimizer.api.*;
import org.qcri.rheem.profiler.core.api.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates Topology for profiling plans
 */
public class TopologyGenerator {

    public static List<Topology> getTopologyList() {
        // add loop topologies
        topologyList.addAll(loopTopologyList);
        return topologyList;
    }

    /**
     * Generated Topologies
     */
    private static List<Topology> topologyList = new ArrayList<>();

    /**
     * Generated Topologies with loop Topologies
     */
    private static List<Topology> loopTopologyList = new ArrayList<>();

    /**
     * Number of nodes in the topology; is used only to generate topology shapes;
     * It is not used to fill the Topology because that will be the topology instantiate task.
     */
    private static int nodeNumber;

    /**
     * cached node number; is used when generating topologies having more node number than the cached node number;
     *
     */
    private static int cachedNodeNumber = 1;

    /**
     * number of new generted toplogy when {@method generateTopology} is colled;
     *
     */
    private static int newGeneratedTopologies = 0;

    /**
     * maximum number of Junctures Topologies to be used in generated topologies
     *
     */
    //private static int maxJunctureTopologies = 0;

    /**
     * maximum number of Loops Topologies to be used in generated topologies
     *
     */
    private static int maxPipelineTopologies = -1 ;

    /**
     * maximum number of Loops Topologies to be used in generated topologies
     *
     */
    private static int maxLoopTopologies = 0 ;

    /**
     * maximum number of Loops Topologies to be used in generated topologies
     *
     */
    private static int minLoopTopologies = 0 ;

    /**
     * Position of the loop topology; in which layer the loop topology begins in the generated Topology
     */
    private Integer Lpos = -1;

    /**
     * number of nodes inside of the loop topology;
     */
    private Integer Lbody = -1;


    /**
     * Current number of Juncture Topologies to be used in generated topologies
     *
     */
    private static Integer currentJunctureNumber = 0 ;


    /**
     * Current number of Pipeline Topologies to be used in generated topologies
     *
     */
    private static Integer currentPipelineNumber = 0 ;


    /**
     * maximum number of Juncture Topologies to be used in generated topologies
     *
     */
    private static int maxJunctureTopologies = 1;

    private static ProfilingConfig config;

    /**
     * cached generated topologies; is used when generating topologies having more node number than the cached node number;
     */
    private static List<Topology> cachedTopologyList = new LinkedList<>();


    public TopologyGenerator() {
        nodeNumber = 0;
    }


    public TopologyGenerator( int nodesNumber,  ProfilingConfig configuration) {
        config = configuration;
        nodeNumber = nodesNumber;

        // set up current generator
        maxJunctureTopologies = config.getMaxJunctureTopologies();
        maxLoopTopologies = config.getMaxLoopTopologies();
    }

    public static void startGeneration(){
        generateTopology(cachedNodeNumber);
    }

    public static List<Topology> generateTopology(int nodesNumber){
        // Disabled case that handles single Operator Topology
        // TODO the below case should be put in an outside part before the the topology loops
        if((nodesNumber==1)&&(false))
            // Single executionOperator topology
            return singleOperatortopology();

        int previousGeneratedTopologies = newGeneratedTopologies;
        newGeneratedTopologies = 0;

        for(int i=1;i<=previousGeneratedTopologies;i++){
            Topology tmpPreviousTopology = topologyList.get(topologyList.size()-i).createCopy(nodesNumber-1);

            //Topology[] tmp = new Topology[1];

            //Topology[] tmp2 = new Topology[1];
            //tmp2[0] = topologyList.get(topologyList.size()-i);
            //System.arraycopy(tmp2, 0, tmp, 0,0);

                if (tmpPreviousTopology.isPipeline()){
                    // if the tmpPreviousTopology is pipeline the only one topology possiblity to generate (i.e 1 new juncture + 1 new pipeline)
                    // Handles the case of creating the first merge Topology (i.e with only one Juncture Topology)
                    PipelineTopology tmpPipeline = new PipelineTopology(nodesNumber);
                    JunctureTopology tmpJuncture = new JunctureTopology(nodesNumber);

                    // Connect the last generated Topology with tmpJuncture
                    tmpPreviousTopology.connectTo(0,tmpJuncture,0);
                    // Connect the created Pipeline Topology with tmpJuncture
                    tmpPipeline.connectTo(0,tmpJuncture,1);

                    // Add the new Topology with Juncture
                    topologyList.add(tmpJuncture);
                    newGeneratedTopologies+=1;

                    // update the number of junctions
                    currentJunctureNumber+=1;


                } else if (tmpPreviousTopology.isJuncture()){
                    // create another copy because we will be adding two topoloogies here
                    Topology tmpPreviousTopology2 = tmpPreviousTopology.createCopy(nodesNumber-1);


                    // case of juncture topology; two possible topologies to generate (i.e: first is 1 new pipeline; second: 1 new juncture + 1 new pipeline)
                    PipelineTopology tmpPipeline = new PipelineTopology(nodesNumber);

                    // Connect the last generated Topology with tmpJuncture
                    tmpPreviousTopology.connectTo(0,tmpPipeline,0);



                    JunctureTopology tmpJuncture = new JunctureTopology(nodesNumber);
                    PipelineTopology tmpPipeline2 = new PipelineTopology(nodesNumber);
                    // Connect the last generated Topology with tmpJuncture
                    tmpPreviousTopology.connectTo(0,tmpJuncture,0);
                    // Connect the created Pipeline Topology with tmpJuncture
                    tmpPipeline2.connectTo(0,tmpJuncture,1);

                    // Add the new juncture generated topology
                    topologyList.add(tmpJuncture);

                    // Add the new pipeline generated topology
                    topologyList.add(tmpPipeline);

                    currentPipelineNumber+=1;
                    newGeneratedTopologies+=2;
                    currentJunctureNumber+=1;

            }
            else if (tmpPreviousTopology.isLoop()){
                // This eventually unecessery case cause the way a loop topology/operators are implemented are topologycally connected
            }
        }

        if(config.isBushyGeneration()){
            // Do bushy generation
            for(int N=2;N<=nodesNumber/2;N++){

                // get list with number of nodes N
                int nodeNumberN = N;
                List<Topology> SubListN = new ArrayList();
                SubListN = topologyList.stream().filter(t -> (t.getTopologyNumber()==nodeNumberN))
                        .collect(Collectors.toList());

                // get list with number of nodes M equals to number of current nodes minus N
                final int nodeNumberM = nodesNumber-N;
                List<Topology> SubListM = new ArrayList();
                SubListM = topologyList.stream().filter(t -> (t.getTopologyNumber()==nodeNumberM))
                        .collect(Collectors.toList());

                // Do exhaustive merge of both lists
                List<Topology> finalSubListM = SubListM;

                SubListN.stream().forEach(t1 ->{
                    finalSubListM.stream().forEach(t2 -> {
                        if(currentJunctureNumber <=maxJunctureTopologies){
                            Topology newT = mergeTopologies(t1,t2,nodesNumber);
                            topologyList.add(newT);
                            newGeneratedTopologies+=1;
                        }
                    });
                });

            }

        }

        // Handles the case of generating One Node Pipeline Topology
        if (nodesNumber==1) {
            topologyList.add(new PipelineTopology(nodesNumber));
            generateLoops(1,nodesNumber);
            currentPipelineNumber +=1;
            newGeneratedTopologies+=1;
        }

        // Handles loop topologies
        generateLoops(previousGeneratedTopologies, nodesNumber);


        // exit if the nodeNumber is equal to 1
        if(nodeNumber==1){
            return topologyList;
        }

        // add recursively topologies until reaching the current TopologyGenerator node number
        //while((nodesNumber+1)<=nodeNumber)
        if ((nodesNumber+1)<=nodeNumber){
            // check if the number of junctions are exceeded
            if(currentJunctureNumber >= maxJunctureTopologies){
                // exit generation
                return topologyList;
            }
            generateTopology(nodesNumber+1);
        }

        return topologyList;
    }


    static private void generateLoops(int previousGeneratedTopologies, int nodesNumber){

        // Add a loop to the last created node (Last Layer)
        // Start the loop from the topl left topology
        for(int i=1;i<=previousGeneratedTopologies;i++){
            if(maxLoopTopologies!=0){

                // create a loop topology topology
                LoopTopology loopTopology = new LoopTopology(nodesNumber,1);

                Topology tmpPreviousTopology = topologyList.get(topologyList.size()-i).createCopy(nodesNumber-1);

                Topology sourceLeftNode = tmpPreviousTopology.getLeftTopNode();

                Topology sourceLeftNodeSuccessor;
                if(!sourceLeftNode.getOutput(0).getOccupiedSlots().isEmpty())
                    sourceLeftNodeSuccessor = sourceLeftNode.getOutput(0).getOccupiedSlots().get(0).getOwner();
                else
                    sourceLeftNodeSuccessor = sourceLeftNode;

                // set the sourceLeftNodeSuccessor as a loop body
                sourceLeftNodeSuccessor.setBooleanBody(true);

                //Reset the output slot of sourceTop and connect with initialize 0
                sourceLeftNode.resetOutputSlots(0);

                //Reset the first input slot of sourceTopSucessor and connect with beginIteration
                sourceLeftNodeSuccessor.resetInputSlots(0);


                if(sourceLeftNodeSuccessor!= sourceLeftNode)
                    loopTopology.initialize(sourceLeftNode,0);

                //Reset input slot for source successor and connect with begin iteration
                sourceLeftNodeSuccessor.resetInputSlots(0);

                loopTopology.beginIteration(sourceLeftNodeSuccessor,0);

                // connect the end iteration with tmpPreviousTopology
                loopTopology.endIteration(tmpPreviousTopology,0);

                // the final output is left for the sunk in the runner

                // update the loop body topologies with (this.isLoopBody = true)
                Topology tmpTop = sourceLeftNodeSuccessor;
                while(tmpTop.getSuccessors().get(0)!=loopTopology){
                    tmpTop.getSuccessors().get(0).setBooleanBody(true);
                    tmpTop = tmpTop.getSuccessors().get(0);
                }

                // add the
                loopTopologyList.add(loopTopology);
                maxLoopTopologies-=1;
            } else {
                // Exit for
                break;
            }
        }
    }



    private static Topology mergeTopologies(Topology t1, Topology t2, int nodeNumber) {
        Topology t1Copy = t1.createCopy(nodeNumber-1);
        Topology t2Copy = t2.createCopy(nodeNumber-1);

        JunctureTopology tmpJuncture = new JunctureTopology(nodeNumber);

        // Merge t1 and t2
        t1Copy.connectTo(0,tmpJuncture,0);
        t2Copy.connectTo(0,tmpJuncture,1);

        // Update juncture number
        currentJunctureNumber +=1;
        return tmpJuncture;
    }

    private static List<Topology> singleOperatortopology(){
        LinkedHashMap<Integer,String> nodes = new LinkedHashMap<>();
        List<Topology> topologyList = new LinkedList<>();
        Topology topology = new TopologyBase();
        //topology.setNodeNumber(1);
        //topology.setNodes(nodes);
        topologyList.add(topology);
        return topologyList;
    }

    private static PipelineTopology Pipelinetopology(int nodesNumber){
        return new PipelineTopology(nodesNumber);
    }
}
