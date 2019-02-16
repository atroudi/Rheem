package org.qcri.rheem.profiler.core;

import org.qcri.rheem.core.optimizer.mloptimizer.api.OperatorProfiler;
import org.qcri.rheem.profiler.core.api.PlanProfiler;
import org.qcri.rheem.profiler.core.api.ProfilingConfig;
import org.qcri.rheem.core.optimizer.mloptimizer.api.Topology;

import java.util.List;

/**
 * Test profiling core.
 */
public class ProfilingTest {



    //@Test
    public void coreProfilingTest(){
        //String profileTesting = "single_operator_profiling";
        String profileTesting;

        profileTesting = "exhaustive_profiling";
        List<Topology> topologies;
        ProfilingConfig profilingConfig;
        List< ? extends OperatorProfiler> operatorProfilers;
        List<List<PlanProfiler>> planProfilers;


        switch (profileTesting){
            case "single_operator_profiling":
                topologies = TopologyGenerator.generateTopology(1);
                profilingConfig = ProfilingConfigurer.exhaustiveProfilingConfig();
                operatorProfilers = ProfilingPlanBuilder.PlanBuilder(topologies.get(0),profilingConfig);
                ProfilingRunner.SingleOperatorProfiling(operatorProfilers,profilingConfig);
            case "exhaustive_profiling":
                topologies = TopologyGenerator.generateTopology(3);
                InstantiateTopology.instantiateTopology(topologies, 3);
                profilingConfig = ProfilingConfigurer.exhaustiveProfilingConfig();
                //planProfilers = ProfilingPlanBuilder.exhaustiveProfilingPlanBuilder(topologies,profilingConfig);
                //ProfilingRunner.exhaustiveProfiling(planProfilers,profilingConfig);
                //System.out.println(result.toCsvString())
        }
    }
}
