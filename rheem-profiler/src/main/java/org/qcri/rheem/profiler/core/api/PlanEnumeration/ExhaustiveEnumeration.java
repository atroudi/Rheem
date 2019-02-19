package org.qcri.rheem.profiler.core.api.PlanEnumeration;
import org.qcri.rheem.core.optimizer.mloptimizer.api.Tuple2;

import java.util.*;

/**
 * Exhaustive generation API used to fill pipeline {@link org.qcri.rheem.core.optimizer.mloptimizer.api.Topology}s
 * inside {@link org.qcri.rheem.profiler.core.ProfilingPlanBuilder}
 */

public class ExhaustiveEnumeration {

    /**total
     * Generate exhaustively all operator combinations for {@param n} number of nodes
     * @param nodes
     * @param operators
     * @param node_pos
     * @return
     */
    static private void recursiveSimpleEnumeration(List<String> nodes, List<String> operators, int node_pos, List<List<String>> totalList ){
        if(nodes.size()==node_pos){
            totalList.add(nodes);
            return;
        }
        else
            for (String op:operators){
                List newNodes = new ArrayList(nodes);
                    newNodes.remove(node_pos);
                    // Add operator
                    newNodes.add(node_pos,op);
                recursiveSimpleEnumeration(newNodes,operators,node_pos+1, totalList);
            }

        return;
    }

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
     * Generate exhaustively all operator combinations for {@param n} number of nodes
     * @param nodes
     * @param operators
     * @param node_pos
     * @return
     */
    static private void recursiveEnumerationWithSwitchConstraint(List<Tuple2<String, String>> nodes, List<String> operators, int node_pos , int remaining_switch, String operator, List totalList){
        if((nodes.size()==node_pos)||(remaining_switch==0)){
//            if (node_pos<nodes.size())
//                // fill the rest of the list with same operator
//                for(int i=node_pos;node_pos<nodes.size();i++) {
//                    nodes.remove(i);
//                    nodes.add(i, nodes.get(node_pos-1));
//                }
            totalList.add(nodes);
            return;
        }
        else
            for (String op:operators){
                List newNodes = new ArrayList(nodes);
                newNodes.remove(node_pos);
                Tuple2<String,String> concat = new Tuple2<>(operator,op);

                // Add operator
                newNodes.add(node_pos,concat);

                if (node_pos==0)
                    recursiveEnumerationWithSwitchConstraint(newNodes,operators,node_pos+1,remaining_switch, operator, totalList);
                else
                    // check if there's an operator switch
                    if (newNodes.get(node_pos-1)!=newNodes.get(node_pos))
                        recursiveEnumerationWithSwitchConstraint(newNodes,operators,node_pos+1, remaining_switch-1, operator, totalList);
                    else
                        recursiveEnumerationWithSwitchConstraint(newNodes,operators,node_pos+1,remaining_switch, operator, totalList);
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
    static public void doubleRecursiveEnumeration(List<Tuple2<String,String>> nodes, List<String> list1, List<String> list2, int node_pos, int node_number, List<List<Tuple2<String, String>>> totalList){
        if(node_number==node_pos){
            recursiveEnumeration(nodes, list1, 0, totalList);
            return;
        }
        else
            for (String plat:list2){

                // fix the operator, now we enumerate platforms
                // Single operator recursivity
                List<Tuple2<String,String>> newNodes = new ArrayList(nodes);
                newNodes.remove(node_pos);
                newNodes.add(node_pos, new Tuple2<>(nodes.get(node_pos).field0,plat));
                doubleRecursiveEnumeration(newNodes, list1, list2, node_pos+1, node_number, totalList);
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
        private static int MAX_PLATEFORM_SWITCH = 1;
        private static List nodes = new ArrayList<String>(5);
        private static List<List<String>> totalEnumOpertors = new ArrayList<>();
        private static List<List<Tuple2<String, String>>> totalEnumOpertorsWithSwitch = new ArrayList<>();
        private static List<List<Tuple2<String,String>>> totalEnumOpertorsPlatforms = new ArrayList<>();

        public static void main(String[] args){


            // Initialize nodes
            for(int i=0;i<NODE_NUMBER;i++)
                nodes.add(new Tuple2<>(OPERATORS[0],PLATEFORMS[0]));

            // store exhaustive enumerations in totalLists
            recursiveSimpleEnumeration(nodes, Arrays.asList(OPERATORS),0, totalEnumOpertors);

            // print
            System.out.println("Exhaustive OPERATOR Generations:");
            System.out.println("Enumerated  : " + totalEnumOpertors.size());


            // store double exhaustive enumerations in totalLists
            doubleRecursiveEnumeration(nodes, Arrays.asList(OPERATORS), Arrays.asList(PLATEFORMS), 0, NODE_NUMBER, totalEnumOpertorsPlatforms);

            // print
            System.out.println("Exhaustive OPERATOR-PLATFORMS Generations:");
            System.out.println("Enumerated  : " + totalEnumOpertorsPlatforms.size());
        }
    }
}
