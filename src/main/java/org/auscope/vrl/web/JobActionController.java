/*
 * This file is part of the AuScope Virtual Rock Lab (VRL) project.
 * Copyright (c) 2010 The University of Queensland, ESSCC
 *
 * Licensed under the terms of the GNU Lesser General Public License.
 */
package org.auscope.vrl.web;

import au.org.arcs.jcommons.constants.Constants;
import au.org.arcs.jcommons.constants.JobSubmissionProperty;
import au.org.arcs.jcommons.interfaces.GridResource;
import au.org.arcs.jcommons.utils.SubmissionLocationHelpers;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.auscope.vrl.VRLJob;
import org.auscope.vrl.VRLJobManager;
import org.auscope.vrl.VRLSeries;
import org.auscope.vrl.Util;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;
import org.springframework.web.servlet.view.RedirectView;

import org.vpac.grisu.control.JobConstants;
import org.vpac.grisu.control.ServiceInterface;
import org.vpac.grisu.control.exceptions.JobPropertiesException;
import org.vpac.grisu.frontend.model.job.JobException;
import org.vpac.grisu.frontend.model.job.JobObject;
import org.vpac.grisu.model.GrisuRegistry;
import org.vpac.grisu.model.GrisuRegistryManager;

/**
 * Controller for job related actions.
 *
 * @author Cihan Altinay
 */
public class JobActionController extends MultiActionController {

    /** Logger for this class */
    private final Log logger = LogFactory.getLog(getClass());
    private static final String FQAN = "/ARCS/AuScope";

    private VRLJobManager jobManager;

    /**
     * Sets the {@link VRLJobManager} to be used to retrieve and store series
     * and job details.
     *
     * @param jobManager the <code>VRLJobManager</code> to use
     */
    public void setJobManager(VRLJobManager jobManager) {
        this.jobManager = jobManager;
    }

    protected ModelAndView handleNoSuchRequestHandlingMethod(
            NoSuchRequestHandlingMethodException ex,
            HttpServletRequest request,
            HttpServletResponse response) {

        logger.warn(ex.getMessage());
        String referer = request.getHeader("referer");
        if (referer != null && referer.contains(request.getServerName())) {
            ModelAndView mav = new ModelAndView("jsonView");
            mav.addObject("error", ex.getMessage());
            mav.addObject("success", false);
            return mav;
        } else {
            return new ModelAndView(
                    new RedirectView("/login.html", true, false, false));
        }
    }

    private ServiceInterface getGrisuService(HttpServletRequest request) {
        return (ServiceInterface)
            request.getSession().getAttribute("grisuService");
    }

    ///////////////////////////////////////////////////////////////////////
    ////////////////////////////// ACTIONS ////////////////////////////////
    ///////////////////////////////////////////////////////////////////////

    /**
     * Deletes a job including all its files from the active series.
     *
     * @param request The servlet request
     * @param response The servlet response
     *
     * @return A JSON object
     */
    public ModelAndView deleteJob(HttpServletRequest request,
                                  HttpServletResponse response) {

        final ServiceInterface si = getGrisuService(request);
        final String user = request.getRemoteUser();
        File seriesDir = (File)request.getSession().getAttribute("seriesDir");
        final String jobStr = request.getParameter("job");
        final ModelAndView mav = new ModelAndView("jsonView");
        String errorString = null;
        VRLJob job = null;
        long jobId = -1;

        if (jobStr != null) {
            try {
                jobId = Long.parseLong(jobStr);
                job = jobManager.getJobById(jobId);
            } catch (NumberFormatException e) {
                logger.warn("Error parsing job ID " + jobStr);
            }
        }

        if (si == null) {
            errorString = ErrorMessages.SESSION_EXPIRED;
            logger.warn("ServiceInterface is null!");
        } else if (seriesDir == null) {
            errorString = ErrorMessages.NO_SERIES;
            logger.warn(errorString);
        } else if (job == null) {
            errorString = ErrorMessages.INVALID_JOB;
            logger.warn("job is null!");
        } else {
            // check if current user is the owner of the job
            VRLSeries s = jobManager.getSeriesById(job.getSeriesId());
            if (user.equals(s.getUser())) {
                String handle = job.getHandle();
                if (handle != null && handle.length() > 0) {
                    try {
                        // try to get the job status
                        JobObject grisuJob = new JobObject(si, handle);
                        int status = grisuJob.getStatus(true);
                        if (status <= JobConstants.ACTIVE) {
                            errorString = ErrorMessages.JOB_IS_RUNNING;
                            logger.warn(errorString);
                        }
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            } else {
                errorString = ErrorMessages.NOT_AUTHORIZED;
                logger.warn(user + "'s attempt to delete " + s.getUser()
                    + "'s job denied!");
            }

            if (errorString == null) {
                try {
                    logger.debug("Deleting job files");
                    File jobDir = new File(seriesDir, jobStr);
                    jobManager.deleteFiles(new File[]{jobDir});
                    String message = "Deleted job '" + job.getName() + "'.";
                    jobManager.saveRevision(jobDir, message);
                    logger.debug("Deleting job "+jobStr+" from database.");
                    jobManager.deleteJob(job);
                } catch (Exception e) {
                    errorString = ErrorMessages.INTERNAL_ERROR;
                    logger.error(e.getMessage(), e);
                }
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
     * Terminates a job given by its identifier.
     *
     * @param request The servlet request including a job parameter
     * @param response The servlet response
     *
     * @return A JSON object with a success attribute and an error attribute
     *         in case of an error.
     */
    public ModelAndView killJob(HttpServletRequest request,
                                HttpServletResponse response) {

        final ServiceInterface si = getGrisuService(request);
        final String user = request.getRemoteUser();
        Long seriesId = (Long)request.getSession().getAttribute("seriesId");
        final String jobStr = request.getParameter("job");
        final ModelAndView mav = new ModelAndView("jsonView");
        String errorString = null;
        VRLJob job = null;
        long jobId = -1;
        String handle = null;

        if (jobStr != null) {
            try {
                jobId = Long.parseLong(jobStr);
                job = jobManager.getJobById(jobId);
                handle = job.getHandle();
            } catch (NumberFormatException e) {
                logger.warn("Error parsing job ID " + jobStr);
            }
        }

        if (si == null) {
            errorString = ErrorMessages.SESSION_EXPIRED;
            logger.warn("ServiceInterface is null!");
        } else if (seriesId == null) {
            errorString = ErrorMessages.NO_SERIES;
            logger.warn(errorString);
        } else if (job == null) {
            errorString = ErrorMessages.INVALID_JOB;
            logger.warn("job is null!");
        } else if (handle == null || handle.length() == 0) {
            errorString = ErrorMessages.NULL_HANDLE;
            logger.warn(errorString);
        } else {
            // check if current user is the owner of the job
            VRLSeries s = jobManager.getSeriesById(job.getSeriesId());
            if (user.equals(s.getUser())) {
                try {
                    logger.debug("Terminating job with ID "+jobStr);
                    JobObject grisuJob = new JobObject(si, handle);
                    grisuJob.kill(true);
                    job.setHandle(null);
                    job.setOutputUrl(null);
                    jobManager.saveJob(job);
                } catch (Exception e) {
                    errorString = ErrorMessages.INTERNAL_ERROR;
                    logger.error(e.getMessage(), e);
                }
            } else {
                errorString = ErrorMessages.NOT_AUTHORIZED;
                logger.warn(user + "'s attempt to kill " + s.getUser()
                    + "'s job denied!");
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
     * Returns a JSON object containing an array of jobs of the active
     * series.
     *
     * @param request The servlet request
     * @param response The servlet response
     *
     * @return
     */
    public ModelAndView listJobs(HttpServletRequest request,
                                 HttpServletResponse response) {

        final ServiceInterface si = getGrisuService(request);
        Long seriesId = (Long)request.getSession().getAttribute("seriesId");
        final ModelAndView mav = new ModelAndView("jsonView");
        String errorString = null;

        if (si == null) {
            errorString = ErrorMessages.SESSION_EXPIRED;
            logger.warn("ServiceInterface is null!");
        } else if (seriesId == null) {
            errorString = ErrorMessages.NO_SERIES;
            logger.warn(errorString);
        } else {
            GrisuRegistry registry = GrisuRegistryManager.getDefault(si);
            List jobs = jobManager.getJobsBySeries(seriesId.longValue());
            JSONArray jsJobs = (JSONArray)JSONSerializer.toJSON(jobs);
            Iterator it = jsJobs.listIterator();
            while (it.hasNext()) {
                JSONObject job = (JSONObject)it.next();
                String handle = job.getString("handle");
                if (handle != null && handle.length() > 0) {
                    try {
                        JobObject grisuJob = new JobObject(si, handle);
                        String status = grisuJob.getStatusString(true);
                        String subLoc = grisuJob.getSubmissionLocation();
                        String queue = SubmissionLocationHelpers
                            .extractQueue(subLoc);
                        String site = registry.getResourceInformation()
                            .getSite(subLoc);
                        String stdOut = null;
                        String stdErr = null;
                        try {
                            stdOut = lastLinesOf(
                                    grisuJob.getStdOutContent(), 10);
                            stdErr = lastLinesOf(
                                    grisuJob.getStdErrContent(), 10);
                        } catch (JobException e) {
                            // there might not be a stdout/err file yet
                        }
                        String version = grisuJob.getApplicationVersion();
                        Long submitDate = Long.parseLong(
                                grisuJob.getJobProperty(
                                    Constants.SUBMISSION_TIME_KEY));
                        job.put("memory", grisuJob.getMemory()/(1024L*1024L));
                        job.put("numProcs", grisuJob.getCpus());
                        job.put("queue", queue);
                        job.put("site", site);
                        job.put("status", status);
                        job.put("stderr", stdErr);
                        job.put("stdout", stdOut);
                        job.put("submitDate", submitDate);
                        job.put("version", version);
                        job.put("walltime", grisuJob.getWalltimeInSeconds()/60);
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                } else {
                    // set some sane defaults
                    job.put("memory", 30720);
                    job.put("numProcs", 2);
                    job.put("walltime", 3000);
                }
            }
            mav.addObject("jobs", jsJobs);
        }

        if (errorString != null) {
            mav.addObject("jobs", "");
            mav.addObject("error", errorString);
            mav.addObject("success", false);
        } else {
            mav.addObject("success", true);
        }

        return mav;
    }

    /**
     * Very simple helper class (bean).
     */
    public final class SimpleBean {
        private final String value;
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

        final ServiceInterface si = getGrisuService(request);
        final String version = request.getParameter("version");
        final ModelAndView mav = new ModelAndView("jsonView");
        String errorString = null;
        List<SimpleBean> sites = new ArrayList<SimpleBean>();

        if (si == null) {
            errorString = ErrorMessages.SESSION_EXPIRED;
            logger.warn("ServiceInterface is null!");
        } else if (version == null) {
            errorString = ErrorMessages.MISSING_PARAMETER;
            logger.warn("No version specified!");
        } else {
            GrisuRegistry registry = GrisuRegistryManager.getDefault(si);
            if (version != null) {
                logger.debug("Retrieving sites with ESyS-Particle " + version
                        + " installations.");
                /* this method seems buggy and returns JCU half of the time
                HashMap propMap = new HashMap();
                propMap.put(JobSubmissionProperty.APPLICATIONVERSION, version);
                Set<GridResource> resources = registry
                    .getApplicationInformation(VRLJob.APPLICATION_NAME)
                    .getBestSubmissionLocations(propMap, FQAN);
                Iterator<GridResource> it = resources.iterator();
                while (it.hasNext()) {
                    sites.add(new SimpleBean(it.next().getSiteName()));
                }
                */
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
                logger.debug("Retrieving sites with ESyS-Particle installations.");
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
     * Returns a JSON object containing an array of ESyS-Particle versions at
     * the specified site.
     *
     * @param request The servlet request including a site parameter
     * @param response The servlet response
     *
     * @return A JSON object with a versions attribute which is an array of
     *         versions installed at requested site.
     */
    public ModelAndView listVersions(HttpServletRequest request,
                                     HttpServletResponse response) {

        final ServiceInterface si = getGrisuService(request);
        final ModelAndView mav = new ModelAndView("jsonView");
        String errorString = null;
        List<SimpleBean> versions = new ArrayList<SimpleBean>();

        if (si == null) {
            errorString = ErrorMessages.SESSION_EXPIRED;
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

        final ServiceInterface si = getGrisuService(request);
        final String site = request.getParameter("site");
        final String version = request.getParameter("version");
        final ModelAndView mav = new ModelAndView("jsonView");
        String errorString = null;
        List<SimpleBean> queues = new ArrayList<SimpleBean>();

        if (si == null) {
            errorString = ErrorMessages.SESSION_EXPIRED;
            logger.warn("ServiceInterface is null!");
        } else if (site == null || version == null) {
            errorString = ErrorMessages.MISSING_PARAMETER;
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
     * Creates a new job or updates job details within the active series.
     *
     * @param request The servlet request
     * @param response The servlet response
     *
     * @return A JSON object with a job attribute.
     */
    public ModelAndView saveJob(HttpServletRequest request,
                                HttpServletResponse response) {

        final String user = request.getRemoteUser();
        File seriesDir = (File)request.getSession().getAttribute("seriesDir");
        Long seriesId = (Long)request.getSession().getAttribute("seriesId");
        final String name = request.getParameter("name");
        final String scriptFile = request.getParameter("scriptFile");
        final String description = request.getParameter("description");
        final String jobStr = request.getParameter("id");
        final ModelAndView mav = new ModelAndView("jsonView");
        String errorString = null;
        VRLSeries series = null;
        VRLJob job = null;

        if (seriesId != null) {
            series = jobManager.getSeriesById(seriesId.longValue());
        }

        if (getGrisuService(request) == null) {
            errorString = ErrorMessages.SESSION_EXPIRED;
            logger.warn("ServiceInterface is null!");
        } else if (series == null) {
            errorString = ErrorMessages.NO_SERIES;
            logger.warn(errorString);
        } else if (!series.getUser().equals(user)) {
            errorString = ErrorMessages.NOT_AUTHORIZED;
            logger.warn(user+" tried to save "+series.getUser()+"'s job");
        } else if (name == null || scriptFile == null) {
            errorString = ErrorMessages.MISSING_PARAMETER;
            logger.warn(errorString);
        } else if (!scriptFile.endsWith(".py")) {
            errorString = ErrorMessages.INVALID_SCRIPTFILE;
            logger.warn(errorString);
        } else if (!scriptFile.equals(Util.sanitizeSubPath(scriptFile))) {
            errorString = ErrorMessages.INVALID_FILENAME;
            logger.warn(errorString);
        } else if (jobStr != null && jobStr.length() > 0) {
            try {
                long jobId = Long.parseLong(jobStr);
                job = jobManager.getJobById(jobId);
            } catch (NumberFormatException e) {
                logger.warn("Error parsing job ID " + jobStr);
            }
            if (job == null) {
                errorString = ErrorMessages.MISSING_PARAMETER;
            }
        }

        if (errorString == null) {
            // prevent duplicate job names
            List<VRLJob> jobs = jobManager.getJobsBySeries(
                    seriesId.longValue());
            Iterator<VRLJob> it = jobs.listIterator();
            while (it.hasNext()) {
                VRLJob j = it.next();
                if (name.equals(j.getName())) {
                    if (job == null || !job.getId().equals(j.getId())) {
                        errorString = ErrorMessages.JOB_EXISTS;
                        logger.warn(errorString+": "+j.getId()+"/"+jobStr);
                        break;
                    }
                }
            }
        }

        if (errorString == null) {
            if (job != null) {
                // update existing job
                job.setName(name);
                job.setDescription(description);
                job.setScriptFile(scriptFile);
                jobManager.saveJob(job);
                mav.addObject("job", job);
            } else {
                // create new job
                VRLJob newJob = new VRLJob(name, description, scriptFile,
                        seriesId);
                jobManager.saveJob(newJob);
                File jobDir = new File(seriesDir, newJob.getId().toString());
                jobDir.mkdir();
                File jobScript = new File(jobDir, scriptFile);
                try {
                    jobScript.createNewFile();
                    jobManager.addFile(jobDir);
                    jobManager.addFile(jobScript);
                    String message = "Created job '" + name + "'.";
                    jobManager.saveRevision(jobDir, message);
                } catch (IOException e) {
                    logger.warn(e.getMessage());
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
                mav.addObject("job", newJob);
            }
            mav.addObject("success", true);
        } else {
            mav.addObject("error", errorString);
            mav.addObject("success", false);
        }

        return mav;
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
                                  HttpServletResponse response) {

        final ServiceInterface si = getGrisuService(request);
        final String user = request.getRemoteUser();
        File seriesDir = (File)request.getSession().getAttribute("seriesDir");
        Long seriesId = (Long)request.getSession().getAttribute("seriesId");
        final String jobStr = request.getParameter("job");
        final String memoryStr = request.getParameter("memory");
        final String numprocsStr = request.getParameter("numprocs");
        final String queue = request.getParameter("queue");
        final String site = request.getParameter("site");
        final String version = request.getParameter("version");
        final String walltimeStr = request.getParameter("walltime");
        final ModelAndView mav = new ModelAndView("jsonView");
        String errorString = null;
        VRLSeries series = null;
        VRLJob job = null;

        if (jobStr != null) {
            try {
                long jobId = Long.parseLong(jobStr);
                job = jobManager.getJobById(jobId);
            } catch (NumberFormatException e) {
                logger.warn("Error parsing job ID " + jobStr);
            }
        }

        if (seriesId != null) {
            series = jobManager.getSeriesById(seriesId.longValue());
        }

        if (si == null) {
            errorString = ErrorMessages.SESSION_EXPIRED;
            logger.warn("ServiceInterface is null!");
        } else if (series == null) {
            errorString = ErrorMessages.NO_SERIES;
            logger.warn(errorString);
        } else if (!series.getUser().equals(user)) {
            errorString = ErrorMessages.NOT_AUTHORIZED;
            logger.warn(user+" tried to submit "+series.getUser()+"'s job");
        } else if (job == null) {
            errorString = ErrorMessages.INVALID_JOB;
            logger.warn("job is null!");
        } else if (memoryStr == null || numprocsStr == null || queue == null
                || site == null || version == null || walltimeStr == null) {
            errorString = ErrorMessages.MISSING_PARAMETER;
            logger.warn(errorString);
        } else {
            File jobDir = new File(seriesDir, jobStr);
            File scriptPath = new File(jobDir, job.getScriptFile());
            if (!scriptPath.exists()) {
                errorString = ErrorMessages.SCRIPT_NOT_FOUND;
                logger.warn(job.getScriptFile()+" does not exist!");
            }
        }

        if (errorString == null) {
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
                    errorString = ErrorMessages.MISSING_PARAMETER;
                    throw new Exception("Site/queue/version combo not found: "
                        + site + ", " + queue + ", " + version);
                }
                grisuJob.setSubmissionLocation(subLoc);
                String cmdline = new String(VRLJob.BINARY_NAME + " "
                        + job.getScriptFile());
                grisuJob.setCommandline(cmdline);
                grisuJob.setApplication(VRLJob.APPLICATION_NAME);
                grisuJob.setApplicationVersion(version);
                grisuJob.setTimestampJobname(job.getName());

                File jobDir = new File(seriesDir, jobStr);
                // Adding directories doesn't work yet in grisu
                //job.addInputFileUrl(jobDir.getPath());
                File[] files = jobDir.listFiles();
                for (File f : files) {
                    if (f.isFile()) {
                        grisuJob.addInputFileUrl(f.getPath());
                    }
                }

                grisuJob.createJob(FQAN);

                // clean up old job if there is one
                if (job.getHandle() != null && job.getHandle().length() > 0) {
                    try {
                        (new JobObject(si, job.getHandle())).kill(true);
                        job.setHandle(null);
                        job.setOutputUrl(null);
                        jobManager.saveJob(job);
                    } catch (Exception e) {
                        logger.warn("Error deleting old job: "+e.getMessage());
                    }
                }

                logger.info("Submitting job with name " + grisuJob.getJobname()
                        + " to " + grisuJob.getSubmissionLocation());
                grisuJob.submitJob();

                job.setHandle(grisuJob.getJobname());
                job.setOutputUrl(grisuJob.getJobDirectoryUrl());
                jobManager.saveJob(job);

                String status = grisuJob.getStatusString(true);
                logger.debug("New job status: "+status);
                mav.addObject("status", status);

            } catch (NumberFormatException e) {
                errorString = ErrorMessages.MISSING_PARAMETER;
                logger.warn("Error parsing integer value(s): " + walltimeStr
                        + " / " + numprocsStr + " / " + memoryStr);

            } catch (JobPropertiesException e) {
                errorString = ErrorMessages.INTERNAL_ERROR;
                logger.error(e.getMessage(), e);

            } catch (Exception e) {
                if (errorString == null) {
                    errorString = ErrorMessages.INTERNAL_ERROR;
                }
                logger.error(e.getMessage(), e);

            } catch (java.lang.Error e) {
                errorString = ErrorMessages.INTERNAL_ERROR;
                logger.error(e.getMessage(), e);
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

    /**
     *
     */
    private String lastLinesOf(final String source, int numLines) {
        int idx = source.lastIndexOf('\n');
        int i = 0;
        while (idx > -1 && i < numLines) {
            idx=source.lastIndexOf('\n', idx-1);
            i++;
        }
        return source.substring(idx+1);
    }
}

