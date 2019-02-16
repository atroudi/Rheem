package org.qcri.rheem.core.optimizer.mloptimizer;


import org.qcri.rheem.core.api.Configuration;
import org.qcri.rheem.core.api.exception.RheemException;
import org.qcri.rheem.core.optimizer.AggregateOptimizationContext;
import org.qcri.rheem.core.optimizer.DefaultOptimizationContext;
import org.qcri.rheem.core.optimizer.OptimizationContext;
import org.qcri.rheem.core.optimizer.enumeration.PlanImplementation;
import org.qcri.rheem.core.optimizer.mloptimizer.api.*;
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
import java.util.*;
import java.util.stream.Collectors;


/**
 * Log generator, feature extraction from {@link org.qcri.rheem.core.plan.rheemplan.RheemPlan}
 * i.e: associated per Rheem/execution plan
 */
public class LogGenerator {

    public static final int MAXIMUM_OPERATOR_NUMBER_PER_SHAPE = 100;


    //TODO: Add a vectorlog nested class for more readablilty purposes
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Configuration configuration = new Configuration();
    // subshapes that will have all exhaustive filled with different nodes;plateforms;Types of the same shape
    private List<Shape> subShapes = new ArrayList<>();
    private List<Topology> allTopologies = new ArrayList<>();
    private List<Topology> sourceTopologies = new ArrayList<>();
    private List<String> plateform;
    private List<PipelineTopology> pipelineTopologies = new ArrayList<>();
    private List<JunctureTopology> junctureTopologies = new ArrayList<>();
    private List<LoopTopology> loopTopologies = new ArrayList<>();
    private static Stack<LoopHeadOperator> loopHeads = new Stack();

    //private final int VECTOR_SIZE = 105;
    //private final int VECTOR_SIZE = 146;
    //private final int VECTOR_SIZE = 194;
    private final int VECTOR_SIZE = 251;
    double[] vectorLogs= new double[VECTOR_SIZE];
    double[][] vectorLogs2D= new double[10][VECTOR_SIZE];

    double[] vectorLogsWithResetPlatforms= new double[VECTOR_SIZE];
    private int topologyNumber;
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
    private static int maxChannelNumber = 6;
    private double estimatedInputCardinality;
    private double estimatedDataQuataSize;
    public static LinkedHashMap<String,Integer> OPERATOR_VECTOR_POSITION_WITHOUT_DUPLICATES = new LinkedHashMap<String,Integer>(){{
        put("map", startOpPos);
        put("filter", startOpPos + 1*opPosStep);put("flatmap", startOpPos +2*opPosStep);put("reduceby:reduce", startOpPos +3*opPosStep);
        put("globalreduce", startOpPos +4*opPosStep);put("distinct", startOpPos +5*opPosStep);put("groupby:globalmaterializedgroup:materializedgroupby", startOpPos +6*opPosStep);
        put("sort", startOpPos +7*opPosStep);put("join", startOpPos +8*opPosStep);put("unionall:union", startOpPos +9*opPosStep);put("cartesian", startOpPos +10*opPosStep);put("randomsample", startOpPos +11*opPosStep);
        put("shufflesample", startOpPos +12*opPosStep);put("bernoullisample", startOpPos +13*opPosStep);put("dowhile", startOpPos +14*opPosStep);put("repeat", startOpPos +15*opPosStep);
        put("collectionsource", startOpPos +16*opPosStep);put("textfilesource:textsource", startOpPos +17*opPosStep);put("localcallbacksink:callbacksink:collectionsink", startOpPos +18*opPosStep);
        put("collect:zipwithid:cache:count:broadcast:repeatexpanded:mappartitions:sample", startOpPos + 19*opPosStep);
        // Below should be added in different position
        // DAtasetchannel is a flink type channel
    }};

    public LinkedHashMap<String,Integer> SOURCE_SINK_OPERATOR_VECTOR_POSITION_WITHOUT_DUPLICATES = new LinkedHashMap<String,Integer>(){{
        put("collectionsource", startOpPos +16*opPosStep);put("textfilesource:textsource", startOpPos +17*opPosStep);put("localcallbacksink:callbacksink", startOpPos +18*opPosStep);
    }};

    public LinkedHashMap<String,Integer> OPERATOR_VECTOR_POSITION = new LinkedHashMap<String,Integer>(){{
        put("Map", startOpPos);put("map", startOpPos);
        put("filter", startOpPos + 1*opPosStep);put("FlatMap", startOpPos +2*opPosStep);put("flatmap", startOpPos +2*opPosStep);put("reduceby", startOpPos +3*opPosStep);
        put("reduce", startOpPos +3*opPosStep);put("globalreduce", startOpPos +4*opPosStep);put("distinct", startOpPos +5*opPosStep);put("groupby", startOpPos +6*opPosStep);put("materializedgroupby", startOpPos +6*opPosStep);put("globalmaterializedgroup", startOpPos +6*opPosStep);
        put("sort", startOpPos +7*opPosStep);put("join", startOpPos +8*opPosStep);put("unionall", startOpPos +9*opPosStep);put("union", startOpPos +9*opPosStep);put("cartesian", startOpPos +10*opPosStep);put("randomsample", startOpPos +11*opPosStep);
        put("shufflesample", startOpPos +12*opPosStep);put("bernoullisample", startOpPos +13*opPosStep);put("dowhile", startOpPos +14*opPosStep);put("repeat", startOpPos +15*opPosStep);
        put("collectionsource", startOpPos +16*opPosStep);put("textfilesource", startOpPos +17*opPosStep);put("textsource", startOpPos + 17*opPosStep);put("callbacksink", startOpPos +18*opPosStep);
        put("localcallbacksink", startOpPos + 18*opPosStep);put("collect", startOpPos + 19*opPosStep);
        // Below should be added in different position
        // Datasetchannel is a flink type channel
        put("zipwithid", startOpPos + 19*opPosStep);put("cache", startOpPos + 19*opPosStep);put("count", startOpPos + 19*opPosStep);put("broadcast", startOpPos + 19*opPosStep);
        put("repeatexpanded", startOpPos + 19*opPosStep);put("mappartitions", startOpPos + 19*opPosStep);put("shufflepartitionsample", startOpPos + 19*opPosStep);
        put("sample", startOpPos + 19*opPosStep);
    }};

    public LinkedHashMap<String,Integer> CHANNEL_VECTOR_POSITION = new LinkedHashMap<String,Integer>(){{
        put("CollectionChannel", startOpPos + (1+maxOperatorNumber)*opPosStep + 0*channelPosStep);
        put("StreamChannel", startOpPos + (1+maxOperatorNumber)*opPosStep + 1*channelPosStep);
        put("RddChannel", startOpPos + (1+maxOperatorNumber)*opPosStep + 2*channelPosStep);
        put("FileChannel", startOpPos + (1+maxOperatorNumber)*opPosStep + 3*channelPosStep);
        // Below should be added in different position
        // Datasetchannel is a flink type channel
        put("DataSetChannel", startOpPos + (1+maxOperatorNumber)*opPosStep + 4*channelPosStep);
        put("BroadcastChannel", startOpPos + (1+maxOperatorNumber)*opPosStep + 4*channelPosStep);
    }};

    public LinkedHashMap<String,Integer> CONVERSION_OPERATOR_VECTOR_POSITION = new LinkedHashMap<String,Integer>(){{
        put("collect", startOpPos + (1+maxOperatorNumber)*opPosStep + 5*channelPosStep);put("collectionsource",startOpPos + (1+maxOperatorNumber)*opPosStep + 6*channelPosStep);
        put("objectfilesource",startOpPos + (1+maxOperatorNumber)*opPosStep + 7*channelPosStep);
        put("cache", startOpPos + (1+maxOperatorNumber)*opPosStep + 8*channelPosStep);put("objectfilesink", startOpPos + (1+maxOperatorNumber)*opPosStep + 9*channelPosStep);
        put("collectionsink", startOpPos + (1+maxOperatorNumber)*opPosStep + 10*channelPosStep);
//        put("cache", startOpPos + (1+maxOperatorNumber)*opPosStep + 10*channelPosStep);
//        put("broadcast", startOpPos + (1+maxOperatorNumber)*opPosStep + 10*channelPosStep);
    }};

    static LinkedHashMap<String,Integer> OLD_CONVERSION_OPERATOR_VECTOR_POSITION = new LinkedHashMap<String,Integer>(){{
        put("JavaCollect", startOpPos + (1+maxOperatorNumber)*opPosStep + 4*channelPosStep);put("JavaCollectionSource",startOpPos + (1+maxOperatorNumber)*opPosStep + 5*channelPosStep);put("JavaObjectFileSink", startOpPos + (1+maxOperatorNumber)*opPosStep + 6*channelPosStep);
        put("JavaObjectFileSource",startOpPos + (1+maxOperatorNumber)*opPosStep + 7*channelPosStep);put("SparkCollect", startOpPos + (1+maxOperatorNumber)*opPosStep + 8*channelPosStep);put("SparkCollectionSource", startOpPos + (1+maxOperatorNumber)*opPosStep + 9*channelPosStep);
        put("SparkObjectFileSink", startOpPos + (1+maxOperatorNumber)*opPosStep + 10*channelPosStep);put("SparkObjectFileSource", startOpPos + (1+maxOperatorNumber)*opPosStep + 11*channelPosStep);
        // Below should be added in different position
        // DAtasetchannel is a flink type channel
        put("FlinkCollectionSink", startOpPos + (1+maxOperatorNumber)*opPosStep + 11*channelPosStep);
        put("FlinkCollectionSource", startOpPos + (1+maxOperatorNumber)*opPosStep + 11*channelPosStep);

    }};

    public static final List<String> DEFUALT_PLATFORMS = new ArrayList<>(Arrays.asList("Java Streams","Apache Spark","Apache Flink"));


    private static List<String> enabledPlatforms = new ArrayList<>();

    public static LinkedHashMap<String,Integer> PLATFORMVECTORPOSITION = new LinkedHashMap<String,Integer>(){{
        put("Java Streams",0);
        put("Apache Spark",1);
        put("Apache Flink",2);
    }};


    /**
     * Prepare vector log topologies
     * @param ispreExecution
     */
    public void prepareVectorLog(RheemPlan rheemPlan, Configuration Configuration, boolean ispreExecution){

        // set platforms
        Configuration.getPlatformProvider().provideAll().stream().forEach(p-> enabledPlatforms.add(p.getName()));

        // collect sinks
        Collection<Operator> sinks = rheemPlan.getSinks();


//        UnarySink sink = (UnarySink) sinkAlternatives.getAlternatives().get(0).getSink();
        //prepareTopologies(sink,ispreExecution);
        // prepare topologies
        sinks.stream()
                .forEach( sink->{
                    Shape shape = prepareTopologies((UnarySink) sink,ispreExecution);
                    shape.getSourceTopologies().stream()
                            .forEach(t->this.sourceTopologies.add(t));
                    shape.getAllTopologies().stream()
                            .forEach(t->this.allTopologies.add(t));
                    shape.getPipelineTopologies().stream()
                            .forEach(t->this.pipelineTopologies.add(t));
                    shape.getJunctureTopologies().stream()
                            .forEach(t->this.junctureTopologies.add(t));
                    shape.getLoopTopologies().stream()
                            .forEach(t->this.loopTopologies.add(t));
                });
//ispreExecution
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
                                if( configuration.getBooleanProperty("rheem.profiler.generate2dLogs",false)){
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

        if( configuration.getBooleanProperty("rheem.profiler.generate2dLogs",false)){
            // set shape's 2D vector log
            this.setVectorLogs2D(tmpVectorLogs2D.clone());
            // set shapes 1D vector log as the first row of the 2D vector log; as we will update the channels only for the first row
            this.setVectorLogs(tmpVectorLogs2D[0].clone());
        }
        // set shapes 1D vector log
        this.setVectorLogs(tmpVectorLogs1D.clone());

        // update with execution operators
        //this.updateExecutionOperators(optimizationContext.getLocalOperatorContexts());

        // reinitialize log array every subShape
        Arrays.fill(tmpVectorLogs1D, 0);
    }


    /**
     * Prepare topologies
     * @param ispreExecution
     */
    public static Shape prepareTopologies(UnarySink sinkOperator, boolean ispreExecution){
        Shape newShape = new Shape(new Configuration());

        // Initiate current and predecessor executionOperator
        Operator currentOperator = sinkOperator;
        final List<Operator> predecessorOperators = Arrays.stream(sinkOperator.getAllInputs())
                .map(inputSlot -> inputSlot.getOccupant().getOwner())
                .collect(Collectors.toList());
        //Operator predecessorOperator = sinkOperator.getInput(0).getOccupant().getOwner();
        // DEclare and initialize predecessor executionOperator
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
                                // do not add existing loop head
                                if((tmpPredecessorOperators.indexOf(operator)>=1))
                                    if(loopHeads.isEmpty()) {
                                    iterpredecessorOperators.add(operator);
                                    } else if(operator!=loopHeads.peek()) {
                                    iterpredecessorOperators.add(operator);
                                    }

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
                                    // do not add existing loop head
                                    if((tmpPredecessorOperators2.indexOf(operator)>=1))
                                        if(loopHeads.isEmpty()) {
                                            iterpredecessorOperators.add(operator);
                                        } else if(operator!=loopHeads.peek()) {
                                            iterpredecessorOperators.add(operator);
                                        }
//                                    if(tmpPredecessorOperators2.indexOf(operator)>=1)
//                                        iterpredecessorOperators.add(operator);
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

                //DONE: add juncture handling
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

//                    // add executionOperator to new topology
//                    newJunctureTopology.getNodes().add(new Tuple2<String, OperatorProfiler>(predecessorOperator.toString(), new OperatorProfilerBase(predecessorOperator)));

                    // update current and predecessor operatorNames
                    currentOperator = predecessorOperator;

                    // update predecessorOperators in case of multiple inputs
                    List<Operator> tmpPredecessorOperators = Arrays.stream(predecessorOperator.getAllInputs())
                            .map(inputSlot -> inputSlot.getOccupant().getOwner())
                            .collect(Collectors.toList());

                    // update predecessorOperators in case of multiple inputs
                    tmpPredecessorOperators.stream()
                            .forEach(operator -> {
                                // do not add existing loop head
                                if((tmpPredecessorOperators.indexOf(operator)>=1))
                                    if(loopHeads.isEmpty()) {
                                        iterpredecessorOperators.add(operator);
                                    } else if(operator!=loopHeads.peek()) {
                                        iterpredecessorOperators.add(operator);
                                    }
//                                if(tmpPredecessorOperators.indexOf(operator)>=1)
//                                    iterpredecessorOperators.add(operator);
                            });

                    // update predecessor with the first input executionOperator
                    predecessorOperator = tmpPredecessorOperators.get(0);

                    if (predecessorOperator.isSource()) {
                        newJunctureTopology.getNodes().add(new Tuple2<String, OperatorProfiler>(predecessorOperator.toString(), new OperatorProfilerBase(predecessorOperator)));
                        newShape.getSourceTopologies().add(newJunctureTopology);
                        //addSourceTopology(predecessorOp erator, newPipelineTopology);
                    }
                }

                //DONE: add loop handling
                if (predecessorOperator instanceof LoopHeadOperator) {
                    LoopTopology newLoopTopology = new LoopTopology();

                    // First check if head is already added
                    if ((loopHeads.isEmpty()) || (loopHeads.peek() != predecessorOperator)) {
                        // Add current loopHead
                        loopHeads.add((LoopHeadOperator) predecessorOperator);

                        if (currentOperator.isSink()) {
                            newLoopTopology.getNodes().add(new Tuple2<String, OperatorProfiler>(currentOperator.toString(), new OperatorProfilerBase(currentOperator)));
                            // add current topology as a sink topology
                            newShape.setSinkTopology(newLoopTopology);

                        }
                        newLoopTopology.getNodes().add(new Tuple2<String, OperatorProfiler>(predecessorOperator.toString(), new OperatorProfilerBase(predecessorOperator)));
                        newShape.getLoopTopologies().add(newLoopTopology);
                        newShape.getAllTopologies().add(newLoopTopology);

//                        // add executionOperator to new topology
//                        newLoopTopology.getNodes().add(new Tuple2<String, OperatorProfiler>(predecessorOperator.toString(), new OperatorProfilerBase(predecessorOperator)));

                        // update current and predecessor operatorNames
                        currentOperator = predecessorOperator;

                        // check if the predecessor loop has been visited before (if no we visit iterIn"1" otherwise
                        // we visit InitIn"0")

                        // if convergence input 1 exists
                        if(predecessorOperator.getNumInputs()>=3)
                            iterpredecessorOperators.add(predecessorOperator.getInput(2).getOccupant().getOwner());

                        // Get the IterIn
                        predecessorOperator = predecessorOperator.getInput(1).getOccupant().getOwner();


                    } else {
                        // remove current loopHead
                        loopHeads.pop();
                        // Get the InitIn
                        predecessorOperator = predecessorOperator.getInput(0).getOccupant().getOwner();
                    }

//                    if (predecessorOperator.isSource()) {
//                        newLoopTopology.getNodes().add(new Tuple2<String, OperatorProfiler>(predecessorOperator.toString(), new OperatorProfilerBase(predecessorOperator)));
//                        newShape.getSourceTopologies().add(newLoopTopology);
//                        //addSourceTopology(predecessorOp erator, newPipelineTopology);
//                    }
                }
            }
        }
        // set new shape as associated shape of current log generator

//        if (prepareVectorLog)
//            newShape.prepareVectorLog(ispreExecution);
        return newShape;
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
        if( configuration.getBooleanProperty("rheem.profiler.generate2dLogs",false)){
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
        //PLATFORMVECTORPOSITION.entrySet().stream().
        //        forEach(tuple->logVector[opPos + tuple.getValue()] = 0);
//        if(replacePlatform!=null) {
//            if(replacePlatform=="Apache Flink")
//                logVector[opPos + PLATFORMVECTORPOSITION.get(replacePlatform)] -= 10;
//            else
//                logVector[opPos + PLATFORMVECTORPOSITION.get(replacePlatform)] -= 1;
//        }
//        if(newPlatform=="Apache Flink")
//            logVector[opPos + PLATFORMVECTORPOSITION.get(newPlatform)] += 10;
//        else
//            logVector[opPos + PLATFORMVECTORPOSITION.get(newPlatform)] += 1;

        if(replacePlatform!=null) {
            logVector[opPos + PLATFORMVECTORPOSITION.get(replacePlatform)] -= 1;
        }
        logVector[opPos + PLATFORMVECTORPOSITION.get(newPlatform)] += 1;

        // update platform
    }

    public void resetAllOperatorPlatforms() {
        vectorLogsWithResetPlatforms = vectorLogs.clone();
        for(String operator:operatorNames){
            // get executionOperator position
            int opPos = getOperatorVectorPosition(operator);
            // reset all platforms to zero
            PLATFORMVECTORPOSITION.entrySet().stream().
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
//        for (String platform : enabledPlatforms) {
//            if (logVector[opPos + getPlatformVectorPosition(platform)] == 2)
//                return platform;
//            // check the case of flink
//            else if(((int)logVector[opPos + getPlatformVectorPosition(platform)]) % 10 == 2)
//                return enabledPlatforms.get(2);
//        }
//        return null;
        int opPos = getOperatorVectorPosition(operator);
        for (String platform : enabledPlatforms) {
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
        for(int i = 2; i< enabledPlatforms.size(); i++){
            int finalI = i;
            exhaustivePlatformVectors.stream()
                    .forEach(platformVector-> exhaustivePlanPlatformFiller(vectorLogsWithResetPlatforms, platformVector, platforms.get(finalI),platforms.get(finalI-1), 0, -1));
        }
    }

    public void exhaustivePlanPlatformFiller(){

        resetAllOperatorPlatforms();
        // call exhaustive plan filler with new Platform: :spark" as currrently tested with only two platforms (java, spark)
        //exhaustivePlanPlatformFiller(vectorLogsWithResetPlatforms, new String[MAXIMUM_OPERATOR_NUMBER_PER_SHAPE], enabledPlatforms.get(1), 0, -1);
        exhaustivePlanPlatformFiller(vectorLogsWithResetPlatforms, new String[MAXIMUM_OPERATOR_NUMBER_PER_SHAPE], enabledPlatforms.get(1), enabledPlatforms.get(0), 0, 1000);

        for(int i = 2; i< enabledPlatforms.size(); i++){
            int finalI = i;
            ArrayList<String[]> exhaustivePlatformVectorsCopy = (ArrayList) exhaustivePlatformVectors.clone();
            //for(String[] platformVector:exhaustivePlatformVectorsCopy){
            for(int j=0; j<exhaustivePlatformVectorsCopy.size();j++){
                exhaustivePlanPlatformFiller(this.exhaustiveVectors.get(j), exhaustivePlatformVectorsCopy.get(j), enabledPlatforms.get(finalI), enabledPlatforms.get(finalI-1), 0, -1);
            }
            exhaustivePlatformVectorsCopy = (ArrayList) exhaustivePlatformVectors.clone();
//            exhaustivePlatformVectors.stream()
//                    .forEach(platformVector-> exhaustivePlanPlatformFiller(vectorLogsWithResetPlatforms, platformVector, enabledPlatforms.get(finalI), 0, -1));
        }
    }

    public void exhaustivePlanPlatformFiller(long exhaustivePlatformVectorsMaxBuffer){
        // call exhaustive plan filler with new Platform: :spark" as currrently tested with only two platforms (java, spark)
        //exhaustivePlanPlatformFiller(vectorLogsWithResetPlatforms, new String[MAXIMUM_OPERATOR_NUMBER_PER_SHAPE], enabledPlatforms.get(1), 0, -1);
        if (enabledPlatforms.size()>1)
            exhaustivePlanPlatformFiller(this.getVectorLogs(), new String[MAXIMUM_OPERATOR_NUMBER_PER_SHAPE], enabledPlatforms.get(1), enabledPlatforms.get(0), 0, exhaustivePlatformVectorsMaxBuffer);
        else
            // case of single platform
            exhaustivePlanPlatformFiller(this.getVectorLogs(), new String[MAXIMUM_OPERATOR_NUMBER_PER_SHAPE], null, enabledPlatforms.get(0), 0, exhaustivePlatformVectorsMaxBuffer);

        for(int i = 2; i< enabledPlatforms.size(); i++){
            int finalI = i;
            ArrayList<String[]> exhaustivePlatformVectorsCopy = (ArrayList) exhaustivePlatformVectors.clone();
            //for(String[] platformVector:exhaustivePlatformVectorsCopy){
            for(int j=0; j<exhaustivePlatformVectorsCopy.size();j++){
                exhaustivePlanPlatformFiller(this.exhaustiveVectors.get(j), exhaustivePlatformVectorsCopy.get(j), enabledPlatforms.get(finalI), enabledPlatforms.get(finalI-1), 0, exhaustivePlatformVectorsMaxBuffer);
            }
            exhaustivePlatformVectorsCopy = (ArrayList) exhaustivePlatformVectors.clone();
//            exhaustivePlatformVectors.stream()
//                    .forEach(platformVector-> exhaustivePlanPlatformFiller(vectorLogsWithResetPlatforms, platformVector, enabledPlatforms.get(finalI), 0, -1));
        }
    }
    /**
     * Will exhaustively generate all platform filled logVectors from the input logVector; and update  the platform vector will be used in the runner
     * PS: currently support only 1D vector log generation
     * @param vectorLog
     * @param newPlatform
     * @param startOperatorIndex
     */
    public void exhaustivePlanPlatformFiller(double[] vectorLog, String[] platformVector, String newPlatform,String replacePlatform, int startOperatorIndex, long exhaustivePlatformVectorsMaxBuffer){
        // if no generated plan fill it with equal values (all oerators in first platform java)

        // clone input vectorLog
        double[] newVectorLog = vectorLog.clone();
        String[] newPlatformLog = platformVector.clone();

        // Initial vectorLog filling: first platform is set for all operatorNames
        if (exhaustiveVectors.isEmpty()){
            int iteration=0;
            for(String operator: operatorNames){
                updateOperatorPlatform(operator, enabledPlatforms.get(0),null,newVectorLog);
                updatePlatformVector(iteration, enabledPlatforms.get(0),newPlatformLog);
                iteration++;
            }
            exhaustiveVectors.add(newVectorLog.clone());
            exhaustivePlatformVectors.add(newPlatformLog.clone());
            exhaustivePlanPlatformFiller(newVectorLog,newPlatformLog, newPlatform, replacePlatform, startOperatorIndex, exhaustivePlatformVectorsMaxBuffer);
            return;
        }

        // exit if new Platform is null
        if (newPlatform==null)
            return;

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
     * print an input feature vector
     */
    public String printLog(double[] featureVector) {
        final String[] outputVector = {""};
        final int[] count = {0,0};
        final int[] opPos = {0};
        NumberFormat nf = new DecimalFormat("##.#");
        System.out.println(featureVector.length);
        Arrays.stream(featureVector).forEach(d -> {
            if (count[0]==0) outputVector[0] = outputVector[0].concat("Topologies< ");
            //if (count[0]==4) outputVector[0] = outputVector[0].concat("> ");
            if ((count[0]!=0)&&((count[0]-startOpPos) % opPosStep==0)&&(opPos[0]<=maxOperatorNumber+1)) {
                outputVector[0] = outputVector[0].concat("> "+this.getSetElement(OPERATOR_VECTOR_POSITION_WITHOUT_DUPLICATES.keySet(),opPos[0])+"< ");
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
        return "Current rheem plan feature vector: " + outputVector[0];
    }

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
            throw new RheemException(String.format("couldn't find conversion operator log vector offset %s",conversionOperator));
        }
    }

    private void addChannelLog(Channel outChannel) {
        String[] channelName = outChannel.toString().split("\\P{Alpha}+");
        channelNames.add(channelName[0]);
        // Each channel has 4 encoding digits as follow (number, consumer, producer, conversion)
        int channelStartPosition = getJunctureVectorPosition(channelName[0]);
        if( configuration.getBooleanProperty("rheem.profiler.generate2dLogs",false)){
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
            return PLATFORMVECTORPOSITION.get(platform);
        } catch (Exception e){
            throw new RheemException(String.format("couldn't find a plateform for executionOperator %s",platform));
        }
    }

    private List<String> executionOperatorNames = new ArrayList<>();

    /**
     * update {@var logVector} with execution executionOperator informations (plateform, output cardinality)
     * @param localOperatorContexts
     * @param updateExecutionOperatorPlatforms
     */
    public void updateExecutionOperators(Map<Operator, OptimizationContext.OperatorContext> localOperatorContexts, boolean updateExecutionOperatorPlatforms) {
        localOperatorContexts.keySet().stream()
                .forEach(operator -> {
                    // Composite operators discard
                    if(operator.isElementary()) {
                        String[] executionOperatorName = new String[1];
                        double averageOutputCardinality = 0;
                        double averageInputCardinality = 0;

                        // Update executionOperator name and platform
                        if(operator.isExecutionOperator()){
                            executionOperatorName[0]=operator.toString().split("\\P{Alpha}+")[0]
                                    .toLowerCase().replace("java","").replace("spark","").replace("flink","");

                            ExecutionOperator executionOperator = (ExecutionOperator) operator;
                            // update platform
                            String platform = executionOperator.getPlatform().getName();

                            if ((!platform.isEmpty())&&(updateExecutionOperatorPlatforms))
                                updateOperatorPlatform(executionOperatorName[0],platform,null,vectorLogs);
                        } else {
                            // update platform
                            Set<Platform> platform = operator.getTargetPlatforms();
                            executionOperatorName[0] = operator.toString().split("\\P{Alpha}+")[0].toLowerCase();
                            //platform.stream().forEach(p->updateOperatorPlatform(executionOperatorName[0],p.getName(),null,vectorLogs) );
                        }

                        if (localOperatorContexts.get(operator).getOutputCardinalities().length!=0){
                            // update present execution operator names
                            executionOperatorNames.add(executionOperatorName[0]);

                            // get average input/output cardinalities
                            averageOutputCardinality = localOperatorContexts.get(operator).getOutputCardinality(0).getAverageEstimate();
                            averageInputCardinality = Arrays.stream(localOperatorContexts.get(operator).getInputCardinalities())
                                    .map(incard->(double)incard.getAverageEstimate())
                                    .reduce(0.0,(av1,av2)->av1+av2);

                            //Arrays.stream(localOperatorContexts.get(operator).getInputCardinalities()).forEach(incard1->incard1.getAverageEstimate());
                            // update shape's vector log with output cardinality
                            if(configuration.getBooleanProperty("rheem.profiler.generate2dLogs",false)){
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

    public Tuple2<double[],Integer> retreiveOperatorPlatform(double[] featureVector, String operatorName) {

        Integer foundIndex=-1;
        // format operator name
        String formattedOperatorName=operatorName.toString().split("\\P{Alpha}+")[0]
                .toLowerCase();
        int operatorPosition = OPERATOR_VECTOR_POSITION.get(formattedOperatorName);


        do{
            foundIndex++;
            if (featureVector[operatorPosition+foundIndex]!=0) {
                featureVector[operatorPosition + foundIndex]--;
                return new Tuple2(featureVector, foundIndex);
            }
        }while(featureVector[operatorPosition+foundIndex]==0);
        throw new RheemException("Feature vector inconsistent: number of platforms doesn't much operators number!");
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

    public void reinitializeLog(boolean keepInputCardinality, boolean keepSourceSink) {
        operatorNames = new ArrayList<>();
        operatorNames2d = new ArrayList<>();
        operatorNamesPostExecution2d = new ArrayList<>();
        pruningFeatureLogs = new ArrayList<>();
        double inputcardinality = 0,dataQuantaSize = 0;
        double[] vectorLogsSnapshot = new double[VECTOR_SIZE];
        double[][] vectorLogs2DSnapshot = new double[10][VECTOR_SIZE];
        if((keepSourceSink)||(keepInputCardinality)){
            vectorLogsSnapshot = vectorLogs.clone();
            vectorLogs2DSnapshot = vectorLogs2D.clone();

        }
        if(keepInputCardinality){
            inputcardinality = vectorLogs[VECTOR_SIZE - 2];
            dataQuantaSize = vectorLogs[VECTOR_SIZE - 1];
        }

        // RESET VECTORLOGS
        vectorLogs2D = new double[10][VECTOR_SIZE];
        vectorLogs = new double[VECTOR_SIZE];
        
        if(keepInputCardinality)
            this.setcardinalities(vectorLogsSnapshot[VECTOR_SIZE - 2],vectorLogsSnapshot[VECTOR_SIZE - 1]);
        if(keepSourceSink){
            double[] finalVectorLogsSnapshot = vectorLogsSnapshot;
            double[][] finalVectorLogs2DSnapshot = vectorLogs2DSnapshot;
            SOURCE_SINK_OPERATOR_VECTOR_POSITION_WITHOUT_DUPLICATES.values().stream().forEach(operatorPosition -> {
                for(int count=operatorPosition+4;count<=operatorPosition+opPosStep;count++) {
                    vectorLogs[count] = finalVectorLogsSnapshot[count];
                    vectorLogs2D[0][count] = finalVectorLogs2DSnapshot[0][count];
                }
            });
        }

    }

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

    public List<PipelineTopology> getPipelineTopologies() {
        return pipelineTopologies;
    }

    public List<JunctureTopology> getJunctureTopologies() {
        return junctureTopologies;
    }

    public List<LoopTopology> getLoopTopologies() {
        return loopTopologies;
    }

    public List<double[]> getExhaustiveVectors() {
        return exhaustiveVectors;
    }


    public List<double[]> getPruningFeatureLogs() {
        return pruningFeatureLogs;
    }

    private List<double[]> pruningFeatureLogs= new ArrayList<>();
    public double[] addPruningFeatureLog(PlanImplementation planImplementation) {
        //cache current vector log
        double[] cachedVectorLogs = vectorLogs.clone();


        double[] vectorLogsSnapshot = new double[VECTOR_SIZE];
        double[][] vectorLogs2DSnapshot = new double[10][VECTOR_SIZE];
        vectorLogsSnapshot = vectorLogs.clone();
        vectorLogs2DSnapshot = vectorLogs2D.clone();

        // save input cardinalies and dataquanta size
        double inputcardinality = vectorLogs[VECTOR_SIZE - 2];
        double dataQuantaSize = vectorLogs[VECTOR_SIZE - 1];

        // reinitialize vector log;
        vectorLogs = new double[VECTOR_SIZE];



        // RESET VECTORLOGS
        vectorLogs2D = new double[10][VECTOR_SIZE];
        vectorLogs = new double[VECTOR_SIZE];

        //TODO: keep topologies and operator topologies

        double[] finalVectorLogsSnapshot = vectorLogsSnapshot;
        double[][] finalVectorLogs2DSnapshot = vectorLogs2DSnapshot;
//        SOURCE_SINK_OPERATOR_VECTOR_POSITION_WITHOUT_DUPLICATES.values().stream().forEach(operatorPosition -> {
//            for(int count=operatorPosition+4;count<=operatorPosition+opPosStep;count++) {
//                vectorLogs[count] = finalVectorLogsSnapshot[count];
//                vectorLogs2D[0][count] = finalVectorLogs2DSnapshot[0][count];
//            }
//        });

        // kkep topologies too

        vectorLogs[0]=finalVectorLogsSnapshot[0];
        vectorLogs[1]=finalVectorLogsSnapshot[1];
        vectorLogs[2]=finalVectorLogsSnapshot[2];
        vectorLogs[4]=finalVectorLogsSnapshot[3];

        OPERATOR_VECTOR_POSITION_WITHOUT_DUPLICATES.values().stream().forEach(operatorPosition -> {
            for(int count=operatorPosition+4;count<=operatorPosition+6;count++) {
                vectorLogs[count] = finalVectorLogsSnapshot[count];
                vectorLogs2D[0][count] = finalVectorLogs2DSnapshot[0][count];
            }
        });


        // set input cardinalies and dataquanta size
        this.setcardinalities(inputcardinality,dataQuantaSize);

        double[] pruningFeatureLog = new double[VECTOR_SIZE];

        // add execution operator logs
        planImplementation.getOperators().stream()
                .forEach(exectionOperator->{
                    if (planImplementation.getOptimizationContext() instanceof DefaultOptimizationContext)
                        this.addExecutionOperatorLog(exectionOperator,planImplementation.getOptimizationContext().getLocalOperatorContexts());
                    else{
                        AggregateOptimizationContext aggregateOptimizationContext = (AggregateOptimizationContext) planImplementation.getOptimizationContext();
                        this.addExecutionOperatorLog(exectionOperator,aggregateOptimizationContext.getDefaultOptimizationContexts().get(0).getLocalOperatorContexts());

                    }
                });

        // add junction logs
        planImplementation.getJunctions().values().stream()
                .forEach(junction->this.addJunctionLog(junction));

        // update with execution operators
        //updateExecutionOperators(planImplementation.getOptimizationContext().getLocalOperatorContexts());

        pruningFeatureLog = vectorLogs.clone();
        pruningFeatureLogs.add(pruningFeatureLog);

        // return the cached vector log
        //vectorLogs = cachedVectorLogs;

        // return the pruning pruning
        return pruningFeatureLog;
    }

    private void addJunctionLog(Junction junction) {
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

    }

    private void addExecutionOperatorLog(ExecutionOperator operator, Map<Operator, OptimizationContext.OperatorContext> localOperatorContexts) {
        String executionOperatorName = operator.toString().split("\\P{Alpha}+")[0]
                .toLowerCase().replace("java","").replace("spark","").replace("flink","");

        // update platform
        String platform = operator.getPlatform().getName();

        if (!platform.isEmpty())
            updateOperatorPlatform(executionOperatorName,platform,null,vectorLogs);

        if (localOperatorContexts.get(operator).getOutputCardinalities().length!=0) {

            // get average input/output cardinalities
            double averageOutputCardinality = localOperatorContexts.get(operator).getOutputCardinality(0).getAverageEstimate();
            double averageInputCardinality = Arrays.stream(localOperatorContexts.get(operator).getInputCardinalities())
                    .map(incard -> (double) incard.getAverageEstimate())
                    .reduce(0.0, (av1, av2) -> av1 + av2);

            // Update 1d vector log
            if (OPERATOR_VECTOR_POSITION.containsKey(executionOperatorName.toLowerCase()))
                updateOperatorOutputCardinality(executionOperatorName, averageOutputCardinality, vectorLogs);
            else
                updateConversionOperatorOutputCardinality(executionOperatorName, averageOutputCardinality, vectorLogs);

            if (OPERATOR_VECTOR_POSITION.containsKey(executionOperatorName.toLowerCase()))
                // update shape's vector log with target platform
                updateOperatorInputCardinality(executionOperatorName, averageInputCardinality, vectorLogs);

            // update the estimate inputcardinality/dataQuantasize
            if (localOperatorContexts.get(operator).getOperator().isSource() && (localOperatorContexts.get(operator).getOperator() instanceof UnarySource)) {
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
                if (this.estimatedInputCardinality!=0)
                    this.setcardinalities(this.estimatedInputCardinality, this.estimatedDataQuataSize);
            }
        }
    }

    public void reinitializepruningLogs() {
        // reuntialize pruning vector logs
        this.pruningFeatureLogs = new ArrayList<>();
    }
}
