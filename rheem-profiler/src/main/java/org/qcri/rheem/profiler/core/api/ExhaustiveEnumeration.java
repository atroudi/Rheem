package org.qcri.rheem.profiler.core.api;
import org.apache.pig.builtin.MAX;
import org.qcri.rheem.core.optimizer.mloptimizer.api.Tuple2;

import java.util.*;

/**
 * Exhaustive generation API used to fill pipeline {@link org.qcri.rheem.core.optimizer.mloptimizer.api.Topology}s
 * inside {@link org.qcri.rheem.profiler.core.ProfilingPlanBuilder}
 */

public class ExhaustiveEnumeration {

    // Platforms to enumerate
    private static String[] PLATEFORMS = {"Java","Spark","Flink"};
    private static String[] OPERATORS = {"Map","Flatmap","Filter"};
    private static final int NODE_NUMBER = 3;
    private static List nodes = new ArrayList<String>(5);
    private static List<List<String>> totalEnumOpertors = new ArrayList<>();
    private static List<List<Tuple2<String, String>>> totalEnumOpertorsWithSwitch = new ArrayList<>();

    private static List<List<Tuple2<String,String>>> totalEnumOpertorsPlatforms = new ArrayList<>();
    private static int MAX_PLATEFORM_SWITCH = 0;

    /**
     * Tests all exhaustive generations
     * @param args
     */
    public static void main(String[] args){


        // Initialize nodes
        for(int i=0;i<NODE_NUMBER;i++)
            nodes.add(new Tuple2<>(OPERATORS[0],PLATEFORMS[0]));

        // store exhaustive enumerations in totalLists
//        recursiveEnumeration(nodes, Arrays.asList(OPERATORS),0);

        // PRINT
//        System.out.println("Exhaustive OPERATOR Generations:");
//        totalEnumOpertors.forEach(l -> System.out.println(l.toString()));
//        System.out.println("Enumerated  : " + totalEnumOpertors.size());

        // Enumeration with switch constraints
//        recursiveEnumerationWithSwitchConstraint(nodes, Arrays.asList(OPERATORS),0, MAX_PLATEFORM_SWITCH, "Java");
//        doubleRecursiveEnumerationWithSwitchConstraint(nodes, Arrays.asList(OPERATORS), Arrays.asList(PLATEFORMS), 0, MAX_PLATEFORM_SWITCH);
//        totalEnumOpertorsPlatforms.forEach(l -> System.out.println(l.toString()));
//        System.out.println("Enumerated  : " + totalEnumOpertorsPlatforms.size());

        // store double exhaustive enumerations in totalLists
        doubleRecursiveEnumerationWithSwitchConstraint(nodes, Arrays.asList(OPERATORS), Arrays.asList(PLATEFORMS), 0,3);

        // PRINT
        System.out.println("Exhaustive OPERATOR-PLATFORMS Generations:");
        totalEnumOpertorsPlatforms.forEach(l -> System.out.println(l.toString()));
        System.out.println(totalEnumOpertorsPlatforms.size());

    }

    /**
     * Generate exhaustively all operator combinations for {@param n} number of nodes
     * @param nodes
     * @param operators
     * @param n
     * @return
     */
    static private List<String> recursiveEnumeration(List<String> nodes, List<String> operators, int n ){
        if(nodes.size()==n){
            totalEnumOpertors.add(nodes);
            return nodes;
        }
        else
            for (String op:operators){
                List newNodes = new ArrayList(nodes);
                    newNodes.remove(n);
                    // Add operator
                    newNodes.add(n,op);
                recursiveEnumeration(newNodes,operators,n+1);
            }

        return new ArrayList<String>();
    }

    /**
     * Will be exhaustively associating all platforms with input {@param operator}
     * @param nodes
     * @param list
     * @param n
     * @param operator
     * @return
     */
    static private List<Tuple2<String,String>> recursiveEnumeration(List<Tuple2<String,String>> nodes, List<String> list, int n, String operator){
        // Change here
        if(nodes.size()==n){
            // the enumeration on all platforms is performed
            totalEnumOpertorsPlatforms.add(nodes);

            // Now is time to enumerate all operators
//            recursiveOperatorEnumeration(nodes, list2, 0, platform);

            return nodes;

        }
        else
            for (String el:list){
                List newNodes = new ArrayList(nodes);
                newNodes.remove(n);
                // Add platform
                Tuple2<String,String> concat = new Tuple2<>(el, nodes.get(n).field1);

                newNodes.add(n,concat);
                recursiveEnumeration(newNodes,list,n+1, operator);
            }

        return new ArrayList<Tuple2<String,String>>();
    }

    static private List<Tuple2<String,String>> recursiveOperatorEnumeration(List<Tuple2<String,String>> nodes, List<String> list, int n, String platform){
        // Change here
        if(nodes.size()==n){
            // the enumeration on all platforms is performed
            totalEnumOpertorsPlatforms.add(nodes);

            // Now is time to enumerate all operators
            return nodes;

        }
        else
            for (String el:list){
                List newNodes = new ArrayList(nodes);
                newNodes.remove(n);
                // Add platform
                Tuple2<String,String> concat = new Tuple2<>(el,platform);

                newNodes.add(n,concat);
                recursiveOperatorEnumeration(newNodes,list,n+1, platform);
            }

        return new ArrayList<Tuple2<String,String>>();
    }

    /**
     * Generate exhaustively all operator combinations for {@param n} number of nodes
     * @param nodes
     * @param operators
     * @param node_pos
     * @return
     */
    static private List<Tuple2<String, String>> recursiveEnumerationWithSwitchConstraint(List<Tuple2<String, String>> nodes, List<String> operators, int node_pos , int remaining_switch, String operator){
        if((nodes.size()==node_pos)||(remaining_switch==0)){
//            if (node_pos<nodes.size())
//                // fill the rest of the list with same operator
//                for(int i=node_pos;node_pos<nodes.size();i++) {
//                    nodes.remove(i);
//                    nodes.add(i, nodes.get(node_pos-1));
//                }
            totalEnumOpertorsPlatforms.add(nodes);
            return nodes;
        }
        else
            for (String op:operators){
                List newNodes = new ArrayList(nodes);
                newNodes.remove(node_pos);
                Tuple2<String,String> concat = new Tuple2<>(operator,op);

                // Add operator
                newNodes.add(node_pos,concat);

                if (node_pos==0)
                    recursiveEnumerationWithSwitchConstraint(newNodes,operators,node_pos+1,remaining_switch, operator);
                else
                    // check if there's an operator switch
                    if (newNodes.get(node_pos-1)!=newNodes.get(node_pos))
                        recursiveEnumerationWithSwitchConstraint(newNodes,operators,node_pos+1, remaining_switch-1, operator);
                    else
                        recursiveEnumerationWithSwitchConstraint(newNodes,operators,node_pos+1,remaining_switch, operator);
            }

        return new ArrayList<Tuple2<String, String>>();
    }

    /**
     * Generate exhaustively all operator and platform combinations for {@param n} number of nodes
     * @param nodes
     * @param list1
     * @param list2
     * @param n
     * @return
     */
    static public List<Tuple2<String,String>> doubleRecursiveEnumerationWithSwitchConstraint(List<Tuple2<String,String>> nodes, List<String> list1, List<String> list2, int n, int maxSwitch){
        if((nodes.size()==n)||(maxSwitch==0)){
            recursiveEnumeration(nodes, list1, 0, "test");
            return nodes;
        }
        else
            for (String plat:list2){
                List<Tuple2<String,String>> newNodes = new ArrayList(nodes);
                newNodes.remove(n);
                newNodes.add(n, new Tuple2<>(nodes.get(n).field0,plat));
                // fix the operator, now we enumerate platforms
                // Single operator recursivity
                if (maxSwitch!=-1)
                    doubleRecursiveEnumerationWithSwitchConstraint(newNodes, list1, list2, n+1, maxSwitch-1);
                else
                    doubleRecursiveEnumerationWithSwitchConstraint(newNodes, list1, list2, n+1, maxSwitch);
            }

        return new ArrayList<Tuple2<String,String>>();
    }

    /**
     * Generate exhaustively all operator and platform combinations for {@param n} number of nodes
     * @param nodes
     * @param list1
     * @param list2
     * @param n
     * @return
     */
    static public List<Tuple2<String,String>> doubleRecursiveEnumeration(List<Tuple2<String,String>> nodes, List<String> list1, List<String> list2, int n){
        if(nodes.size()==n){
            recursiveEnumeration(nodes, list1, 0, "test");

            return nodes;
        }
        else
            for (String plat:list2){

                // fix the operator, now we enumerate platforms
                // Single operator recursivity
                List<Tuple2<String,String>> newNodes = new ArrayList(nodes);
                newNodes.remove(n);
                newNodes.add(n, new Tuple2<>(nodes.get(n).field0,plat));
                doubleRecursiveEnumeration(newNodes, list1, list2, n+1);
            }

        return new ArrayList<Tuple2<String,String>>();
    }

    public static List<List<Tuple2<String,String>>> getTotalEnumOpertorsPlatforms() {
        return totalEnumOpertorsPlatforms;
    }

}
