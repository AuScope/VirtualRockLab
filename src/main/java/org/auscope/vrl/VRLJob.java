package org.auscope.vrl;

import org.auscope.gridtools.GridJob;

/**
 * Simple class that stores information about a VRL job submitted to the grid
 * by a user.
 * 
 * @author Cihan Altinay
 */
public class VRLJob extends GridJob {
    /** A description of this job */
    private String   description;
    /** The number of bonds */
    private Integer  numBonds;
    /** The number of particles */
    private Integer  numParticles;
    /** The number of timesteps */
    private Integer  numTimesteps;
    /** Directory containing output files */
    private String   outputDir;
    /** A unique job reference (e.g. EPR). */
    private String   reference;
    /** The script filename */
    private String   scriptFile;
    /** The job status */
    private String   status;
    /** The submission date and time */
    private String   submitDate;
    /** The user owning this job */
    private String   user;
    

    /**
     * Does some <em>very</em> basic setting up of the class variables. Just
     * to prevent errors. The strings are initialized to the empty String, and 
     * the arrays are initialized to a length of zero. 
     */
    public VRLJob() {
        description = outputDir = reference = scriptFile = status =
            submitDate = user = "";
        numBonds = numParticles = numTimesteps = 0;
    }

    /**
     * Alternate constructor - calls super class' constructor with the same
     * arguments.
     *
     * @param site              The site the job will be run at
     * @param name              A descriptive name for this job
     * @param code              Executable/code to be run
     * @param version           Version of code to use
     * @param arguments         Arguments for the code
     * @param queue             Which queue to use
     * @param jobType           The type of job to run
     * @param maxWallTime       Amount of time we plan to use
     * @param maxMemory         Amount of memory we plan to use
     * @param cpuCount          Number of CPUs to use (if jobType is single)
     * @param inTransfers       Files to be transferred in
     * @param outTransfers      Files to be transferred out
     * @param stdInput          The std input file for the job
     * @param stdOutput         The std output file for the job
     * @param stdError          The std error file for the job
     */
    public VRLJob(String site, String name, String code, String version,
                  String[] arguments, String queue, String jobType,
                  String maxWallTime, String maxMemory, Integer cpuCount,
                  String[] inTransfers, String[] outTransfers,
                  String stdInput, String stdOutput, String stdError)
    {
        super(site, name, code, version, arguments, queue, jobType, maxWallTime,
                maxMemory, cpuCount, inTransfers, outTransfers, stdInput,
                stdOutput, stdError);
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
        assert (description != null);
        this.description = description;
    }

    /**
     * Returns the user owning this job.
     * 
     * @return The user owning this job.
     */
    public String getUser() {
        return user;
    }

    /**
     * Sets the user owning this job.
     * 
     * @param user The user owning this job.
     */
    public void setUser(String user) {
        assert (user != null);
        this.user = user;
    }

    /**
     * Returns the number of bonds in this simulation job.
     * 
     * @return The number of bonds in this simulation job.
     */
    public Integer getNumBonds() {
        return numBonds;
    }

    /**
     * Sets the number of bonds in this simulation job.
     * 
     * @param name The number of bonds in this simulation job.
     */
    public void setNumBonds(Integer numBonds) {
        assert (numBonds != null);
        this.numBonds = numBonds;
    }

    /**
     * Returns the number of particles in this simulation job.
     * 
     * @return The number of particles in this simulation job.
     */
    public Integer getNumParticles() {
        return numParticles;
    }

    /**
     * Sets the number of particles in this simulation job.
     * 
     * @param name The number of particles in this simulation job.
     */
    public void setNumParticles(Integer numParticles) {
        assert (numParticles != null);
        this.numParticles = numParticles;
    }

    /**
     * Returns the number of timesteps in this simulation job.
     * 
     * @return The number of timesteps in this simulation job.
     */
    public Integer getNumTimesteps() {
        return numTimesteps;
    }

    /**
     * Sets the number of timesteps in this simulation job.
     * 
     * @param name The number of timesteps in this simulation job.
     */
    public void setNumTimesteps(Integer numTimesteps) {
        assert (numTimesteps != null);
        this.numTimesteps = numTimesteps;
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
     * @param name The output directory of this job.
     */
    public void setOutputDir(String outputDir) {
        assert (outputDir != null);
        this.outputDir = outputDir;
    }

    /**
     * Returns the unique reference of this job.
     * 
     * @return The reference of this job.
     */
    public String getReference() {
        return reference;
    }

    /**
     * Sets the unique reference of this job.
     * 
     * @param reference The unique reference of this job.
     */
    public void setReference(String reference) {
        assert (reference != null);
        this.reference = reference;
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
        assert (scriptFile != null);
        this.scriptFile = scriptFile;
    }

    /**
     * Returns the submit date of this job.
     * 
     * @return The submit date of this job.
     */
    public String getSubmitDate() {
        return submitDate;
    }

    /**
     * Sets the submit date of this job.
     * 
     * @param submitDate The submit date of this job.
     */
    public void setSubmitDate(String submitDate) {
        assert (submitDate != null);
        this.submitDate = submitDate;
    }

    /**
     * Returns the status of this job.
     * 
     * @return The status of this job.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the status of this job.
     * 
     * @param status The status of this job.
     */
    public void setStatus(String status) {
        assert (status != null);
        this.status = status;
    }

    /**
     * Returns a String representing the state of this <code>UserJob</code>
     * object.
     * 
     * @return A summary of the values of this object's fields
     */
    public String toString() {
        return super.toString() +
               ",user=\"" + user + "\"" +
               ",description=\"" + description + "\"" +
               ",numBonds=" + numBonds +
               ",numParticles=" + numParticles +
               ",numTimesteps=" + numTimesteps +
               ",outputDir=\"" + outputDir + "\"" +
               ",reference=\"" + reference + "\"" +
               ",scriptFile=\"" + scriptFile + "\"" +
               ",submitDate=\"" + submitDate + "\"" +
               ",status=\"" + status + "\"";
    }
}

