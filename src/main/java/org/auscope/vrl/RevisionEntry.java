/*
 * This file is part of the AuScope Virtual Rock Lab (VRL) project.
 * Copyright (c) 2010 The University of Queensland, ESSCC
 *
 * Licensed under the terms of the GNU Lesser General Public License.
 */
package org.auscope.vrl;

import java.util.Date;

/**
 * 
 */
public class RevisionEntry {

    private Date   date;
    private String path;
    private long   revision;
    private long   size;

    /**
     * 
     */
    public RevisionEntry(String path, long revision, Date date, long size) {
        this.date = date;
        this.path = path;
        this.revision = revision;
        this.size = size;
    }

    /**
     * 
     */
    public Date getDate() {
        return date;
    }

    /**
     * 
     */
    public String getPath() {
        return path;
    }

    /**
     * 
     */
    public long getRevision() {
        return revision;
    }

    /**
     * 
     */
    public long getSize() {
        return size;
    }

}
