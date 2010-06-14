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
public class RevisionLog {

    private long revision;
    private Date date;
    private String message;

    /**
     * 
     */
    public RevisionLog(long revision, Date date, String message) {
        this.revision = revision;
        this.date = date;
        this.message = message;
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
    public Date getDate() {
        return date;
    }

    /**
     * 
     */
    public String getMessage() {
        return message;
    }

}
