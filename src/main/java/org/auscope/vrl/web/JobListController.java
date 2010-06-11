/*
 * This file is part of the AuScope Virtual Rock Lab (VRL) project.
 * Copyright (c) 2010 The University of Queensland, ESSCC
 *
 * Licensed under the terms of the GNU Lesser General Public License.
 */
package org.auscope.vrl.web;

import au.org.arcs.jcommons.constants.Constants;
import au.org.arcs.jcommons.utils.SubmissionLocationHelpers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.auscope.vrl.FileInformation;
import org.auscope.vrl.Util;
import org.auscope.vrl.VRLJob;
import org.auscope.vrl.VRLJobManager;
import org.auscope.vrl.VRLSeries;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;
import org.springframework.web.servlet.view.RedirectView;

import org.vpac.grisu.control.JobConstants;
import org.vpac.grisu.control.ServiceInterface;
import org.vpac.grisu.frontend.model.job.JobObject;
import org.vpac.grisu.model.GrisuRegistry;
import org.vpac.grisu.model.GrisuRegistryManager;
import org.vpac.grisu.model.dto.DtoFile;
import org.vpac.grisu.model.dto.DtoFolder;


/**
 * Controller for the job list view.
 *
 * @author Cihan Altinay
 */
public class JobListController extends MultiActionController {

    /** Logger for this class */
    private final Log logger = LogFactory.getLog(getClass());

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
            logger.debug("No/invalid action parameter; returning joblist view.");
            return new ModelAndView("joblist");
        } else {
            request.getSession().setAttribute(
                    "redirectAfterLogin", "/joblist.html");
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
     * Kills the job given by its identifier.
     *
     * @param request The servlet request including a jobId parameter
     * @param response The servlet response
     *
     * @return A JSON object with a success attribute and an error attribute
     *         in case the job was not found in the job manager.
     */
    public ModelAndView killJob(HttpServletRequest request,
                                HttpServletResponse response) {

        ServiceInterface si = getGrisuService(request);
        final String user = request.getRemoteUser();
        final String jobIdStr = request.getParameter("jobId");
        VRLJob job = null;
        String handle = null;
        ModelAndView mav = new ModelAndView("jsonView");
        String errorString = null;

        if (jobIdStr != null) {
            try {
                long jobId = Long.parseLong(jobIdStr);
                job = jobManager.getJobById(jobId);
                handle = job.getHandle();
            } catch (NumberFormatException e) {
                logger.error("Error parsing job ID!");
            }
        }

        if (si == null) {
            errorString = "You are not logged in or your session has expired";
            logger.warn("ServiceInterface is null!");
        } else if (job == null) {
            errorString = "No/Invalid job specified";
            logger.error(errorString);
        } else {
            // check if current user is the owner of the job
            VRLSeries s = jobManager.getSeriesById(job.getSeriesId());
            if (user.equals(s.getUser())) {
                try {
                    logger.info("Terminating job with ID "+jobIdStr);
                    JobObject grisuJob = new JobObject(si, handle);
                    grisuJob.kill(true);
                    //job.setHandle(null);
                    //jobManager.saveJob(job);
                } catch (Exception e) {
                    errorString = "Error terminating task";
                    logger.error(e.getMessage(), e);
                }
            } else {
                logger.warn(user + "'s attempt to kill " + s.getUser()
                        + "'s job denied!");
                errorString = "You are not authorised to terminate this job.";
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
     * Kills all jobs of given series.
     *
     * @param request The servlet request including a seriesId parameter
     * @param response The servlet response
     *
     * @return A JSON object with a success attribute and an error attribute
     *         in case the series was not found in the job manager.
     */
    public ModelAndView killSeriesJobs(HttpServletRequest request,
                                       HttpServletResponse response) {

        ServiceInterface si = getGrisuService(request);
        final String user = request.getRemoteUser();
        String seriesIdStr = request.getParameter("seriesId");
        List<VRLJob> jobs = null;
        ModelAndView mav = new ModelAndView("jsonView");
        long seriesId = -1;
        String errorString = null;

        if (seriesIdStr != null) {
            try {
                seriesId = Long.parseLong(seriesIdStr);
                jobs = jobManager.getSeriesJobs(seriesId);
            } catch (NumberFormatException e) {
                logger.error("Error parsing series ID!");
            }
        }

        if (si == null) {
            errorString = "You are not logged in or your session has expired";
            logger.warn("ServiceInterface is null!");
        } else if (jobs == null) {
            errorString = "No/Invalid series specified";
            logger.error(errorString);
        } else {
            // check if current user is the owner of the series
            VRLSeries s = jobManager.getSeriesById(seriesId);
            if (user.equals(s.getUser())) {
                logger.info("Terminating jobs within series "+seriesIdStr);
                for (VRLJob job : jobs) {
                    String handle = job.getHandle();
                    if (handle == null || handle.length() == 0) {
                        continue;
                    }
                    try {
                        JobObject grisuJob = new JobObject(si, handle);
                        int oldStatus = grisuJob.getStatus(true);
                        if (oldStatus <= JobConstants.ACTIVE) {
                            logger.debug("Killing job with ID "+job.getId());
                            grisuJob.kill(true);
                            //job.setHandle(null);
                            //jobManager.saveJob(job);
                        }
                    } catch (Exception e) {
                        logger.warn(e.getMessage());
                    }
                }
            } else {
                logger.warn(user + "'s attempt to kill " + s.getUser()
                        + "'s jobs denied!");
                errorString = "You are not authorised to terminate these jobs.";
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
     * Returns a JSON object containing an array of files belonging to a
     * given job.
     *
     * @param request The servlet request including a jobId parameter
     * @param response The servlet response
     *
     * @return A JSON object with a files attribute which is an array of
     *         FileInformation objects. If the job was not found in the job
     *         manager the JSON object will contain an error attribute
     *         indicating the error.
     */
    public ModelAndView jobFiles(HttpServletRequest request,
                                 HttpServletResponse response) {

        ServiceInterface si = getGrisuService(request);
        String jobIdStr = request.getParameter("jobId");
        VRLJob job = null;
        ModelAndView mav = new ModelAndView("jsonView");
        String errorString = null;

        if (jobIdStr != null) {
            try {
                long jobId = Long.parseLong(jobIdStr);
                job = jobManager.getJobById(jobId);
            } catch (NumberFormatException e) {
                logger.error("Error parsing job ID!");
            }
        }

        if (si == null) {
            errorString = "You are not logged in or your session has expired";
            logger.warn("ServiceInterface is null!");
        } else if (job == null) {
            errorString = "The requested job was not found.";
            logger.error(errorString);
        } else {
            logger.debug("Retrieving file list for job with ID "+jobIdStr+".");
            List<FileInformation> fileList = new ArrayList<FileInformation>();
            String handle = job.getHandle();

            if (handle != null && handle.length() > 0) {
                try {
                    JobObject grisuJob = new JobObject(si, handle);
                    String url = grisuJob.getJobDirectoryUrl();
                    DtoFolder folder = si.ls(url, 1);
                    for (DtoFile file : folder.getChildrenFiles()) {
                        fileList.add(new FileInformation(
                            file.getName(), file.getSize()));
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                    errorString = "There was an error getting the file listing";
                }
            }

            mav.addObject("files", fileList.toArray());
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
     * Sends the contents of a job file to the client.
     *
     * @param request The servlet request including a jobId parameter and a
     *                filename parameter
     * @param response The servlet response receiving the data
     *
     * @return null on success or the joblist view with an error parameter on
     *         failure.
     */
    public ModelAndView downloadFile(HttpServletRequest request,
                                     HttpServletResponse response) {

        ServiceInterface si = getGrisuService(request);
        final String jobIdStr = request.getParameter("jobId");
        final String fileName = request.getParameter("filename");
        VRLJob job = null;
        String errorString = null;

        if (jobIdStr != null) {
            try {
                long jobId = Long.parseLong(jobIdStr);
                job = jobManager.getJobById(jobId);
            } catch (NumberFormatException e) {
                logger.error("Error parsing job ID!");
            }
        }

        if (si == null) {
            errorString = "You are not logged in or your session has expired";
            logger.warn("ServiceInterface is null!");
        } else if (job == null) {
            errorString = "No/Invalid job specified";
            logger.error(errorString);
        } else if (fileName == null) {
            errorString = "No filename provided";
            logger.error(errorString);
        } else {
            logger.debug("Download "+fileName+" of job with ID "+jobIdStr+".");
            File f = null;
            try {
                JobObject grisuJob = new JobObject(si, job.getHandle());
                f = grisuJob.downloadAndCacheOutputFile(fileName);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }

            if (f == null || !f.canRead()) {
                logger.error("File "+f.getPath()+" not readable!");
                errorString = new String("File could not be read.");
            } else {
                response.setContentType("application/octet-stream");
                response.setHeader("Content-Disposition",
                        "attachment; filename=\""+fileName+"\"");

                try {
                    byte[] buffer = new byte[16384];
                    int count = 0;
                    OutputStream out = response.getOutputStream();
                    FileInputStream fin = new FileInputStream(f);
                    while ((count = fin.read(buffer)) != -1) {
                        out.write(buffer, 0, count);
                    }
                    out.flush();
                    return null;

                } catch (IOException e) {
                    errorString = new String("Could not send file: " +
                            e.getMessage());
                    logger.error(errorString);
                }
            }
        }

        // We only end up here in case of an error so return the message
        return new ModelAndView("joblist", "error", errorString);
    }

    /**
     * Sends the contents of one or more job files as a ZIP archive to the
     * client.
     *
     * @param request The servlet request including a jobId parameter and a
     *                files parameter with the filenames separated by comma
     * @param response The servlet response receiving the data
     *
     * @return null on success or the joblist view with an error parameter on
     *         failure.
     */
    public ModelAndView downloadAsZip(HttpServletRequest request,
                                      HttpServletResponse response) {

        ServiceInterface si = getGrisuService(request);
        String jobIdStr = request.getParameter("jobId");
        String filesParam = request.getParameter("files");
        VRLJob job = null;
        String errorString = null;

        if (jobIdStr != null) {
            try {
                long jobId = Long.parseLong(jobIdStr);
                job = jobManager.getJobById(jobId);
            } catch (NumberFormatException e) {
                logger.error("Error parsing job ID!");
            }
        }

        if (si == null) {
            errorString = "You are not logged in or your session has expired";
            logger.warn("ServiceInterface is null!");
        } else if (job == null) {
            errorString = "No/Invalid job specified";
            logger.error(errorString);
        } else if (filesParam == null) {
            errorString = "No filename(s) provided";
            logger.error(errorString);
        } else {
            String[] fileNames = filesParam.split(",");
            logger.debug("Archiving " + fileNames.length + " file(s) of job " +
                    jobIdStr);

            response.setContentType("application/zip");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"jobfiles.zip\"");

            try {
                JobObject grisuJob = new JobObject(si, job.getHandle());
                boolean readOneOrMoreFiles = false;
                ZipOutputStream zout = new ZipOutputStream(
                        response.getOutputStream());
                for (String fileName : fileNames) {
                    File f = grisuJob.downloadAndCacheOutputFile(fileName);
                    if (!f.canRead()) {
                        // if a file could not be read we go ahead and try the
                        // next one.
                        logger.error("File "+f.getPath()+" not readable!");
                    } else {
                        byte[] buffer = new byte[16384];
                        int count = 0;
                        zout.putNextEntry(new ZipEntry(fileName));
                        FileInputStream fin = new FileInputStream(f);
                        while ((count = fin.read(buffer)) != -1) {
                            zout.write(buffer, 0, count);
                        }
                        zout.closeEntry();
                        readOneOrMoreFiles = true;
                    }
                }
                if (readOneOrMoreFiles) {
                    zout.finish();
                    zout.flush();
                    zout.close();
                    return null;

                } else {
                    zout.close();
                    errorString = new String("Could not access the files!");
                    logger.error(errorString);
                }

            } catch (Exception e) {
                errorString = "Could not archive files";
                logger.error(e.getMessage());
            }
        }

        // We only end up here in case of an error so return the message
        return new ModelAndView("joblist", "error", errorString);
    }

    /**
     * Returns a JSON object containing an array of series that match the query
     * parameters.
     *
     * @param request The servlet request with query parameters
     * @param response The servlet response
     *
     * @return A JSON object with a series attribute which is an array of
     *         VRLSeries objects matching the criteria.
     */
    public ModelAndView querySeries(HttpServletRequest request,
                                    HttpServletResponse response) {

        String qUser = request.getParameter("qUser");
        String qName = request.getParameter("qSeriesName");
        String qDesc = request.getParameter("qSeriesDesc");

        if (qUser == null && qName == null && qDesc == null) {
            qUser = request.getRemoteUser();
            logger.debug("No query parameters provided. Will return "+qUser+"'s series.");
        }

        logger.debug("qUser="+qUser+", qName="+qName+", qDesc="+qDesc);
        List<VRLSeries> series = jobManager.querySeries(qUser, qName, qDesc);

        logger.debug("Returning list of "+series.size()+" series.");
        return new ModelAndView("jsonView", "series", series);
    }

    /**
     * Returns a JSON object containing an array of jobs for the given series.
     *
     * @param request The servlet request including a seriesId parameter
     * @param response The servlet response
     *
     * @return A JSON object with a jobs attribute which is an array of
     *         job properties.
     */
    public ModelAndView listJobs(HttpServletRequest request,
                                 HttpServletResponse response) {

        ServiceInterface si = getGrisuService(request);
        String seriesIdStr = request.getParameter("seriesId");
        ModelAndView mav = new ModelAndView("jsonView");
        String errorString = null;
        List<VRLJob> seriesJobs = null;
        long seriesId = -1;

        if (seriesIdStr != null) {
            try {
                seriesId = Long.parseLong(seriesIdStr);
                seriesJobs = jobManager.getSeriesJobs(seriesId);
            } catch (NumberFormatException e) {
                logger.error("Error parsing series ID '"+seriesIdStr+"'");
            }
        }

        if (si == null) {
            errorString = "You are not logged in or your session has expired";
            logger.warn("ServiceInterface is null!");
        } else if (seriesJobs == null) {
            errorString = new String("No/invalid series specified");
            logger.warn(errorString);
        } else {
            GrisuRegistry registry = GrisuRegistryManager.getDefault(si);
            JSONArray jsJobs = (JSONArray)JSONSerializer.toJSON(seriesJobs);
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
                        String version = grisuJob.getApplicationVersion();
                        Long submitDate = Long.parseLong(grisuJob
                            .getJobProperty(Constants.SUBMISSION_TIME_KEY));
                        job.put("memory", grisuJob.getMemory()/(1024L*1024L));
                        job.put("numProcs", grisuJob.getCpus());
                        job.put("queue", queue);
                        job.put("site", site);
                        job.put("status", status);
                        job.put("submitDate", submitDate);
                        job.put("version", version);
                        job.put("walltime", grisuJob
                            .getWalltimeInSeconds()/60);
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                } else {
                    // set some sane defaults
                    job.put("memory", 30000);
                    job.put("numProcs", 2);
                    job.put("walltime", 24*60);
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
     * Re-submits a single job.
     *
     * @param request The servlet request including a jobId parameter
     * @param response The servlet response
     *
     * @return The scriptbuilder view prepared to resubmit the job or the
     *         joblist view with an error parameter if the job was not found.
     */
    public ModelAndView resubmitJob(HttpServletRequest request,
                                    HttpServletResponse response) {

        String jobIdStr = request.getParameter("jobId");
        VRLJob job = null;

        if (jobIdStr != null) {
            try {
                long jobId = Long.parseLong(jobIdStr);
                job = jobManager.getJobById(jobId);
            } catch (NumberFormatException e) {
                logger.error("Error parsing job ID!");
            }
        } else {
            logger.warn("No job ID specified!");
        }

        if (job == null) {
            final String errorString = "Could not retrieve job details!";
            logger.error(errorString);
            return new ModelAndView("joblist", "error", errorString);
        }

        logger.info("Re-submitting job " + jobIdStr + ".");
        request.getSession().setAttribute("resubmitJob", jobIdStr);
        return useScript(request, response);
    }

    /**
     * Allows the user to edit a copy of an input script from a previous job
     * and use it for a new job.
     *
     * @param request The servlet request including a jobId parameter
     * @param response The servlet response
     *
     * @return The scriptbuilder model and view for editing the script or
     *         the joblist model and view with an error parameter if the job
     *         or file was not found.
     */
    public ModelAndView useScript(HttpServletRequest request,
                                  HttpServletResponse response) {

        ServiceInterface si = getGrisuService(request);
        String jobIdStr = request.getParameter("jobId");
        VRLJob job = null;
        String handle = null;
        String errorString = null;
        String scriptFileName = null;
        File sourceFile = null;

        if (jobIdStr != null) {
            try {
                long jobId = Long.parseLong(jobIdStr);
                job = jobManager.getJobById(jobId);
                handle = job.getHandle();
            } catch (NumberFormatException e) {
                logger.error("Error parsing job ID!");
            }
        }

        if (si == null) {
            errorString = "You are not logged in or your session has expired";
            logger.warn("ServiceInterface is null!");
        } else if (job == null) {
            errorString = new String("No/Invalid job specified");
            logger.error("job is null!");
        } else if (handle == null || handle.length() == 0) {
            errorString = new String("Could not access job files");
            logger.error("handle is null!");
        } else {
            scriptFileName = job.getScriptFile();
            try {
                JobObject grisuJob = new JobObject(si, handle);
                final String url = grisuJob.getJobDirectoryUrl();
                sourceFile = grisuJob.downloadAndCacheOutputFile(
                        scriptFileName);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }

            if (sourceFile == null || !sourceFile.canRead()) {
                errorString = new String("Script file could not be read.");
                logger.error("File "+scriptFileName+" not readable!");
            }
        }

        if (errorString == null) {
            logger.debug("Copying script file of job " + jobIdStr + " to temp.");
            String tempDir = System.getProperty("java.io.tmpdir");
            File tempScript = new File(tempDir+File.separator+scriptFileName);
            boolean success = Util.copyFile(sourceFile, tempScript);
            if (success) {
                tempScript.deleteOnExit();
            } else {
                errorString = new String("Script file could not be read.");
                logger.error(errorString);
            }
        }

        if (errorString != null) {
            request.getSession().removeAttribute("resubmitJob");
            return new ModelAndView("joblist", "error", errorString);
        }

        request.getSession().setAttribute("scriptFile", scriptFileName);
        return new ModelAndView(
                new RedirectView("/scriptbuilder.html", true, false, false));
    }
}

