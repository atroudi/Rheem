package org.qcri.rheem.tests;

import org.qcri.rheem.basic.data.Tuple2;
import org.qcri.rheem.basic.data.Tuple3;
import org.qcri.rheem.basic.function.ProjectionDescriptor;
import org.qcri.rheem.basic.operators.*;
import org.qcri.rheem.core.api.RheemContext;
import org.qcri.rheem.core.function.FlatMapDescriptor;
import org.qcri.rheem.core.function.ReduceDescriptor;
import org.qcri.rheem.core.function.TransformationDescriptor;
import org.qcri.rheem.core.plan.rheemplan.RheemPlan;
import org.qcri.rheem.core.types.DataSetType;
import org.qcri.rheem.core.types.DataUnitGroupType;
import org.qcri.rheem.core.types.DataUnitType;
import org.qcri.rheem.core.util.ReflectionUtils;
import org.qcri.rheem.core.util.Tuple;
import org.qcri.rheem.java.Java;
import org.qcri.rheem.spark.Spark;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.qcri.rheem.tests.RheemPlans.createUri;

/**
 * Created by migiwara on 23/07/17.
 */
public class BioApp {

    private Object tuple;

    public static void main(String[] args){

        // Instantiate Rheem and activate the backend.
        RheemContext rheemContext = new RheemContext().with(Spark.basicPlugin());
        rheemContext.with(Java.basicPlugin());

        URI sourceLink = createUri("/genotypes.txt");
        TextFileSource textFileSource = new TextFileSource(sourceLink.toString());

        int[] row = {1};

        List<Character> leastCharList = new ArrayList<>();

        // for each line (input) output an iterator of each couple of characters
        // It also compute the frequency for each column by starting with picking the first letter as initial values stored in leastCharList
        // It output tuple3 with corresponding values for each cell (RowID,ColID,frequency)
        FlatMapOperator<String, Tuple3> flatMapOperator = new FlatMapOperator<>(
                new FlatMapDescriptor<>(line -> {
                    // process to execute per row
                    // split the row string into words
                    int[] col={1};

                    //String[] row = Arrays.asList((String[]) line.split(" "));

                    List<String> RowVars = Arrays.asList((String[]) line.split(" "));

                    if (leastCharList.size()==0)// set the leastCharList
                        RowVars.stream().forEach(s->leastCharList.add(s.charAt(0)));

                    List<Tuple3> output = new ArrayList<Tuple3>();
                    RowVars.stream().forEach(s->{
                        int frequency=0;
                        if(s.substring(0,1).contains(leastCharList.get(col[0]-1).toString())) frequency++;
                        if(s.substring(1,2).contains(leastCharList.get(col[0]-1).toString())) frequency++;
                        Tuple3<Integer,Integer,Integer> tuple3 = new Tuple3<>(row[0],col[0],frequency);
                        output.add(tuple3);
                        col[0]++;
                    });

                    row[0]++;
                    return  output;

                },
                        String.class,
                        Tuple3.class
                )
        );

        final ProjectionDescriptor<Tuple3<Integer,Integer, Integer>, String> keyDescriptor = new ProjectionDescriptor<>(
                DataUnitType.createBasicUnchecked(Tuple3.class),
                DataUnitType.createBasicUnchecked(Integer.class),
                "field1");


        // Group all elements by columns
        GroupByOperator<Tuple3<Integer,Integer,Integer>,Integer> groupByOperator = new GroupByOperator(
                keyDescriptor,
                DataSetType.createDefaultUnchecked(Tuple3.class),
                DataSetType.createDefaultUnchecked(List.class)
        );

        // Correct the frequencies
        FlatMapOperator<List,Tuple2> flatMapOperator1 = new FlatMapOperator<>(
                new FlatMapDescriptor<>(tuple3List ->{

                    List<Tuple3<Integer,Integer, Integer>> tmplist = tuple3List;
                    List<Tuple2> tmplist2 = new ArrayList<>();
                    int sum = tmplist.stream().map(t->(Integer)t.field2).mapToInt(Integer::intValue).sum();
                    double ratio = sum/tuple3List.size();
                    if (ratio>0.5){
                        //correct the accuracy frequency
                        tmplist2 = tmplist.stream().map(t -> new Tuple2<>((Integer)t.field0,new ArrayList<>(Arrays.asList(2-(Integer)t.field2)))).collect(Collectors.toList());
                    }else{
                        tmplist2 = tmplist.stream().map(t -> new Tuple2<>((Integer)t.field0,new ArrayList<>(Arrays.asList(t.field2)))).collect(Collectors.toList());

                    }

                    //List<Integer> list = new ArrayList<>(Arrays.asList(t3.field2));
                    //Tuple2 t2 = new Tuple2<>(t3.field0,list);
                    //return t2;

                    return tmplist2;
                },
                        List.class,
                        Tuple2.class
                )
        );

        // The below map is to transform from tuple3<Integer,Integer,Integer> to Tuple2<Integer,List<Integer>>

        MapOperator<Tuple3<Integer,Integer,Integer>,Tuple2<Integer,List<Integer>>> mapOperator = new MapOperator<>(
                t3->{
                    List<Integer> list = new ArrayList<>(Arrays.asList(t3.field2));
                    Tuple2 t2 = new Tuple2<>(t3.field0,list);
                    return t2;
                },
                ReflectionUtils.specify(Tuple3.class),
                ReflectionUtils.specify(Tuple2.class)
        );

        // Reduce By Key to restore the Rows with corresponding frequencies
        // The output will be for each iteration a row of the final matrix to use for the Analysis
        // Output format List(RowID,freq1,freq2,...)

        ReduceByOperator<Tuple2<Integer,List<Integer>>,Integer> reduceByOperator = new ReduceByOperator<>(
                new TransformationDescriptor<>(tuple -> tuple.field0,
                        DataUnitType.createBasicUnchecked(Tuple2.class),
                        DataUnitType.createBasicUnchecked(Integer.class)),
                new ReduceDescriptor<>(
                        ((t1,t2)->{
                            //return new ArrayList<Integer>();

                            //concatenate the two arrays
                            List<Integer> array1and2 = new ArrayList<>();//[t1.field1.length + t2.field1.length];

                            array1and2.addAll(t1.field1);
                            array1and2.addAll(t2.field1);

                            Tuple2<Integer,List<Integer>> tmpt = new Tuple2<>(t1.field0,array1and2);

                            return tmpt;
                        }),
                        DataUnitType.createGroupedUnchecked(Tuple2.class),
                        DataUnitType.createBasicUnchecked(Tuple2.class)
                ),DataSetType.createDefaultUnchecked(Tuple2.class)

        );

        // Analysis step
        MapOperator<Tuple2<Integer,List<Integer>>,Tuple2<Integer,List<Integer>>> testMapOperator = new MapOperator<>(
                t2->{
                    return t2;
                },
                ReflectionUtils.specify(Tuple2.class),
                ReflectionUtils.specify(Tuple2.class)
        );

        // Add the cartesian product

        CartesianOperator<Tuple2<Integer,List<Integer>>,Tuple2<Integer,List<Integer>>> cartesianOperator = new CartesianOperator<>(
                DataSetType.createDefaultUnchecked(Tuple2.class),
                DataSetType.createDefaultUnchecked(Tuple2.class)
        );

        // Analysis step
        MapOperator<Tuple2<Integer,List<Integer>>,Tuple2<Integer,List<Integer>>> analysisMapOperator = new MapOperator<>(
                t2->{
                    return t2;
                },
                ReflectionUtils.specify(Tuple2.class),
                ReflectionUtils.specify(Tuple2.class)
        );



        textFileSource.connectTo(0, flatMapOperator, 0);

        // write results to a sink
        List<Tuple2> results = new ArrayList<>();
        LocalCallbackSink<Tuple2> sink = LocalCallbackSink.createCollectingSink(results, DataSetType.createDefault(Tuple2.class));

        flatMapOperator.connectTo(0,groupByOperator,0);
        groupByOperator.connectTo(0,flatMapOperator1,0);
        flatMapOperator1.connectTo(0,reduceByOperator,0);
        // read the
        //mapOperator.connectTo(0,reduceByOperator,0);
        reduceByOperator.connectTo(0,testMapOperator,0);
        testMapOperator.connectTo(0,cartesianOperator,0);
        reduceByOperator.connectTo(0,cartesianOperator,1);
        cartesianOperator.connectTo(0,analysisMapOperator,0);
        analysisMapOperator.connectTo(0,sink,0);

        // Have Rheem execute the plan.
        rheemContext.execute(new RheemPlan(sink));


        System.out.println(results.toString());

    }
}