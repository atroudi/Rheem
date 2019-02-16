package org.qcri.rheem.profiler.core.api;

import org.qcri.rheem.core.api.Configuration;
import org.qcri.rheem.core.optimizer.mloptimizer.api.OperatorProfiler;
import org.qcri.rheem.core.optimizer.mloptimizer.api.Shape;
import org.qcri.rheem.core.optimizer.mloptimizer.api.Topology;
import org.qcri.rheem.profiler.core.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by migiwara on 04/07/17.
 */
public class ProfilingApp {

    public static void main(String[] args) {
        //String profileTesting = "single_operator_profiling";
        String profileTesting;
        String platform="";
        String cardinality="";
        String quantaSize="";

        int maxNodeNumber = 10;
        int minNodeNumber = 1;

        if (args.length>=1)
            profileTesting=args[0];
        else
            profileTesting = "exhaustive_profiling";
        List<Topology> topologies;
        ProfilingConfig profilingConfig;
        List< ? extends OperatorProfiler> operatorProfilers;
        List<Shape> shapes = new ArrayList<>();
        List<List<PlanProfiler>> planProfilers;
        List<List<OperatorProfiler>> planProfilersSinks;

        //RheemContext rheemContext = new RheemContext().with(Java.basicPlugin());


        if (args.length>=2)
            minNodeNumber = Integer.valueOf(args[1]);

        if (args.length>=3)
            maxNodeNumber = Integer.valueOf(args[2]);

        if (args.length>=4)
            cardinality = String.valueOf(args[3]);

        if (args.length>=5)
            quantaSize = String.valueOf(args[4]);

        if (args.length>=6)
            platform = String.valueOf(args[5]);

        switch (profileTesting){
            case "single_operator_profiling":
                topologies = TopologyGenerator.generateTopology(1);
                profilingConfig = ProfilingConfigurer.exhaustiveProfilingConfig();
                operatorProfilers = ProfilingPlanBuilder.PlanBuilder(topologies.get(0),profilingConfig);
                ProfilingRunner.SingleOperatorProfiling(operatorProfilers,profilingConfig);
            case "exhaustive_profiling":
                for(int nodeNumber=minNodeNumber;nodeNumber<=maxNodeNumber;nodeNumber++){

                    profilingConfig = ProfilingConfigurer.exhaustiveProfilingConfig();

                    if (platform.length()!=0)
                        profilingConfig.setProfilingPlateform(Arrays.stream(platform.split(",")).map(String::valueOf).collect(Collectors.toList()));
                    if (cardinality.length()!=0)
                        profilingConfig.setInputCardinality(Arrays.stream(cardinality.split(",")).map(Long::valueOf).collect(Collectors.toList()));
                    if (quantaSize.length()!=0)
                        profilingConfig.setDataQuantaSize(Arrays.stream(quantaSize.split(",")).map(Integer::valueOf).collect(Collectors.toList()));
                    // Initialize the generator
                    TopologyGenerator topologyGenerator =new TopologyGenerator(nodeNumber,profilingConfig);

                    // Generate the topologies
                    topologyGenerator.startGeneration();
                    topologies = topologyGenerator.getTopologyList();

                    // Instantiate the topologies
                    InstantiateTopology.instantiateTopology(topologies, nodeNumber);

                    // create shapes
                    shapes = topologies.stream().map(t -> new Shape(t, new Configuration())).collect(Collectors.toList());
                    shapes.stream().forEach(s -> s.populateShape(s.getSinkTopology()));

                    // populate shapes
                    //profilingConfig = ProfilingConfigurer.exhaustiveProfilingConfig();
                    planProfilers = ProfilingPlanBuilder.exhaustiveProfilingPlanBuilder(shapes,profilingConfig);
                    //shapes.stream().forEach(s -> s.prepareVectorLogs());

                    ProfilingRunner.exhaustiveProfiling(shapes,profilingConfig);
                    //System.out.println(result.toCsvString())

                }
        }

    }
}
