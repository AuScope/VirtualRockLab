/*
 * This file is part of the AuScope Virtual Rock Lab (VRL) project.
 * Copyright (c) 2010 The University of Queensland, ESSCC
 *
 * Licensed under the terms of the GNU Lesser General Public License.
 */
package org.auscope.vrl;

/**
 * Simple class that stores information about a VRL job.
 *
 * @author Cihan Altinay
 */
public class VRLJob {
    /** The name of the software to run VRL jobs with (as in MDS) */
    public static final String APPLICATION_NAME = "ESyS-Particle";
    /** The binary to execute with a script parameter */
    public static final String BINARY_NAME = "mpipython";
    /** A description for this job */
    private String    description;
    /** The handle to a corresponding Grid job (if applicable) */
    private String    handle;
    /** A unique identifier for this job */
    private Long      id;
    /** Name of this job */
    private String    name;
    /** Directory containing output files */
    private String    outputDir;
    /** The script filename */
    private String    scriptFile;
    /** The identifier of the series this job belongs to */
    private Long      seriesId;

    /**
     * Default empty constructor.
     */
    public VRLJob() {
    }

    /**
     * Constructor with values.
     */
    public VRLJob(String name, String description, String scriptFile,
                  String outputDir, Long seriesId) {
        setName(name);
        setDescription(description);
        setOutputDir(outputDir);
        setScriptFile(scriptFile);
        setSeriesId(seriesId);
        handle = null;
        id = null;
    }

    /**
     * Returns the unique identifier of this job.
     *
     * @return The ID of this job.
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the unique identifier of this job.
     *
     * @param id The unique ID of this job.
     */
    private void setId(Long id) {
        this.id = id;
    }

    /**
     * Returns the description of this job.
     *
     * @return The description of this job.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of this job.
     *
     * @param description The description of this job.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the job handle.
     *
     * @return The job handle.
     */
    public String getHandle() {
        return handle;
    }

    /**
     * Sets the job handle.
     *
     * @param handle The job handle.
     */
    public void setHandle(String handle) {
        this.handle = handle;
    }

    /**
     * Returns the name of this job.
     *
     * @return The name of this job.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this job.
     *
     * @param name The name of this job.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the output directory of this job.
     *
     * @return The output directory of this job.
     */
    public String getOutputDir() {
        return outputDir;
    }

    /**
     * Sets the output directory of this job.
     *
     * @param outputDir The output directory of this job.
     */
    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    /**
     * Returns the script filename of this job.
     *
     * @return The script filename of this job.
     */
    public String getScriptFile() {
        return scriptFile;
    }

    /**
     * Sets the script filename of this job.
     *
     * @param scriptFile The script filename.
     */
    public void setScriptFile(String scriptFile) {
        this.scriptFile = scriptFile;
    }

    /**
     * Returns the identifier of the series this job belongs to.
     *
     * @return The series ID of this job.
     */
    public Long getSeriesId() {
        return seriesId;
    }

    /**
     * Sets the ID of the series this job belongs to.
     *
     * @param seriesId The series ID of this job.
     */
    public void setSeriesId(Long seriesId) {
        this.seriesId = seriesId;
    }

    /**
     * Returns a String representing the state of this <code>VRLJob</code>
     * object.
     *
     * @return A summary of the values of this object's fields
     */
    public String toString() {
        return "id=" + id +
               ", seriesId=" + seriesId +
               ", name=\"" + name + "\"" +
               ", description=\"" + description + "\"" +
               ", handle=\"" + handle + "\"" +
               ", outputDir=\"" + outputDir + "\"" +
               ", scriptFile=\"" + scriptFile + "\"";
    }
}

