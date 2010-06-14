/*
 * This file is part of the AuScope Virtual Rock Lab (VRL) project.
 * Copyright (c) 2010 The University of Queensland, ESSCC
 *
 * Licensed under the terms of the GNU Lesser General Public License.
 */
package org.auscope.vrl;

import java.io.Serializable;

/**
 * Simple bean class that stores information about a file.
 *
 * @author Cihan Altinay
 */
public class FileInformation implements Serializable {
    /** The filename */
    private String name;
    /** The file size in bytes */
    private long size;
    /** A file status if applicable (eg "M" for modified) */
    private String state;

    /**
     * Constructor with name, size and state
     */
    public FileInformation(String name, long size, String state) {
        this.name = name;
        this.size = size;
        this.state = state;
    }

    /**
     * Returns the filename.
     *
     * @return The filename.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the filename.
     *
     * @param name The filename.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the file size.
     *
     * @return The file size in bytes.
     */
    public long getSize() {
        return size;
    }

    /**
     * Sets the file size in bytes.
     *
     * @param size The file size.
     */
    public void setSize(long size) {
        this.size = size;
    }

    /**
     * Returns the file status.
     *
     * @return The file status string.
     */
    public String getState() {
        return state;
    }

    /**
     * Sets the file status string.
     *
     * @param state The file status.
     */
    public void setState(String state) {
        this.state = state;
    }
}

