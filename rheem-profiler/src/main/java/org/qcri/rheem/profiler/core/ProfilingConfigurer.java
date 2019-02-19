package org.qcri.rheem.profiler.core;

import org.qcri.rheem.core.api.Configuration;
import org.qcri.rheem.core.types.DataSetType;
import org.qcri.rheem.profiler.core.api.ProfilingConfig;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * Generates profiling configuration for a profiling plan
 */
public class ProfilingConfigurer {

    private static List<String> ALL_EXECUTION_OPLERATORS = new ArrayList<String>(Arrays.asList("textsource", "collectionsource", "map", "filter", "flatmap", "reduce", "globalreduce", "distinct", "distinct-string",
            "distinct-integer", "sort", "sort-string", "sort-integer", "count", "groupby", "join", "union", "cartesian", "callbacksink", "collect",
            "word-count-split", "word-count-canonicalize", "word-count-count"));
    private static String SOURCE_EXECUTION_OPLERATORS = "collectionsource,textsource";
    private static String UNARY_EXECUTION_OPLERATORS = "map,reduce,randomsample,shufflesample,bernoullisample";
    private static String BINARY_EXECUTION_OPLERATORS =  "union";
    private static String LOOP_EXECUTION_OPLERATORS = "repeat";
    private static String SINK_EXECUTION_OPLERATORS = "callbacksink,collect";
    private static final String DEFAULT_INPUT_CARDINALITIES = "1,100,1000,10000,100000,1000000";
    private static final String DEFAULT_ITERATIONS = "100";
    private static final Integer DEFAULT_SAMPLESIZE = 3;
    private static final String DEFAULT_DATA_QUATA_SIZES = "1,10,100,1000";
    private static final String DEFAULT_UDF_COMPLEXITIES = "1,2,3";
    private static final String DEFAULT_SELECTIVITY_COMPLEXITIES = "1,2,3";
    private static final String DEFAULT_BINARY_INPUT_RATIOS = "1,10,100";
    private static final String DEFAULT_LOOP_ITERATION_NUMBERS = "10,100";
    public static final String DEFAULT_PLATEFORMS = "java,spark,flink";
    private static final List<DataSetType> DEFAULT_DATATYPE = Arrays.asList(DataSetType.createDefault(String.class));
    public static final boolean DEFAULT_BUSHY_GENERATION = true;
    private static final Integer MAX_JUNCTURE_TOPOLOGIES = 0;
    private static final Integer MAX_LOOP_TOPOLOGIES = 0;
    //Number of running plan [-1: no limitation]
    private static final Integer NUMBER_RUNNING_PLANS_PER_SHAPE = -1;
    // Read Rheem configuration
    private static Configuration configuration = new Configuration();

    public static ProfilingConfig exhaustiveProfilingConfig(){
        // Initiate configuration
        ProfilingConfig pc = new ProfilingConfig();

        List<String> platforms = Arrays.stream(configuration.getStringProperty("rheem.profiler.platforms",DEFAULT_INPUT_CARDINALITIES).split(",")).map(String::valueOf).collect(Collectors.toList());
        List<Long> inputCardinality = Arrays.stream(configuration.getStringProperty("rheem.profiler.inputCards",DEFAULT_INPUT_CARDINALITIES).split(",")).map(Long::valueOf).collect(Collectors.toList());
        List<Integer> dataQuantas = Arrays.stream(configuration.getStringProperty("rheem.profiler.quantaSizes",DEFAULT_DATA_QUATA_SIZES).split(",")).map(Integer::valueOf).collect(Collectors.toList());
        List<Integer> UdfsComplexity = Arrays.stream(configuration.getStringProperty("rheem.profiler.udfComplexities",DEFAULT_UDF_COMPLEXITIES).split(",")).map(Integer::valueOf).collect(Collectors.toList());
        List<Integer> inputRatio = Arrays.stream(configuration.getStringProperty("rheem.profiler.binaryInputRatio",DEFAULT_BINARY_INPUT_RATIOS).split(",")).map(Integer::valueOf).collect(Collectors.toList());
        List<Integer> iterations = Arrays.stream(configuration.getStringProperty("rheem.profiler.iterations",DEFAULT_ITERATIONS).split(",")).map(Integer::valueOf).collect(Collectors.toList());

        // Set profiling configuration
        pc.setProfilingPlateform(platforms);
        pc.setBushyGeneration(configuration.getBooleanProperty("rheem.profiler.isBushy",DEFAULT_BUSHY_GENERATION));
        pc.setDataType(DEFAULT_DATATYPE);
        pc.setProfilingPlanGenerationEnumeration(configuration.getStringProperty("rheem.profiler.planEnumeration","exhaustive"));
        pc.setProfilingConfigurationEnumeration(configuration.getStringProperty("rheem.profiler.configurationEnumeration","exhaustive"));
        pc.setInputCardinality(inputCardinality);
        pc.setDataQuantaSize(dataQuantas);
        pc.setUdfsComplexity(UdfsComplexity);
        pc.setInputRatio(inputRatio);
        pc.setIterations(iterations);
        pc.setMaxJunctureTopologies((int) configuration.getLongProperty("rheem.profiler.maxJunctureTopologies", MAX_JUNCTURE_TOPOLOGIES));
        pc.setMaxLoopTopologies((int) configuration.getLongProperty("rheem.profiler.maxLoopTopologies", MAX_LOOP_TOPOLOGIES));
        pc.setSampleSize((int) configuration.getLongProperty("rheem.profiler.sampleSize", DEFAULT_SAMPLESIZE));
        pc.setSelectivity(Arrays.stream(configuration.getStringProperty("rheem.profiler.selectivity", DEFAULT_SELECTIVITY_COMPLEXITIES).split(",")).map(Integer::valueOf).collect(Collectors.toList()));
        pc.setNumberRunningPlansPerShape((int) configuration.getLongProperty("rheem.profiler.numberRunningPlansPerShape",NUMBER_RUNNING_PLANS_PER_SHAPE));
        pc.setMaxPlatformSwitch((int)configuration.getLongProperty("rheem.profiler.maxPlatformSwitch"));
        // Set profiling execution operators
        pc.setUnaryExecutionOperators(Arrays.stream(configuration.getStringProperty("rheem.profiler.unaryOperators",UNARY_EXECUTION_OPLERATORS).split(",")).map(String::valueOf).collect(Collectors.toList()));
        pc.setBinaryExecutionOperators(Arrays.stream(configuration.getStringProperty("rheem.profiler.binaryOperators",BINARY_EXECUTION_OPLERATORS).split(",")).map(String::valueOf).collect(Collectors.toList()));
        pc.setLoopExecutionOperators(Arrays.stream(configuration.getStringProperty("rheem.profiler.loopOperators",LOOP_EXECUTION_OPLERATORS).split(",")).map(String::valueOf).collect(Collectors.toList()));
        pc.setSourceExecutionOperators(Arrays.stream(configuration.getStringProperty("rheem.profiler.sourceOperators",SOURCE_EXECUTION_OPLERATORS).split(",")).map(String::valueOf).collect(Collectors.toList()));
        pc.setSinkExecutionOperators(Arrays.stream(configuration.getStringProperty("rheem.profiler.sinkOperators",SINK_EXECUTION_OPLERATORS).split(",")).map(String::valueOf).collect(Collectors.toList()));
        pc.setSinkPlatform(configuration.getStringProperty( "rheem.profiler.sinkPlatform", SINK_EXECUTION_OPLERATORS));
        return pc;
    }


    /**
     * Get a list of string properties from comma separated [,] {@param property}
     * @param configuration
     * @param property
     * @param defaultValue
     * @return
     */
    private static Object getStringListProperty(Configuration configuration, String property, String defaultValue) {
        assert ((!property.isEmpty()) && (!defaultValue.isEmpty()));
        List<String> propertyValue =Arrays.stream(configuration.getStringProperty(property, defaultValue).split(",")).map(String::valueOf).collect(Collectors.toList());
        return propertyValue;
    }

    public static LinkedHashMap<Integer,ProfilingConfig> pipelineProfilingConfig(int nodesNumber){
        LinkedHashMap<Integer,ProfilingConfig> pcHashList = new LinkedHashMap();

        ProfilingConfig pc = new ProfilingConfig();

        // Initiate configuration
        // TODO: the below should be read from a configuration profiling file
        pc.setInputCardinality(Arrays.stream(DEFAULT_INPUT_CARDINALITIES.split(",")).map(Long::valueOf).collect(Collectors.toList()));
        pc.setDataQuantaSize(Arrays.stream(DEFAULT_DATA_QUATA_SIZES.split(",")).map(Integer::valueOf).collect(Collectors.toList()));

        pcHashList.put(1,pc);

        // Case of 2 nodes the configuration will apply only on input node
        if (nodesNumber==2)
            return pcHashList;
        for (int i=2;i<nodesNumber;i++){
            pc = new ProfilingConfig();
            // Add a configuration for each intermediate node (Unaryoperator/BinaryOperator)
            pc.setUdfsComplexity(Arrays.stream(DEFAULT_UDF_COMPLEXITIES.split(",")).map(Integer::valueOf).collect(Collectors.toList()));
            pc.setInputRatio(Arrays.stream(DEFAULT_BINARY_INPUT_RATIOS.split(",")).map(Integer::valueOf).collect(Collectors.toList()));
            pcHashList.put(i,pc);
        }
        return pcHashList;
    }
}
