package org.qcri.rheem.profiler.core.api.PlanEnumeration;

import org.qcri.rheem.core.optimizer.mloptimizer.api.Tuple2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * MaxPlatformPruningEnumeration API used to fill pipeline {@link org.qcri.rheem.core.optimizer.mloptimizer.api.Topology}s
 * inside {@link org.qcri.rheem.profiler.core.ProfilingPlanBuilder}
 */
public class MaxPlatformPruningEnumeration {

    /**
     * Will be exhaustively associating all platforms with input {@param operator}
     * @param nodes
     * @param list
     * @param node_pos
     * @param total_list
     * @return
     */
    static private void recursiveEnumeration(List<Tuple2<String,String>> nodes, List<String> list, int node_pos, List<List<Tuple2<String, String>>> total_list){
        // Change here
        if(nodes.size()==node_pos){
            // the enumeration on all platforms is performed
            total_list.add(nodes);
            return;

        }
        else
            for (String el:list){
                List newNodes = new ArrayList(nodes);
                newNodes.remove(node_pos);
                // Add platform
                Tuple2<String,String> concat = new Tuple2<>(el, nodes.get(node_pos).field1);

                newNodes.add(node_pos,concat);
                recursiveEnumeration(newNodes,list,node_pos+1, total_list);
            }

        return;
    }


    /**
     * Generate exhaustively all operator and platform combinations for {@param n} number of nodes
     * @param nodes
     * @param list1
     * @param list2
     * @param node_pos
     * @return
     */
    static public void doubleRecursiveEnumerationWithSwitchPruning(List<Tuple2<String,String>> nodes, List<String> list1, List<String> list2, int node_pos, int maxSwitch, int total_node_number, List<List<Tuple2<String, String>>> totalList){
        assert (maxSwitch<total_node_number): "maxPlatformSwitch property should be strictly lower than Shape total_Node_Number";

        // In case assert doesn't work
        if (maxSwitch>=total_node_number)
            maxSwitch = total_node_number-1;

        // Exit if platform switches are passed
        if (maxSwitch==0){
            // Add all remaining platform combinations if maxSwitch is zero
            for (String plat:list2.subList(0,list2.size())){
                List<Tuple2<String,String>> newNodes = new ArrayList(nodes);
                for(int index=node_pos;index<newNodes.size();index++) {
                    newNodes.remove(index);
                    newNodes.add(index, new Tuple2<>(nodes.get(index).field0, plat));
                }
                recursiveEnumeration(newNodes, list1, 0, totalList);
            }
            return;

        } else if(node_pos==total_node_number){
            // Exit in case we enumerate all nodes
            recursiveEnumeration(nodes, list1, 0, totalList);
            return;
        } else
            for (String plat:list2){
                // replace node on node_pos platform
                List<Tuple2<String,String>> newNodes = new ArrayList(nodes);
                newNodes.remove(node_pos);
                newNodes.add(node_pos, new Tuple2<>(nodes.get(node_pos).field0,plat));
                if (maxSwitch!=-1)
                    doubleRecursiveEnumerationWithSwitchPruning(newNodes, list1, list2, node_pos+1, maxSwitch-1, total_node_number, totalList);
                else
                    doubleRecursiveEnumerationWithSwitchPruning(newNodes, list1, list2, node_pos+1, maxSwitch, total_node_number, totalList);
            }
        return;
    }

    /**
     * Driver class to Tests all above exhaustive generations
     */
    static class Main{
        // Platforms to enumerate
        private static String[] PLATEFORMS = {"Java","Spark","Flink"};
        private static String[] OPERATORS = {"Map","Flatmap","Filter"};
        private static final int NODE_NUMBER = 4;
        private static int MAX_PLATEFORM_SWITCH = 3;
        private static List nodes = new ArrayList<String>(5);
        private static List<List<String>> totalEnumOpertors = new ArrayList<>();
        private static List<List<Tuple2<String, String>>> totalEnumOpertorsWithSwitch = new ArrayList<>();
        private static List<List<Tuple2<String,String>>> totalEnumOpertorsPlatforms = new ArrayList<>();

        public static void main(String[] args){


            // Initialize nodes
            for(int i=0;i<NODE_NUMBER;i++)
                nodes.add(new Tuple2<>(OPERATORS[0],PLATEFORMS[0]));

            // store double exhaustive enumerations with platform switch pruning in totalLists
            doubleRecursiveEnumerationWithSwitchPruning(nodes, Arrays.asList(OPERATORS), Arrays.asList(PLATEFORMS), 0,MAX_PLATEFORM_SWITCH, NODE_NUMBER, totalEnumOpertorsWithSwitch);

            // print
            System.out.println("Exhaustive OPERATOR-PLATFORMS  with platform switch pruning Generations:");
            System.out.println("Enumerated  : " + totalEnumOpertorsWithSwitch.size());
        }
    }
}
