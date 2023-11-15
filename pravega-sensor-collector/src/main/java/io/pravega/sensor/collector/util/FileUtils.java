package io.pravega.sensor.collector.util;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtils {

    private static final Logger log = LoggerFactory.getLogger(FileUtils.class);
    final static String separator = ",";

    /**
     * @return list of file name and file size in bytes
     * Handle the below cases
     *  1. If given file path does not exist then log the message and continue
     *  2. If directory does not exist and no file with given extn like .csv then log the message and continue
     *  3. check for empty file, log the message and continue with valid files
     *
     */
    static public List<FileNameWithOffset> getDirectoryListing(String fileSpec, String fileExtension, TransactionStateSQLiteImpl state) throws IOException {
        String[] directories= fileSpec.split(separator);
        List<FileNameWithOffset> directoryListing = new ArrayList<>();
        for (String directory : directories) {
            final Path pathSpec = Paths.get(directory);
            if (!Files.isDirectory(pathSpec.toAbsolutePath())) {
                log.error("getDirectoryListing: Directory does not exist or spec is not valid : {}", pathSpec.toAbsolutePath());
                throw new IOException("Directory does not exist or spec is not valid");
            }
            getDirectoryFiles(pathSpec, fileExtension, directoryListing, state);        
        }
        return directoryListing;
    }

    /**
     * @return get all files in directory(including subdirectories) and their respective file size in bytes
     */
    static protected void getDirectoryFiles(Path pathSpec, String fileExtension, List<FileNameWithOffset> directoryListing, TransactionStateSQLiteImpl state) throws IOException{        
        try(DirectoryStream<Path> dirStream=Files.newDirectoryStream(pathSpec)){
            for(Path path: dirStream){
                if(Files.isDirectory(path))         //traverse subdirectories
                    getDirectoryFiles(path, fileExtension, directoryListing, state);
                else {
                    FileNameWithOffset fileEntry = new FileNameWithOffset(path.toAbsolutePath().toString(), path.toFile().length());
                    if(isValidFile(fileEntry, fileExtension, state))
                        directoryListing.add(fileEntry);        
                }
            }
        } catch(Exception ex){
            if(ex instanceof IOException){
                log.error("getDirectoryListing: Directory does not exist or spec is not valid : {}", pathSpec.toAbsolutePath());
                throw new IOException("Directory does not exist or spec is not valid");
            } else{
                log.error("getDirectoryListing: Exception while listing files: {}", pathSpec.toAbsolutePath());
                throw new IOException(ex);
            }
        }
        return;
    }

    /*
    Check for below file validation
        1. Is File empty
        2. If extension is null or extension is valid ingest all file
     */
    public static boolean isValidFile(FileNameWithOffset fileEntry, String fileExtension, TransactionStateSQLiteImpl state){

        if(fileEntry.offset<=0){
            log.warn("isValidFile: Empty file {} can not be processed",fileEntry.fileName);
        }
        // If extension is null, ingest all files
        else if(fileExtension.isEmpty() || fileExtension.equals(fileEntry.fileName.substring(fileEntry.fileName.lastIndexOf(".")+1)))
            return true;
        else
            log.warn("isValidFile: File format {} is not supported ", fileEntry.fileName);
        
        try {
            state.addFailedFileRecord(fileEntry.fileName, fileEntry.offset);
        } catch (SQLException e) {
            log.error("Error adding failed file record for file {}: {}", fileEntry.fileName, e.getMessage());
        }
        return false;
    }
    
}
