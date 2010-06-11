/*
 * This file is part of the AuScope Virtual Rock Lab (VRL) project.
 * Copyright (c) 2010 The University of Queensland, ESSCC
 *
 * Licensed under the terms of the GNU Lesser General Public License.
 */
package org.auscope.vrl.web;

import au.org.arcs.jcommons.utils.SubmissionLocationHelpers;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.auscope.vrl.FileInformation;
import org.auscope.vrl.Util;
import org.auscope.vrl.VRLJob;
import org.auscope.vrl.VRLJobManager;
import org.auscope.vrl.VRLSeries;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;
import org.springframework.web.servlet.view.RedirectView;

import org.vpac.grisu.control.ServiceInterface;
import org.vpac.grisu.control.exceptions.JobPropertiesException;
import org.vpac.grisu.frontend.model.job.JobObject;
import org.vpac.grisu.model.GrisuRegistry;
import org.vpac.grisu.model.GrisuRegistryManager;

/**
 * Controller for the job submission view.
 *
 * @author Cihan Altinay
 */
public class GridSubmitController extends MultiActionController {

    /** Logger for this class */
    private final Log logger = LogFactory.getLog(getClass());

    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir")
        + File.separator + "vrl" + File.separator;
    private static final String FQAN = "/ARCS/AuScope";
    private String jobFileArchiveDir = "/home/vrl/repo";
    private VRLJobManager jobManager;

    /**
     * Sets the <code>VRLJobManager</code> to be used to retrieve and store
     * series and job details.
     *
     * @param jobManager the JobManager to use
     */
    public void setJobManager(VRLJobManager jobManager) {
        this.jobManager = jobManager;
    }

    protected ModelAndView handleNoSuchRequestHandlingMethod(
            NoSuchRequestHandlingMethodException ex,
            HttpServletRequest request,
            HttpServletResponse response) {

        // Ensure Grid Service is initialized
        if (getGrisuService(request) != null) {
            logger.debug("No/invalid action parameter; returning gridsubmit view.");
            return new ModelAndView("gridsubmit");
        } else {
            request.getSession().setAttribute(
                    "redirectAfterLogin", "/gridsubmit.html");
            logger.debug("ServiceInterface not initialized. Redirecting to login.");
            return new ModelAndView(
                    new RedirectView("/login.html", true, false, false));
        }
    }

    private ServiceInterface getGrisuService(HttpServletRequest request) {
        return (ServiceInterface)
            request.getSession().getAttribute("grisuService");
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

        logger.debug("Querying series of "+user);
        List<VRLSeries> series = jobManager.querySeries(user, null, null);

        logger.debug("Returning list of "+series.size()+" series.");
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
     * Returns a JSON object containing an array of ESyS-Particle sites.
     *
     * @param request The servlet request
     * @param response The servlet response
     *
     * @return A JSON object with a sites attribute which is an array of
     *         sites on the grid that have an installation of ESyS-Particle.
     */
    public ModelAndView listSites(HttpServletRequest request,
                                  HttpServletResponse response) {

        ServiceInterface si = getGrisuService(request);
        String version = request.getParameter("version");
        List<SimpleBean> sites = new ArrayList<SimpleBean>();
        ModelAndView mav = new ModelAndView("jsonView");
        String errorString = null;

        if (si == null) {
            errorString = "You are not logged in or your session has expired";
            logger.warn("ServiceInterface is null!");
        } else {
            GrisuRegistry registry = GrisuRegistryManager.getDefault(si);
            if (version != null) {
                logger.debug("Retrieving sites with ESyS-Particle " + version
                        + " installations.");
                Set<String> subLocs = registry
                    .getApplicationInformation(VRLJob.APPLICATION_NAME)
                    .getAvailableSubmissionLocationsForVersionAndFqan(
                            version, FQAN);
                Set<String> siteSet = registry.getResourceInformation()
                    .distillSitesFromSubmissionLocations(subLocs);
                Iterator<String> it = siteSet.iterator();
                while (it.hasNext()) {
                    sites.add(new SimpleBean(it.next()));
                }
            } else {
                logger.debug(
                        "Retrieving sites with ESyS-Particle installations.");
                Set<String> siteSet = registry
                    .getUserApplicationInformation(VRLJob.APPLICATION_NAME)
                    .getAllAvailableSitesForUser();
                Iterator<String> it = siteSet.iterator();
                while (it.hasNext()) {
                    sites.add(new SimpleBean(it.next()));
                }
            }
            logger.debug("Returning list of "+sites.size()+" sites.");
        }
        // always need to return this property to allow client to parse
        // the error if any
        mav.addObject("sites", sites);

        if (errorString != null) {
            mav.addObject("error", errorString);
            mav.addObject("success", false);
        } else {
            mav.addObject("success", true);
        }

        return mav;
    }

    /**
     * Returns a JSON object containing an array of job manager queues at
     * the specified site.
     *
     * @param request The servlet request including a site parameter
     * @param response The servlet response
     *
     * @return A JSON object with a queues attribute which is an array of
     *         job queues available at requested site.
     */
    public ModelAndView listSiteQueues(HttpServletRequest request,
                                       HttpServletResponse response) {

        ServiceInterface si = getGrisuService(request);
        String site = request.getParameter("site");
        String version = request.getParameter("version");
        List<SimpleBean> queues = new ArrayList<SimpleBean>();
        ModelAndView mav = new ModelAndView("jsonView");
        String errorString = null;

        if (si == null) {
            errorString = "You are not logged in or your session has expired";
            logger.warn("ServiceInterface is null!");
        } else if (site == null || version == null) {
            errorString = new String("Missing parameter");
            logger.warn("No site or version specified!");
        } else {
            GrisuRegistry registry = GrisuRegistryManager.getDefault(si);

            logger.debug("Retrieving queue names at "+site+" for version "
                    +version);
            Set<String> subLocs = registry
                .getApplicationInformation(VRLJob.APPLICATION_NAME)
                .getAvailableSubmissionLocationsForVersionAndFqan(
                        version, FQAN);
            Iterator<String> it = subLocs.iterator();
            while (it.hasNext()) {
                String sl = it.next();
                if (site.equals(registry.getResourceInformation()
                            .getSite(sl))) {
                    queues.add(new SimpleBean(SubmissionLocationHelpers
                            .extractQueue(sl)));
                }
            }
            logger.debug("Returning list of "+queues.size()+" queue names.");
        }
        // always need to return this property to allow client to parse
        // the error if any
        mav.addObject("queues", queues);

        if (errorString != null) {
            mav.addObject("error", errorString);
            mav.addObject("success", false);
        } else {
            mav.addObject("success", true);
        }

        return mav;
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

        ServiceInterface si = getGrisuService(request);
        List<SimpleBean> versions = new ArrayList<SimpleBean>();
        ModelAndView mav = new ModelAndView("jsonView");
        String errorString = null;

        if (si == null) {
            errorString = "You are not logged in or your session has expired";
            logger.warn("ServiceInterface is null!");
        } else {
            logger.debug("Retrieving available ESyS-Particle versions");
            GrisuRegistry registry = GrisuRegistryManager.getDefault(si);
            Set<String> versionSet = registry.
                getUserApplicationInformation(VRLJob.APPLICATION_NAME)
                .getAllAvailableVersionsForUser();
            Iterator<String> it = versionSet.iterator();
            while (it.hasNext()) {
                versions.add(new SimpleBean(it.next()));
            }

            logger.debug("Returning list of "+versions.size()+" versions.");
        }
        // always need to return this property to allow client to parse
        // the error if any
        mav.addObject("versions", versions);

        if (errorString != null) {
            mav.addObject("error", errorString);
            mav.addObject("success", false);
        } else {
            mav.addObject("success", true);
        }

        return mav;
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

        logger.debug("Returning job.");
        ModelAndView result = new ModelAndView("jsonView");
        result.addObject("data", job);
        result.addObject("success", true);

        return result;
    }

    /**
     * Returns a JSON object containing an array of filenames and sizes which
     * are currently in the job's stage in directory.
     *
     * @param request The servlet request
     * @param response The servlet response
     *
     * @return A JSON object with a files attribute which is an array of
     *         filenames.
     */
    public ModelAndView listJobFiles(HttpServletRequest request,
                                     HttpServletResponse response) {

        String jobInputDir = (String) request.getSession()
            .getAttribute("jobInputDir");

        List files = new ArrayList<FileInformation>();

        if (jobInputDir != null) {
            File dir = new File(jobInputDir);
            String fileNames[] = dir.list();
            for (int i=0; i<fileNames.length; i++) {
                File f = new File(dir, fileNames[i]);
                files.add(new FileInformation(fileNames[i], f.length()));
            }
        }

        logger.debug("Returning list of "+files.size()+" files.");
        return new ModelAndView("jsonView", "files", files);
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
                        jobInputDir, f.getOriginalFilename());
                if (destination.exists()) {
                    logger.debug("Will overwrite existing file.");
                }
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
     * Deletes one or more uploaded files of the current job.
     *
     * @param request The servlet request
     * @param response The servlet response
     *
     * @return A JSON object with a success attribute that indicates whether
     *         the files were successfully deleted.
     */
    public ModelAndView deleteFiles(HttpServletRequest request,
                                    HttpServletResponse response) {

        String jobInputDir = (String) request.getSession()
            .getAttribute("jobInputDir");
        ModelAndView mav = new ModelAndView("jsonView");
        boolean success;

        if (jobInputDir != null) {
            success = true;
            String filesPrm = request.getParameter("files");
            logger.debug("Request to delete "+filesPrm);
            String[] files = (String[]) JSONArray.toArray(
                    JSONArray.fromObject(filesPrm), String.class);

            for (String filename: files) {
                File f = new File(jobInputDir, filename);
                if (f.exists() && f.isFile()) {
                    logger.debug("Deleting "+f.getPath());
                    boolean lsuccess = f.delete();
                    if (!lsuccess) {
                        logger.warn("Unable to delete "+f.getPath());
                        success = false;
                    }
                } else {
                    logger.warn(f.getPath()+" does not exist or is not a file!");
                }
            }
        } else {
            success = false;
        }

        mav.addObject("success", success);
        return mav;
    }

    /**
     * Cancels the current job submission. Called to clean up temporary files.
     *
     * @param request The servlet request
     * @param response The servlet response
     *
     * @return null
     */
    public ModelAndView cancelSubmission(HttpServletRequest request,
                                         HttpServletResponse response) {

        String jobInputDir = (String) request.getSession()
            .getAttribute("jobInputDir");

        if (jobInputDir != null) {
            logger.debug("Deleting temporary job files.");
            File jobDir = new File(jobInputDir);
            Util.deleteFilesRecursive(jobDir);
            request.getSession().removeAttribute("jobInputDir");
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

        final String user = request.getRemoteUser();
        ServiceInterface si = getGrisuService(request);
        final String jobInputDir = (String) request.getSession()
            .getAttribute("jobInputDir");
        final String newSeriesName = request.getParameter("seriesName");
        final String seriesIdStr = request.getParameter("seriesId");
        final String memoryStr = request.getParameter("memory");
        final String numprocsStr = request.getParameter("numprocs");
        final String queue = request.getParameter("queue");
        final String site = request.getParameter("site");
        final String version = request.getParameter("version");
        final String walltimeStr = request.getParameter("walltime");
        VRLSeries series = null;
        ModelAndView mav = new ModelAndView("jsonView");
        String errorString = null;

        // if seriesName parameter was provided then we create a new series
        // (after ensuring that a series by that name doesn't already exist)
        // otherwise seriesId contains the id of the series to use.
        if (newSeriesName != null && newSeriesName != "") {
            List<VRLSeries> exSeries = jobManager.querySeries(
                    user, newSeriesName, "");
            if (!exSeries.isEmpty()) {
                logger.debug("Using existing series '"+newSeriesName+"'.");
                series = exSeries.get(0);
            } else {
                String newSeriesDesc = request.getParameter("seriesDesc");

                logger.debug("Creating new series '"+newSeriesName+"'.");
                series = new VRLSeries();
                series.setUser(user);
                series.setName(newSeriesName);
                if (newSeriesDesc != null) {
                    series.setDescription(newSeriesDesc);
                }
                jobManager.saveSeries(series);
                // Note that we can now access the series' new ID
            }
        } else if (seriesIdStr != null && seriesIdStr != "") {
            try {
                long seriesId = Long.parseLong(seriesIdStr);
                series = jobManager.getSeriesById(seriesId);
            } catch (NumberFormatException e) {
                logger.error("Error parsing series ID!");
            }
        }

        if (si == null) {
            errorString = "You are not logged in or your session has expired";
            logger.warn("ServiceInterface is null!");
        } else if (series == null) {
            errorString = "Invalid series";
            logger.error("series is null!");
        } else if (!series.getUser().equals(user)) {
            errorString = "You can only submit your own jobs";
            logger.warn(user+" tried to submit "+series.getUser()+"'s job");
        } else if (memoryStr == null || numprocsStr == null || queue == null
                || site == null || version == null || walltimeStr == null) {
            errorString = new String("Missing parameter(s)");
            logger.warn(errorString);
        } else {
            JobObject grisuJob = new JobObject(si);
            try {
                int walltime = Integer.parseInt(walltimeStr) * 60;
                int numprocs = Integer.parseInt(numprocsStr);
                long memory = Long.parseLong(memoryStr) * 1024L * 1024L;
                grisuJob.setCpus(numprocs);
                grisuJob.setMemory(memory);
                grisuJob.setWalltimeInSeconds(walltime);

                String subLoc = getSubmissionLocationForVersionSiteQueue(si,
                        version, site, queue);
                if (subLoc == null) {
                    errorString = new String(
                        "Invalid site, queue, or version specified");
                    throw new Exception("Site/queue/version combo not found: "
                        + site + ", " + queue + ", " + version);
                }
                grisuJob.setSubmissionLocation(subLoc);

                String cmdline = new String(VRLJob.BINARY_NAME
                        + " " + job.getScriptFile());
                grisuJob.setCommandline(cmdline);

                grisuJob.setApplication(VRLJob.APPLICATION_NAME);
                grisuJob.setApplicationVersion(version);
                grisuJob.setTimestampJobname(job.getName());

                File jobDir = new File(jobInputDir);
                // Adding directories doesn't work yet in grisu
                //job.addInputFileUrl(taskDir.getPath());
                File[] files = jobDir.listFiles();
                for (File f : files) {
                    if (f.isFile()) {
                        grisuJob.addInputFileUrl(f.getPath());
                    }
                }

                grisuJob.createJob(FQAN);
                logger.info("Submitting job with name "
                        + grisuJob.getJobname() + " to "
                        + grisuJob.getSubmissionLocation());
                grisuJob.submitJob();

                job.setHandle(grisuJob.getJobname());
                job.setSeriesId(series.getId());
                jobManager.saveJob(job);
                request.getSession().removeAttribute("jobInputDir");

                String status = grisuJob.getStatusString(true);
                logger.debug("New job status: "+status);

            } catch (NumberFormatException e) {
                logger.warn("Error parsing integer value(s): " + walltimeStr
                        + " / " + numprocsStr + " / " + memoryStr);
                errorString = new String("Invalid wall time / CPUs / memory");

            } catch (JobPropertiesException e) {
                logger.error(e.getMessage());
                errorString = new String("The job could not be submitted");

            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                if (errorString == null) {
                    errorString = new String("The job could not be submitted");
                }

            } catch (java.lang.Error e) {
                logger.error(e,e);
                errorString = new String(
                        "Internal error. Please contact site administrator.");
            }
        }

        if (errorString != null) {
            mav.addObject("error", errorString);
            mav.addObject("success", false);
        } else {
            mav.addObject("success", true);
        }

        return mav;
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
        final String maxWallTime = "3000"; // 50 hours
        final String maxMemory = "30720"; // 30 GB
        String name = "VRLjob";
        String site = "ESSCC";
        Integer cpuCount = 2;
        String version = "2.0";
        String queue = "workq";
        String description = "";
        String scriptFile = "";

        // Create a new directory to put all files for this job into.
        // This directory will always be the first stageIn directive.
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String jobInputDir = TEMP_DIR + user + File.separator
            + sdf.format(new Date());

        boolean success = (new File(jobInputDir)).mkdirs();

        if (!success) {
            logger.error("Could not create directory "+jobInputDir);
        }

        // Save in session to use it when submitting job
        request.getSession().setAttribute("jobInputDir", jobInputDir);

        // Check if the user requested to re-submit a previous job.
        String jobIdStr = (String) request.getSession().
            getAttribute("resubmitJob");
        VRLJob existingJob = null;
        if (jobIdStr != null) {
            request.getSession().removeAttribute("resubmitJob");
            logger.debug("Request to re-submit a job.");
            try {
                long jobId = Long.parseLong(jobIdStr);
                existingJob = jobManager.getJobById(jobId);
            } catch (NumberFormatException e) {
                logger.error("Error parsing job ID!");
            }
        }

        if (existingJob != null) {
            logger.debug("Using attributes of "+existingJob.getName());
            name = existingJob.getName()+"_resubmit";
            scriptFile = existingJob.getScriptFile();
            description = existingJob.getDescription();

            logger.debug("Copying files from old job to stage-in directory");
            File srcDir = new File(existingJob.getOutputDir());
            File destDir = new File(jobInputDir);
            success = Util.copyFilesRecursive(srcDir, destDir);
            if (!success) {
                logger.error("Could not copy all files!");
                // TODO: Let user know this didn't work
            }
        }

        // Check if the ScriptBuilder was used. If so, there is a file in the
        // system temp directory which needs to be staged in.
        String newScript = (String) request.getSession().
            getAttribute("scriptFile");
        if (newScript != null) {
            request.getSession().removeAttribute("scriptFile");
            logger.debug("Adding "+newScript+" to stage-in directory");
            File tmpScriptFile = new File(System.getProperty("java.io.tmpdir") +
                    File.separator+newScript+".py");
            File newScriptFile = new File(jobInputDir, tmpScriptFile.getName());
            success = Util.moveFile(tmpScriptFile, newScriptFile);
            if (success) {
                logger.info("Moved "+newScript+" to stageIn directory");
                scriptFile = newScript+".py";
            } else {
                logger.warn("Could not move "+newScript+" to stage-in!");
            }
        }

        logger.debug("Creating new VRLJob instance");
        VRLJob job = new VRLJob(name, description, scriptFile, null, null);

        return job;
    }

    /**
     *
     */
    private String getSubmissionLocationForVersionSiteQueue(
            ServiceInterface si, String version, String site, String queue) {
        GrisuRegistry registry = GrisuRegistryManager.getDefault(si);
        String subLoc = null;
        Set<String> subLocs = registry
            .getApplicationInformation(VRLJob.APPLICATION_NAME)
            .getAvailableSubmissionLocationsForVersionAndFqan(
                    version, FQAN);
        Iterator<String> it = subLocs.iterator();
        while (it.hasNext()) {
            String sl = it.next();
            if (site.equals(registry.getResourceInformation().getSite(sl))
                    && queue.equals(
                        SubmissionLocationHelpers.extractQueue(sl))) {
                subLoc=sl;
                break;
            }
        }
        return subLoc;
    }
}

