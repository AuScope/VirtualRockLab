package org.auscope.vrl;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.exec.generated.JobDescriptionType;

// The following are for proxy checking
import org.globus.wsrf.utils.FaultHelper;
import org.gridforum.jgss.ExtendedGSSManager;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;

import org.auscope.gridtools.GramJobControl;
import org.auscope.gridtools.GridJob;
import org.auscope.gridtools.RegistryQueryClient;
import org.auscope.gridtools.SiteInfo;


/**
 * Following the MVC pattern, this class acts on events received by the GUI,
 * and calls the methods in the Models (which actually do the work).
 * 
 * @author Ryan Fraser
 * @author Terry Rankine
 * @author Darren Kidd
 */
public class GridAccessController {
    /** The handle to the <code>RegistryQueryClient</code> model. */
    private final static RegistryQueryClient RQC = new RegistryQueryClient();

    /** The logger for this class and subclasses */
    private static Log logger = LogFactory.getLog(
            GridAccessController.class.getName());

    private String gridFtpServer = "gsiftp://ng2.esscc.uq.edu.au:2811";
    private String gridFtpStageInDir = "/tmp/";
    private String gridFtpStageOutDir = "/tmp/";

    public void setLocalGridFtpServer(String gridFtpServer) {
        this.gridFtpServer = gridFtpServer;
    }

    public String getLocalGridFtpServer() {
        return gridFtpServer;
    }

    public void setLocalGridFtpStageInDir(String gridFtpStageInDir) {
        this.gridFtpStageInDir = gridFtpStageInDir;
    }

    public String getLocalGridFtpStageInDir() {
        return gridFtpStageInDir;
    }

    public void setLocalGridFtpStageOutDir(String gridFtpStageOutDir) {
        this.gridFtpStageOutDir = gridFtpStageOutDir;
    }

    public String getLocalGridFtpStageOutDir() {
        return gridFtpStageOutDir;
    }

    /**
     * Submits a job with certain properties. The GUI packages the job 
     * properties into a <code>GridJob</code> object, which we use to get the
     * information we need to submit the job properly.
     * <p>
     * However, at this point, we still don't have all the information required
     * to submit a job. We need to retrieve
     * <ul>
     *   <li>the <em>executable name</em> of the code - this may not be the
     *       same as the name (i.e. 'List' is '<code>ls</code>'),</li>
     *   <li>the job type allowed by that code (i.e. single, MPI),</li>
     *   <li>the name of any modules that need to be loaded for the code 
     *       to work,</li>
     *   <li>and the site's GridFTP server (where the data will be staged to, 
     *       worked on, then staged from).</li>
     * </ul>
     * This method grabs this information, updates the <code>GridJob</code>
     * object, then uses the <code>GramJobControl</code> to construct
     * a job script and submit the job.
     * 
     * @param  job A <code>GridJob</code> object which contains all the 
     *             information required to run a job
     * @return The submitted job's End Point Reference (EPR)
     */
    public String submitJob(GridJob job) {
        String jobSubmitEPR = "";
        try {
            String siteAddr = RQC.getJobManagerAtSite(job.getSite());
            //siteAddr += "/wsrf/services/ManagedJobFactoryService";
            String moduleName = RQC.getModuleNameOfCodeAtSite(job.getSite(), job.getCode(), job.getVersion());
            String exeName = RQC.getExeNameOfCodeAtSite(job.getSite(), job.getCode(), job.getVersion());
            
            String siteEmail = RQC.getSiteContactEmailAtSite(job.getSite());

            job.setModules(new String[] {moduleName});
            job.setExeName(exeName);
            job.setSiteGridFTPServer(RQC.getClusterGridFTPServerAtSite(job.getSite()));
            String jobStr = null;
            GramJobControl gjc = new GramJobControl();

            // Construct the XML Job Script.
            if (job.getJobType().equalsIgnoreCase("single") ||
                job.getJobType().equalsIgnoreCase("mpi"))
            {
                logger.info("Constructing job script...");
                jobStr = gjc.constructJobScript(job);
                logger.info("Submitting job:\n" + jobStr + "\nto " + siteAddr);
                jobSubmitEPR = gjc.submitJob(jobStr, siteAddr);
            } else {
                logger.info("Constructing multijob script...");
                JobDescriptionType[] jobStrMulti = gjc.constructMultiJobScript(job);
                logger.info("Submitting job " + job);

                jobSubmitEPR = gjc.submitMultiJob(jobStrMulti, siteAddr);
            }

            if (jobSubmitEPR == null) {                   
                logger.error("Job did not submit (EPR was null)");

                StringBuffer myBuffer = new StringBuffer();
                myBuffer.append("Please contact - ");
                myBuffer.append(siteEmail);
                myBuffer.append(" or help@grid.apac.edu.au\n");
                logger.error(myBuffer);
            } else {
                logger.info("Successfully submitted job script. EPR = " +
                        jobSubmitEPR);
            }
        }
        catch (Exception e) {
            logger.error("Failed to submit job: " + e.getMessage());
        }
        return jobSubmitEPR;
    }
 
    public GridJob getJobByReference(String jobStr) {
        GramJobControl ggj = new GramJobControl();
        return ggj.getJobByReference(jobStr);
    }

    /**
     * Kill one or more jobs.
     * 
     * @param jobStr The array of jobs to kill
     * 
     * @return The condition of each of the jobs (a
     * <code>StateEnumeration</code> String)
     */
    public String[] killJob(String[] jobStr) {
        String condition[] = new String[jobStr.length];
        GramJobControl ggj = new GramJobControl();

        for (int i = 0; i < jobStr.length; i++) {
            condition[i] = ggj.killJob(jobStr[i]);
        }
        return condition;
    }
 
    /**
     * Check the status of one or more jobs.
     * 
     * @param jobStrings The array of jobs to check the status of
     * 
     * @return The status of each of the jobs (a <code>StateEnumeration</code>
     * String)
     */
    public String[] retrieveJobStatus(String[] jobStrings) {
        String status[] = new String[jobStrings.length];
        GramJobControl ggj = new GramJobControl();

        for (int i = 0; i < jobStrings.length; i++) {
            status[i] = ggj.getJobStatus(jobStrings[i]);
        }
        return status;
    }

    /**
     * Get the results from one or more jobs.
     * 
     * @param jobStr The array of jobs to get results from
     * 
     * @return An array containing the results of each job
     */
    public String[] retrieveJobResults(String[] jobStr) {

        String results[] = new String[jobStr.length];
        GramJobControl ggj = new GramJobControl();

        for (int i = 0; i < jobStr.length; i++) {
            results[i] = ggj.getJobResults(jobStr[i]);
        }
        return results;
    }

    /**
     * Get a list of available sites.
     * 
     * @return The list of available sites
     */
    public String[] retrieveAllSitesOnGrid()
    {
        String sites[] = RQC.getAllSitesOnGrid();

        // Order the sites alphabetically.
        Arrays.sort(sites);

        return sites;
    }

    /**
     * Get a list of the codes available at a particular site.
     * 
     * @param site The site to check
     * 
     * @return The list of codes available
     */
    public String[] retrieveAllCodesAtSite(String site)
    {
        return RQC.getAllCodesAtSite(site);
    }

    /**
     * Get the module name for a particular code.
     * 
     * @param code The code to determine the module name for
     * @param site The site that code is being selected from
     * @return The module name
     */
    public String retrieveModuleNameForCode(String code, String site, String version)
    {
        return RQC.getModuleNameOfCodeAtSite(site, code, version);
    }

    /**
     * Get a list of sites that have a particular version of a code.
     * 
     * @param code    The code to look for
     * @param version The particular version
     * @return A list of sites that have the version of the code
     */
    public String[] retrieveSitesWithSoftwareAndVersion(String code, String version)
    {
        return RQC.getAllSitesWithAVersionOfACode(code, version);
    }
    
    
    /**
     * Return subcluster with matching requirements. CPUs, Mem, and Version can 
     * be <code>null</code> string.
     * 
     * @param code    The code to use
     * @param version The version of the code required
     * @param cpus    The number of CPUs required
     * @param mem     The amount of memory required
     * @return The subcluster that satisfies the requirements
     */
    public String[] retrieveSubClusterWithSoftwareAndVersionWithMemAndCPUs(
            String code, String version, String cpus, String mem)
    {
        return RQC.getSubClusterWithSoftwareAndVersionWithMemAndCPUs(code, version, cpus, mem);
    }
    
    
    /**
     * Get the queues that will honour the walltime and subcluster.
     * 
     * @param currSubCluster
     * @param wallTime
     * @return A list of queues
     */
    public String[] retrieveComputingElementForWalltimeAndSubcluster(String currSubCluster, String wallTime)
    {
        return RQC.getComputingElementForWalltimeAndSubcluster(currSubCluster, wallTime);
    }
    
    public String retrieveStorageElementFromComputingElementWithDiskAvailable(String queue, String diskSpace)
    {
        String defaultSE = "";
        String storagePath = "";

        defaultSE = RQC.getStorageElementFromComputingElement(queue);
        storagePath = RQC.getStoragePathWithSpaceAvailFromDefaultStorageElement(defaultSE, diskSpace);
    
        return storagePath;
    }
        

    public String[] retrieveSubClusterWithMemAndCPUsAtSite(String site, String cluster, String cpus, String mem)
    {
        return RQC.getSubClusterWithMemAndCPUsFromClusterFromSite(site, cluster, cpus, mem);
    }
    
    /**
     * Get a list of the versions of a code that is available at a site.
     * 
     * @param site The site to check
     * @param code The code that will be used
     * @return An array of the different versions of 'code' available at 'site'
     */
    public String[] retrieveCodeVersionsAtSite(String site, String code)
    {
        return RQC.getVersionsOfCodeAtSite(site, code);
    }

    /**
     * Get the list of queues available at a given site.
     * 
     * @param site The site that is being checked for queues
     * @return A list of the different queues available
     */
    public String[] retrieveQueueNamesAtSite(String site)
    {
        return RQC.getQueueNamesAtSite(site);
    }

    /**
     * Get all the different codes available on the Grid.
     * 
     * @return A list of all the codes available
     */
    public String[] retrieveAllSiteCodes()
    {
        return RQC.getAllCodesOnGrid();
    }
    
    /**
     * Get a list of all the versions of this code that are available on
     * the grid.
     * 
     * @param code The code to check for versions of
     * @return A list of all the version avalailable
     */
    public String[] retrieveAllVersionsOfCodeOnGrid(String code)
    {
        return RQC.getAllVersionsOfCodeOnGrid(code);
    }
    
    public SiteInfo[] retrieveSiteStatus()
    {
        return RQC.getAllSitesStatus();
    }
    
    /**
     * Get a list of all the GridFTP servers available on the Grid. These are
     * used for data transfer.
     * 
     * @return A list of GridFTP servers
     */
    public String[] retrieveAllGridFtpServersOnGrid()
    {
        return RQC.getAllGridFTPServersOnGrid();
    }

    /**
     * Get a site contact email address for site
     * 
     * @param site The site in question 
     * @return the email address 
     */
    
    public String getSiteContactEmailAtSite(String site)
    {
        return RQC.getSiteContactEmailAtSite(site);
    }   

    /**
     * Check if the user has a valid proxy
     * 
     * @return true/false
     */
    public static boolean validProxy()
    {
        boolean retval = false;
        try
        {
            ExtendedGSSManager manager = (ExtendedGSSManager) ExtendedGSSManager.getInstance();
            GSSCredential cred = manager.createCredential(GSSCredential.INITIATE_AND_ACCEPT);
            // Lifetime check - in seconds - 5 mins.
            if (cred.getRemainingLifetime() > (5*60))
            {
                logger.info("Valid proxy found: " +
                        cred.getRemainingLifetime()/60 + "min, " +
                        cred.getRemainingLifetime()%60 + "sec");
                retval = true;
            } else {
                logger.info("Proxy lifetime too short: should have at least 5 mins, time left in mins");
                logger.info(cred.getRemainingLifetime()/60 + "min, "
                        + cred.getRemainingLifetime()%60 + "sec");
            }
        } catch (GSSException e)
        {
            logger.error(FaultHelper.getMessage(e)); 
        }
        
        return retval;
    }
}

