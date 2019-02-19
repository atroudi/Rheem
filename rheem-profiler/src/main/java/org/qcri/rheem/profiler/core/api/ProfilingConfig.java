package org.qcri.rheem.profiler.core.api;

import org.qcri.rheem.core.types.DataSetType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by migiwara on 07/06/17.
 */
public class ProfilingConfig {


    private int maxJunctureTopologies;
    private int maxLoopTopologies;
    private int numberRunningPlansPerShape;
    private int sampleSize;
    private String profilingPlanGenerationEnumeration;
    private String profilingConfigurationEnumeration;
    private List<String> loopExecutionOperators = new ArrayList<>();
    private List<String> sinkExecutionOperators = new ArrayList<>();
    private List<String> sourceExecutionOperators = new ArrayList<>();
    private List<String> unaryExecutionOperators = new ArrayList<>();
    private List<String> binaryExecutionOperators = new ArrayList<>();
    private List<Long> inputCardinality = new ArrayList<>();
    private List<Integer> dataQuantaSize = new ArrayList<>();
    private List<Integer> UdfsComplexity = new ArrayList<>();
    private List<Integer> inputRatio = new ArrayList<>();
    private List<Integer> iterations = new ArrayList<>();
    private List<Integer> selectivity = new ArrayList<>();
    private List<String> profilingPlateforms;
    private List<DataSetType> dataTypes;
    private String sinkPlatform;
    private boolean bushyGeneration;
    private int maxPlatformSwitch;

    public boolean isBushyGeneration() {
        return bushyGeneration;
    }

    /**
     * GETTERS & SETTERS
     */
    public List<String> getUnaryExecutionOperators() {
        return unaryExecutionOperators;
    }

    public void setUnaryExecutionOperators(List<String> unaryExecutionOperators) {this.unaryExecutionOperators = unaryExecutionOperators;}

    public List<String> getBinaryExecutionOperators() {
        return binaryExecutionOperators;
    }

    public void setBinaryExecutionOperators(List<String> binaryExecutionOperators) {
        this.binaryExecutionOperators = binaryExecutionOperators;
    }

    public List<String> getSinkExecutionOperators() {
        return sinkExecutionOperators;
    }

    public void setSinkExecutionOperators(List<String> sinkExecutionOperators) {
        this.sinkExecutionOperators = sinkExecutionOperators;
    }

    public List<String> getSourceExecutionOperators() {
        return sourceExecutionOperators;
    }

    public void setSourceExecutionOperators(List<String> sourceExecutionOperators) {
        this.sourceExecutionOperators = sourceExecutionOperators;
    }

    public Integer getMaxJunctureTopologies() {
        return maxJunctureTopologies;
    }

    public void setMaxJunctureTopologies(Integer maxJunctureTopologies) {
        this.maxJunctureTopologies = maxJunctureTopologies;
    }

    public Integer getMaxLoopTopologies() {
        return maxLoopTopologies;
    }

    public void setMaxLoopTopologies(Integer maxLoopTopologies) {
        this.maxLoopTopologies = maxLoopTopologies;
    }

    public List<String> getLoopExecutionOperators() {
        return loopExecutionOperators;
    }

    public void setLoopExecutionOperators(List<String> loopExecutionOperators) {
        this.loopExecutionOperators = loopExecutionOperators;
    }

    public void setBushyGeneration(boolean bushyGeneration) {
        this.bushyGeneration = bushyGeneration;
    }

    public List<DataSetType> getDataType() {
        return dataTypes;
    }

    public void setDataType(List<DataSetType> dataType) {
        this.dataTypes = dataType;
    }

    public void setInputCardinality(List<Long> inputCardinality) {
        this.inputCardinality = inputCardinality;
    }

    public void setDataQuantaSize(List<Integer> dataQuantaSize) {
        this.dataQuantaSize = dataQuantaSize;
    }

    public void setUdfsComplexity(List<Integer> udfsComplexity) {
        UdfsComplexity = udfsComplexity;
    }

    public void setInputRatio(List<Integer> inputRatio) {
        this.inputRatio = inputRatio;
    }


    public String getProfilingPlanGenerationEnumeration() {
        return profilingPlanGenerationEnumeration;
    }

    public void setProfilingPlanGenerationEnumeration(String profilingPlanGenerationEnumeration) {
        this.profilingPlanGenerationEnumeration = profilingPlanGenerationEnumeration;
    }

    public String getProfilingConfigurationEnumeration() {
        return profilingConfigurationEnumeration;
    }

    public void setProfilingConfigurationEnumeration(String profilingConfigurationEnumeration) {
        this.profilingConfigurationEnumeration = profilingConfigurationEnumeration;
    }

    public List<String> getProfilingPlateform() {
        return profilingPlateforms;
    }

    public void setProfilingPlateform(List<String> profilingPlateform) {
        this.profilingPlateforms = profilingPlateform;
    }

    public List<Long> getInputCardinality() {
        return inputCardinality;
    }

    public List<Integer> getDataQuantaSize() {
        return dataQuantaSize;
    }

    public List<Integer> getUdfsComplexity() {
        return UdfsComplexity;
    }

    public List<Integer> getInputRatio() {
        return inputRatio;
    }

    public void setIterations(List<Integer> iterations) {
        this.iterations = iterations;
    }

    public List<Integer> getIterations() {
        return iterations;
    }

    public int getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(Integer sampleSize) {
        this.sampleSize = sampleSize;
    }

    public void setSelectivity(List<Integer> sampleSizes) {
        this.sampleSize = sampleSize;
    }

    public void setNumberRunningPlansPerShape(int numberRunningPlansPerShape) {
        this.numberRunningPlansPerShape = numberRunningPlansPerShape;
    }

    public int getNumberRunningPlansPerShape() {
        return numberRunningPlansPerShape;
    }

    public void setSinkPlatform(String sinkPlatform) {this.sinkPlatform = sinkPlatform;}

    public String getSinkPlatform(){return sinkPlatform;};

    public void setMaxPlatformSwitch(int maxPlatformSwitch) { this.maxPlatformSwitch = maxPlatformSwitch; }

    public int getMaxPlatformSwitch() { return this.maxPlatformSwitch;
    }
}
