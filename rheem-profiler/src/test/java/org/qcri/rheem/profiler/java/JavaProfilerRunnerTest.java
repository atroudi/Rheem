package org.qcri.rheem.profiler.java;

import org.junit.Test;
import org.qcri.rheem.core.types.DataSetType;
import org.qcri.rheem.core.types.DataUnitType;

/**
 * Test for Java profiler
 */
public class JavaProfilerRunnerTest {

    @Test
    public void testJavaProfiler(){
        String operator = "";
        //String cardinalities = "1,100,1000,10000,100000,1000000,10000000,20000000";
        DataUnitType dataType = DataUnitType.createBasicUnchecked(String.class);
        String cardinalities = "1";
        String UdfComplexity = "1";
        //String UdfComplexity = "1,2,3";
        //String dataQuataSize = "1,10,100,1000,5000,10000";
        String dataQuataSize = "1";

        String dataInputRatio = "100";
        //for(int i=0;i<10;i++){
        //    System.out.println(Integer.toString(i) + Boolean.toString((i & 1) == 0));
        //}
        String[] input = {operator, cardinalities, dataQuataSize, UdfComplexity,dataInputRatio};
        JavaProfilerRunner.main(input);
    }
}
