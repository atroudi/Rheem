package org.qcri.rheem.core.optimizer.mloptimizer.api;

//import org.qcri.rheem.basic.data.Tuple2;
//import org.qcri.rheem.basic.operators.LocalCallbackSink;
//import org.qcri.rheem.basic.operators.TextFileSource;
import org.qcri.rheem.core.api.Configuration;
import org.qcri.rheem.core.api.exception.RheemException;
import org.qcri.rheem.core.optimizer.OptimizationContext;
import org.qcri.rheem.core.optimizer.mloptimizer.LogGenerator;
import org.qcri.rheem.core.plan.executionplan.Channel;
import org.qcri.rheem.core.plan.executionplan.ExecutionTask;
import org.qcri.rheem.core.plan.rheemplan.*;
import org.qcri.rheem.core.platform.Junction;
import org.qcri.rheem.core.platform.Platform;
import org.qcri.rheem.core.util.fs.FileSystems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.AbstractMap.SimpleEntry;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by migiwara on 16/07/17.
 */
public class Shape {

    public static final int MAXIMUM_OPERATOR_NUMBER_PER_SHAPE = 100;

    //TODO: Add a vectorlog nested class for more readability purposes
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Configuration config;
    // subshapes that will have all exhaustive filled with different nodes;plateforms;Types of the same shape
    private List<Shape> subShapes = new ArrayList<>();
    private List<Topology> allTopologies = new ArrayList<>();
    private List<Topology> sourceTopologies = new ArrayList<>();
    private List<String> plateform;
    private List<PipelineTopology> pipelineTopologies = new ArrayList<>();
    private List<JunctureTopology> junctureTopologies = new ArrayList<>();
    private List<LoopTopology> loopTopologies = new ArrayList<>();
    private static Stack<LoopHeadOperator> loopHeads = new Stack();

    private int averageSelectivity = 1;
    private int averageComplexity = 1;
    private int numberOperators = 0;

    private final int VECTOR_SIZE = 251;
    double[] vectorLogs= new double[VECTOR_SIZE];
    double[][] vectorLogs2D= new double[10][VECTOR_SIZE];

    double[] vectorLogsWithResetPlatforms= new double[VECTOR_SIZE];
    private int topologyNumber;
    private List<String> existingPlatforms = new ArrayList<>();
    private List<String> operatorNames = new ArrayList<>();
    private List<String> channelNames = new ArrayList<>();

    List<List<String>> operatorNames2d = new ArrayList<>();
    List<List<String>> operatorNamesPostExecution2d = new ArrayList<>();
    private List<Junction> junctions = new ArrayList<>();
    private List<ExecutionTask> executionTasks = new ArrayList<>();
    private List<double[]> exhaustiveVectors = new ArrayList<>();
    private static int startOpPos = 4;
    private static int opPosStep = 10;
    private static int channelPosStep = 4;
    private static int maxOperatorNumber = 19;
    private static int maxChannelNumber = 5;

    private double estimatedInputCardinality;
    private double estimatedDataQuataSize;

    public HashMap<String,Integer> OPERATOR_VECTOR_POSITION = new HashMap<String,Integer>(){{
        put("Map", startOpPos);put("map", startOpPos);
        put("filter", startOpPos + 1*opPosStep);put("FlatMap", startOpPos +2*opPosStep);put("flatmap", startOpPos +2*opPosStep);put("reduceby", startOpPos +3*opPosStep);
        put("reduce", startOpPos +3*opPosStep);put("globalreduce", startOpPos +4*opPosStep);put("distinct", startOpPos +5*opPosStep);put("groupby", startOpPos +6*opPosStep);put("materializedgroupby", startOpPos +6*opPosStep);put("globalmaterializedgroup", startOpPos +6*opPosStep);
        put("sort", startOpPos +7*opPosStep);put("join", startOpPos +8*opPosStep);put("unionall", startOpPos +9*opPosStep);put("union", startOpPos +9*opPosStep);put("cartesian", startOpPos +10*opPosStep);put("randomsample", startOpPos +11*opPosStep);
        put("shufflesample", startOpPos +12*opPosStep);put("bernoullisample", startOpPos +13*opPosStep);put("dowhile", startOpPos +14*opPosStep);put("repeat", startOpPos +15*opPosStep);
        put("collectionsource", startOpPos +16*opPosStep);put("textfilesource", startOpPos +17*opPosStep);put("textsource", startOpPos + 17*opPosStep);put("callbacksink", startOpPos +18*opPosStep);
        put("localcallbacksink", startOpPos + 18*opPosStep);put("collect", startOpPos + 19*opPosStep);
        // Below should be added in different position
        // DAtasetchannel is a flink type channel
        put("zipwithid", startOpPos + 19*opPosStep);put("cache", startOpPos + 19*opPosStep);put("count", startOpPos + 19*opPosStep);put("broadcast", startOpPos + 19*opPosStep);
    }};

    public HashMap<String,Integer> CHANNEL_VECTOR_POSITION = new HashMap<String,Integer>(){{
        put("CollectionChannel", startOpPos + (1+maxOperatorNumber)*opPosStep + 0*channelPosStep);
        put("StreamChannel", startOpPos + (1+maxOperatorNumber)*opPosStep + 1*channelPosStep);
        put("RddChannel", startOpPos + (1+maxOperatorNumber)*opPosStep + 2*channelPosStep);
        put("FileChannel", startOpPos + (1+maxOperatorNumber)*opPosStep + 3*channelPosStep);
        // Below should be added in different position
        // DAtasetchannel is a flink type channel
        put("DataSetChannel", startOpPos + (1+maxOperatorNumber)*opPosStep + 4*channelPosStep);
    }};

    public HashMap<String,Integer> CONVERSION_OPERATOR_VECTOR_POSITION = new HashMap<String,Integer>(){{
        put("collect", startOpPos + (1+maxOperatorNumber)*opPosStep + 5*channelPosStep);put("collectionsource",startOpPos + (1+maxOperatorNumber)*opPosStep + 6*channelPosStep);
        put("objectfilesource",startOpPos + (1+maxOperatorNumber)*opPosStep + 7*channelPosStep);
        put("cache", startOpPos + (1+maxOperatorNumber)*opPosStep + 8*channelPosStep);put("objectfilesink", startOpPos + (1+maxOperatorNumber)*opPosStep + 9*channelPosStep);
        put("collectionsink", startOpPos + (1+maxOperatorNumber)*opPosStep + 10*channelPosStep);
//        put("cache", startOpPos + (1+maxOperatorNumber)*opPosStep + 10*channelPosStep);
    }};

    static HashMap<String,Integer> OLD_CONVERSION_OPERATOR_VECTOR_POSITION = new HashMap<String,Integer>(){{
        put("JavaCollect", startOpPos + (1+maxOperatorNumber)*opPosStep + 4*channelPosStep);put("JavaCollectionSource",startOpPos + (1+maxOperatorNumber)*opPosStep + 5*channelPosStep);put("JavaObjectFileSink", startOpPos + (1+maxOperatorNumber)*opPosStep + 6*channelPosStep);
        put("JavaObjectFileSource",startOpPos + (1+maxOperatorNumber)*opPosStep + 7*channelPosStep);put("SparkCollect", startOpPos + (1+maxOperatorNumber)*opPosStep + 8*channelPosStep);put("SparkCollectionSource", startOpPos + (1+maxOperatorNumber)*opPosStep + 9*channelPosStep);
        put("SparkObjectFileSink", startOpPos + (1+maxOperatorNumber)*opPosStep + 10*channelPosStep);put("SparkObjectFileSource", startOpPos + (1+maxOperatorNumber)*opPosStep + 11*channelPosStep);
        // Below should be added in different position
        // DAtasetchannel is a flink type channel
        put("FlinkCollectionSink", startOpPos + (1+maxOperatorNumber)*opPosStep + 11*channelPosStep);
        put("FlinkCollectionSource", startOpPos + (1+maxOperatorNumber)*opPosStep + 11*channelPosStep);

    }};

    public static final List<String> DEFAULT_PLATFORMS = new ArrayList<>(Arrays.asList("Java Streams","Apache Spark","Apache Flink"));

    HashMap<String,Integer> plateformVectorPostion = new HashMap<String,Integer>(){{
        put("Java Streams",0);
        put("Apache Spark",1);
        put("Apache Flink",2);
    }};
    // TODO: Currently only single sink topology generation is supported
    private Topology sinkTopology;

    //private int nodeNumber = sinkTopologies.getNodeNumber()

    /**
     * Shape Constructor *empty
     * @param configuration
     */
    public Shape(Configuration configuration){
        this.config = configuration;
    }
    /**
     * Shape Constructor that creates a shape from a sink topology then filling the shape in down to up way
     * @param topology
     * @param configuration
     */
    public Shape(Topology topology, Configuration configuration){
        this.config = configuration;
        this.sinkTopology = topology;

        // set the shape nodenumber
        topologyNumber = topology.getTopologyNumber();
    }
    /**
     * generate a clone of the current Shape
     */

    // TODO: need maintainablility: whenever you add new variable to topology you have to
    public Shape clone(){
        Shape newShape = new Shape(this.sinkTopology.createCopy(this.getSinkTopology().getTopologyNumber()), this.config);
        newShape.populateShape(newShape.getSinkTopology());
        newShape.setPlateform(this.plateform);
        return newShape;
    }

    /**
     * empty all nodes in all topologies
     */

    public void resetAllNodes(){
        for(Topology t:allTopologies){
            t.setNodes(new Stack<>());
        }

    }


    /**
         * assign shape variables (i.e. number of pipelines; junctures; sinks;.. )
         * @param topology
         */
    public void populateShape(Topology topology){

        // Handle the case if the topology is pipeline Topology
        if (topology.isPipeline()){
            if (!this.pipelineTopologies.contains(topology))
                this.pipelineTopologies.add((PipelineTopology) topology);
            else
                // exit
                return;
            // Add to all topologies too
            this.allTopologies.add((PipelineTopology) topology);
            // get the predecessor of tmp topology
            if  (!(topology.getInput(0).getOccupant()==null)){
                List<Topology> predecessors = topology.getPredecessors();
                for(Topology t:predecessors)
                    // recurse for predecessor topologies
                    populateShape(t);
            } else {
                // This case means it's source topology
                sourceTopologies.add(topology);
            }

            // recurse the predecessor tpg
        } else if (topology.isJuncture()){
            // Handle the case if the topology is juncture Topology
            if (!this.junctureTopologies.contains(topology))
                this.junctureTopologies.add((JunctureTopology) topology);
            else
                // exit
                return;
            // Add to all topologies too
            this.allTopologies.add((JunctureTopology) topology);

            List<Topology> predecessors = topology.getPredecessors();

            //get the predecessors of tmp topology
            if  (!(predecessors.isEmpty())){
                //this.junctureTopologies.add((JunctureTopology) topology);
                for(Topology t:predecessors)
                    // recurse for predecessor topologies
                    populateShape(t);
            }else{
                // This case means it's source topology
                sourceTopologies.add(topology);
            }
        } else if (topology.isLoop()){
            if (!this.loopTopologies.contains(topology))
                this.loopTopologies.add((LoopTopology) topology);
            else
                // exit
                return;

            // Add to all topologies too
            this.allTopologies.add((LoopTopology) topology);
            List<Topology> predecessors = topology.getPredecessors();
            if (predecessors.size()==1){
                // imply it has only iteration input topology; no source topology
                this.sourceTopologies.add(topology);
            }
            if  (!(predecessors.isEmpty())){
                int index=0;
                for(Topology t:predecessors)
                    // recurse for predecessor topologies
                    populateShape(t);
            }
            // check if there's an output node connected to
        }
    }

    /**
     * Create a string to describe the shape; for printing purpose
     * @return
     */
    public String toString(){
        return"";
    }

    //int tmpstart =0;



    /**
     * Create a preExecution shape from a sink executionOperator
     * @param sinkOperator
     * @return
     */
    public static Shape createShape(UnarySink sinkOperator) {
        return createShape(sinkOperator,true, true);
    }

    /**
     * Create a shape from a sink executionOperator
     * @param sinkOperator
     * @param ispreExecution
     * @return
     */
    public static Shape createShape(UnarySink sinkOperator, boolean ispreExecution, boolean prepareVectorLog){
        Shape newShape = new Shape(Configuration.getDefaultConfiguration());

        // Initiate current and predecessor executionOperator
        Operator currentOperator = sinkOperator;
        final List<Operator> predecessorOperators = Arrays.stream(sinkOperator.getAllInputs())
                .map(inputSlot -> inputSlot.getOccupant().getOwner())
                .collect(Collectors.toList());
        //Operator predecessorOperator = sinkOperator.getInput(0).getOccupant().getOwner();
        // Declare and initialize predecessor executionOperator
        Operator predecessorOperator = predecessorOperators.get(0);
        ListIterator iterpredecessorOperators = predecessorOperators.listIterator();
        //for (Operator predecessorOperator:predecessorOperators){
        iterpredecessorOperators.next();
        while (iterpredecessorOperators.hasPrevious()){
            predecessorOperator = (Operator) iterpredecessorOperators.previous();
            // Loop until the source
            while (!predecessorOperator.isSource()) {
                // Handle pipeline cas
                // check if predecessor is unary executionOperator
                if (predecessorOperator instanceof UnaryToUnaryOperator) {
                    PipelineTopology newPipelineTopology = new PipelineTopology();

                    if (currentOperator.isSink()) {
                        newPipelineTopology.getNodes().add(new Tuple2<String, OperatorProfiler>(currentOperator.toString(), new OperatorProfilerBase(currentOperator)));
                        // add current topology as a sink topology
                        newShape.setSinkTopology(newPipelineTopology);
                    }

                    // add predecessor and current operatorNames
                    //newPipelineTopology.getNodes().add(new Tuple2<String,OperatorProfiler>(currentOperator.toString(), new OperatorProfilerBase(currentOperator)));
                    newPipelineTopology.getNodes().add(new Tuple2<String, OperatorProfiler>(predecessorOperator.toString(), new OperatorProfilerBase(predecessorOperator)));

                    // update current operatorNames
                    currentOperator = predecessorOperator;

                    // Handle broadcast case
                    // update predecessorOperators in case of multiple inputs
                    List<Operator> tmpPredecessorOperators = Arrays.stream(predecessorOperator.getAllInputs())
                            .map(inputSlot -> inputSlot.getOccupant().getOwner())
                            .collect(Collectors.toList());

                    // update predecessorOperators in case of multiple inputs
                    tmpPredecessorOperators.stream()
                            .forEach(operator -> {
                                if(tmpPredecessorOperators.indexOf(operator)>=1)
                                    iterpredecessorOperators.add(operator);
                            });

                    // update predecessor with the first input executionOperator
                    predecessorOperator = predecessorOperator.getInput(0).getOccupant().getOwner();


                    //for(Operator tmpPredecessorOperator:tmpPredecessorOperators) {
                    while (predecessorOperator instanceof UnaryToUnaryOperator) {
                        // add executionOperator to new topology
                        newPipelineTopology.getNodes().add(new Tuple2<String, OperatorProfiler>(predecessorOperator.toString(), new OperatorProfilerBase(predecessorOperator)));
                        // update current and predecessor operatorNames
                        currentOperator = predecessorOperator;

                        // get all predecessors
                        List<Operator> tmpPredecessorOperators2 = Arrays.stream(predecessorOperator.getAllInputs())
                                .map(inputSlot -> inputSlot.getOccupant().getOwner())
                                .collect(Collectors.toList());

                        // update predecessorOperators in case of multiple inputs
                        tmpPredecessorOperators2.stream()
                                .forEach(operator -> {
                                    if(tmpPredecessorOperators2.indexOf(operator)>=1)
                                        iterpredecessorOperators.add(operator);
                                });
                        // Handle broadcast case
                        predecessorOperator = tmpPredecessorOperators2.get(0);
                    }
                    //}
                    // check if the current node has source node
                    if (predecessorOperator.isSource()) {
                        newPipelineTopology.getNodes().add(new Tuple2<String, OperatorProfiler>(predecessorOperator.toString(), new OperatorProfilerBase(predecessorOperator)));
                        newShape.getSourceTopologies().add(newPipelineTopology);


                        //addSourceTopology(predecessorOp erator, newPipelineTopology);
                    }

                    newShape.getPipelineTopologies().add(newPipelineTopology);
                    newShape.getAllTopologies().add(newPipelineTopology);
                }

                //Juncture handling
                if (predecessorOperator instanceof BinaryToUnaryOperator) {
                    JunctureTopology newJunctureTopology = new JunctureTopology();
                    if (currentOperator.isSink()) {
                        newJunctureTopology.getNodes().add(new Tuple2<String, OperatorProfiler>(currentOperator.toString(), new OperatorProfilerBase(currentOperator)));
                        // add current topology as a sink topology
                        newShape.setSinkTopology(newJunctureTopology);
                    }
                    newJunctureTopology.getNodes().add(new Tuple2<String, OperatorProfiler>(predecessorOperator.toString(), new OperatorProfilerBase(predecessorOperator)));
                    newShape.getJunctureTopologies().add(newJunctureTopology);
                    newShape.getAllTopologies().add(newJunctureTopology);

                    // add executionOperator to new topology
                    newJunctureTopology.getNodes().add(new Tuple2<String, OperatorProfiler>(predecessorOperator.toString(), new OperatorProfilerBase(predecessorOperator)));

                    // update current and predecessor operatorNames
                    currentOperator = predecessorOperator;

                    // update predecessorOperators in case of multiple inputs
                    List<Operator> tmpPredecessorOperators = Arrays.stream(predecessorOperator.getAllInputs())
                            .map(inputSlot -> inputSlot.getOccupant().getOwner())
                            .collect(Collectors.toList());

                    // update predecessorOperators in case of multiple inputs
                    tmpPredecessorOperators.stream()
                            .forEach(operator -> {
                                if(tmpPredecessorOperators.indexOf(operator)>=1)
                                    iterpredecessorOperators.add(operator);
                            });

                    // update predecessor with the first input executionOperator
                    predecessorOperator = tmpPredecessorOperators.get(0);

                    if (predecessorOperator.isSource()) {
                        newJunctureTopology.getNodes().add(new Tuple2<String, OperatorProfiler>(predecessorOperator.toString(), new OperatorProfilerBase(predecessorOperator)));
                        newShape.getSourceTopologies().add(newJunctureTopology);
                        //addSourceTopology(predecessorOp erator, newPipelineTopology);
                    }
                }

                //Loop handling
                if (predecessorOperator instanceof LoopHeadOperator) {
                    LoopTopology newLoopTopology = new LoopTopology();
                    if (currentOperator.isSink()) {
                        newLoopTopology.getNodes().add(new Tuple2<String, OperatorProfiler>(currentOperator.toString(), new OperatorProfilerBase(currentOperator)));
                        // add current topology as a sink topology
                        newShape.setSinkTopology(newLoopTopology);

                    }
                    newLoopTopology.getNodes().add(new Tuple2<String, OperatorProfiler>(predecessorOperator.toString(), new OperatorProfilerBase(predecessorOperator)));
                    newShape.getLoopTopologies().add(newLoopTopology);
                    newShape.getAllTopologies().add(newLoopTopology);

                    // add executionOperator to new topology
                    newLoopTopology.getNodes().add(new Tuple2<String, OperatorProfiler>(predecessorOperator.toString(), new OperatorProfilerBase(predecessorOperator)));

                    // update current and predecessor operatorNames
                    currentOperator = predecessorOperator;

                    // check if the predecessor loop has been visited before (if no we visit iterIn"1" otherwise
                    // we visit InitIn"0")
                    if ((loopHeads.isEmpty()) || (loopHeads.peek() != predecessorOperator)) {
                        // Add current loopHead
                        loopHeads.add((LoopHeadOperator) predecessorOperator);
                        // Get the IterIn
                        predecessorOperator = predecessorOperator.getInput(1).getOccupant().getOwner();
                    } else {
                        // remove current loopHead
                        loopHeads.pop();
                        // Get the InitIn
                        predecessorOperator = predecessorOperator.getInput(0).getOccupant().getOwner();
                    }

                    if (predecessorOperator.isSource()) {
                        newLoopTopology.getNodes().add(new Tuple2<String, OperatorProfiler>(predecessorOperator.toString(), new OperatorProfilerBase(predecessorOperator)));
                        newShape.getSourceTopologies().add(newLoopTopology);
                        //addSourceTopology(predecessorOp erator, newPipelineTopology);
                    }
                }
            }
        }
        if (prepareVectorLog)
            newShape.prepareVectorLog(ispreExecution);
        return newShape;
    }

    /**
     * SETTERS & GETTERS
     *
     */

    public void setSinkTopology(Topology sinkTopology) {
        this.sinkTopology = sinkTopology;
    }

    public Topology getSinkTopology() {
        return sinkTopology;
    }
    public List<Shape> getSubShapes() {
        return subShapes;
    }

    public void setSubShapes(List<Shape> subShapes) {
        this.subShapes = subShapes;
    }


    public List<String> getPlateform() {
        return plateform;
    }

    public void setPlateform(List<String> plateform) {
        this.plateform = plateform;
    }

    public List<Topology> getAllTopologies() {
        return allTopologies;
    }

    public List<Topology> getSourceTopologies() {
        return sourceTopologies;
    }

    public List<PipelineTopology> getPipelineTopologies() {
        return pipelineTopologies;
    }

    public List<JunctureTopology> getJunctureTopologies() {
        return junctureTopologies;
    }

    public List<LoopTopology> getLoopTopologies() {
        return loopTopologies;
    }

    public void setLoopTopologies(List<LoopTopology> loopTopologies) {
        this.loopTopologies = loopTopologies;
    }

    public int getTopologyNumber() {
        return topologyNumber;
    }


    public void updateIteration() {
    }


    /*****************************************
     * LOG subclass: will contains all feature information fr the containing shape
     ********************************************/



    public void prepareVectorLog(boolean ispreExecution){

        double[] tmpVectorLogs1D = new double[VECTOR_SIZE];
        double[][] tmpVectorLogs2D= new double[10][VECTOR_SIZE];

        // Initialize the 2d Operator names\
        for (int i = 0; i < 10; i++) {
            operatorNames2d.add(new ArrayList<String>());
            operatorNamesPostExecution2d.add(new ArrayList<String>());
        }

        // Loop through all topologies
        this.allTopologies.stream()
                .forEach(t -> {
                    // Loop through all nodes
                    t.getNodes().stream()
                            .forEach(tuple ->{
                                int start = 4;
                                String[] operatorName = tuple.getField0().split("\\P{Alpha}+");
                                // remove platform prefix from operator
                                operatorName[0] = operatorName[0].toLowerCase().replace("java","").replace("spark","").replace("flink","");
                                if( config.getBooleanProperty("rheem.profiler.generate2dLogs",false)){
                                    // Handle the case of 2D generated logs

                                    // add topoliges to 2d vector log
                                    //tmpVectorLogs2D[0]=tmpVectorLogs1D;
                                    // Loop through all subShapes
                                    tmpVectorLogs2D[0][0]=this.getPipelineTopologies().size();
                                    tmpVectorLogs2D[0][1]=this.getJunctureTopologies().size();
                                    tmpVectorLogs2D[0][2]=this.getLoopTopologies().size();
                                    tmpVectorLogs2D[0][3]=0;
                                    // check if there's a duplicate executionOperator
                                    operatorNames2d.stream()
                                            .filter(list-> !list.contains(operatorName[0]))
                                            .findFirst()
                                            .map(list -> {
                                                int index = operatorNames2d.indexOf(list);

                                                list.add(operatorName[0]);
                                                addOperatorLog(tmpVectorLogs2D[index], t, tuple, operatorName[0],ispreExecution);
                                                return list;
                                            });
                                    //if (!operatorNames2d.get(0).contains(operatorName[0]))
                                }
                                //else {

                                // prepare 1D log vector in all cases
                                tmpVectorLogs1D[0]=this.getPipelineTopologies().size();
                                tmpVectorLogs1D[1]=this.getJunctureTopologies().size();
                                tmpVectorLogs1D[2]=this.getLoopTopologies().size();
                                tmpVectorLogs1D[3]=0;
                                // Handle the case of 1D generated logs
                                operatorNames.add(operatorName[0]);
                                addOperatorLog(tmpVectorLogs1D, t, tuple, operatorName[0],ispreExecution);
                                //}
                            });
                });
        averageSelectivityComplexity(tmpVectorLogs1D);

        if( config.getBooleanProperty("rheem.profiler.generate2dLogs",false)){
            // set shape's 2D vector log
            this.setVectorLogs2D(tmpVectorLogs2D.clone());
            // set shapes 1D vector log as the first row of the 2D vector log; as we will update the channels only for the first row
            this.setVectorLogs(tmpVectorLogs2D[0].clone());
        }
        // set shapes 1D vector log
        this.setVectorLogs(tmpVectorLogs1D.clone());

        // reinitialize log array every subShape
        Arrays.fill(tmpVectorLogs1D, 0);
    }

    /**
     * Add log for only one executionOperator
     * @param logs
     * @param t
     * @param tuple
     * @param operator
     * @param ispreExecution
     */
    private void addOperatorLog(double[] logs, Topology t, Tuple2<String, OperatorProfiler> tuple, String operator, boolean ispreExecution) {
        // check if the s
        //assert (OPERATOR_VECTOR_POSITION.get(s)!=null);
        if (ispreExecution)
            preFillLog((OperatorProfilerBase) tuple.getField1(),logs,t,getOperatorVectorPosition(operator));
        else
            fillLog(tuple,logs,t, getOperatorVectorPosition(operator));

    }

    private int getOperatorVectorPosition(String operator) {
        try {
            return OPERATOR_VECTOR_POSITION.get(operator.toLowerCase());
        } catch (Exception e){
            throw new RheemException(String.format("couldn't find position log for executionOperator %s",operator.toLowerCase()));
        }
    }

    private int getConversionOperatorVectorPosition(String operator) {
        try {
            return CONVERSION_OPERATOR_VECTOR_POSITION.get(operator.toLowerCase());
        } catch (Exception e){
            throw new RheemException(String.format("couldn't find position log for executionOperator %s",operator.toLowerCase()));
        }
    }

    /**
     * Calculate average selectivity
     * @param logs
     */
    void averageSelectivityComplexity(double[] logs){
        for(int i=9; i<=97; i=i+7){
            if(logs[i-4]+logs[i-5]!=0){
                logs[i]=logs[i]/(logs[i-4]+logs[i-5]);
                logs[i+1]=logs[i+1]/(logs[i-4]+logs[i-5]);
            }
        }
    }

    /**
     * Fill {@var vectorLogs} with rheem executionOperator parameters
     * @param operatorProfilerBase
     * @param logs
     * @param t
     * @param start
     */
    void preFillLog(OperatorProfilerBase operatorProfilerBase, double[] logs, Topology t, int start){

        //Tuple2<String,OperatorProfilerBase> tuple2 = (Tuple2<String,OperatorProfilerBase>) tuple;
        // TODO: if the executionOperator is inside pipeline and the pipeline is inside a loop body then the executionOperator should be put as pipeline and loop
        if (t.isPipeline())
            logs[start+4]+=1;
        else if(t.isJuncture())
            logs[start+5]+=1;
        if((t.isLoop())||(t.isLoopBody()))
            logs[start+6]+=1;

        // average complexity
        logs[start+7] += operatorProfilerBase.getUDFcomplexity();

        // average selectivity
        double  selectivity = 0;
        if ((!operatorProfilerBase.getRheemOperator().isSource())&&(!operatorProfilerBase.getRheemOperator().isSink())&&(operatorProfilerBase.getRheemOperator().isLoopHead()))
            // average selectivity of non source/sink/loop operatorNames
            selectivity= operatorProfilerBase.getRheemOperator().getOutput(0).getCardinalityEstimate().getAverageEstimate()/
                    operatorProfilerBase.getRheemOperator().getInput(0).getCardinalityEstimate().getAverageEstimate();
        else if(operatorProfilerBase.getRheemOperator().isSource())
            // case of source executionOperator we set the selectivity to 1
            selectivity = 1;
        else if(operatorProfilerBase.getRheemOperator().isLoopHead()){
            // case of a loop head (e.g: repeat executionOperator) we replace the selectivity with number of iterations
            LoopHeadOperator loopOperator = (LoopHeadOperator)operatorProfilerBase.getRheemOperator();
            selectivity = loopOperator.getNumExpectedIterations();
        }
        logs[start+8] += (int) selectivity;
        //TODO: duplicate and selectivity to be added
    }

    /**
     * Fill {@var vectorLogs} with execution executionOperator parameters
     * @param tuple
     * @param logs
     * @param t
     * @param start
     */
    void fillLog(Tuple2<String,OperatorProfiler> tuple, double[] logs, Topology t, int start){
        switch (tuple.getField1().getExecutionOperator().getPlatform().getName()){
            case "Java Streams":
                logs[start]+=1;
                break;
            case "Apache Spark":
                logs[start+1]+=1;
                break;
            case "Apache Flink":
                logs[start+2]+=1;
                break;
            default:
                throw new RheemException("wrong plateform!");
        }
        // TODO: if the executionOperator is inside pipeline and the pipeline is inside a loop body then the executionOperator should be put as pipeline and loop
        if (t.isPipeline())
            logs[start+4]+=1;
        else if(t.isJuncture())
            logs[start+5]+=1;
        if((t.isLoop())||(t.isLoopBody()))
            logs[start+6]+=1;

        // average complexity
        logs[start+7] += tuple.getField1().getUDFcomplexity();

        // average selectivity
        double  selectivity = 0;
        if ((!tuple.getField1().getExecutionOperator().isSource())&&(!tuple.getField1().getExecutionOperator().isSink())&&(!tuple.getField1().getExecutionOperator().isLoopHead()))
            // average selectivity of non source/sink/loop operatorNames
            selectivity= tuple.getField1().getExecutionOperator().getOutput(0).getCardinalityEstimate().getAverageEstimate()/
                    tuple.getField1().getExecutionOperator().getInput(0).getCardinalityEstimate().getAverageEstimate();
        else if(tuple.getField1().getExecutionOperator().isSource())
            // case of source executionOperator we set the selectivity to 1
            selectivity = 1;
        else if(tuple.getField1().getExecutionOperator().isLoopHead()){
            // case of a loop head (e.g: repeat executionOperator) we replace the selectivity with number of iterations
            LoopHeadOperator loopOperator = (LoopHeadOperator)tuple.getField1().getExecutionOperator();
            selectivity = loopOperator.getNumExpectedIterations();
        }
        logs[start+8] += (int) selectivity;
        //TODO: duplicate and selectivity to be added
    }

    public void setcardinalities(double inputCardinality, double dataQuantaSize) {
        if( config.getBooleanProperty("rheem.profiler.generate2dLogs",false)){
            vectorLogs2D[0][VECTOR_SIZE - 2] = inputCardinality;
            vectorLogs2D[0][VECTOR_SIZE - 1] = dataQuantaSize;
        }

        vectorLogs[VECTOR_SIZE - 2] = (int) inputCardinality;
        vectorLogs[VECTOR_SIZE - 1] = dataQuantaSize;
    }

    /**
     * Modify executionOperator cost
     * @param operator
     * @param outputCardinality
     * @param logVector
     */
    private void updateOperatorOutputCardinality(String operator, double outputCardinality, double[] logVector) {
        // get executionOperator position
        int opPos = getOperatorVectorPosition(operator);

        // update cost
        logVector[opPos + 9] = outputCardinality;
    }

    /**
     * Modify executionOperator cost
     * @param operator
     * @param outputCardinality
     * @param logVector
     */
    private void updateConversionOperatorOutputCardinality(String operator, double outputCardinality, double[] logVector) {
        // get executionOperator position
        int opPos = getConversionOperatorVectorPosition(operator);

        // update cost
        logVector[opPos + 3] = outputCardinality;
    }

    /**
     * Modify executionOperator cost
     * @param operator
     * @param inputCardinality
     * @param logVector
     */
    private void updateOperatorInputCardinality(String operator, double inputCardinality, double[] logVector) {
        // get executionOperator position
        int opPos = getOperatorVectorPosition(operator);

        // update cost
        logVector[opPos + 8] = inputCardinality;
    }

    /**
     * Modify executionOperator platform
     * @param operator
     * @param newPlatform
     * @param logVector
     */
    private void updateOperatorPlatform(String operator, String newPlatform, String replacePlatform, double[] logVector) {
        // get executionOperator position
        int opPos = getOperatorVectorPosition(operator);
        // reset all platforms to zero
        //plateformVectorPostion.entrySet().stream().
        //        forEach(tuple->logVector[opPos + tuple.getValue()] = 0);
//        if(replacePlatform!=null) {
//            if(replacePlatform=="Apache Flink")
//                logVector[opPos + plateformVectorPostion.get(replacePlatform)] -= 10;
//            else
//                logVector[opPos + plateformVectorPostion.get(replacePlatform)] -= 1;
//        }
//        if(newPlatform=="Apache Flink")
//            logVector[opPos + plateformVectorPostion.get(newPlatform)] += 10;
//        else
//            logVector[opPos + plateformVectorPostion.get(newPlatform)] += 1;

        if(replacePlatform!=null) {
                logVector[opPos + plateformVectorPostion.get(replacePlatform)] -= 1;
        }
            logVector[opPos + plateformVectorPostion.get(newPlatform)] += 1;

        // update platform
    }

    public void resetAllOperatorPlatforms() {
        vectorLogsWithResetPlatforms = vectorLogs.clone();
        for(String operator:operatorNames){
            // get executionOperator position
            int opPos = getOperatorVectorPosition(operator);
            // reset all platforms to zero
            plateformVectorPostion.entrySet().stream().
                    forEach(tuple->vectorLogsWithResetPlatforms[opPos + tuple.getValue()] = 0);
        }
    }

    /**
     * get executionOperator platform
     * @param operator
     * @param logVector
     */
    private String getOperatorPlatform(String operator, double[] logVector) {
        // get executionOperator position
//        int opPos = getOperatorVectorPosition(operator);
//        for (String platform : PLATFORMS) {
//            if (logVector[opPos + getPlatformVectorPosition(platform)] == 2)
//                return platform;
//            // check the case of flink
//            else if(((int)logVector[opPos + getPlatformVectorPosition(platform)]) % 10 == 2)
//                return PLATFORMS.get(2);
//        }
//        return null;
        int opPos = getOperatorVectorPosition(operator);
        for (String platform : DEFAULT_PLATFORMS) {
            if (logVector[opPos + getPlatformVectorPosition(platform)] >= 1)
                return platform;
                // check the case of flink
        }
        return null;
    }

    //private String[] platformVector = new String[MAXIMUM_OPERATOR_NUMBER_PER_SHAPE];

    /**
     * Platform exhaustive list per operator: Stores exhaustive generated platforms per operators
     */
    public ArrayList<String[]> exhaustivePlatformVectors = new ArrayList<>();


    public void exhaustivePlanPlatformFiller(List<String> platforms){
        resetAllOperatorPlatforms();
        // call exhaustive plan filler with new Platform: :spark" as currrently tested with only two platforms (java, spark)
        exhaustivePlanPlatformFiller(vectorLogsWithResetPlatforms, new String[MAXIMUM_OPERATOR_NUMBER_PER_SHAPE], platforms.get(1), platforms.get(0), 0, -1);
        for(int i=2;i<DEFAULT_PLATFORMS.size();i++){
            int finalI = i;
            exhaustivePlatformVectors.stream()
                    .forEach(platformVector-> exhaustivePlanPlatformFiller(vectorLogsWithResetPlatforms, platformVector, platforms.get(finalI),platforms.get(finalI-1), 0, -1));
        }
    }

    public void exhaustivePlanPlatformFiller(){

        resetAllOperatorPlatforms();
        // call exhaustive plan filler with new Platform: :spark" as currrently tested with only two platforms (java, spark)
        //exhaustivePlanPlatformFiller(vectorLogsWithResetPlatforms, new String[MAXIMUM_OPERATOR_NUMBER_PER_SHAPE], PLATFORMS.get(1), 0, -1);
        exhaustivePlanPlatformFiller(vectorLogsWithResetPlatforms, new String[MAXIMUM_OPERATOR_NUMBER_PER_SHAPE], DEFAULT_PLATFORMS.get(1), DEFAULT_PLATFORMS.get(0), 0, 1000);

        for(int i=2;i<DEFAULT_PLATFORMS.size();i++){
            int finalI = i;
            ArrayList<String[]> exhaustivePlatformVectorsCopy = (ArrayList) exhaustivePlatformVectors.clone();
            //for(String[] platformVector:exhaustivePlatformVectorsCopy){
            for(int j=0; j<exhaustivePlatformVectorsCopy.size();j++){
                exhaustivePlanPlatformFiller(this.exhaustiveVectors.get(j), exhaustivePlatformVectorsCopy.get(j), DEFAULT_PLATFORMS.get(finalI),DEFAULT_PLATFORMS.get(finalI-1), 0, -1);
            }
            exhaustivePlatformVectorsCopy = (ArrayList) exhaustivePlatformVectors.clone();
//            exhaustivePlatformVectors.stream()
//                    .forEach(platformVector-> exhaustivePlanPlatformFiller(vectorLogsWithResetPlatforms, platformVector, PLATFORMS.get(finalI), 0, -1));
        }
    }

    public void exhaustivePlanPlatformFiller(int exhaustivePlatformVectorsMaxBuffer){
        // call exhaustive plan filler with new Platform: :spark" as currrently tested with only two platforms (java, spark)
        //exhaustivePlanPlatformFiller(vectorLogsWithResetPlatforms, new String[MAXIMUM_OPERATOR_NUMBER_PER_SHAPE], PLATFORMS.get(1), 0, -1);
        exhaustivePlanPlatformFiller(this.getVectorLogs(), new String[MAXIMUM_OPERATOR_NUMBER_PER_SHAPE], DEFAULT_PLATFORMS.get(1),DEFAULT_PLATFORMS.get(0), 0, exhaustivePlatformVectorsMaxBuffer);

        for(int i=2;i<DEFAULT_PLATFORMS.size();i++){
            int finalI = i;
            ArrayList<String[]> exhaustivePlatformVectorsCopy = (ArrayList) exhaustivePlatformVectors.clone();
            //for(String[] platformVector:exhaustivePlatformVectorsCopy){
            for(int j=0; j<exhaustivePlatformVectorsCopy.size();j++){
                exhaustivePlanPlatformFiller(this.exhaustiveVectors.get(j), exhaustivePlatformVectorsCopy.get(j), DEFAULT_PLATFORMS.get(finalI),DEFAULT_PLATFORMS.get(finalI-1), 0, exhaustivePlatformVectorsMaxBuffer);
            }
            exhaustivePlatformVectorsCopy = (ArrayList) exhaustivePlatformVectors.clone();
//            exhaustivePlatformVectors.stream()
//                    .forEach(platformVector-> exhaustivePlanPlatformFiller(vectorLogsWithResetPlatforms, platformVector, PLATFORMS.get(finalI), 0, -1));
        }
    }
        /**
         * Will exhaustively generate all platform filled logVectors from the input logVector; and update  the platform vector will be used in the runner
         * PS: currently support only 1D vector log generation
         * @param vectorLog
         * @param newPlatform
         * @param startOperatorIndex
         */
    public void exhaustivePlanPlatformFiller(double[] vectorLog, String[] platformVector, String newPlatform,String replacePlatform, int startOperatorIndex, int exhaustivePlatformVectorsMaxBuffer){
        // if no generated plan fill it with equal values (all oerators in first platform java)

        // clone input vectorLog
        double[] newVectorLog = vectorLog.clone();
        String[] newPlatformLog = platformVector.clone();

        // Initial vectorLog filling: first platform is set for all operatorNames
        if (exhaustiveVectors.isEmpty()){
            int iteration=0;
            for(String operator: operatorNames){
                updateOperatorPlatform(operator,DEFAULT_PLATFORMS.get(0),null,newVectorLog);
                updatePlatformVector(iteration,DEFAULT_PLATFORMS.get(0),newPlatformLog);
                iteration++;
            }
            exhaustiveVectors.add(newVectorLog.clone());
            exhaustivePlatformVectors.add(newPlatformLog.clone());
            exhaustivePlanPlatformFiller(newVectorLog,newPlatformLog, newPlatform, replacePlatform, startOperatorIndex, exhaustivePlatformVectorsMaxBuffer);
            return;
        }

        // exit if @var{exhaustivePlatformVector} has reached buffer
        if((exhaustivePlatformVectorsMaxBuffer!=-1)&&(exhaustivePlatformVectorsMaxBuffer<exhaustivePlatformVectors.size()))
            //exit
            return;

        // Recursive exhaustive filling platforms for each executionOperator
        for(int i = startOperatorIndex; i< operatorNames.size(); i++){
            String operator = operatorNames.get(i);
            //TODO: change if to check if the number of platform for executionOperator meets the new required executionOperator number
            //if(!(getOperatorPlatform(operator,vectorLog)==newPlatform)){
                String tmpReplacePlatform = getOperatorPlatform(operator,vectorLog);
                // re-clone vectorLog
                newVectorLog = vectorLog.clone();
                // re-clone platformVector
                newPlatformLog = platformVector.clone();
                // update newVectorLog
                updateOperatorPlatform(operator,newPlatform,tmpReplacePlatform,newVectorLog);
                updatePlatformVector(i,newPlatform,newPlatformLog);

                // add current vector to exhaustiveVectors
                exhaustiveVectors.add(newVectorLog);
                exhaustivePlatformVectors.add(newPlatformLog);
                // recurse over newVectorLog
                exhaustivePlanPlatformFiller(newVectorLog,newPlatformLog , newPlatform, replacePlatform, i+1, exhaustivePlatformVectorsMaxBuffer);
            //}
        }
    }

    private void updatePlatformVector(int iteration, String platform, String[] newPlatformLog) {
        // update platform
        newPlatformLog[iteration] = platform;
    }


    /**
     * Logging {@link Shape}'s vector log
     */
    public String printLog() {
        final String[] outputVector = {""};
        NumberFormat nf = new DecimalFormat("##.#");
        Arrays.stream(vectorLogs).forEach(d -> {
            outputVector[0] = outputVector[0].concat( nf.format( d) + " ");
        });
        this.logger.info("Current rheem plan feature vector: " + outputVector[0]);
        return "Current rheem plan feature vector: " + outputVector[0];
    }

    /**
     * Logging {@link Shape}'s enumerated vector logs
     */
    public String printEnumeratedLogs() {
        final String[] finaloutputVector = {""};
        final String[] outputVector = {""};
        NumberFormat nf = new DecimalFormat("##.#");
        for(double[] vectorLog:exhaustiveVectors) {
            outputVector[0] = "";
            Arrays.stream(vectorLog).forEach(d -> {
                outputVector[0] = outputVector[0].concat(nf.format(d) + " ");
            });
            this.logger.info("Enumerated rheem plan feature vector: " + outputVector[0]);
            finaloutputVector[0] = finaloutputVector[0] + outputVector[0] + "\n";
        }
        return "Enumerated rheem plan feature vector: " + finaloutputVector[0];
    }


    public void updateChannels(Map<OutputSlot<?>, Junction> nodes) {
        nodes.values().stream()
                .forEach(junction -> {
                    // add junctions
                    junctions.add(junction);

                    // handle conversion tastks
                    junction.getConversionTasks().stream()
                            .forEach( conversionOperator-> {
                                executionTasks.add(conversionOperator);
                                addConversionOperator(conversionOperator);
                                addChannelLog(conversionOperator.getOutputChannel(0));
                            });

                    // add output channels
                    junction.getTargetChannels().stream().
                            forEach(outChannel->addChannelLog(outChannel));

                });

    }

    private void addConversionOperator(ExecutionTask conversionOperator) {
        String[] channelName = conversionOperator.toString().split("\\P{Alpha}+");
        //operatorName[0]=operator.toString().split("\\P{Alpha}+")[0]
                //.toLowerCase().replace("java","").replace("spark","").replace("flink","");
        // Each channel has 4 encoding digits as follow (number, consumer, producer, conversion)
        // get conversion operator
        String platform = conversionOperator.getPlatform().getName();

        int conversionOpStartPosition = getconversionOperatorVectorPosition(channelName[1].toLowerCase().replace("java","").replace("spark","").replace("flink",""));

        // update number of conversion operators for digit position 1
        vectorLogs[conversionOpStartPosition]+=1;

        // update the platform for digits position 2-3
        switch (platform){
            case "Java Streams":
                vectorLogs[conversionOpStartPosition+1]+=1;
                break;
            case "Apache Spark":
                vectorLogs[conversionOpStartPosition+2]+=1;
                break;
            case "Apache Flink":
                vectorLogs[conversionOpStartPosition+3]+=1;
                break;
            default:
                throw new RheemException("wrong plateform!");
        }

        //update with output cardinality in digit 4
        // get channel cardinality
        //double outputCard = conversionOperator.getOperator().getCardinalityEstimator(0);
    }

    private int getconversionOperatorVectorPosition(String conversionOperator) {
        try{
            return CONVERSION_OPERATOR_VECTOR_POSITION.get(conversionOperator);
        } catch (Exception e){
            throw new RheemException(String.format("couldn't find channel log vector offset %s",conversionOperator));
        }
    }

    private void addChannelLog(Channel outChannel) {
        String[] channelName = outChannel.toString().split("\\P{Alpha}+");
        channelNames.add(channelName[0]);
        // Each channel has 4 encoding digits as follow (number, consumer, producer, conversion)
        int channelStartPosition = getJunctureVectorPosition(channelName[0]);
        if( config.getBooleanProperty("rheem.profiler.generate2dLogs",false)){
            // add the channel log to the first row
            vectorLogs2D[0][channelStartPosition]+=1;
        }
        vectorLogs[channelStartPosition]+=1;
    }

    private int getJunctureVectorPosition(String channelName) {
        try{
            return CHANNEL_VECTOR_POSITION.get(channelName);
        } catch (Exception e){
        throw new RheemException(String.format("couldn't find channel log vector offset %s",channelName));
        }
    }

    private void addJunctureLog(Junction junction) {

    }

    /**
     * get platform log offset in a log vector
     * @param platform
     * @return
     */
    private int getPlatformVectorPosition(String platform) {
        try {
            return plateformVectorPostion.get(platform);
        } catch (Exception e){
            throw new RheemException(String.format("couldn't find a plateform for executionOperator %s",platform));
        }
    }

    private List<String> executionOperatorNames = new ArrayList<>();
    /**
     * update with execution executionOperator informations (plateform, output cardinality)
     * @param localOperatorContexts
     * @param updatePlatform
     */
    public void updateExecutionOperators(Map<Operator, OptimizationContext.OperatorContext> localOperatorContexts, boolean updatePlatform) {

        // First clear already contained platforms
        existingPlatforms.clear();

        // Loop through all operators
        localOperatorContexts.keySet().stream()
            .forEach(operator -> {
                // Composite operators discard
                if(operator.isElementary()) {
                    String[] executionOperatorName = new String[1];
                    double averageOutputCardinality = 0;
                    double averageInputCardinality = 0;

                    // Update executionOperator name
                    if(operator.isExecutionOperator()){
                        executionOperatorName[0]=operator.toString().split("\\P{Alpha}+")[0]
                                .toLowerCase().replace("java","").replace("spark","").replace("flink","");
                        ExecutionOperator executionOperator = (ExecutionOperator) operator;
                        // update platform
                        String platform = executionOperator.getPlatform().getName();

                        // update platform
                        existingPlatforms.add(platform);
                        if ((!platform.isEmpty())&&(updatePlatform))
                            updateOperatorPlatform(executionOperatorName[0],platform,null,vectorLogs);
                    } else {
                        Set<Platform> platform = operator.getTargetPlatforms();
                        executionOperatorName[0] = operator.toString().split("\\P{Alpha}+")[0].toLowerCase();
                        // update platform
                        if (updatePlatform)
                            platform.stream().forEach(p->updateOperatorPlatform(executionOperatorName[0],p.getName(),null,vectorLogs) );
                    }

                    if (localOperatorContexts.get(operator).getOutputCardinalities().length!=0){
                        // update present execution operator names
                        executionOperatorNames.add(executionOperatorName[0]);

                        // get average input/output cardinalities
                        averageOutputCardinality = localOperatorContexts.get(operator).getOutputCardinality(0).getAverageEstimate();
                        averageInputCardinality = Arrays.stream(localOperatorContexts.get(operator).getInputCardinalities())
                                .map(incard->(double)incard.getAverageEstimate())
                                .reduce(0.0,(av1,av2)->av1+av2);

                        Arrays.stream(localOperatorContexts.get(operator).getInputCardinalities()).forEach(incard1->incard1.getAverageEstimate());
                        // update shape's vector log with output cardinality
                        //updateOperatorOutputCardinality(operatorName[0],averageOutputCardinality,vectorLogs);
                        if(config.getBooleanProperty("rheem.profiler.generate2dLogs",false)){
                            double finalAverageOutputCardinality = averageOutputCardinality;
                            double finalAverageInputCardinality = averageInputCardinality;
                            operatorNamesPostExecution2d.stream()
                                    .filter(list-> !list.contains(executionOperatorName[0]))
                                    .findFirst()
                                    .map(list -> {
                                        int index = operatorNamesPostExecution2d.indexOf(list);
                                        list.add(executionOperatorName[0]);
                                        // check if the operator is a conversion op
                                        if(OPERATOR_VECTOR_POSITION.containsKey(executionOperatorName[0].toLowerCase()))
                                            // update shape's vector log with output cardinality
                                            updateOperatorOutputCardinality(executionOperatorName[0], finalAverageOutputCardinality,vectorLogs2D[index]);
                                        else
                                            updateConversionOperatorOutputCardinality(executionOperatorName[0], finalAverageOutputCardinality,vectorLogs2D[index]);
                                        // check if the operator is a conversion op
                                        if(OPERATOR_VECTOR_POSITION.containsKey(executionOperatorName[0].toLowerCase()))
                                            // update shape's vector log with target platform
                                            updateOperatorInputCardinality(executionOperatorName[0], finalAverageInputCardinality,vectorLogs2D[index]);
                                        //else

                                        return list;
                                    });
                        }

                        // Update 1d vector log
                        if(OPERATOR_VECTOR_POSITION.containsKey(executionOperatorName[0].toLowerCase()))
                            updateOperatorOutputCardinality(executionOperatorName[0],averageOutputCardinality,vectorLogs);
                        else
                            updateConversionOperatorOutputCardinality(executionOperatorName[0],averageOutputCardinality,vectorLogs);

                        if(OPERATOR_VECTOR_POSITION.containsKey(executionOperatorName[0].toLowerCase()))
                            // update shape's vector log with target platform
                            updateOperatorInputCardinality(executionOperatorName[0],averageInputCardinality,vectorLogs);

                        // update the estimate inputcardinality/dataQuantasize
                        if(localOperatorContexts.get(operator).getOperator().isSource()&&(localOperatorContexts.get(operator).getOperator() instanceof UnarySource)){
                            this.setEstimatedInputCardinality(averageOutputCardinality);
                            UnarySource textFileSource = (UnarySource) localOperatorContexts.get(operator).getOperator();
                            if(textFileSource.getInputUrl()!=null){
                                double fileSize = FileSystems.getFileSize(textFileSource.getInputUrl()).getAsLong();
                                // avoid having infinity
                                if (averageOutputCardinality!=0)
                                    this.setEstimatedDataQuataSize(fileSize/averageOutputCardinality);
                                else
                                    this.setEstimatedDataQuataSize(fileSize/1);
                            }
                            this.setcardinalities(this.estimatedInputCardinality,this.estimatedDataQuataSize);
                        }
                    }
                }
            });
    }


    // GETTERS AND SETTERS
    public double[] getVectorLogs() {
        return vectorLogs;
    }

    public double[][] getVectorLogs2D() {
        return vectorLogs2D;
    }

    public void setVectorLogs(double[] vectorLogs) {
        this.vectorLogs = vectorLogs;
    }

    public void setVectorLogs2D(double[][] vectorLogs2D) {
        this.vectorLogs2D = vectorLogs2D;
    }

    public void reinitializeLog() {
        operatorNames = new ArrayList<>();
        operatorNames2d = new ArrayList<>();
        operatorNamesPostExecution2d = new ArrayList<>();

        vectorLogs2D = new double[10][VECTOR_SIZE];
        vectorLogs = new double[VECTOR_SIZE];
    }

    // Feature vector length: startOpPos + maxOperatorNumber * opPosStep + maxChannelNumber * channelPosStep +

    public double getEstimatedInputCardinality() {
        return estimatedInputCardinality;
    }

    public void setEstimatedInputCardinality(double estimatedInputCardinality) {
        this.estimatedInputCardinality = estimatedInputCardinality;
    }

    public double getEstimatedDataQuataSize() {
        return estimatedDataQuataSize;
    }

    public void setEstimatedDataQuataSize(double estimatedDataQuataSize) {
        this.estimatedDataQuataSize = estimatedDataQuataSize;
    }

    // Get the vector logs metadata
    public String getVectorMetadata() {

        final String[] outputVector = {""};
        final int[] count = {0,0};
        final int[] opPos = {0};
        NumberFormat nf = new DecimalFormat("##.#");
        Arrays.stream(vectorLogs).forEach(d -> {
            if (count[0]==0) outputVector[0] = outputVector[0].concat("Topologies< ");
            //if (count[0]==4) outputVector[0] = outputVector[0].concat("> ");
            if ((count[0]!=0)&&((count[0]-startOpPos) % opPosStep==0)&&(opPos[0]<=maxOperatorNumber+1)) {
                outputVector[0] = outputVector[0].concat("> "+this.getSetElement(LogGenerator.OPERATOR_VECTOR_POSITION_WITHOUT_DUPLICATES.keySet(),opPos[0])+"< ");
                opPos[0]++;
            }
            // Handle channels printing
            if ((count[0]!=0)&&(opPos[0]>maxOperatorNumber+1)&&(opPos[0]<=maxChannelNumber+maxOperatorNumber+1)&&((count[0]-4) % channelPosStep==0)) {
                outputVector[0] = outputVector[0].concat("> "+this.getSetElement(CHANNEL_VECTOR_POSITION.keySet(),opPos[0]-maxOperatorNumber-2)+"< ");
                opPos[0]++;
            }
            // Handle channels printing
            if ((count[0]!=0)&&(opPos[0]>maxOperatorNumber+1)&&(opPos[0]>maxChannelNumber+maxOperatorNumber+1)&&((count[0]-4) % channelPosStep==0)) {
                outputVector[0] = outputVector[0].concat("> "+this.getSetElement(CONVERSION_OPERATOR_VECTOR_POSITION.keySet(),opPos[0]-maxOperatorNumber-2-maxChannelNumber)+"< ");
                opPos[0]++;
            }

//                    keySet().stream().reduce((key1,key2)->{
//                if (count[1]==opPos[0]) return key1;
//                count[1]++;
//            }));

            count[0]++;
            outputVector[0] = outputVector[0].concat( nf.format( d) + " ");
        });
        //this.logger.info("Best plan feature plan: " + outputVector[0]);
        return outputVector[0];    }


    private String getSetElement(Set<String>set,int pos){
        int count =0;
        List<String> indexes = new ArrayList<String>(set);
        for(String element:indexes){
            if (count==pos)
                return element;
            count++;
        }
        return "";
    }

    /**
     * Return a descriptive name of the shape's distinctive features in the following format:{@dPipelines@dJuntures@dLoops@dDuplicate@TopologyNumber@averageSelectivity@AverageComplexity@Operators}
     * @return
     */
    public String getDescriptionName() {
        final String[] descriptionName = {String.format("%dP%dJ%dL%dD_Sel%d_Compx%d_Operators",pipelineTopologies.size(),junctureTopologies.size(),loopTopologies.size(),1,averageSelectivity,averageComplexity)};

        // order the lists
        java.util.Collections.sort(operatorNames);
        java.util.Collections.sort(existingPlatforms);


        Map<String,Long> wordcountNames = operatorNames.stream()
                //.map(names->new Tuple2<>(names,1))
                .map(names->new SimpleEntry<>(names,1))
                .collect(
                    Collectors.groupingBy(SimpleEntry::getKey,Collectors.counting())
                );
                //);
                //.reduce((t1,t2)->new Tuple2<Integer, String>(t1.getField0()+t2.getField0(),t1.getField1()));
        wordcountNames.forEach((opName,opReccurence)->{
            //

            descriptionName[0]=descriptionName[0].concat("_"+opName);
            descriptionName[0]=descriptionName[0].concat(opReccurence.toString());
        });

        // Add platforms
        // count per platform
//        existingPlatforms.stream()
//                .forEach(p->{
//                    descriptionName[0]=descriptionName[0].concat("_"+p.replace(" ",""));
//                });

        Map<String,Long> wordcountPlatforms = existingPlatforms.stream()
                //.map(names->new Tuple2<>(names,1))
                .map(p->new SimpleEntry<>(p.replace(" ",""),1))
                .collect(
                        Collectors.groupingBy(SimpleEntry::getKey,Collectors.counting())
                );


        wordcountPlatforms.forEach((platName,platReccurence)->{
            //

            descriptionName[0]=descriptionName[0].concat("_"+platName);
            descriptionName[0]=descriptionName[0].concat(platReccurence.toString());
        });

        return descriptionName[0];
    }
}
