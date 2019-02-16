package org.qcri.rheem.profiler.java;

import org.qcri.rheem.basic.data.Tuple2;
import org.qcri.rheem.core.api.exception.RheemException;
import org.qcri.rheem.java.operators.JavaTextFileSource;
import org.qcri.rheem.core.optimizer.mloptimizer.api.OperatorProfiler;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * {@link OperatorProfiler} for sources.
 */
public class JavaTextFileSourceProfiler extends JavaSourceProfiler {

    private File tempFile;

    //private final String fileUrl;

    private static List<Tuple2> createdData = new ArrayList<>();

    public JavaTextFileSourceProfiler(Supplier<String> dataQuantumGenerator, String fileUrl) {
        //this.setFileUrl(fileUrl);
        super(() -> new JavaTextFileSource(fileUrl), dataQuantumGenerator);
        //this.fileUrl = fileUrl;
        this.setFileUrl(fileUrl);
    }

    @Override
    public void setUpSourceData(long cardinality) throws Exception {
/*
        if (this.tempFile != null) {
            if (!this.tempFile.delete()) {
                this.logger.warn("Could not delete {}.", this.tempFile);
            }
        }
        this.tempFile = File.createTempFile("rheem-java", "txt");
        URI uri = URI.create(this.fileUrl);
        File inputFile = new File(uri.getRawPath());


        // Create a file if does not exist
        if (!inputFile.exists()) {
            inputFile.createNewFile();
        }
        FileOutputStream fooStream = new FileOutputStream(this.tempFile, false);

        // Create input data.
        BufferedWriter writer1 = new BufferedWriter(new FileWriter(this.tempFile));
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(inputFile))) {
            final Supplier<?> supplier = this.dataQuantumGenerators.get(0);
            for (int i = 0; i < cardinality; i++) {
                writer1.write(supplier.get().toString());
                writer1.write('\n');
                writer.write(supplier.get().toString());
                writer.write('\n');
            }
        }
        */

        // write
        try {
            File file = new File(this.getFileUrl());

            // Try to delete existing file
            try{
                file.delete();
            } catch (Exception e){
                this.logger.error(String.format("Deleting %s failed.", this.getFileUrl()), e);
            }

            final File parentFile = file.getParentFile();
            if (!parentFile.exists() && !file.getParentFile().mkdirs()) {
                throw new RheemException("Could not initialize cardinality repository.");
            }
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true), "UTF-8"));

            Supplier<?> supplier = this.dataQuantumGenerators.get(0);

            for (long i = 0; i < cardinality; i++) {
                writer.write(supplier.get().toString());
                writer.write('\n');
            }
            writer.close();
            supplier=null;
            writer = null;
        } catch (RheemException e) {
            throw e;
        } catch (Exception e) {
            throw new RheemException(String.format("Cannot write to %s.", this.getFileUrl()), e);
        }
    }

    @Override
    public void clearSourceData() {

    }

    @Override
    protected long provideDiskBytes() {
        return this.tempFile.length();
    }


    @Override
    protected void prepareInput(int inputIndex, long dataQuantaSize, long inputCardinality) {

        assert inputIndex == 0;
        File file = new File(this.getFileUrl()+"-"+dataQuantaSize+"-"+inputCardinality+".txt");

        this.logger.info(String.format("[PROFILING] can read input url: %b \n",file.canRead()));
        //System.out.printf("[PROFILING] can read input url: %b \n",file.canRead());
        Tuple2 newData = new Tuple2(inputCardinality,dataQuantaSize);
        // check if input data is already created
        if(createdData.contains(newData)||file.exists())
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
            throw new RheemException(String.format("Cannot write to %s.", this.getFileUrl()), e);
        }
    }
}
