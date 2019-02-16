package org.qcri.rheem.profiler.spark;

import org.qcri.rheem.basic.data.Tuple2;
import org.qcri.rheem.core.api.Configuration;
import org.qcri.rheem.core.api.exception.RheemException;
import org.qcri.rheem.core.util.fs.FileSystems;
import org.qcri.rheem.spark.operators.SparkExecutionOperator;
import org.qcri.rheem.spark.operators.SparkTextFileSource;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * {@link SparkOperatorProfiler} for the {@link SparkTextFileSource}.
 */
public class SparkTextFileSourceProfiler extends SparkSourceProfiler {

    private final String fileUrl;

    private static List<Tuple2> createdData = new ArrayList<>();

    public SparkTextFileSourceProfiler(Configuration configuration,
                                       Supplier<?> dataQuantumGenerator) {
        //this(configuration.getStringProperty("rheem.profiler.datagen.url"), configuration, dataQuantumGenerator);
        this(configuration.getStringProperty("rheem.profiler.logs.syntheticDataURL.prefix"),configuration,dataQuantumGenerator);
    }

    private SparkTextFileSourceProfiler(String fileUrl,
                                        Configuration configuration,
                                        Supplier<?> dataQuantumGenerator) {
        super((Supplier<SparkExecutionOperator> & Serializable) () -> {SparkTextFileSource op= new SparkTextFileSource("file:///"+fileUrl); op.setName("SparkTextFileSource"); return op;},
                configuration, dataQuantumGenerator);
        this.fileUrl = fileUrl;
    }

    @Override
    protected void prepareInput(int inputIndex, long dataQuantaSize, long inputCardinality) {

        assert inputIndex == 0;
        File file = new File(this.fileUrl+"-"+dataQuantaSize+"-"+inputCardinality+".txt");

        //System.out.printf("[PROFILING] Input data already exist and read: %b \n",file.canRead());
        this.logger.info(String.format("[PROFILING] can read input url: %b \n",file.canRead()));

        Tuple2 newData = new Tuple2(inputCardinality,dataQuantaSize);
        // check if input data is already created
        if(createdData.contains(newData)||file.exists()||true)
            return;

        // add new data
        createdData.add(newData);
        try {
            final File parentFile = file.getParentFile();
            if (!parentFile.exists() && !file.getParentFile().mkdirs()) {
                throw new RheemException("Could not initialize log repository.");
            }
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false), "UTF-8"));

            final Supplier<?> supplier = this.dataQuantumGenerators.get(0);

            for (long i = 0; i < inputCardinality; i++) {
                String tmp= supplier.get().toString();
                writer.write(tmp);
                writer.write('\n');
            }
            writer.flush();
            writer.close();
        } catch (RheemException e) {
            throw e;
        } catch (Exception e) {
            throw new RheemException(String.format("Cannot write to %s.", this.fileUrl), e);
        }
    }

    @Override
    public void cleanUp() {
        super.cleanUp();

        FileSystems.getFileSystem(this.fileUrl).ifPresent(fs -> {
                    try {
                        fs.delete(this.fileUrl, true);
                    } catch (IOException e) {
                        this.logger.error(String.format("Could not delete profiling file %s.", this.fileUrl), e);
                    }
                }
        );
    }
}
