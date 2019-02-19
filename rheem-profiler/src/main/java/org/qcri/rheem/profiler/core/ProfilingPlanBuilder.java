package org.qcri.rheem.profiler.core;

import org.qcri.rheem.core.api.exception.RheemException;
import org.qcri.rheem.core.optimizer.mloptimizer.api.Tuple2;
import org.qcri.rheem.core.api.Configuration;
import org.qcri.rheem.core.optimizer.mloptimizer.api.LoopTopology;
import org.qcri.rheem.core.optimizer.mloptimizer.api.OperatorProfiler;
import org.qcri.rheem.core.optimizer.mloptimizer.api.Shape;
import org.qcri.rheem.core.optimizer.mloptimizer.api.Topology;
import org.qcri.rheem.core.plan.rheemplan.InputSlot;
import org.qcri.rheem.core.plan.rheemplan.OutputSlot;
import org.qcri.rheem.core.types.DataSetType;
import org.qcri.rheem.profiler.core.api.*;
import org.qcri.rheem.profiler.core.api.PlanEnumeration.ExhaustiveEnumeration;
import org.qcri.rheem.profiler.generators.DataGenerators;
import org.qcri.rheem.profiler.generators.ProfilingOperatorGenerator;
import org.qcri.rheem.profiler.spark.SparkOperatorProfiler;
import org.qcri.rheem.profiler.spark.SparkOperatorProfilers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
/**
 * Generates rheem plans for profiling.
 */
public class ProfilingPlanBuilder implements Serializable {

    /**
     * Logging for profiling plan building
     */
    public static final Logger logger =  LoggerFactory.getLogger(ProfilingPlanBuilder.class);


    /**
     * Configuration file for the plan builder
     */
    public static ProfilingConfig profilingConfig;

    /**
     * Configuration file for the plan builder
     */
    public static Configuration configuration = new Configuration();

    /**
     * Used to stop loop body connection from going into an endless loop
     */
    private static LoopTopology currentLoop;


    public static List<List<PlanProfiler>> ProfilingPlanBuilder(List<Shape> shapes, ProfilingConfig profilingConfiguration){
        profilingConfig = profilingConfiguration;
        assert (shapes.size()!=0);

        List<List<PlanProfiler>> topologyPlanProfilers = new ArrayList<>();

        for(Shape s:shapes){
            if (profilingConfig.getProfilingPlanGenerationEnumeration().equals("random")){
                topologyPlanProfilers.add(randomProfilingPlanBuilder(s, shapes, configuration));
            } else if (profilingConfig.getProfilingPlanGenerationEnumeration().equals("exhaustive")){
                topologyPlanProfilers.add(exhaustiveProfilingPlanBuilder(s, configuration, "exhaustive"));
            } else if (profilingConfig.getProfilingPlanGenerationEnumeration().equals("maxPlatSwitch")){
            topologyPlanProfilers.add(exhaustiveProfilingPlanBuilder(s, configuration, "maxPlatSwitch"));
            } else {
                // TODO: add more profiling plan generation pruning: worstPlanGen (more execution time),betterPlanGen (less execution time)
                new RheemException("Unhandled profiling plan generation!");
            }
        }
        return topologyPlanProfilers;
    }

    /**
     * Generates a list that contains a {@param numberPlans} of exhaustive generated {@link PlanProfiler}s for the given {@param shape}
     * through filling all possible `platform combinations`
     * @param shape
     * @return
     */
    public static List<PlanProfiler> exhaustiveProfilingPlanBuilder(Shape shape, Configuration configuration, String pruning){
        List<PlanProfiler> planProfilers = new ArrayList<>();
        PlanProfiler planProfiler = new PlanProfiler(shape, profilingConfig);
        List<Shape> tmpSubShapes = new ArrayList<>();
        String tmpPlateform = "";
        String tmpOperator = "";

        // Set platforms contained in the shape
        shape.setPlateform(profilingConfig.getProfilingPlateform());

        int PlatformRnd = (int)(Math.random() * profilingConfig.getProfilingPlateform().size());
        String platform = profilingConfig.getProfilingPlateform().get(PlatformRnd);

        // Loop through all dataTypes
        for(DataSetType type:profilingConfig.getDataType()){

            // Set the shape's platform
            List<String> platforms = profilingConfig.getProfilingPlateform();
            // Get total node numbers
            Optional<Integer> total_pipeline_nodes = shape.getPipelineTopologies().stream()
                    .map(t1 -> {
                        if (t1.isSource())
                            // Remove first node as it will be a source operator
                            return t1.getNodeNumber() - 1;
                        else
                            return t1.getNodeNumber();
                    })
                    .reduce((n1, n2) -> n1 + n2);

            List nodes = new ArrayList<String>(total_pipeline_nodes.get());
            List<List<Tuple2<String,String>>> totalEnumOpertorsPlatforms = new ArrayList<>();

            // Initialize nodes
            for(int i=0;i<total_pipeline_nodes.get();i++)
                nodes.add(new Tuple2<>(profilingConfig.getUnaryExecutionOperators().get(0),profilingConfig.getProfilingPlateform()));

            // Get exhaustive operator-plateform generation
            switch (pruning){
                case "exhaustive":
                    ExhaustiveEnumeration.doubleRecursiveEnumeration(nodes, profilingConfig.getUnaryExecutionOperators() , profilingConfig.getProfilingPlateform(), 0, total_pipeline_nodes.get() , totalEnumOpertorsPlatforms);
                    break;
                case "maxPlatSwitch":
                    MaxPlatformPruningEnumeration.doubleRecursiveEnumerationWithSwitchPruning(nodes, profilingConfig.getUnaryExecutionOperators() , profilingConfig.getProfilingPlateform(), 0, profilingConfig.getMaxPlatformSwitch(), total_pipeline_nodes.get() , totalEnumOpertorsPlatforms);
                    break;
            }

            // all pipelines node filling counter
            int enumCounter=0;
            // Loop through all combinations
            for(List<Tuple2<String,String>> OPenumeration:totalEnumOpertorsPlatforms) {
                // Fill with unary executionOperator profilers
                for (Topology t : shape.getPipelineTopologies()) {
                    int topologyNodeNumber = 0;
                    // check if the nodes are not already filled in the source or sink
                    if (t.getNodes().isEmpty())
                        // Fill with source operators
                        if (t.isSource()) {
                            topologyNodeNumber = t.getNodeNumber() - 1;
                            PlatformRnd = (int)(Math.random() * profilingConfig.getProfilingPlateform().size());
                            platform = profilingConfig.getProfilingPlateform().get(PlatformRnd);
                            // fill first node
                            t.getNodes().push(sourceNodeFill(type, platform));
                            t.addPlatform(platform);
                        }
                        else
                            topologyNodeNumber = t.getNodeNumber();
                        for (int i = 1; i <= topologyNodeNumber; i++) {
                            // Get enumerated platform
                            tmpOperator = OPenumeration
                                    .get(enumCounter)
                                    .field0;
                            // Get enumerated operator
                            tmpPlateform = OPenumeration
                                    .get(enumCounter)
                                    .field1;
                            t.getNodes().push(UnaryNodeFill(type,tmpPlateform, tmpOperator));
                            t.addPlatform(tmpPlateform);
                            enumCounter++;
                        }
                }
                // Reinitialize all pipelines node filling counter
                enumCounter = 0;

                // Fill with binary executionOperator profilers
                for (Topology t : shape.getJunctureTopologies()) {
                    // check if the nodes are not already filled in the source or sink
                    PlatformRnd = (int)(Math.random() * profilingConfig.getProfilingPlateform().size());
                    platform = profilingConfig.getProfilingPlateform().get(PlatformRnd);
                    t.getNodes().push(binaryNodeFill(type, platform));
                    t.addPlatform(platform);
                }

                // Fill the loop topologies
                for (Topology t : shape.getLoopTopologies()) {
                    // if there's multiple platform; case of flink loop operator is avoided to avoid having non flink operator inside loop subPlan
                    if (profilingConfig.getProfilingPlateform().size() > 1)
                        while (platform.equals("flink")) {
                            // select a new platform randomly
                            PlatformRnd = (int) (Math.random() * profilingConfig.getProfilingPlateform().size());
                            platform = profilingConfig.getProfilingPlateform().get(PlatformRnd);
                        }
                    t.getNodes().push(loopNodeFill(type, platform));
                    t.addPlatform(platform);

                }

                // Fill sink topology
                prepareSink(shape.getSinkTopology(), type, platform, configuration);

                // Build the planProfiler
                buildPlanProfiler(shape, type);

                // add the filled shape to tempSubShapes
                tmpSubShapes.add(shape.clone());

                // Reset shape
                shape.resetAllNodes();

                // Add planProfiler
                planProfilers.add(planProfiler);
            }

        }
        shape.setExecutionShapes(tmpSubShapes);
        return planProfilers;
    }

    /**
     * Fills the toplogy instance with unary profiling operators
     * @return
     */
    private static Tuple2<String,List<OperatorProfiler>> exhaustiveUnaryNodeFill(DataSetType type, String plateform){
        int udfRnd = profilingConfig.getUdfsComplexity().get( (int)(Math.random() * profilingConfig.getUdfsComplexity().size()));
        // Generate all operator Profilers with the same execution platform
        List<OperatorProfiler> OperatorProfilerList = profilingConfig.getUnaryExecutionOperators().stream()
                .map(operator->ProfilingOperatorGenerator.getProfilingOperator(operator, type, plateform,1,udfRnd))
                .collect(Collectors.toList());

        Tuple2<String,List<OperatorProfiler>> returnNode = new Tuple2(plateform, ProfilingOperatorGenerator.getProfilingOperator(plateform, type, plateform,1,udfRnd));
        return returnNode;
    }

    /**
     * Generates a list that contains a {@param numberPlans} of random {@link PlanProfiler}s for the given {@param shape}
     * @param shape
     * @return
     */
    public static List<PlanProfiler> randomProfilingPlanBuilder(Shape shape, List<Shape> shapes, Configuration configuration){
        List<PlanProfiler> planProfilers = new ArrayList<>();
        PlanProfiler planProfiler = new PlanProfiler(shape, profilingConfig);
        List<Shape> tmpSubShapes = new ArrayList<>();
        // Loop through all dataTypes
        for(DataSetType type:profilingConfig.getDataType()){
            // Loop through all plateforms
            //for (String platform:profilingConfig.getProfilingPlateform()){
                // Set the shape's platform
            // TODO: the below set is not required
            List<String> platforms = profilingConfig.getProfilingPlateform();
            // select a new platform randomly
            int PlatformRnd = (int)(Math.random() * profilingConfig.getProfilingPlateform().size());
            String platform = profilingConfig.getProfilingPlateform().get(PlatformRnd);
                shape.setPlateform(platforms);
                //Loop through all unary operators
                // TODO: we have to multiply by number of pipelines existing in current shape and number of nodes per pipeline
                for (String unaryOperator:profilingConfig.getUnaryExecutionOperators()){
                    //Loop through all binary operators
                    for (String binaryOperator:profilingConfig.getBinaryExecutionOperators()){
                        //Loop through all loop operators
                        for (String loopOperator:profilingConfig.getLoopExecutionOperators()) {
                            // Fill the sources
                            for (Topology t : shape.getSourceTopologies()) {
                                //if (t.getNodes().isEmpty())
                                prepareSource(t, type, platforms);
                            }

                            // TODO: Fill pipeline topologies with exhaustive way (pass a list of operators (e.g:[map,map,map])   to be filled in the below loop)
                            // Fill with unary executionOperator profilers
                            for (Topology t : shape.getPipelineTopologies()) {
                                // check if the nodes are not already filled in the source or sink
                                if ((t.getNodes().isEmpty() || (!t.isSource())))
                                    for (int i = 1; i <= t.getNodeNumber(); i++) {
                                        // select a new platform randomly
                                        PlatformRnd = (int)(Math.random() * profilingConfig.getProfilingPlateform().size());
                                        platform = profilingConfig.getProfilingPlateform().get(PlatformRnd);
                                        // TODO: fill the nodes with the executionOperator list
                                        t.getNodes().push(randomUnaryNodeFill(type, platform));
                                        t.addPlatform(platform);

                                    }
                            }

                            // Fill with binary executionOperator profilers
                            for (Topology t : shape.getJunctureTopologies()) {
                                // select a new platform randomly
                                PlatformRnd = (int)(Math.random() * profilingConfig.getProfilingPlateform().size());
                                platform = profilingConfig.getProfilingPlateform().get(PlatformRnd);
                                // check if the nodes are not already filled in the source or sink
                                t.getNodes().push(binaryNodeFill(type, platform));
                                t.addPlatform(platform);
                            }

                            // Fill loop topologies
                            for (Topology t : shape.getLoopTopologies()) {
                                // select a new platform randomly
                                PlatformRnd = (int)(Math.random() * profilingConfig.getProfilingPlateform().size());
                                platform = profilingConfig.getProfilingPlateform().get(PlatformRnd);
                                // if there's multiple platform; case of flink loop operator is avoided to avoid having non flink operator inside loop subPlan
                                if(profilingConfig.getProfilingPlateform().size()>1)
                                    while(platform.equals("flink")){
                                        // select a new platform randomly
                                        PlatformRnd = (int)(Math.random() * profilingConfig.getProfilingPlateform().size());
                                        platform = profilingConfig.getProfilingPlateform().get(PlatformRnd);
                                    }
                                t.getNodes().push(loopNodeFill(type, platform));
                                t.addPlatform(platform);

                            }

                            // Fill the sinks
                            //for(Topology t:shape.getSinkTopology()){
                            //if (shape.getSinkTopology().getNodes().isEmpty())
                            prepareSink(shape.getSinkTopology(), type, platform, configuration);
                            //}

                            // Build the planProfiler
                            buildPlanProfiler(shape, type);

                            // add the filled shape to tempSubShapes
                            tmpSubShapes.add(shape.clone());

                            // Reset shape
                            shape.resetAllNodes();
                            
                            //planProfiler
                            planProfilers.add(planProfiler);
                        }
                    }
                }
            //}
        }
        shape.setExecutionShapes(tmpSubShapes);
        return planProfilers;
    }

    /**
     * Generates a list of exhaustive {@link PlanProfiler}s for the given {@link Shape}
     * @param shape
     * @return
     */
    public static List<PlanProfiler> ProfilingPlanBuilder(Shape shape){
        List<PlanProfiler> planProfilers = new ArrayList<>();
        PlanProfiler planProfiler = new PlanProfiler(shape, profilingConfig);

        // Start with the sink executionOperator
        //shape.getSinkTopology().getNode
        return planProfilers;
    }
        /**
         * Build the plan profiler; connect the nodes and build rheem plan;
         */
    private static void buildPlanProfiler(Shape shape, DataSetType type){

        // Start with the sink executionOperator
        Topology sinkTopology = shape.getSinkTopology();

        // Check the dataType
        checkDataType(shape, type);
        // recursively connect predecessor nodes

        Stack<Tuple2<String, OperatorProfiler>> sinkNodes = (Stack<Tuple2<String, OperatorProfiler>>) sinkTopology.getNodes().clone();

        //Tuple2<String, OperatorProfiler> sinkNode =sinkTopology.getNodes().pop();
        // Check if it's not already connected
        Tuple2<String, OperatorProfiler> lastConnectedNode = connectNodes(sinkTopology, sinkTopology.getNodes().pop(),0);


        sinkTopology.setNodes(sinkNodes);
        // connect the lastConnectedNode with the head node of successive topology
        //for

        //preparePlanProfiler(planProfiler);

    }

    private static void checkDataType(Shape shape, DataSetType type) {
        final DataSetType tst = DataSetType.createDefault(Integer.class);
        for (Topology t:shape.getAllTopologies()){
            t.getNodes().stream()
                    .forEach(n->{
                        OperatorProfiler op = n.getField1();
                        DataSetType opIn1DataType,opOutDataType;
                        // check the executionOperator type
                        // handle the source case
                        if (op.getExecutionOperator().isSource()) {
                            opIn1DataType = null;
                            opOutDataType = op.getExecutionOperator().getOutput(0).getType();
                        }else if (op.getExecutionOperator().isSink()) {
                            opIn1DataType = op.getExecutionOperator().getInput(0).getType();
                            opOutDataType = null;
                        }else {
                            opIn1DataType = op.getExecutionOperator().getInput(0).getType();
                            opOutDataType = op.getExecutionOperator().getOutput(0).getType();
                        }

                        // check if the executionOperator profiler has the required dataType
                        if((opIn1DataType!=null)&&(!opIn1DataType.equals(profilingConfig.getDataType()))){
                            // TODO: Need to change the executionOperator udf to be compatible with new datatype or create intermediate executionOperator
                            // Remplace the slot
                            op.getExecutionOperator().setInput(0,new InputSlot<>("in", op.getExecutionOperator(), type));
                        }

                        if((opOutDataType!=null)&&(!opOutDataType.equals(profilingConfig.getDataType()))){
                            // TODO: Need to change the executionOperator udf to be compatible with new datatype or create intermediate executionOperator
                            // Remplace the slot
                            op.getExecutionOperator().setOutput(0,new OutputSlot<>("out", op.getExecutionOperator(), type));
                        }
                    });

            if(profilingConfig.getDataType().equals(DataSetType.createDefault(Integer.class))){
                // Check that all dataTypes are conforming to the datatype otherwise change them accordingly

            } else if(profilingConfig.getDataType().equals(DataSetType.createDefault(String.class))){

            } else if(profilingConfig.getDataType().equals(DataSetType.createDefault(List.class))){

            }
        }
    }

    /**
     * The below method will connect nodes inside a topology and returns the head node so to connect it with the previous topology
     * @param topology
     * @return
     */
    private static Tuple2<String,OperatorProfiler> connectNodes(Topology topology, Tuple2<String, OperatorProfiler> currentnode, int inputSlot) {

        // check if the current topology is loop
        if (topology.isLoop())
            return connectLoopNodes((LoopTopology) topology, currentnode,inputSlot);
        Stack nodes = (Stack) topology.getNodes().clone();

        // previous = bottom to top ((previous) sink => (current) source)
        Tuple2<String, OperatorProfiler> previousNode = null;
        Tuple2<String, OperatorProfiler> currentNode = currentnode;

        // Loop through all the nodes
        while(!nodes.isEmpty()){
            previousNode = currentNode;
            currentNode = (Tuple2<String, OperatorProfiler>) nodes.pop();


                // TODO: check the dataType and correct it
                // connect previous with current node
                try{
                    currentNode.getField1().getExecutionOperator().connectTo(0,previousNode.getField1().getExecutionOperator(),inputSlot);
                } catch (Exception e){
//                    logger.error(e.getMessage());
                }

                // Reset inputSlot to 0 after connecting the last pipeline node with the jucture topology's node
                // so to avoid error when connecting the nodes of a pipeline topology
                if (inputSlot!=0)
                    inputSlot = 0;
        }

        // at the end of this method should connect the lastConnectedNode with the head node of predecessor topology
        // recurse the predecessor tpgs
        //get the predecessors of tmp topology
        if  (!(topology.getInput(0).getOccupant()==null)){
            List<Topology> predecessors = topology.getPredecessors();
            int inputslot = 0;
            for(Topology t:predecessors){
                connectNodes(t, currentNode, inputslot);
                inputslot++;
            }

        }

        return currentNode;
    }

    /**
     * Connects a loop topology what's inside a loop then returns the source node (previous node to a loop head )
     * @param loopTopology
     * @param currentnode
     * @param inputSlot
     * @return
     */
    private static Tuple2<String,OperatorProfiler> connectLoopNodes(LoopTopology loopTopology, Tuple2<String, OperatorProfiler> currentnode, int inputSlot) {

        Stack nodes = (Stack) loopTopology.getNodes().clone();


        if (loopTopology.equals(currentLoop)){
            // should stop the current loop execution adn return the initialization input

            // thereis connect here Loop => initial iteration on the current node
            Tuple2<String, OperatorProfiler> loopNode = currentnode;
            loopNode = (Tuple2<String, OperatorProfiler>) nodes.pop();

            loopNode.getField1().getExecutionOperator().connectTo(LoopTopology.ITERATION_OUTPUT_INDEX,
                    currentnode.getField1().getExecutionOperator(),inputSlot);

            // return null
            return new Tuple2<>();
        }

        currentLoop = loopTopology;
        Tuple2<String, OperatorProfiler> previousNode = null;
        Tuple2<String, OperatorProfiler> loopNode = currentnode;

        if (loopNode.getField1().getExecutionOperator().isSink()){
            // Handle the case where loop topology is sink topology too

            // update the nodes
            // previous = bottom to top ((previous) sink => (current) source)
            previousNode=loopNode;
            loopNode = (Tuple2<String, OperatorProfiler>) nodes.pop();

            // connect the loop topology to final output
            loopNode.getField1().getExecutionOperator().connectTo(LoopTopology.FINAL_OUTPUT_INDEX,previousNode.getField1().getExecutionOperator(),inputSlot);
            // Get the start Iteration Topology
            Topology startInterationTopology = loopTopology.getLoopBodyInput();

            // Get the final Iteration Topology
            Topology endIterationTopology = loopTopology.getLoopBodyOutput();

            // connect final Iteration Topology to loop topology
            connectNodes(endIterationTopology, loopNode,LoopTopology.ITERATION_INPUT_INDEX);
            // Reset current loop

        }
        // TODO: Handle the case where the loop is not a sink topology

        //return connectNodes(loopTopology,loopNode,inputSlot);

        //get the initial topology of loop topology
        if  (!(loopTopology.getInput(0).getOccupant()==null)){
            //If exist
            List<Topology> predecessors = loopTopology.getPredecessors();

            // this will connect the INITIAL_INPUT_INDEX
            //connectNodes(predecessors.get(0), loopNode, LoopTopology.INITIAL_INPUT_INDEX);
            Tuple2<String, OperatorProfiler> initialInputIndexNode = predecessors.get(0).getNodes().peek();
            //initialInputIndexNode.getField1().getExecutionOperator().connectTo(0,loopNode.getField1().getExecutionOperator(),LoopTopology.INITIAL_INPUT_INDEX);

            // connect the nodes of the initialization topology
            connectNodes(predecessors.get(0), loopNode,LoopTopology.INITIAL_INPUT_INDEX);
        } else{
            // means the loop is a source node too
            // connect the source node to loop node
            Tuple2<String, OperatorProfiler> sourceNode;
            sourceNode = (Tuple2<String, OperatorProfiler>) nodes.pop();

            sourceNode.getField1().getExecutionOperator().connectTo(0,loopNode.getField1().getExecutionOperator(), LoopTopology.INITIAL_INPUT_INDEX);
        }

        currentLoop = new LoopTopology(0,0);

        // should

        return loopNode;
    }

    private static void preparePlanProfiler(Topology topology) {
        //for(Topology t:planProfiler.ge)
    }

    /**
     * Add nodes to source Topology (i.e. pipeline topology)
     * PS: Source node i counted in the node number
     * @param topology
     */
    private static void prepareSource(Topology topology, DataSetType type, List<String> plateforms) {
        // select teh first platform to appear in the config file for the source operators
        int PlatformRnd;
        String plateform = profilingConfig.getProfilingPlateform().get(0);
        // add the first source node
        topology.getNodes().push(sourceNodeFill(type, plateform));
        topology.addPlatform(plateform);

        // exit in the case of a loop
        if (topology.isLoop())
                return;

        // add the remaining nodes with unary nodes
        for(int i=1;i<=topology.getNodeNumber();i++) {
            // re-select a new platform randomly
            PlatformRnd = (int)(Math.random() * profilingConfig.getProfilingPlateform().size());
            plateform = profilingConfig.getProfilingPlateform().get(PlatformRnd);
            topology.getNodes().push(randomUnaryNodeFill(type, plateform));
            topology.addPlatform(plateform);
        }
    }

    /**
     * Add nodes to source Topology (i.e. pipeline topology)
     * PS: Source node i counted in the node number
     * @param topology
     */
    private static void prepareSourceExhaustive(Topology topology, DataSetType type, List<String> plateforms) {
        // select teh first platform to appear in the config file for the source operators
        int PlatformRnd;
        String plateform = profilingConfig.getProfilingPlateform().get(0);
        // add the first source node
        topology.getNodes().push(sourceNodeFill(type, plateform));
        topology.addPlatform(plateform);

        // exit in the case of a loop
        if (topology.isLoop())
            return;

        // add the remaining nodes with unary nodes
        for(int i=1;i<=topology.getNodeNumber();i++) {
            // re-select a new platform randomly
            PlatformRnd = (int)(Math.random() * profilingConfig.getProfilingPlateform().size());
            plateform = profilingConfig.getProfilingPlateform().get(PlatformRnd);
            topology.getNodes().push(randomUnaryNodeFill(type, plateform));
            topology.addPlatform(plateform);
        }
    }


    /**
     * Add the sink to the sink topology; NOTE: that the sink is node included as a node
     * @param topology
     */
    private static void prepareSink(Topology topology, DataSetType type, String plateform, Configuration configuration) {

        // check if the sink is already filled as pipeline/source Topology.
        if (topology.getNodes().empty()){
            //Add the first unary nodes
            for(int i=1;i<=topology.getNodeNumber();i++)
                topology.getNodes().push(randomUnaryNodeFill(type, plateform));
                topology.addPlatform(plateform);
            // Add the last sink node
            topology.getNodes().push(sinkNodeFill(type, plateform, configuration));
            topology.addPlatform(plateform);

        } else{
            topology.getNodes().push( sinkNodeFill(type, plateform, configuration));
            topology.addPlatform(plateform);
        }

    }

    private static void prepareSinkLoop(LoopTopology topology) {
    }

    /**
     * Fills the toplogy instance with unary profiling operators
     * @return
     */
    private static Tuple2<String,OperatorProfiler> sinkNodeFill(DataSetType type, String plateform, Configuration configuration){
        // we currently support use the collection source
        String operator = profilingConfig.getSinkExecutionOperators().get(0);
        Tuple2<String,OperatorProfiler> returnNode =new Tuple2(operator, ProfilingOperatorGenerator.getProfilingOperator(operator, type, profilingConfig.getSinkPlatform(),1,1)) ;
        returnNode.getField1().setUDFcomplexity(1);
        return returnNode;
    }

    /**
     * Fills the toplogy instance with unary profiling operators
     * @return
     */
    private static Tuple2<String,OperatorProfiler> sourceNodeFill(DataSetType type, String plateform){
        // we currently support collection source
        String operator = profilingConfig.getSourceExecutionOperators().get(1);
        Tuple2<String,OperatorProfiler> returnNode = new Tuple2(operator, ProfilingOperatorGenerator.getProfilingOperator(operator, type, plateform,1,1));
        returnNode.getField1().setUDFcomplexity(1);
        return returnNode;
    }

    /**
     * Fills the toplogy instance with unary profiling operators
     * @return
     */
    private static Tuple2<String,OperatorProfiler> randomUnaryNodeFill(DataSetType type, String plateform){
        int rnd = (int)(Math.random() * profilingConfig.getUnaryExecutionOperators().size());
        int udfRnd = profilingConfig.getUdfsComplexity().get( (int)(Math.random() * profilingConfig.getUdfsComplexity().size()));
        String operator = profilingConfig.getUnaryExecutionOperators().get(rnd);
        Tuple2<String,OperatorProfiler> returnNode = new Tuple2(operator, ProfilingOperatorGenerator.getProfilingOperator(operator, type, plateform,1,udfRnd));
        returnNode.getField1().setUDFcomplexity(udfRnd);
        return returnNode;
    }




    /**
     * Fills the toplogy instance with unary profiling operators
     * @return
     */
    private static Tuple2<String,OperatorProfiler> UnaryNodeFill(DataSetType type, String plateform, String operator){
        int udfRnd = 1 + (int)(Math.random() * profilingConfig.getUdfsComplexity().size());
        Tuple2<String,OperatorProfiler> returnNode = new Tuple2(operator, ProfilingOperatorGenerator.getProfilingOperator(operator, type, plateform,1,udfRnd));
        returnNode.getField1().setUDFcomplexity(udfRnd);
        return returnNode;
    }

    /**
     * Fills the toopology instance with binary profiling executionOperator
     * @return
     */
    private static Tuple2<String,OperatorProfiler> binaryNodeFill(DataSetType type, String plateform){
        int rnd = (int)(Math.random() * profilingConfig.getBinaryExecutionOperators().size());
        int udfRnd = profilingConfig.getUdfsComplexity().get( (int)(Math.random() * profilingConfig.getUdfsComplexity().size()));
        String operator = profilingConfig.getBinaryExecutionOperators().get(rnd);
        return new Tuple2(operator, ProfilingOperatorGenerator.getProfilingOperator(operator, type, plateform,1,udfRnd));
    }

    /**
     * Fills the toopology instance with binary profiling executionOperator
     * @return
     */
    private static Tuple2<String,OperatorProfiler> binaryNodeFill(DataSetType type, String plateform, String operator){
        int udfRnd = 1 + (int)(Math.random() * profilingConfig.getUdfsComplexity().size());
        Tuple2<String,OperatorProfiler> returnNode = new Tuple2(operator, ProfilingOperatorGenerator.getProfilingOperator(operator, type, plateform,1,udfRnd));
        returnNode.getField1().setUDFcomplexity(udfRnd);
        return returnNode;
    }

    /**
     * Fills the toopology instance with loop profiling executionOperator
     * @return
     */
    private static Tuple2<String,OperatorProfiler> loopNodeFill(DataSetType type, String plateform){
        int rnd = (int)(Math.random() * profilingConfig.getLoopExecutionOperators().size());
        int udfRnd = profilingConfig.getUdfsComplexity().get( (int)(Math.random() * profilingConfig.getUdfsComplexity().size()));
        String operator = profilingConfig.getLoopExecutionOperators().get(rnd);
        return new Tuple2(operator, ProfilingOperatorGenerator.getProfilingOperator(operator, type, plateform,1,udfRnd));
    }


    /**
     * NOTE: THE BELOW IS OBSOLETE (NOT update/tested was used for single operator profiling)
     */




    /**
     * Builds all possible combinations of profiling plans of the input {@link Topology}ies
     * @param topologies
     * @param profilingConfiguration
     * @return List of all sink profiling plans
     */

    public static List<List<OperatorProfiler>> exhaustiveProfilingTopologyPlanBuilder(List<Topology> topologies,ProfilingConfig profilingConfiguration){
        profilingConfig = profilingConfiguration;
        assert (topologies.size()!=0);

        List<List<OperatorProfiler>> topologyPlanProfilers = new ArrayList<>();

        for(Topology t:topologies){
            if (profilingConfig.getProfilingPlanGenerationEnumeration().equals("exhaustive")){
                topologyPlanProfilers.add(singleExhaustiveProfilingPlanBuilder2(t));
            } else {
                // TODO: add more profiling plan generation enumeration: random, worstPlanGen (more execution time),betterPlanGen (less execution time)
                topologyPlanProfilers.add(singleExhaustiveProfilingPlanBuilder2(t));
            }
        }
        return topologyPlanProfilers;
    }



    public static List<OperatorProfiler> singleExhaustiveProfilingPlanBuilder2(Topology topology){
        List<OperatorProfiler> planProfilers = null;

        return planProfilers;
    }





    /**
     * Builds a profiling plan for a pipeline Topology
     * @param shape
     * @param profilingConfiguration
     * @return
     */
    public static List<PlanProfiler> pipelineProfilingPlanBuilder(Shape shape,ProfilingConfig profilingConfiguration){
        profilingConfig = profilingConfiguration;
        if (profilingConfig.getProfilingPlanGenerationEnumeration().equals("exhaustive")){
            return exhaustivePipelineProfilingPlanBuilder(shape);
        } else {
            // TODO: add more profiling plan generation enumeration: random, worstPlanGen (more execution time),betterPlanGen (less execution time)
            return exhaustivePipelineProfilingPlanBuilder(shape);
        }
    }

    /**
     * To remove
     * @param shape
     * @param profilingConfiguration
     * @return
     */
    public static List<PlanProfiler> ProfilingPlanBuilder(Shape shape, ProfilingConfig profilingConfiguration){
        profilingConfig = profilingConfiguration;
        if (profilingConfig.getProfilingPlanGenerationEnumeration().equals("exhaustive")){
            return exhaustivePipelineProfilingPlanBuilder(shape);
        } else {
            // TODO: add more profiling plan generation enumeration: random, worstPlanGen (more execution time),betterPlanGen (less execution time)
            return exhaustivePipelineProfilingPlanBuilder(shape);
        }
    }



    /**
     * Still for test
     * @param shape
     * @return
     */

    private static List<PlanProfiler> exhaustivePipelineProfilingPlanBuilder(Shape shape) {
        List<PlanProfiler> profilingPlans = new ArrayList<>();

        PlanProfiler planProfiler = new PlanProfiler(shape, profilingConfig);
        // Set the pla Profiler

            // Set source executionOperator profiler
            planProfiler.setSourceOperatorProfiler(SparkOperatorProfilers.createSparkCollectionSourceProfiler(1,String.class));
            // Set unary executionOperator profiler
            planProfiler.setUnaryOperatorProfilers(Arrays.asList(SparkOperatorProfilers.createSparkMapProfiler(1,3)));
            // Set sink executionOperator profiler
            planProfiler.setSinkOperatorProfiler(SparkOperatorProfilers.createSparkLocalCallbackSinkProfiler(1));

        profilingPlans.add(planProfiler);
        return profilingPlans;
    }


    public static List< ? extends OperatorProfiler> PlanBuilder(Topology topology, ProfilingConfig profilingConfig){
        //topology = topology;
        //profilingConfig = profilingConfig;

        // check the topology node number
        if (topology.getNodeNumber()==1){
            return singleOperatorProfilingPlanBuilder(profilingConfig);
        } else {
            return singleOperatorProfilingPlanBuilder(profilingConfig);
        }
    }

    /**
     * Builds an executionOperator profiling specifically for single executionOperator profiling with fake jobs
     * @param profilingConfig
     * @return
     */
    public static List<? extends OperatorProfiler> singleOperatorProfilingPlanBuilder(ProfilingConfig profilingConfig){
        if (profilingConfig.getProfilingPlanGenerationEnumeration().equals("exhaustive")){
            return exhaustiveSingleOperatorProfilingPlanBuilder(profilingConfig.getProfilingPlateform().get(0));
        } else {
            // TODO: add more profiling plan generation enumeration: random, worstPlanGen (more execution time),betterPlanGen (less execution time)
            return exhaustiveSingleOperatorProfilingPlanBuilder(profilingConfig.getProfilingPlateform().get(0));
        }
    }

    // The below is for single executionOperator profiling
    /**
     * Builds an executionOperator profiling specifically for single executionOperator profiling with fake jobs
     * @param Plateform
     * @return
     */
    private static List<? extends OperatorProfiler> exhaustiveSingleOperatorProfilingPlanBuilder(String Plateform) {
        List<String> operators = new ArrayList<String>(Arrays.asList("textsource","map", "collectionsource",  "filter", "flatmap", "reduce", "globalreduce", "distinct", "distinct-string",
                "distinct-integer", "sort", "sort-string", "sort-integer", "count", "groupby", "join", "union", "cartesian", "callbacksink", "collect",
                "word-count-split", "word-count-canonicalize", "word-count-count"));
        List<SparkOperatorProfiler> profilingOperators = new ArrayList<>();

        for (String operator : operators) {
            switch (operator) {
                case "textsource":
                    profilingOperators.add(SparkOperatorProfilers.createSparkTextFileSourceProfiler(1));
                    break;
                case "collectionsource":
                    profilingOperators.add(SparkOperatorProfilers.createSparkCollectionSourceProfiler(1));
                    break;
                case "map":
                    profilingOperators.add(SparkOperatorProfilers.createSparkMapProfiler(1, 1));
                    break;
                case "filter":
                    profilingOperators.add(SparkOperatorProfilers.createSparkFilterProfiler(1, 1));
                    break;
                case "flatmap":
                    profilingOperators.add(SparkOperatorProfilers.createSparkFlatMapProfiler(1, 1));
                    break;
                case "reduce":
                    profilingOperators.add(SparkOperatorProfilers.createSparkReduceByProfiler(1, 1));
                    break;
                case "globalreduce":
                    profilingOperators.add(SparkOperatorProfilers.createSparkGlobalReduceProfiler(1, 1));
                    break;
                case "distinct":
                case "distinct-string":
                    profilingOperators.add(SparkOperatorProfilers.createSparkDistinctProfiler(1));
                    break;
                case "distinct-integer":
                    profilingOperators.add(SparkOperatorProfilers.createSparkDistinctProfiler(
                            DataGenerators.createReservoirBasedIntegerSupplier(new ArrayList<>(), 0.7d, new Random(42)),
                            Integer.class,
                            configuration
                    ));
                    break;
                case "sort":
                case "sort-string":
                    profilingOperators.add(SparkOperatorProfilers.createSparkSortProfiler(1));
                    break;
                case "sort-integer":
                    profilingOperators.add(SparkOperatorProfilers.createSparkSortProfiler(
                            DataGenerators.createReservoirBasedIntegerSupplier(new ArrayList<>(), 0.7d, new Random(42)),
                            Integer.class,
                            configuration
                    ));
                    break;
                case "count":
                    profilingOperators.add(SparkOperatorProfilers.createSparkCountProfiler(1));
                    break;
                case "groupby":
                    profilingOperators.add(SparkOperatorProfilers.createSparkMaterializedGroupByProfiler(1, 1));
                    break;
                case "join":
                    profilingOperators.add(SparkOperatorProfilers.createSparkJoinProfiler(1, 1));
                    break;
                case "union":
                    profilingOperators.add(SparkOperatorProfilers.createSparkUnionProfiler(1));
                    break;
                case "cartesian":
                    profilingOperators.add(SparkOperatorProfilers.createSparkCartesianProfiler(1));
                    break;
                case "callbacksink":
                    profilingOperators.add(SparkOperatorProfilers.createSparkLocalCallbackSinkProfiler(1));
                    break;
            }
        }
        return profilingOperators;
    }


}
