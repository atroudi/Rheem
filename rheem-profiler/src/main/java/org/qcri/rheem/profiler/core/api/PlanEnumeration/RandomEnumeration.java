package org.qcri.rheem.profiler.core.api.PlanEnumeration;

import org.qcri.rheem.core.optimizer.mloptimizer.api.Topology;
import org.qcri.rheem.core.optimizer.mloptimizer.api.Tuple2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * fills the {@link Topology}s nodes with random operator-platform Tuples
 */
public class RandomEnumeration {

    static public void randomEnumeration(List nodes, List operators, List platforms , int nodeNumber, int totalEnum, List totalList){
        // Loop through totaEnum
        for(int i=0;i<totalEnum;i++){
            nodes.stream()
                    .map(t->{
                        int rnd1 = (int)(Math.random() * operators.size());
                        int rnd2 = (int)(Math.random() * platforms.size());
                        return new Tuple2<>(operators.get(rnd1),platforms.get(rnd2));
                    });
            totalList.add(nodes);
        }
    }
    /**
     * Driver class to Tests above random generations
     */
    static class Main{
        // Platforms to enumerate
        private static String[] PLATEFORMS = {"Java","Spark","Flink"};
        private static String[] OPERATORS = {"Map","Flatmap","Filter"};
        private static final int NODE_NUMBER = 4;
        private static final int TOTAL_ENUMERATIONS = 4;

        private static List nodes = new ArrayList<String>(5);
        private static List<List<Tuple2<String,String>>> totalEnumOpertorsPlatforms = new ArrayList<>();

        public static void main(String[] args){

            // Initialize nodes
            for(int i=0;i<NODE_NUMBER;i++)
                nodes.add(new Tuple2<>(OPERATORS[0],PLATEFORMS[0]));

            // store double exhaustive enumerations with platform switch pruning in totalLists
            randomEnumeration(nodes, Arrays.asList(OPERATORS), Arrays.asList(PLATEFORMS), NODE_NUMBER, TOTAL_ENUMERATIONS, totalEnumOpertorsPlatforms);
            // print
            System.out.println("Exhaustive OPERATOR-PLATFORMS  with platform switch pruning Generations:");
            System.out.println("Enumerated  : " + totalEnumOpertorsPlatforms.size());
        }
    }
}
