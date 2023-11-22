package io.pravega.sensor.collector.util;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
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
    static public List<FileNameWithOffset> getDirectoryListing(String fileSpec, String fileExtension, String databaseFileName) throws IOException {
        String[] directories= fileSpec.split(separator);
        List<FileNameWithOffset> directoryListing = new ArrayList<>();
        for (String directory : directories) {
            final Path pathSpec = Paths.get(directory);
            if (!Files.isDirectory(pathSpec.toAbsolutePath())) {
                log.error("getDirectoryListing: Directory does not exist or spec is not valid : {}", pathSpec.toAbsolutePath());
                throw new IOException("Directory does not exist or spec is not valid");
            }
            //Failed files will be moved to a separate folder next to the database file
            String failedFilesDirectory = databaseFileName.substring(0, databaseFileName.lastIndexOf('/'));
            getDirectoryFiles(pathSpec, fileExtension, directoryListing, failedFilesDirectory);        
        }
        return directoryListing;
    }

    /**
     * @return get all files in directory(including subdirectories) and their respective file size in bytes
     */
    static protected void getDirectoryFiles(Path pathSpec, String fileExtension, List<FileNameWithOffset> directoryListing, String failedFilesDirectory) throws IOException{        
        try(DirectoryStream<Path> dirStream=Files.newDirectoryStream(pathSpec)){
            for(Path path: dirStream){
                if(Files.isDirectory(path))         //traverse subdirectories
                    getDirectoryFiles(path, fileExtension, directoryListing, failedFilesDirectory);
                else {
                    FileNameWithOffset fileEntry = new FileNameWithOffset(path.toAbsolutePath().toString(), path.toFile().length());                    
                    if(isValidFile(fileEntry, fileExtension))
                        directoryListing.add(fileEntry);    
                    else                            //move failed file to different folder
                        moveFailedFile(fileEntry, failedFilesDirectory);                
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
    public static boolean isValidFile(FileNameWithOffset fileEntry, String fileExtension) throws Exception{

        if(fileEntry.offset<=0){
            log.warn("isValidFile: Empty file {} can not be processed",fileEntry.fileName);
        }
        // If extension is null, ingest all files
        else if(fileExtension.isEmpty() || fileExtension.equals(fileEntry.fileName.substring(fileEntry.fileName.lastIndexOf(".")+1)))
            return true;
        else
            log.warn("isValidFile: File format {} is not supported ", fileEntry.fileName);

        return false;
    }

    /*
    Move failed files to different directory
     */
    static void moveFailedFile(FileNameWithOffset fileEntry, String failedFilesDirectory) throws IOException {
        Path targetPath = Paths.get(failedFilesDirectory).resolve("Failed_Files");
        Files.createDirectories(targetPath);
        //Obtain a lock on file before moving
        try(FileChannel channel = FileChannel.open(Paths.get(fileEntry.fileName), StandardOpenOption.WRITE)) {
            try(FileLock lock = channel.tryLock()) {
                if(lock!=null){
                    Path failedFile = targetPath.resolve(Paths.get(fileEntry.fileName).getFileName());
                    Files.move(Paths.get(fileEntry.fileName), failedFile, StandardCopyOption.REPLACE_EXISTING);
                    log.info("moveFailedFiles: Moved file to {}", failedFile);
                    lock.release();
                }
                else{
                    log.warn("Unable to obtain lock on file {} for moving. File is locked by another process.", fileEntry.fileName);
                    throw new Exception();
                }
            }                
        } catch (Exception e) {
            log.warn("Unable to move failed file {}", e.getMessage());
            log.warn("Failed file will be moved on the next iteration.");
            // We can continue on this error. Moving will be retried on the next iteration.
        }
    }    
}
