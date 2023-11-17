package io.pravega.sensor.collector.file;

import io.pravega.sensor.collector.file.csvfile.CsvFileSequenceProcessor;
import io.pravega.sensor.collector.file.parquet.ParquetFileProcessor;
import io.pravega.sensor.collector.file.rawfile.RawFileProcessor;
import io.pravega.sensor.collector.util.EventWriter;
import io.pravega.sensor.collector.util.TransactionCoordinator;
import io.pravega.sensor.collector.util.TransactionStateSQLiteImpl;

/*
 *
 */
public class FileProcessorFactory {

    public static FileProcessor createFileSequenceProcessor(final FileConfig config, TransactionStateSQLiteImpl state,
                                                                    EventWriter<byte[]> writer,
                                                                    TransactionCoordinator transactionCoordinator,
                                                                    String writerId){

        final String className = config.fileType.substring(config.fileType.lastIndexOf(".")+1);

            switch(className){
                case "ParquetFileIngestService":
                    return new ParquetFileProcessor(config, state, writer, transactionCoordinator, writerId);

                case "CsvFileIngestService":
                    return new CsvFileSequenceProcessor(config, state, writer, transactionCoordinator, writerId);

                case "RawFileIngestService":
                    return new RawFileProcessor(config, state, writer, transactionCoordinator, writerId);

                default :
                    throw new RuntimeException("Unsupported className "+ "test");
            }
        /*try{
            // Class<?> clazz = Class.forName(config.fileType);
            //className = clazz.getSimpleName();
            // System.out.println(" className  is "+className);

        }catch (ClassNotFoundException ex){
            throw new RuntimeException(ex);
        }*/



    }
}