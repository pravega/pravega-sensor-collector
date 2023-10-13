package io.pravega.sensor.collector.util;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;

public class FileUtils {

    final static String separator = ",";

    /**
     * @return list of file name and file size in bytes
     */
    static public List<FileNameWithOffset> getDirectoryListing(String fileSpec, String fileExtension) throws IOException {
        String[] directories= fileSpec.split(separator);
        List<FileNameWithOffset> directoryListing = new ArrayList<>();
        for (String directory : directories) {
            final Path pathSpec = Paths.get(directory);
            getDirectoryFiles(pathSpec, fileExtension, directoryListing);   
        }
        return directoryListing;
    }

    /**
     * @return get all files in directory(including subdirectories) and their respective file size in bytes
     */
    static protected void getDirectoryFiles(Path dirPath, String fileExtension, List<FileNameWithOffset> directoryListing) throws IOException{        
        try(DirectoryStream<Path> dirStream=Files.newDirectoryStream(dirPath)){
            for(Path path: dirStream){
                if(Files.isDirectory(path))         
                    getDirectoryFiles(path, fileExtension, directoryListing);
                else {
                    FileNameWithOffset fileEntry = new FileNameWithOffset(path.toAbsolutePath().toString(), path.toFile().length());
                    // If extension is null, ingest all files 
                    if(fileExtension.isEmpty() || fileExtension.equals(fileEntry.fileName.substring(fileEntry.fileName.lastIndexOf(".")+1)))
                        directoryListing.add(fileEntry);            
                }
            }
        }
        return;
    }
    
}
