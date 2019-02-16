package org.qcri.rheem.profiler.generators;

import org.apache.commons.lang3.Validate;
import org.qcri.rheem.core.api.exception.RheemException;
import org.qcri.rheem.core.function.FunctionDescriptor;
import org.qcri.rheem.core.function.PredicateDescriptor;
import org.qcri.rheem.core.types.DataSetType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Utility to create common data generators.
 */
public class DataGenerators implements Serializable {

    private static final String[] CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".split("");

    /**
     *  Create a random string with reuse probability
     * @param stringReservoir
     * @param reuseProbability
     * @param random
     * @param minLen
     * @param maxLen
     * @return
     */
    public static Generator<String> createReservoirBasedStringSupplier(List<String> stringReservoir,
                                                                      double reuseProbability,
                                                                      Random random,
                                                                      int minLen,
                                                                      int maxLen) {
        return () -> {
            if (random.nextDouble() > reuseProbability || stringReservoir.isEmpty()) {
                final String randomString = createRandomString(minLen, maxLen, random);
                stringReservoir.add(randomString);
                return randomString;
            } else {
                return stringReservoir.get(random.nextInt(stringReservoir.size()));
            }
        };
    }

    public static Generator<String> createRandomStringSupplier(int minLen, int maxLen, Random random) {
        return () -> createRandomString(minLen, maxLen, random);
    }

    /**
     * Create a Random string with the below parameters
     * @param minLen
     * @param maxLen
     * @param random
     * @return
     */
    private static String createRandomString(int minLen, int maxLen, Random random) {
        int len = (minLen == maxLen) ? minLen : (random.nextInt(maxLen - minLen) + minLen);
        StringBuilder sb = new StringBuilder(len);
        while (sb.length() < len) {
            sb.append(CHARACTERS[random.nextInt(CHARACTERS.length)]);
        }
        return sb.toString();
    }

    /**
     *
     * @param reservoir
     * @param reuseProbability
     * @param random
     * @return
     */
    public static Generator<Integer> createReservoirBasedIntegerSupplier(List<Integer> reservoir,
                                                                        double reuseProbability,
                                                                        Random random) {
        return () -> {
            if (random.nextDouble() > reuseProbability || reservoir.isEmpty()) {
                final Integer randomInteger = random.nextInt();
                reservoir.add(randomInteger);
                return randomInteger;
            } else {
                return reservoir.get(random.nextInt(reservoir.size()));
            }
        };
    }

    public static Generator<Integer> createRandomIntegerSupplier(Random random) {
        return random::nextInt;
    }

    public static Generator<Integer> createRandomIntegerSupplier(int min, int max, Random random) {
        Validate.isTrue(min <= max);
        return () -> min + random.nextInt(max - min);
    }

    /**
     * Generate Generators
     * @param dataQuantaSize
     * @param type
     * @return
     */
    public static Supplier generateGenerator(int dataQuantaSize, DataSetType type){
        switch (type.getDataUnitType().getTypeClass().getSimpleName()) {
            case "Integer":
                return createReservoirBasedIntegerSupplier(new ArrayList<>(), 0.5d, new Random());
            case "String":
                return createReservoirBasedStringSupplier(new ArrayList<>(), 0.1, new Random(), 4 + dataQuantaSize, 20 + dataQuantaSize);
            case "List":
                return createReservoirBasedIntegerListSupplier(new ArrayList<List<Integer>>(),0.0,new Random(),dataQuantaSize);
            default:
                return createReservoirBasedIntegerSupplier(new ArrayList<>(), 0.5d, new Random());
        }
    }

    /**
     * Generate UDFs
     * @param UdfComplexity
     * @param dataQuantaScale
     * @param type
     * @return
     */
    public static FunctionDescriptor.SerializableFunction generateUDF(int UdfComplexity, int dataQuantaScale, DataSetType type, String operator){

        switch (type.getDataUnitType().getTypeClass().getSimpleName()) {
            case "Integer":
                switch (operator){
                    case "map":
                        return  UdfGenerators.mapIntUDF(UdfComplexity,dataQuantaScale);
                    case "flatmap":
                        return i -> UdfGenerators.flatMapIntUDF(UdfComplexity,dataQuantaScale).apply((Integer) i);
                    default:
                        return i -> UdfGenerators.mapIntUDF(UdfComplexity,dataQuantaScale).apply((Integer) i);
                }
            case "String":
                switch (operator) {
                    case "map":
                        return s -> UdfGenerators.mapStringUDF(UdfComplexity, dataQuantaScale).apply((String) s);
                    case "flatmap":
                        return s -> UdfGenerators.flatMapStringUDF(UdfComplexity, dataQuantaScale).apply((String) s);
                    default:
                        return s -> UdfGenerators.mapStringUDF(UdfComplexity, dataQuantaScale).apply((String) s);
                }
            case "List":
                switch (operator) {
                    case "map":
                        return list -> UdfGenerators.MapListIntUDF(UdfComplexity, dataQuantaScale).apply((List<Integer>) list);
                    case "flatmap":
                        return list -> UdfGenerators.flatMapListIntUDF(UdfComplexity, dataQuantaScale).apply((List<Integer>) list);
                    default:
                        return list -> UdfGenerators.MapListIntUDF(UdfComplexity, dataQuantaScale).apply((List<Integer>) list);
                }
            default:
                return i -> new RheemException("Unsupported data type!");
        }
    }

    /**
     * Generate UDFs
     * @param UdfComplexity
     * @param dataQuantaScale
     * @param type
     * @return
     */
    public static PredicateDescriptor.SerializablePredicate generatefilterUDF(int UdfComplexity, int dataQuantaScale, DataSetType type, String operator) {
        switch (type.getDataUnitType().getTypeClass().getSimpleName()) {
            case "Integer":
                return UdfGenerators.filterIntUDF(UdfComplexity, dataQuantaScale);
            case "String":
                return UdfGenerators.filterStringUDF(UdfComplexity, dataQuantaScale);
            case "List":
                return UdfGenerators.filterIntListUDF(UdfComplexity, dataQuantaScale);
            default:
                return UdfGenerators.filterIntUDF(UdfComplexity, dataQuantaScale);
        }
    }

    /**
     * GEnerate Binary UDFs
     * @param type
     * @return
     */
public  static FunctionDescriptor.SerializableBinaryOperator generateBinaryUDF(int UdfComplexity, int dataQuantaScale, DataSetType type){
    switch (type.getDataUnitType().getTypeClass().getSimpleName()) {
        case "Integer":
            return (n,m) -> (Integer) n + (Integer) m;
        case "String":
            return (n,m) -> (String) n + (String) m;
        case "List":
            return (n,m) -> (List)n;
        default:
            return (n,m) -> (Integer) n + (Integer) m;
    }
}

    public interface Generator<T> extends Supplier<T>, Serializable {
        Random rand = new Random();

        public default void setRandom(Random random){
            //rand = random;
        }
    }

    public static Generator<List<Integer>> createReservoirBasedIntegerListSupplier(ArrayList<List<Integer>> reservoir,
                                                                                   double reuseProbability,
                                                                                   Random random, int dataQuantaSize) {
        return () -> {
            if (random.nextDouble() > reuseProbability || reservoir.isEmpty()) {
                final List<Integer> randomIntegerList = new ArrayList<>();
                for(int i=0;i<dataQuantaSize;i++)
                    randomIntegerList.add(random.nextInt());
                reservoir.add(randomIntegerList);
                return randomIntegerList;
            } else {
                return reservoir.get(random.nextInt(reservoir.size()));
            }
        };
    }

}
