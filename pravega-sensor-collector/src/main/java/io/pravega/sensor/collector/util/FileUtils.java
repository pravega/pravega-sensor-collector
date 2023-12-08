package io.pravega.sensor.collector.util;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
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
    static public List<FileNameWithOffset> getDirectoryListing(String fileSpec, String fileExtension, String movedFilesDirectory, long minTimeInMillisToUpdateFile) throws IOException {
        String[] directories= fileSpec.split(separator);
        List<FileNameWithOffset> directoryListing = new ArrayList<>();
        for (String directory : directories) {
            final Path pathSpec = Paths.get(directory);
            if (!Files.isDirectory(pathSpec.toAbsolutePath())) {
                log.error("getDirectoryListing: Directory does not exist or spec is not valid : {}", pathSpec.toAbsolutePath());
                throw new IOException("Directory does not exist or spec is not valid");
            }
            getDirectoryFiles(pathSpec, fileExtension, directoryListing, movedFilesDirectory, minTimeInMillisToUpdateFile);
        }
        return directoryListing;
    }

    /**
     * @return get all files in directory(including subdirectories) and their respective file size in bytes
     */
    static protected void getDirectoryFiles(Path pathSpec, String fileExtension, List<FileNameWithOffset> directoryListing, String movedFilesDirectory, long minTimeInMillisToUpdateFile) throws IOException{
        DirectoryStream.Filter<Path> lastModifiedTimeFilter = getLastModifiedTimeFilter(minTimeInMillisToUpdateFile);
        try(DirectoryStream<Path> dirStream=Files.newDirectoryStream(pathSpec, lastModifiedTimeFilter)){
            for(Path path: dirStream){
                if(Files.isDirectory(path))         //traverse subdirectories
                    getDirectoryFiles(path, fileExtension, directoryListing, movedFilesDirectory, minTimeInMillisToUpdateFile);
                else {
                    FileNameWithOffset fileEntry = new FileNameWithOffset(path.toAbsolutePath().toString(), path.toFile().length());                    
                    if(isValidFile(fileEntry, fileExtension))
                        directoryListing.add(fileEntry);    
                    else                            //move failed file to different folder
                        moveFailedFile(fileEntry, movedFilesDirectory);
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

    /**
     * The last modified time filer for files older than #{timeBefore} milliseconds from current timestamp.
     * This filter helps to eliminate the files that are partially written in to lookup directory by external services.
     */
    private static DirectoryStream.Filter<Path> getLastModifiedTimeFilter(long minTimeInMillisToUpdateFile) {
        log.info("getLastModifiedTimeFilter: minTimeInMillisToUpdateFile: {}", minTimeInMillisToUpdateFile);
        return new DirectoryStream.Filter<Path> () {
            public boolean accept(Path entry) throws IOException {
                BasicFileAttributes attr = Files.readAttributes(entry, BasicFileAttributes.class);
                if(attr.isDirectory()) {
                    return true;
                }
                FileTime fileTime = attr.lastModifiedTime();
                return (fileTime.toMillis() <= (System.currentTimeMillis() - minTimeInMillisToUpdateFile));
            }
        };
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

    static void moveFailedFile(FileNameWithOffset fileEntry, String filesDirectory) throws IOException {
        Path sourcePath = Paths.get(fileEntry.fileName);
        Path targetPath = Paths.get(filesDirectory).resolve("Failed_Files").resolve(sourcePath.getFileName());
        moveFile(sourcePath, targetPath);
    }

    public static void moveCompletedFile(FileNameWithOffset fileEntry, String filesDirectory) throws IOException {
        Path sourcePath = Paths.get(fileEntry.fileName);
        Path completedFilesPath = Paths.get(filesDirectory).resolve("Completed_Files");
        String completedFileName = FileUtils.createCompletedFileName(filesDirectory, fileEntry.fileName);
        Path targetPath = completedFilesPath.resolve(completedFileName);
        moveFile(sourcePath, targetPath);
    }

    public static String createCompletedFileName(String completedFilesDir, String fileName) {
        if(fileName==null || fileName.isEmpty() || completedFilesDir==null || completedFilesDir.isEmpty()) {
            return fileName;
        }

        int validFileNameLength = 255 - completedFilesDir.length();

        if(fileName.length() > validFileNameLength) {
            fileName = fileName.substring(fileName.indexOf(File.separator, fileName.length() - validFileNameLength-1));
        }
        return fileName.replace(File.separator,"_");
    }

    /*
    Move failed files to different directory
     */
    static void moveFile(Path sourcePath, Path targetPath) throws IOException {
        Files.createDirectories(targetPath.getParent());
        //Obtain a lock on file before moving
        try(FileChannel channel = FileChannel.open(sourcePath, StandardOpenOption.WRITE)) {
            try(FileLock lock = channel.tryLock()) {
                if(lock!=null){
                    Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    log.info("movedFile: Moved file from {} to {}", sourcePath, targetPath);
                    lock.release();
                }
                else{
                    log.warn("Unable to obtain lock on file {} for moving. File is locked by another process.", sourcePath);
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
