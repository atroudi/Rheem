package org.qcri.rheem.profiler.core.api;

import org.qcri.rheem.core.api.RheemContext;
import org.qcri.rheem.core.optimizer.mloptimizer.api.OperatorProfiler;
import org.qcri.rheem.core.optimizer.mloptimizer.api.Shape;
import org.qcri.rheem.core.optimizer.mloptimizer.api.Topology;
import org.qcri.rheem.core.plan.rheemplan.RheemPlan;

import java.util.List;

/**
 * Contains the {@link OperatorProfiler}s that respects an instantiated {@link Topology} that will be used to build the {@link RheemPlan}
 * in the {@link org.qcri.rheem.profiler.core.ProfilingRunner};
 *
 * Describes a Profiling plan that will be used in the runner to create a rheem plan
 */
public class PlanProfiler {

    public OperatorProfiler sourceOperatorProfiler;

    public List<OperatorProfiler> unaryOperatorProfilers;

    public List<OperatorProfiler> binaryOperatorProfilers;

    /**
     * Sink executionOperator profiler
     */
    public OperatorProfiler sinkOperatorProfiler;

    private Shape shape;

    private static ProfilingConfig profilingConfig;

    private static RheemContext rheemContext;


    public PlanProfiler(Shape shape, ProfilingConfig profilingConfig) {
        this.shape = shape;
        this.profilingConfig = profilingConfig;
    }


    /**
     * Builds a Rheem plan
     * @return
     */
    /*
    private PlanProfiler buildPlan(List<OperatorProfiler> operatorProfilers){
        //assert this.topology.get
        for(OperatorProfiler operatorProfiler:operatorProfilers){

        }
    }*/


    public OperatorProfiler getSourceOperatorProfiler() {
        return sourceOperatorProfiler;
    }

    public void setSourceOperatorProfiler(OperatorProfiler sourceOperatorProfiler) {
        this.sourceOperatorProfiler = sourceOperatorProfiler;
    }

    public List<OperatorProfiler> getUnaryOperatorProfilers() {
        return unaryOperatorProfilers;
    }

    public void setUnaryOperatorProfilers(List<OperatorProfiler> unaryOperatorProfilers) {
        this.unaryOperatorProfilers = unaryOperatorProfilers;
    }

    public OperatorProfiler getSinkOperatorProfiler() {
        return sinkOperatorProfiler;
    }

    public void setSinkOperatorProfiler(OperatorProfiler sinkOperatorProfiler) {
        this.sinkOperatorProfiler = sinkOperatorProfiler;
    }

}
