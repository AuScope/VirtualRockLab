package org.auscope.vrl.web;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.auscope.vrl.FileInformation;
import org.auscope.vrl.GridAccessController;
import org.auscope.vrl.ScriptParser;
import org.auscope.vrl.Util;
import org.auscope.vrl.VRLJob;
import org.auscope.vrl.VRLJobManager;
import org.auscope.vrl.VRLSeries;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

/**
 * Controller for the job submission view.
 */
public class GridSubmitController extends MultiActionController {

    /** Logger for this class */
    private final Log logger = LogFactory.getLog(getClass());

    private GridAccessController gridAccess;
    private VRLJobManager jobManager;

    public void setGridAccess(GridAccessController gridAccess) {
        this.gridAccess = gridAccess;
    }

    public void setJobManager(VRLJobManager jobManager) {
        this.jobManager = jobManager;
    }

    protected ModelAndView handleNoSuchRequestHandlingMethod(
            NoSuchRequestHandlingMethodException ex,
            HttpServletRequest request,
            HttpServletResponse response) {

        // Store resubmission request or scriptBuilder filename in session
        // object to be able to modify VRLJob later accordingly.
        String newScript = (String) request.getParameter("newscript");
        String resubmit = (String) request.getParameter("resubmitJob");
        if (newScript != null) {
            logger.info("Storing script filename in session.");
            request.getSession().setAttribute("newscript", newScript);
        } else if (resubmit != null) {
            logger.info("Storing resubmitJob ID in session.");
            request.getSession().setAttribute("resubmitJob", resubmit);
        }

        logger.info("No/invalid action parameter; returning gridsubmit view.");
        return new ModelAndView("gridsubmit");
    }

    /**
     * Returns a JSON object containing a list of the current user's series.
     * 
     * @param request The servlet request
     * @param response The servlet response
     *
     * @return A JSON object with a series attribute which is an array of
     *         VRLSeries objects.
     */
    public ModelAndView mySeries(HttpServletRequest request,
                                 HttpServletResponse response) {

        String user = request.getRemoteUser();

        logger.info("Querying series of "+user);
        List<VRLSeries> series = jobManager.querySeries(user, null, null);

        logger.info("Returning list of "+series.size()+" series.");
        return new ModelAndView("jsonView", "series", series);
    }

    /**
     * Very simple helper class (bean).
     */
    public class SimpleBean {
        private String value;
        public SimpleBean(String value) { this.value = value; }
        public String getValue() { return value; }
    }

    /**
     * Returns a JSON object containing an array of ESyS-particle sites.
     * 
     * @param request The servlet request
     * @param response The servlet response
     *
     * @return A JSON object with a sites attribute which is an array of
     *         sites on the grid that have an installation of ESyS-particle.
     */
    public ModelAndView listSites(HttpServletRequest request,
                                  HttpServletResponse response) {

        logger.info("Retrieving sites with ESyS-Particle installations.");
        String[] particleSites = gridAccess.
                retrieveSitesWithSoftwareAndVersion(VRLJob.CODE_NAME, "");

        List<SimpleBean> sites = new ArrayList<SimpleBean>();
        for (int i=0; i<particleSites.length; i++) {
            sites.add(new SimpleBean(particleSites[i]));
        }

        logger.info("Returning list of "+particleSites.length+" sites.");
        return new ModelAndView("jsonView", "sites", sites);
    }

    /**
     * Returns a JSON object containing an array of ESyS-particle versions at
     * the specified site.
     * 
     * @param request The servlet request including a site parameter
     * @param response The servlet response
     *
     * @return A JSON object with a versions attribute which is an array of
     *         versions installed at requested site.
     */
    public ModelAndView listSiteVersions(HttpServletRequest request,
                                         HttpServletResponse response) {

        String site = request.getParameter("site");
        List<SimpleBean> versions = new ArrayList<SimpleBean>();

        if (site != null) {
            logger.info("Retrieving ESyS-Particle versions at "+site);

            String[] siteVersions = gridAccess.
                    retrieveCodeVersionsAtSite(site, VRLJob.CODE_NAME);

            for (int i=0; i<siteVersions.length; i++) {
                versions.add(new SimpleBean(siteVersions[i]));
            }
        } else {
            logger.warn("No site specified!");
        }

        logger.info("Returning list of "+versions.size()+" versions.");
        return new ModelAndView("jsonView", "versions", versions);
    }

    /**
     * Returns a JSON object containing a populated VRLJob object.
     * 
     * @param request The servlet request
     * @param response The servlet response
     *
     * @return A JSON object with a data attribute containing a populated
     *         VRLJob object and a success attribute.
     */
    public ModelAndView getJobObject(HttpServletRequest request,
                                     HttpServletResponse response) {

        VRLJob job = prepareModel(request);

        logger.info("Returning job.");
        ModelAndView result = new ModelAndView("jsonView");
        result.addObject("data", job);
        result.addObject("success", true);

        return result;
    }

    /**
     * Processes a file upload request returning a JSON object which indicates
     * whether the upload was successful and contains the filename and file
     * size.
     * 
     * @param request The servlet request
     * @param response The servlet response containing the JSON data
     *
     * @return null
     */
    public ModelAndView uploadFile(HttpServletRequest request,
                                  HttpServletResponse response) {

        String jobInputDir = (String) request.getSession()
            .getAttribute("jobInputDir");

        boolean success = true;
        String error = null;
        FileInformation fileInfo = null;

        if (jobInputDir != null) {
            MultipartHttpServletRequest mfReq =
                (MultipartHttpServletRequest) request;

            MultipartFile f = mfReq.getFile("file");
            if (f == null) {
                logger.error("No file parameter provided.");
                success = false;
                error = new String("Invalid request.");
            } else {
                logger.info("Saving uploaded file "+f.getOriginalFilename());
                File destination = new File(
                        jobInputDir+f.getOriginalFilename());
                if (destination.exists()) {
                    success = false;
                    error = new String("A file by that name already exists.");
                    logger.warn("Tried to upload a file with existing filename.");
                } else {
                    try {
                        f.transferTo(destination);
                    } catch (IOException e) {
                        logger.error("Could not move file: "+e.getMessage());
                        success = false;
                        error = new String("Could not process file.");
                    }
                    fileInfo = new FileInformation(
                            f.getOriginalFilename(), f.getSize());
                }
            }

        } else {
            logger.error("Input directory not found in current session!");
            success = false;
            error = new String("Internal error. Please reload the page.");
        }

        // We cannot use jsonView here since this is a file upload request and
        // ExtJS uses a hidden iframe which receives the response.
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        try {
            PrintWriter pw = response.getWriter();
            pw.print("{success:'"+success+"'");
            if (error != null) {
                pw.print(",error:'"+error+"'");
            }
            if (fileInfo != null) {
                pw.print(",name:'"+fileInfo.getName()+"',size:"+fileInfo.getSize());
            }
            pw.print("}");
            pw.flush();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    /**
     * Processes a job submission request.
     * 
     * @param request The servlet request
     * @param response The servlet response
     *
     * @return A JSON object with a success attribute that indicates whether
     *         the job was successfully submitted.
     */
    public ModelAndView submitJob(HttpServletRequest request,
                                  HttpServletResponse response,
                                  VRLJob job) {

        logger.debug("Job details:\n"+job.toString());
        /*
        java.util.Enumeration eParams = request.getParameterNames();
        while (eParams.hasMoreElements()) {
            String name = (String) eParams.nextElement();
            String value = (String) request.getParameter(name).toString();
            logger.error(name+"  =  "+value);
        }*/

        VRLSeries series = null;
        boolean success = true;
        String jobInputDir = (String) request.getSession()
            .getAttribute("jobInputDir");
        String newSeriesName = request.getParameter("seriesName");
        String seriesIdStr = request.getParameter("seriesId");

        // if seriesName parameter was provided then we create a new series
        // otherwise seriesId contains the id of the series to use.
        if (newSeriesName != null && newSeriesName != "") {
            String newSeriesDesc = request.getParameter("seriesDesc");

            logger.info("Creating new series '"+newSeriesName+"'.");
            series = new VRLSeries();
            series.setUser(request.getRemoteUser());
            series.setName(newSeriesName);
            if (newSeriesDesc != null) {
                series.setDescription(newSeriesDesc);
            }
            jobManager.saveSeries(series);
            // Note that we can now access the series' new ID

        } else if (seriesIdStr != null && seriesIdStr != "") {
            try {
                int seriesId = Integer.parseInt(seriesIdStr);
                series = jobManager.getSeriesById(seriesId);
            } catch (NumberFormatException e) {
                logger.error("Error parsing series ID!");
            }
        }

        if (series == null) {
            success = false;
            logger.error("No valid series found. NOT submitting job!");

        } else {
            job.setSeriesId(series.getId());
            job.setArguments(new String[] { job.getScriptFile() });

            // Add server part to local stage-in dir
            String stageInURL = gridAccess.getLocalGridFtpServer()+jobInputDir;
            job.setInTransfers(new String[] { stageInURL });

            // Create a new directory for the output files of this job
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String dateFmt = sdf.format(new Date());
            String jobID = request.getRemoteUser() + "-" + job.getName() +
                    "-" + dateFmt + File.separator;
            String jobOutputDir = gridAccess.getLocalGridFtpStageOutDir()+jobID;
            success = (new File(jobOutputDir)).mkdir();

            String submitEPR = null;

            if (success) {
                job.setOutputDir(jobOutputDir);
                job.setOutTransfers(new String[]
                        { gridAccess.getLocalGridFtpServer() + jobOutputDir });

                logger.info("Submitting job with name " + job.getName() +
                        " to " + job.getSite());
                // ACTION!
                submitEPR = gridAccess.submitJob(job);
            } else {
                logger.error("Could not create directory "+jobOutputDir);
            }

            if (submitEPR == null) {
                success = false;
            } else {
                logger.info("SUCCESS! EPR: "+submitEPR);
                String status = gridAccess.retrieveJobStatus(submitEPR);
                job.setReference(submitEPR);
                job.setStatus(status);
                job.setSubmitDate(new Date().toString());
                jobManager.saveJob(job);
            }
        }

        ModelAndView result = new ModelAndView("jsonView");
        result.addObject("success", success);

        return result;
    }

    /**
     * Creates a new VRLJob object with predefined values for some fields.
     * If the ScriptBuilder was used the file is moved to the job input
     * directory whereas a resubmission request is handled by using the
     * attributes of the job to be resubmitted.
     * 
     * @param request The servlet request containing a session object
     *
     * @return The new job object.
     */
    private VRLJob prepareModel(HttpServletRequest request) {
        final String user = request.getRemoteUser();
        final String maxWallTime = "2";
        final String maxMemory = "1024";
        final String stdInput = "";
        final String stdOutput = "stdOutput.txt";
        final String stdError = "stdError.txt";
        final String[] arguments = new String[0];
        final String[] inTransfers = new String[0];
        final String[] outTransfers = new String[0];
        String name = "vrljob";
        String site = "ESSCC";
        Integer cpuCount = 2;
        Integer numBonds = 0;
        Integer numParticles = 0;
        Integer numTimesteps = 0;
        String version = "";
        String queue = "";
        String description = "";
        String scriptFile = "";

        // Set a default version and queue
        String[] allVersions = gridAccess.retrieveCodeVersionsAtSite(
                site, VRLJob.CODE_NAME);
        if (allVersions.length > 0)
            version = allVersions[0];
        
        String[] allQueues = gridAccess.retrieveQueueNamesAtSite(site);
        if (allQueues.length > 0)
            queue = allQueues[0];

        // Create a new directory to put all files for this job into.
        // This directory will always be the first stageIn directive.
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String dateFmt = sdf.format(new Date());
        String jobID = user + "-" + dateFmt + File.separator;
        String jobInputDir = gridAccess.getLocalGridFtpStageInDir() + jobID;

        boolean success = (new File(jobInputDir)).mkdir();

        if (!success) {
            logger.error("Could not create directory "+jobInputDir);
            jobInputDir = gridAccess.getLocalGridFtpStageInDir();
        }

        // Save in session to use it when submitting job
        request.getSession().setAttribute("jobInputDir", jobInputDir);

        // Check if the ScriptBuilder was used. If so, there is a file in the
        // system temp directory which needs to be staged in.
        String newScript = (String) request.getSession().
            getAttribute("newscript");
        if (newScript != null) {
            request.getSession().removeAttribute("newscript");
            logger.info("Adding "+newScript+" to stageIn directory");
            File tmpScriptFile = new File(System.getProperty("java.io.tmpdir") +
                    File.separator+newScript+".py");
            File newScriptFile = new File(jobInputDir, tmpScriptFile.getName());
            success = Util.moveFile(tmpScriptFile, newScriptFile);
            if (success) {
                logger.info("Moved "+newScript+" to stageIn directory");
                scriptFile = newScript+".py";

                // Extract information from script file
                ScriptParser parser = new ScriptParser();
                try {
                    parser.parse(newScriptFile);
                    cpuCount = parser.getNumWorkerProcesses()+1;
                    numTimesteps = parser.getNumTimeSteps();
                } catch (IOException e) {
                    logger.warn("Error parsing file: "+e.getMessage());
                }
            } else {
                logger.warn("Could not move "+newScript+" to stageIn!");
            }
        }

        // Check if the user requested to re-submit a previous job.
        String jobIdStr = (String) request.getSession().
            getAttribute("resubmitJob");
        VRLJob existingJob = null;
        if (jobIdStr != null) {
            request.getSession().removeAttribute("resubmitJob");
            logger.info("Request to re-submit a job.");
            try {
                int jobId = Integer.parseInt(jobIdStr);
                existingJob = jobManager.getJobById(jobId);
            } catch (NumberFormatException e) {
                logger.error("Error parsing job ID!");
            }
        }

        if (existingJob != null) {
            logger.info("Using attributes of "+existingJob.getName());
            site = existingJob.getSite();
            version = existingJob.getVersion();
            name = existingJob.getName()+"_resubmit";
            scriptFile = existingJob.getScriptFile();
            description = existingJob.getDescription();
            numBonds = existingJob.getNumBonds();
            numParticles = existingJob.getNumParticles();
            numTimesteps = existingJob.getNumTimesteps();

            allQueues = gridAccess.retrieveQueueNamesAtSite(site);
            if (allQueues.length > 0)
                queue = allQueues[0];

            logger.info("Copying files from old job to stage-in directory");
            File srcDir = new File(existingJob.getOutputDir());
            File destDir = new File(jobInputDir);
            success = Util.copyFilesRecursive(srcDir, destDir);
            if (!success) {
                logger.error("Could not copy all files!");
                // TODO: Let user know this didn't work
            }
        }

        logger.info("Creating new VRLJob instance");
        VRLJob job = new VRLJob(site, name, version, arguments, queue,
                maxWallTime, maxMemory, cpuCount, inTransfers, outTransfers,
                stdInput, stdOutput, stdError);

        job.setScriptFile(scriptFile);
        job.setDescription(description);
        job.setNumBonds(numBonds);
        job.setNumParticles(numParticles);
        job.setNumTimesteps(numTimesteps);

        return job;
    }
}

