package org.qcri.rheem.profiler.core;

import org.qcri.rheem.basic.data.Tuple2;
import org.qcri.rheem.core.optimizer.mloptimizer.api.OperatorProfiler;
import org.qcri.rheem.core.optimizer.mloptimizer.api.PipelineTopology;
import org.qcri.rheem.core.optimizer.mloptimizer.api.Topology;
import org.qcri.rheem.core.optimizer.mloptimizer.api.TopologyInstance;

import java.util.List;
import java.util.Stack;

/**
 * Instantiate a topology; filling the topology with nodes to be replaced with execution operators;
 * The filling starts with the {@link Topology} connected with the sink then fills the connected {@link Topology}s with associated number
 * of nodes
 */
public class  InstantiateTopology {



    // is a structure designed to contains the topology after that has been filled with nodes;
    // PS: Currently is not fully being used
    private List<TopologyInstance> topologyInstances;

    // Number of total node number
    private static int totalNodeNumber;

    // Number of node to fill in each pipeline topology
    private static int equalFillNumber;

    // the rest number of node to fill with the topologies; the below rest will be used to generate different instances with
    // exhaustively filling all possible combinations of the topologies
    private static int restFillNumber;

    //
    public InstantiateTopology(List<Topology> sinkTopologies) {
        this.topologyInstances = topologyInstances;
    }

    /**
     * Method that instantiates all generated Topologies
     * @param sinkTopologies
     *
     * @return
     */
    public static TopologyInstance instantiateTopology(List<Topology> sinkTopologies, int nodeNumber){
        totalNodeNumber = nodeNumber;
        for(Topology sinkTopology:sinkTopologies){
            // first fill with equal number nodes ie each pipeline topology
            int PipelineNumber = sinkTopology.getTopologyNumber();

            equalFillNumber = totalNodeNumber/PipelineNumber;
            restFillNumber =totalNodeNumber%PipelineNumber;
            instantiateTopology(sinkTopology);
        }
        TopologyInstance topologyInstance = new TopologyInstance();
        return topologyInstance;
    }

    /**
     * Instantiate the topology with the number of nodes it contains BUT the filling happens in the Profiling Plan Builder
     * PS: After this step the number of nodes of each Topology should be correct
     * @param topology
     * @return
     */

    public static TopologyInstance instantiateTopology(Topology topology){

        //Topology tmpTopology = sinkTopology;
        // Stop when we reach the source topologies from the
        //do {
            // Handle the case if the sink is pipeline Topology
        Stack<Tuple2<String, OperatorProfiler>> nodes = new Stack<>();
        int nodeNumber=0;

        if (topology.isPipeline()){
                // here we try to fill the topologies as we go up until sources with actual nodes
                // update node number of current topology
                nodeNumber = equalFillNumber;
                // Add one node from the restFillingNodes
                if (restFillNumber!=0) {
                    nodeNumber += 1;
                    restFillNumber -= 1;
                }

                // correct node number
                topology.setNodeNumber(nodeNumber);
            } else {
                if ((topology.isLoop())&&(topology.getNodeNumber()!=-1))
                    return new TopologyInstance();
                // Handle the case of Juncture topology

                //Set nodes for the sink topology
                // Always set only one node number that will be replaced in the ProfilingPlanBuilder with a binary Profiling Operator
                // correct node number
                topology.setNodeNumber(1);
            }
            // Handle the case if the sink is juncture Topology

        List<Topology> predecessors = topology.getPredecessors();

        // recurse the predecessor tpgs
        //get the predecessors of tmp topology
        if  (!(predecessors.isEmpty())){
            for(Topology t:predecessors)
                instantiateTopology(t);
        }

            // loop for source topologies
        //} while (sinkTopology.getAllInputs().length!=0);

        TopologyInstance topologyInstance = new TopologyInstance();
        return topologyInstance;
    }
    /**
     * Instantiate a pipeline Topology
     * @param pipelineTopology
     * @return
     */
    private void InstantiatePipelinetopology(PipelineTopology pipelineTopology){

        Stack<Tuple2<String, OperatorProfiler>> nodes = new Stack<>();
        // Check if the current pipeline topology need a source node
        if (pipelineTopology.getAllInputs().length==0)
            //nodes.push(new Tuple2<String, OperatorProfiler>("sourceNode", new OperatorProfiler(){}));

        for(int i=1;i<=pipelineTopology.getNodeNumber();i++)
            //nodes.push(new Tuple2<String, OperatorProfiler>("unaryNode", new OperatorProfiler(){}));
        pipelineTopology.setNodes(nodes);
        //Topology topology = new Topology();
        //topology.setNodeNumber(nodesNumber);
        //topology.setNodes(nodes);



        //return new TopologyInstance();
    }

    private static void InstantiatePipelinetopology(int nodesNumber){
        TopologyInstance nodes = new TopologyInstance();
        for(int i=0;i<nodesNumber;i++)
            nodes.add(i, "node"+i);
        //Topology topology = new Topology();
        //topology.setNodeNumber(nodesNumber);
        //topology.setNodes(nodes);
        //return new TopologyInstance();
    }
}
