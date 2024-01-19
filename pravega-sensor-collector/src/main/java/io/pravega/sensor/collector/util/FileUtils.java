/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
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
    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtils.class);
    static final String SEPARATOR = ",";
    public static final String FAILED_FILES = "Failed_Files";
    public static final String COMPLETED_FILES = "Completed_Files";

    /**
     * Get directory list.
     * @param fileSpec
     * @param fileExtension
     * @param movedFilesDirectory
     * @param minTimeInMillisToUpdateFile
     * @throws IOException
     * @return list of file name and file size in bytes
     * Handle the below cases
     *  1. If given file path does not exist then log the message and continue
     *  2. If directory does not exist and no file with given extn like .csv then log the message and continue
     *  3. check for empty file, log the message and continue with valid files
     *
     */
    public static List<FileNameWithOffset> getDirectoryListing(String fileSpec, String fileExtension, Path movedFilesDirectory, long minTimeInMillisToUpdateFile) throws IOException {
        String[] directories = fileSpec.split(SEPARATOR);
        List<FileNameWithOffset> directoryListing = new ArrayList<>();
        for (String directory : directories) {
            final Path pathSpec = Paths.get(directory);
            if (!Files.isDirectory(pathSpec.toAbsolutePath())) {
                LOGGER.error("getDirectoryListing: Directory does not exist or spec is not valid : {}", pathSpec.toAbsolutePath());
                throw new IOException("Directory does not exist or spec is not valid");
            }
            getDirectoryFiles(pathSpec, fileExtension, directoryListing, movedFilesDirectory, minTimeInMillisToUpdateFile);
        }
        return directoryListing;
    }

    /**
     * get all files in directory(including subdirectories) and their respective file size in bytes.
     * @param pathSpec
     * @param fileExtension
     * @param directoryListing
     * @param movedFilesDirectory
     * @param minTimeInMillisToUpdateFile
     * @throws IOException
     */
    protected static void getDirectoryFiles(Path pathSpec, String fileExtension, List<FileNameWithOffset> directoryListing, Path movedFilesDirectory, long minTimeInMillisToUpdateFile) throws IOException {
        DirectoryStream.Filter<Path> lastModifiedTimeFilter = getLastModifiedTimeFilter(minTimeInMillisToUpdateFile);
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(pathSpec, lastModifiedTimeFilter)) {
            for (Path path: dirStream) {
                if (Files.isDirectory(path)) {        //traverse subdirectories
                    getDirectoryFiles(path, fileExtension, directoryListing, movedFilesDirectory, minTimeInMillisToUpdateFile);
                } else {
                    FileNameWithOffset fileEntry = new FileNameWithOffset(path.toAbsolutePath().toString(), path.toFile().length());                    
                    if (isValidFile(fileEntry, fileExtension)) {
                        directoryListing.add(fileEntry);
                    } else {                            //move failed file to different folder
                        moveFailedFile(fileEntry, movedFilesDirectory);
                    }
                }
            }
        } catch (Exception ex) {
            if (ex instanceof IOException) {
                LOGGER.error("getDirectoryListing: Directory does not exist or spec is not valid : {}", pathSpec.toAbsolutePath());
                throw new IOException("Directory does not exist or spec is not valid");
            } else {
                LOGGER.error("getDirectoryListing: Exception while listing files: {}", pathSpec.toAbsolutePath());
                throw new IOException(ex);
            }
        }
    }

    /**
     * The last modified time filter for files older than #{timeBefore} milliseconds from current timestamp.
     * This filter helps to eliminate the files that are partially written in to lookup directory by external services.
     * @param minTimeInMillisToUpdateFile
     * @return last modified time filter.
     */
    private static DirectoryStream.Filter<Path> getLastModifiedTimeFilter(long minTimeInMillisToUpdateFile) {
        LOGGER.debug("getLastModifiedTimeFilter: minTimeInMillisToUpdateFile: {}", minTimeInMillisToUpdateFile);
        return entry -> {
            BasicFileAttributes attr = Files.readAttributes(entry, BasicFileAttributes.class);
            if (attr.isDirectory()) {
                return true;
            }
            FileTime fileTime = attr.lastModifiedTime();
            return (fileTime.toMillis() <= (System.currentTimeMillis() - minTimeInMillisToUpdateFile));
        };
    }

    /*
    Check for below file validation
        1. Is File empty
        2. If extension is null or extension is valid ingest all file
     */
    public static boolean isValidFile(FileNameWithOffset fileEntry, String fileExtension) {

        if (fileEntry.offset <= 0) {
            LOGGER.warn("isValidFile: Empty file {} can not be processed", fileEntry.fileName);
        } // If extension is null, ingest all files
        else if (fileExtension.isEmpty() || fileExtension.equals(fileEntry.fileName.substring(fileEntry.fileName.lastIndexOf(".") + 1))) {
            return true;
        } else {
            LOGGER.warn("isValidFile: File format {} is not supported ", fileEntry.fileName);
        }
        return false;
    }

    static void moveFailedFile(FileNameWithOffset fileEntry, Path filesDirectory) throws IOException {
        Path sourcePath = Paths.get(fileEntry.fileName);
        Path targetPath = filesDirectory.resolve(FAILED_FILES).resolve(sourcePath.getFileName());
        moveFile(sourcePath, targetPath);
    }

    public static void moveCompletedFile(FileNameWithOffset fileEntry, Path filesDirectory) throws IOException {
        Path sourcePath = Paths.get(fileEntry.fileName);
        Path completedFilesPath = filesDirectory.resolve(COMPLETED_FILES);
        String completedFileName = FileUtils.createCompletedFileName(filesDirectory, fileEntry.fileName);
        Path targetPath = completedFilesPath.resolve(completedFileName);
        moveFile(sourcePath, targetPath);
    }

    /**
     * To keep same file name of different directories in completed file directory.
     * Creating completed file name with _ instead of /, so that it includes all subdirectories.
     * If the full file name is greater than 255 characters, it will be truncated to 255 characters.
     * @param completedFilesDir
     * @param fileName
     * @return completed file name.
     */
    public static String createCompletedFileName(Path completedFilesDir, String fileName) {
        if (fileName == null || fileName.isEmpty() || completedFilesDir == null) {
            return fileName;
        }

        int validFileNameLength = 255 - completedFilesDir.toString().length();

        if (fileName.length() > validFileNameLength) {
            fileName = fileName.substring(fileName.indexOf(File.separator, fileName.length() - validFileNameLength - 1));
        }
        return fileName.replace(File.separator, "_");
    }

    /*
    Move failed files to different directory
     */
    static void moveFile(Path sourcePath, Path targetPath) throws IOException {
        Files.createDirectories(targetPath.getParent());
        //Obtain a lock on file before moving
        try (FileChannel channel = FileChannel.open(sourcePath, StandardOpenOption.WRITE)) {
            try (FileLock lock = channel.tryLock()) {
                if (lock != null) {
                    Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.debug("movedFile: Moved file from {} to {}", sourcePath, targetPath);
                    lock.release();
                } else {
                    LOGGER.warn("Unable to obtain lock on file {} for moving. File is locked by another process.", sourcePath);
                    throw new Exception();
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Unable to move failed file {}", e.getMessage());
            LOGGER.warn("Failed file will be moved on the next iteration.");
            // We can continue on this error. Moving will be retried on the next iteration.
        }
    }    
}
