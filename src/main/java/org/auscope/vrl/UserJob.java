package org.auscope.vrl;

import java.io.Serializable;

/**
 * Simple class that stores information about a Grid job submitted by a user.
 * 
 * @author Cihan Altinay
 */
public class UserJob implements Serializable
{
    /** The user ID */
    private String   userId;
    /** The job name */
    private String   name;
    /** A unique job reference (e.g. EPR). */
    private String   reference;
    /** The submission timestamp */
    private String   timeStamp;
    /** The job status */
    private String   status;
    

    /**
     * Does some <em>very</em> basic setting up of the class variables. Just
     * to prevent errors. The strings are initialized to the empty String, and 
     * the arrays are initialized to a length of zero. 
     */
    public UserJob()
    {
        userId = name = reference = timeStamp = status = ""; 
    }

    /**
     * Alternate constructor 1 - allows for construction by the View. It will
     * construct all the elements that the View knows about. The rest of them
     * can be set by the Control using the available mutators.
     *
     * @param userId            The ID of the user who owns this job
     * @param name              The name of this job
     * @param reference         The reference for this job
     * @param timeStamp         The submission timestamp of this job
     * @param status            The status of this job
     */
    public UserJob(String userId, String name, String reference,
            String timeStamp, String status)
    {
        this.userId = userId;
        this.name = name;
        this.reference = reference;
        this.timeStamp = timeStamp;
        this.status = status;
    }

    /**
     * Returns the ID of the user owning this job.
     * 
     * @return The user ID for this job.
     */
    public String getUserId()
    {
        return userId;
    }

    /**
     * Sets the ID of the user owning this job.
     * 
     * @param userId The user ID for this job.
     */
    public void setUserId(String userId)
    {
        assert (userId != null);
        this.userId = userId;
    }

    /**
     * Returns the name of this job.
     * 
     * @return The name of this job.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Sets the name of this job.
     * 
     * @param name The name of this job.
     */
    public void setName(String name)
    {
        assert (name != null);
        this.name = name;
    }

    /**
     * Returns the unique reference of this job.
     * 
     * @return The reference of this job.
     */
    public String getReference()
    {
        return reference;
    }

    /**
     * Sets the unique reference of this job.
     * 
     * @param reference The unique reference of this job.
     */
    public void setReference(String reference)
    {
        assert (reference != null);
        this.reference = reference;
    }

    /**
     * Returns the timestamp of this job.
     * 
     * @return The timestamp of this job.
     */
    public String getTimeStamp()
    {
        return timeStamp;
    }

    /**
     * Sets the timestamp of this job.
     * 
     * @param timeStamp The timestamp of this job.
     */
    public void setTimeStamp(String timeStamp)
    {
        assert (timeStamp != null);
        this.timeStamp = timeStamp;
    }

    /**
     * Returns the status of this job.
     * 
     * @return The status of this job.
     */
    public String getStatus()
    {
        return status;
    }

    /**
     * Sets the status of this job.
     * 
     * @param status The status of this job.
     */
    public void setStatus(String status)
    {
        assert (status != null);
        this.status = status;
    }


    /**
     * Returns a String representing the state of this <code>UserJob</code>
     * object.
     * 
     * @return A summary of the values of this object's fields
     */
    public String toString()
    {
        return "userId=\"" + userId +
               ",name=\"" + name +
               ",reference=\"" + reference +
               "\",timeStamp=\"" + timeStamp +
               "\",status=\"" + status + "\"";
    }
}

