/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.parquet;

import java.util.Objects;

public class FileNameWithOffset implements Comparable<FileNameWithOffset> {
    public final String fileName;
    /**
     * In some contexts, this is the size of the file.
     * In the future, this will represent the offset in the file for incrementally ingesting growing log files.
     * This is partially implemented today.
     * TODO: Clarify usage of offset.
     */
    public final long offset;

    public FileNameWithOffset(String fileName, long offset) {
        this.fileName = fileName;
        this.offset = offset;
    }

    @Override
    public String toString() {
        return "FileNameWithOffset{" +
                "fileName='" + fileName + '\'' +
                ", offset=" + offset +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileNameWithOffset that = (FileNameWithOffset) o;
        return offset == that.offset &&
                Objects.equals(fileName, that.fileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName, offset);
    }

    @Override
    public int compareTo(FileNameWithOffset o) {
        return this.fileName.compareTo(o.fileName);
    }
}
