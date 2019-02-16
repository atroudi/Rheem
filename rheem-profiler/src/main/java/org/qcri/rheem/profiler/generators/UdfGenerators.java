package org.qcri.rheem.profiler.generators;

import org.qcri.rheem.core.function.FunctionDescriptor;
import org.qcri.rheem.core.function.PredicateDescriptor;
import org.qcri.rheem.core.types.BasicDataUnitType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class that generates UDFs
 */
public class UdfGenerators <Input, Output> implements Serializable{

    protected final BasicDataUnitType<Input> inputType;

    protected final BasicDataUnitType<Output> outputType;

    public UdfGenerators(BasicDataUnitType<Input> inputType, BasicDataUnitType<Output> outputType) {
        this.inputType = inputType;
        this.outputType = outputType;
    }

    /**
     * Map UDFs for Integer Basic dataType
     * @param complexity CPU complexity of the processing of the UDF
     * @param dataQuataSize data Quanta size to be processed
     * @param complexityCoef other coefficients to use when provided
     * @return
     */
    public static FunctionDescriptor.SerializableFunction<Integer, Integer> mapIntUDF(int complexity, int dataQuataSize, long... complexityCoef) {
        FunctionDescriptor.SerializableFunction<Integer, Integer> lala = null;
        switch (complexity) {
            case 1:
                // O(n) UDF complexity with linear cost
                lala = i -> {
                    return i * 5;
                };
                break;
            case 12:
                // O(n) UDF complexity with log cost
                lala = i -> i * i + (int) Math.log(i);
                break;
            case 13:
                // O(n) UDF complexity with exp cost
                lala = i -> (int) Math.exp(i);
                //case 4:
                //    return i -> (int) Math.cos(i);
                break;
            case 2:
                //if (complexityCoef.length==1)
                // O(log(n)) UDF complexity
                lala = i -> {
                    int iterations = 0;
                    for (int count = 1; count <= dataQuataSize; count = count * 2) {
                        iterations = iterations + i * 5;
                    }
                    return iterations;
                };
                break;
                //else
                //return i -> i * 5;
            case 3:
                lala = i -> {
                    int iterations = 0;
                    for (int count = 1; count <= dataQuataSize; count++) {
                        iterations = iterations + i * 5;
                    }
                    return iterations;
                };
                //if (complexityCoef.length==1)
                // O(n2) UDF complexity

                //else
                //    return i -> i * 5;
                //new RheemException("Missing UDF complexity coefficient!");
                break;
            default:
                lala = null;
                break;
        }

        return lala;
    }

    /**
     * FlatMap UDFs for Integer List Basic dataType
     * @param complexity CPU complexity of the processing of the UDF
     * @param dataQuataSize data Quanta size to be processed
     * @param complexityCoef other coefficients to use when provided
     * @return
     */
    public static FunctionDescriptor.SerializableFunction<List, List> MapListIntUDF( int complexity,  int dataQuataSize, long... complexityCoef) {
        return list -> (ArrayList<Integer>) list.stream().map(el->mapIntUDF(complexity,dataQuataSize).apply((Integer) el))
                .collect(Collectors.toList());
    }

    /**
     * FlatMap UDFs for Integer List Basic dataType
     * @param complexity CPU complexity of the processing of the UDF
     * @param dataQuataSize data Quanta size to be processed
     * @param complexityCoef other coefficients to use when provided
     * @return
     */
    public static FunctionDescriptor.SerializableFunction<List, List> flatMapListIntUDF( int complexity,  int dataQuataSize, long... complexityCoef) {
        return list -> {
            List output = new ArrayList();
            for(int itr=0;itr<=dataQuataSize;itr++)
                output.add(list.stream().map(el->mapIntUDF(complexity,dataQuataSize).apply((Integer) el))
                        .collect(Collectors.toList()));
            return output;
        };
    }

    /**
     * FlatMap UDFs for Integer List Basic dataType
     * @param complexity CPU complexity of the processing of the UDF
     * @param dataQuataSize data Quanta size to be processed
     * @param complexityCoef other coefficients to use when provided
     * @return
     */
    public static FunctionDescriptor.SerializableFunction<Integer, List> flatMapIntUDF( int complexity,  int dataQuataSize, long... complexityCoef) {
        return  i -> {
            List output = new ArrayList();
            for(int itr=0;itr<=dataQuataSize;itr++)
                output.add(i);
            return output;
        };
    }

    /**
     * FlatMap UDFs for String list Basic dataType
     * @param complexity CPU complexity of the processing of the UDF
     * @param dataQuataSize data Quanta size to be processed
     * @param complexityCoef other coefficients to use when provided
     * @return
     */
    public static FunctionDescriptor.SerializableFunction<List, List> MapListStringUDF( int complexity,  int dataQuataSize, long... complexityCoef) {
        return list -> (ArrayList<String>) list.stream().map(el->mapStringUDF(complexity,dataQuataSize).apply((String) el))
                .collect(Collectors.toList());
    }

    /**
     * FlatMap UDFs for String list Basic dataType
     * @param complexity CPU complexity of the processing of the UDF
     * @param dataQuataSize data Quanta size to be processed
     * @param complexityCoef other coefficients to use when provided
     * @return
     */
    public static FunctionDescriptor.SerializableFunction<String, List> flatMapStringUDF( int complexity,  int dataQuataSize, long... complexityCoef) {
        return s -> new ArrayList<>(Arrays.asList(s.split("a")));
    }

    /**
     * Map UDFs for Integer Lists dataType
     * @param complexity CPU complexity of the processing of the UDF
     * @param dataQuataSize data Quanta size to be processed
     * @param coef other coefficients to use when provided
     * @return
     */
    public static FunctionDescriptor.SerializableFunction<List,List> mapIntListUDF(int complexity,  int dataQuataSize, long... coef){
        return  i -> (List<Integer>)i.stream()
                .map(el -> (Integer) UdfGenerators.mapIntUDF(complexity,dataQuataSize).apply((Integer) el))
                .collect(Collectors.toList());
    }



    public static FunctionDescriptor.SerializableFunction<String, String> mapStringUDF( int complexity,  int dataQuataSize, long... coef) {
        switch (complexity) {
            case 1:
                return String::new;
            case 2:
                return i -> {String tempString = "**";for (int count = 1; count<= dataQuataSize; count=count*2) {
                    if (i!= null)tempString= tempString+i;} return tempString;};
                //tmpInt=tmpInt+5*(i+ tempString).length();
            //tempString= tempString +i;
            case 3:
                return i -> {String tempString = "**"; for (int count = 1; count<= dataQuataSize; count++) {if (i!= null)tempString= tempString+i;} return tempString;};
            default:
                return null;
        }
    }


    public static PredicateDescriptor.SerializablePredicate <Integer> filterIntUDF(int complexity,  int dataQuataSize, long... coef) {
        switch (complexity) {
            case 1:
                return i ->  {
                    //mapIntUDF(complexity).apply(i);
                    //return (i & 1)==0;
                    mapIntUDF(complexity,dataQuataSize).apply(i);
                    return true;
                };
            case 2:
                return i -> {
                    mapIntUDF(complexity,dataQuataSize).apply(i);
                    return (i > 0);
                };
            case 3:
                return i -> {
                    mapIntUDF(complexity,dataQuataSize).apply(i);
                    return (i > 0 & ((i & 1) == 0));
                };
            default:
                return null;
        }
    }

    public static PredicateDescriptor.SerializablePredicate <String> filterStringUDF(int complexity,  int dataQuataSize, long... coef) {
        switch (complexity) {
            case 1:
                return s ->  {
                    //mapIntUDF(complexity).apply(i);
                    //return (i & 1)==0;
                    mapStringUDF(complexity,dataQuataSize).apply(s);
                    return true;
                };
            case 2:
                return s -> {
                    mapStringUDF(complexity,dataQuataSize).apply(s);
                    return (s.indexOf("a") > 0);
                };
            case 3:
                return s -> {
                    mapStringUDF(complexity,dataQuataSize).apply(s);
                    //return false;
                    return (s.indexOf("a")> 0 || ((s.indexOf("a") & 1) == 0));
                };
            default:
                return null;
        }
    }


    public static PredicateDescriptor.SerializablePredicate <List> filterIntListUDF(int complexity,  int dataQuataSize, long... coef) {
        switch (complexity) {
            case 1:
                //i ->  ((Integer)i.get(0) & 1)==0;
                return i -> {
                    //i.stream().map(el -> mapIntUDF(complexity).apply((Integer)el));
                    mapIntListUDF(complexity,dataQuataSize).apply(i);
                    //return true;
                    return (i.stream().allMatch(x -> ((int)x & 1)==0));
                };
            case 2:
                return i -> {
                    //i.stream().map(el -> (int) Math.exp((int)el));
                    mapIntListUDF(complexity,dataQuataSize).apply(i);
                    return (i.stream().allMatch(x -> (((int)x & 1)==0) & ((int)x > 0)));
                };
            case 3:
                return i -> {
                    //i.stream().map(el -> mapIntUDF(complexity).apply((Integer)el));
                    mapIntListUDF(complexity,dataQuataSize).apply(i);
                    //return true;
                    return (i.stream().allMatch(x -> ((((int)x & 1)==0) & ((int)x > 0)) | (((int)x > 1000))));
                };
            default:
                return null;
        }
    }

    public static FunctionDescriptor.SerializableFunction<Integer, String> InttoString( int complexity,  int dataQuataSize, long... complexityCoef) {
        return i -> Integer.toString(i);
    }

    public static FunctionDescriptor.SerializableFunction<Integer, String> StringtoInt( int complexity,  int dataQuataSize, long... complexityCoef) {
        return i -> Integer.toString(i);
    }

    private static Integer highCompexity(int dataQuataSize, int input){
        int iterations=0;
        for (int count = 1; count<= dataQuataSize; count++) {
            //iterations = iterations + (int) Math.log(input);
            iterations = iterations + (int) Math.log(input);
        }
        return iterations;
    }


    interface TwoIntArgInterface<Input, Output> {

        public Output operation(Input a);
    }

}

/*
public class UdfGenerators <Input, Output> {

    protected final BasicDataUnitType<Input> inputType;

    protected final BasicDataUnitType<Output> outputType;

    public UdfGenerators(BasicDataUnitType<Input> inputType, BasicDataUnitType<Output> outputType) {
        this.inputType = inputType;
        this.outputType = outputType;
    }

    public FunctionDescriptor.SerializableFunction<Input, Output> mapUDF(Class<Input> inClass, int complexity, long... coef){
        if (inClass == )
        switch (inClass.getComponentType()){
            case (Class<?>) Integer.class.getComponentType():
                switch (complexity){
                    case 1:
                        return i -> 5 + i.value;
                    case 2:
                        return (Object i) -> {
                            return (i) ->  5 * i;
                        };
                }
        }

        String a;
        Pattern p;

        p.

            return i -> i;
    }

    /} */


/*
SECOND VERSION

public class UdfGenerators <Input, Output> {

    protected final BasicDataUnitType<Input> inputType;

    protected final BasicDataUnitType<Output> outputType;

    public UdfGenerators(BasicDataUnitType<Input> inputType, BasicDataUnitType<Output> outputType) {
        this.inputType = inputType;
        this.outputType = outputType;
    }

    public FunctionDescriptor.SerializableFunction<Input, Output> mapUDF(BasicDataUnitType<Input> inputType, int complexity, long... coef){
        if ((inputType.getTypeClass() == Integer.class)&&(outputType.getTypeClass() == Integer.class)){
            switch (complexity){
                case 1:
                    return i -> 5 + i;
                case 2:
                    return (Object i) -> {
                        return (i) ->  5 * i;
                    };
            }
        }
        Class<Input> in = inputType.getTypeClass();

    }


}

 */