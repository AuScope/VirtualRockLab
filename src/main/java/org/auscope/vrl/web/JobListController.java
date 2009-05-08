package org.auscope.vrl.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.auscope.vrl.FileInformation;
import org.auscope.vrl.GridAccessController;
import org.auscope.vrl.Util;
import org.auscope.vrl.VRLJob;
import org.auscope.vrl.VRLJobManager;
import org.auscope.vrl.VRLSeries;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;
import org.springframework.web.servlet.view.RedirectView;

/**
 *
 */
public class JobListController extends MultiActionController {

    protected final Log logger = LogFactory.getLog(getClass());

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
            HttpServletResponse response) throws Exception {

        logger.info("No/invalid action parameter; returning default view");
        return new ModelAndView("joblist");
    }

    /**
     * Kills the job given by its reference.
     * 
     * @param request The servlet request including a jobId parameter
     * @param response The servlet response
     *
     * @return A JSON object with a success attribute and an error attribute
     *         in case the job was not found in the job manager.
     */
    public ModelAndView killJob(HttpServletRequest request,
                                HttpServletResponse response) {

        String jobIdStr = request.getParameter("jobId");
        logger.info("Cancelling job with ID "+jobIdStr);
        int jobId;
        VRLJob job = null;
        ModelAndView mav = new ModelAndView("jsonView");

        try {
            jobId = Integer.parseInt(jobIdStr);
            job = jobManager.getJobById(jobId);
        } catch (NumberFormatException e) {
            logger.error("Error parsing job ID!");
        }

        if (job == null) {
            final String errorString = "Requested job not in job manager! Not cancelling.";
            logger.error(errorString);
            mav.addObject("error", errorString);
            mav.addObject("success", false);

        } else {
            String newState = gridAccess.killJob(job.getReference());
            if (newState == null)
                newState = "Unknown";
            logger.info("New job state: "+newState);

            job.setStatus(newState);
            jobManager.saveJob(job);
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

        String seriesIdStr = request.getParameter("seriesId");
        logger.info("Cancelling jobs of series "+seriesIdStr);
        int seriesId;
        List<VRLJob> jobs = null;
        ModelAndView mav = new ModelAndView("jsonView");

        try {
            seriesId = Integer.parseInt(seriesIdStr);
            jobs = jobManager.getSeriesJobs(seriesId);
        } catch (NumberFormatException e) {
            logger.error("Error parsing series ID!");
        }

        if (jobs == null) {
            final String errorString = "Requested series not in job manager!";
            logger.error(errorString);
            mav.addObject("error", errorString);
            mav.addObject("success", false);

        } else {
            for (VRLJob job : jobs) {
                String oldStatus = job.getStatus();
                if (oldStatus.equals("Failed") || oldStatus.equals("Done")) {
                    logger.info("Skipping finished job "+job.getId());
                    continue;
                }
                logger.info("Killing job with ID "+job.getId());
                String newState = gridAccess.killJob(job.getReference());
                if (newState == null)
                    newState = "Unknown";
                logger.info("New job state: "+newState);

                job.setStatus(newState);
                jobManager.saveJob(job);
            }
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

        String jobIdStr = request.getParameter("jobId");
        logger.info("Getting file list for job with ID "+jobIdStr+".");
        int jobId;
        VRLJob job = null;
        ModelAndView mav = new ModelAndView("jsonView");

        try {
            jobId = Integer.parseInt(jobIdStr);
            job = jobManager.getJobById(jobId);
        } catch (NumberFormatException e) {
            logger.error("Error parsing job ID!");
        }

        if (job == null) {
            final String errorString = "Requested job not in job manager!";
            logger.error(errorString);
            mav.addObject("error", errorString);

        } else {
            FileInformation[] fileDetails = null;

            File dir = new File(job.getOutputDir());
            File[] files = dir.listFiles();
            if (files != null) {
                Arrays.sort(files);

                fileDetails = new FileInformation[files.length];
                for (int i=0; i<files.length; i++) {
                    fileDetails[i] = new FileInformation(
                            files[i].getName(), files[i].length());
                }
            }

            mav.addObject("files", fileDetails);
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
     * @return null on success or the joblist Model and View with an error
     *         parameter on failure
     */
    public ModelAndView downloadFile(HttpServletRequest request,
                                     HttpServletResponse response) {

        String jobIdStr = request.getParameter("jobId");
        String fileName = request.getParameter("filename");
        logger.info("Download "+fileName+" of job with ID "+jobIdStr+".");

        int jobId;
        VRLJob job = null;
        String errorString = null;

        try {
            jobId = Integer.parseInt(jobIdStr);
            job = jobManager.getJobById(jobId);
        } catch (NumberFormatException e) {
            logger.error("Error parsing job ID!");
        }

        if (job != null && fileName != null) {
            File f = new File(job.getOutputDir()+File.separator+fileName);
            if (!f.canRead()) {
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

        if (errorString == null) {
            if (job == null) {
                errorString = new String("Invalid job specified!");
                logger.error(errorString);
            } else if (fileName == null) {
                errorString = new String("No filename provided!");
                logger.error(errorString);
            } else {
                // should never get here
                errorString = new String("Something went wrong.");
                logger.error(errorString);
            }
        }
        return new ModelAndView("joblist", "error", errorString);
    }

    /**
     * Sends the contents of multiple job files as a ZIP archive to the client.
     * 
     * @param request The servlet request including a jobId parameter and a
     *                files parameter with the filenames separated by comma
     * @param response The servlet response receiving the data
     *
     * @return null on success or the joblist Model and View with an error
     *         parameter on failure
     */
    public ModelAndView downloadMulti(HttpServletRequest request,
                                      HttpServletResponse response) {

        String jobIdStr = request.getParameter("jobId");
        String filesParam = request.getParameter("files");
        int jobId;
        VRLJob job = null;
        String errorString = null;

        try {
            jobId = Integer.parseInt(jobIdStr);
            job = jobManager.getJobById(jobId);
        } catch (NumberFormatException e) {
            logger.error("Error parsing job ID!");
        }

        if (job != null && filesParam != null) {
            String[] fileNames = filesParam.split(",");
            logger.info("Archiving " + fileNames.length + " files of job " +
                    jobIdStr);

            response.setContentType("application/zip");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"jobfiles.zip\"");

            try {
                boolean readOneOrMoreFiles = false;
                ZipOutputStream zout = new ZipOutputStream(
                        response.getOutputStream());
                for (String fileName : fileNames) {
                    File f = new File(job.getOutputDir()+File.separator+fileName);
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
                    errorString = new String("None of the files could be read!");
                    logger.error(errorString);
                }

            } catch (IOException e) {
                errorString = new String("Could not create ZIP file: " +
                        e.getMessage());
                logger.error(errorString);
            }
        }

        if (errorString == null) {
            if (job == null) {
                errorString = new String("Invalid job specified!");
                logger.error(errorString);
            } else if (filesParam == null) {
                errorString = new String("No filename(s) provided!");
                logger.error(errorString);
            } else {
                // should never get here
                errorString = new String("Something went wrong.");
                logger.error(errorString);
            }
        }
        return new ModelAndView("joblist", "error", errorString);
    }

    /**
     * Returns a JSON object containing an array of series matching the query
     * parameters.
     * 
     * @param request The servlet request
     * @param response The servlet response
     *
     * @return A JSON object with a series attribute which is an array of
     *         VRLSeries objects.
     */
    public ModelAndView querySeries(HttpServletRequest request,
                                    HttpServletResponse response) {

        String qUser = request.getParameter("qUser");
        String qName = request.getParameter("qSeriesName");
        String qDesc = request.getParameter("qSeriesDesc");

        if (qUser == null && qName == null && qDesc == null) {
            qUser = request.getRemoteUser();
            logger.info("No query parameters provided. Will return "+qUser+"'s series.");
        }

        logger.info("Querying series...");
        logger.info("qUser="+qUser+", qName="+qName+", qDesc="+qDesc);
        List<VRLSeries> result = jobManager.querySeries(qUser, qName, qDesc);

        logger.info("Returning list of "+result.size()+" series.");
        return new ModelAndView("jsonView", "series", result);
    }

    /**
     * Returns a JSON object containing an array of jobs for the given series.
     * 
     * @param request The servlet request
     * @param response The servlet response
     *
     * @return A JSON object with a jobs attribute which is an array of
     *         VRLJob objects.
     */
    public ModelAndView listJobs(HttpServletRequest request,
                                 HttpServletResponse response) {

        String seriesIdStr = request.getParameter("seriesId");
        List<VRLJob> seriesJobs = null;
        ModelAndView mav = new ModelAndView("jsonView");

        try {
            int seriesId = Integer.parseInt(seriesIdStr);
            seriesJobs = jobManager.getSeriesJobs(seriesId);
        } catch (NumberFormatException e) {
            logger.error("Error parsing series ID '"+seriesIdStr+"'");
        }

        if (seriesJobs != null) {
            logger.info("Updating status of jobs attached to series " +
                    seriesIdStr + ".");
            for (VRLJob j : seriesJobs) {
                String state = j.getStatus();
                if (!state.equals("Done") && !state.equals("Failed")) {
                    String newState = gridAccess.retrieveJobStatus(
                            j.getReference());
                    if (newState != null) {
                        j.setStatus(newState);
                        jobManager.saveJob(j);
                    }
                }
            }
            mav.addObject("jobs", seriesJobs);
        }

        logger.info("Returning series job list");
        return mav;
    }

    /**
     * Re-submits a single job.
     * 
     * @param request The servlet request including a jobId parameter
     * @param response The servlet response
     *
     * @return The gridsubmit model and view prepared to resubmit the job or
     *         the joblist model and view with an error parameter if the job
     *         was not found.
     */
    public ModelAndView resubmitJob(HttpServletRequest request,
                                    HttpServletResponse response) {

        String jobIdStr = request.getParameter("jobId");
        logger.info("Re-submitting job " + jobIdStr + ".");
        VRLJob job = null;

        try {
            int jobId = Integer.parseInt(jobIdStr);
            job = jobManager.getJobById(jobId);
        } catch (NumberFormatException e) {
            logger.error("Error parsing job ID!");
        }

        if (job == null) {
            final String errorString = "Requested job not in job manager!";
            logger.error(errorString);
            return new ModelAndView("joblist", "error", errorString);
        }

        ModelAndView mav =  new ModelAndView(
                new RedirectView("gridsubmit.html"));
        mav.addObject("resubmitJob", jobIdStr);
        return mav;
    }

    /**
     * Re-submits a job series.
     * 
     * @param request The servlet request including a seriesId parameter
     * @param response The servlet response
     *
     * @return The gridsubmit model and view prepared to resubmit the series or
     *         the joblist model and view with an error parameter if the series
     *         was not found.
     */
    public ModelAndView resubmitSeries(HttpServletRequest request,
                                       HttpServletResponse response) {

        String seriesIdStr = request.getParameter("seriesId");
        logger.info("Re-submitting series " + seriesIdStr + ".");
        VRLSeries series = null;

        try {
            int seriesId = Integer.parseInt(seriesIdStr);
            series = jobManager.getSeriesById(seriesId);
        } catch (NumberFormatException e) {
            logger.error("Error parsing series ID!");
        }

        if (series == null) {
            final String errorString = "Requested series not in job manager!";
            logger.error(errorString);
            return new ModelAndView("joblist", "error", errorString);
        }

        ModelAndView mav =  new ModelAndView(
                new RedirectView("gridsubmit.html"));
        mav.addObject("resubmitSeries", seriesIdStr);
        return mav;
    }

    /**
     * Re-use existing script
     * 
     * @param request The servlet request including a jobId parameter and a
     *                filename parameter
     * @param response The servlet response
     *
     * @return The scriptbuilder model and view for editing the script or
     *         the joblist model and view with an error parameter if the job
     *         or file was not found.
     */
    public ModelAndView useScript(HttpServletRequest request,
                                  HttpServletResponse response) {

        String jobIdStr = request.getParameter("jobId");
        String fileName = request.getParameter("filename");
        logger.info("Re-using script file of job " + jobIdStr + ".");

        int jobId;
        VRLJob job = null;
        String errorString = null;

        try {
            jobId = Integer.parseInt(jobIdStr);
            job = jobManager.getJobById(jobId);
        } catch (NumberFormatException e) {
            logger.error("Error parsing job ID!");
        }

        if (job == null) {
            errorString = new String("Requested job not in job manager!");
            logger.error(errorString);
        } else if (fileName == null) {
            errorString = new String("No filename provided!");
            logger.error(errorString);
        }

        File sourceFile = new File(job.getOutputDir()+File.separator+fileName);
        if (!sourceFile.canRead()) {
            errorString = new String("Script file could not be read.");
            logger.error("File "+sourceFile.getPath()+" not readable!");
        }

        if (errorString == null) {
            String tempDir = System.getProperty("java.io.tmpdir");
            File tempScript = new File(tempDir+File.separator+fileName);
            boolean success = Util.copyFile(sourceFile, tempScript);
            if (success) {
                tempScript.deleteOnExit();
            } else {
                errorString = new String("Script file could not be read.");
                logger.error(errorString);
            }
        }

        if (errorString != null) {
            return new ModelAndView("joblist", "error", errorString);
        }

        ModelAndView mav =  new ModelAndView(
                new RedirectView("scriptbuilder.html"));
        mav.addObject("usescript", fileName);
        return mav;
    }
}

