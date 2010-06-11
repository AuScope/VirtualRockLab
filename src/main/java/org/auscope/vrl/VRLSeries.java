/*
 * This file is part of the AuScope Virtual Rock Lab (VRL) project.
 * Copyright (c) 2010 The University of Queensland, ESSCC
 *
 * Licensed under the terms of the GNU Lesser General Public License.
 */
package org.auscope.vrl;

import java.io.Serializable;

/**
 * Simple class that stores information about a job series consisting of
 * one or more jobs.
 *
 * @author Cihan Altinay
 */
public class VRLSeries implements Serializable {
    /** A unique identifier for this series */
    private Long id;
    /** The user owning this series */
    private String  user;
    /** A short name for this series */
    private String  name;
    /** A description of this series */
    private String  description;

    /**
     * Default constructor.
     */
    public VRLSeries() {
        user = name = description = "";
    }

    /**
     * Returns the unique identifier of this series.
     *
     * @return The unique ID of this series.
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the unique identifier of this series.
     *
     * @param id The new ID for this series.
     */
    private void setId(Long id) {
        assert (id != null);
        this.id = id;
    }

    /**
     * Returns the description of this series.
     *
     * @return The description of this series.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of this series.
     *
     * @param description The description of this series.
     */
    public void setDescription(String description) {
        assert (description != null);
        this.description = description;
    }

    /**
     * Returns the user owning this series.
     *
     * @return The user owning this series.
     */
    public String getUser() {
        return user;
    }

    /**
     * Sets the user owning this series.
     *
     * @param user The user owning this series.
     */
    public void setUser(String user) {
        assert (user != null);
        this.user = user;
    }

    /**
     * Returns the name of this series.
     *
     * @return The name of this series.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this series.
     *
     * @param name The name of this series.
     */
    public void setName(String name) {
        assert (name != null);
        this.name = name;
    }

    /**
     * Returns a String representing the state of this <code>VRLSeries</code>
     * object.
     *
     * @return A summary of the values of this object's fields
     */
    public String toString() {
        return super.toString() +
               ",id=" + id +
               ",user=\"" + user + "\"" +
               ",name=\"" + name + "\"" +
               ",description=\"" + description + "\"";
    }
}

