/*
 * This file is part of the AuScope Virtual Rock Lab (VRL) project.
 * Copyright (c) 2010 The University of Queensland, ESSCC
 *
 * Licensed under the terms of the GNU Lesser General Public License.
 */
package org.auscope.vrl.web;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.auscope.vrl.VRLJobManager;
import org.auscope.vrl.VRLJob;
import org.auscope.vrl.FileInformation;
import org.auscope.vrl.Util;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;
import org.springframework.web.servlet.view.RedirectView;

import org.vpac.grisu.control.ServiceInterface;
import org.vpac.grisu.frontend.model.job.JobObject;
import org.vpac.grisu.model.FileManager;
import org.vpac.grisu.model.GrisuRegistryManager;
import org.vpac.grisu.model.dto.DtoFile;
import org.vpac.grisu.model.dto.DtoFolder;

/**
 * Controller for file related actions.
 *
 * @author Cihan Altinay
 */
public class FileActionController extends MultiActionController {

    /** Logger for this class */
    private final Log logger = LogFactory.getLog(getClass());

    private static final String viewName = "home";
    /** Maximum file size allowed for inline display = 10 kB */
    private static final long MAX_FILE_SIZE_FOR_DISPLAY = 10240L;
    /** Location where job files should be archived to (ARCS Data Fabric) */
    private static final String ARCHIVE_URL =
        "gsiftp://arcs-df.vpac.org:2810/~/grisu_archive";
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
     * Archives the output files of a job to the default archive location
     *
     * @param request The servlet request
     * @param response The servlet response
     *
     * @return A JSON object
     */
    public ModelAndView archiveFiles(HttpServletRequest request,
                                     HttpServletResponse response) {

        final ServiceInterface si = getGrisuService(request);
        Long seriesId = (Long)request.getSession().getAttribute("seriesId");
        final String jobStr = request.getParameter("job");
        final ModelAndView mav = new ModelAndView("jsonView");
        String errorString = null;
        VRLJob job = null;

        if (jobStr != null) {
            try {
                long jobId = Long.parseLong(jobStr);
                job = jobManager.getJobById(jobId);
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
        } else if (job.getHandle() == null || job.getHandle().length() == 0) {
            errorString = ErrorMessages.NULL_HANDLE;
            logger.warn("job handle is null!");
        } else {
            try {
                logger.debug("Archiving files of job " + jobStr);
                String archiveUrl = si.archiveJob(job.getHandle(), ARCHIVE_URL);
                logger.debug("Archive location: " + archiveUrl);
                // grisu deletes the job from its database after archiving so
                // discard handle
                job.setHandle(null);
                job.setOutputUrl(archiveUrl);
                jobManager.saveJob(job);
            } catch (Exception e) {
                errorString = ErrorMessages.INTERNAL_ERROR;
                logger.error(e.getMessage());
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
     * Copies one or more output files from one job to another (or the same)
     * job's input directory within the active series.
     *
     * @param request The servlet request
     * @param response The servlet response
     *
     * @return A JSON object which indicates success or failure.
     */
    public ModelAndView copyFiles(HttpServletRequest request,
                                  HttpServletResponse response) {

        final ServiceInterface si = getGrisuService(request);
        File seriesDir = (File)request.getSession().getAttribute("seriesDir");
        final String srcJobStr = request.getParameter("srcJob");
        final String destJobStr = request.getParameter("destJob");
        final String filesParam = request.getParameter("files");
        final String overwriteParam = request.getParameter("overwrite");
        final ModelAndView mav = new ModelAndView("jsonView");
        String errorString = null;
        VRLJob srcJob = null;
        VRLJob destJob = null;

        if (srcJobStr != null && destJobStr != null) {
            try {
                long jobId = Long.parseLong(srcJobStr);
                srcJob = jobManager.getJobById(jobId);
                jobId = Long.parseLong(destJobStr);
                destJob = jobManager.getJobById(jobId);
            } catch (NumberFormatException e) {
                logger.warn("Error parsing job ID " + srcJobStr
                        + "/" + destJobStr);
            }
        }

        if (si == null) {
            errorString = ErrorMessages.SESSION_EXPIRED;
            logger.warn("ServiceInterface is null!");
        } else if (seriesDir == null) {
            errorString = ErrorMessages.NO_SERIES;
            logger.warn(errorString);
        } else if (srcJob == null || destJob == null) {
            errorString = ErrorMessages.INVALID_JOB;
            logger.warn("srcJob or destJob is null!");
        } else if (filesParam == null) {
            errorString = ErrorMessages.MISSING_PARAMETER;
            logger.warn("No files specified!");
        } else if (srcJob.getOutputUrl() == null
                || srcJob.getOutputUrl().length() == 0) {
            errorString = ErrorMessages.NULL_HANDLE;
            logger.warn("source job URL is null!");
        } else {
            String[] fileNames = filesParam.split(",");
            List<String> list = new ArrayList<String>();
            for (String fileName : fileNames) {
                if (fileName.equals(Util.sanitizeSubPath(fileName))) {
                    list.add(new String(fileName));
                } else {
                    logger.warn("Invalid file "+fileName);
                    continue;
                }
            }
            if (list.size() == 0) {
                errorString = ErrorMessages.INVALID_FILENAME;
            } else {
                try {
                    final String url = srcJob.getOutputUrl();
                    File destDir = new File(seriesDir, destJobStr);
                    final boolean overwrite = "on".equals(overwriteParam);
                    logger.debug("Transferring " + list.size()
                            + " files from remote to " + destDir.getPath()
                            + ", overwrite="+overwrite);
                    FileManager fm = GrisuRegistryManager.getDefault(si)
                        .getFileManager();
                    Iterator<String> it = list.iterator();
                    while (it.hasNext()) {
                        final String fileName = it.next();
                        fm.cp(url+"/"+fileName, destDir.getPath(), overwrite);
                        try {
                            File destFile = new File(destDir, fileName);
                            jobManager.addFile(destFile);
                        } catch (Exception e) {
                            // ignore errors when adding files -
                            // they might be already in version control
                        }
                    }
                    //si.cp(DtoStringList.fromStringList(list),
                    //        "file://"+jobDir.getPath(), overwrite, true);
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
     * Deletes one or more files from the active series.
     *
     * @param request The servlet request
     * @param response The servlet response
     *
     * @return A JSON object
     */
    public ModelAndView deleteFiles(HttpServletRequest request,
                                    HttpServletResponse response) {

        File seriesDir = (File)request.getSession().getAttribute("seriesDir");
        final String jobStr = request.getParameter("job");
        final String filesParam = request.getParameter("files");
        final ModelAndView mav = new ModelAndView("jsonView");
        String errorString = null;
        VRLJob job = null;

        if (jobStr != null) {
            try {
                long jobId = Long.parseLong(jobStr);
                job = jobManager.getJobById(jobId);
            } catch (NumberFormatException e) {
                logger.warn("Error parsing job ID " + jobStr);
            }
        }

        if (getGrisuService(request) == null) {
            errorString = ErrorMessages.SESSION_EXPIRED;
            logger.warn("ServiceInterface is null!");
        } else if (seriesDir == null) {
            errorString = ErrorMessages.NO_SERIES;
            logger.warn(errorString);
        } else if (job == null) {
            errorString = ErrorMessages.INVALID_JOB;
            logger.warn("job is null!");
        } else if (filesParam == null) {
            errorString = ErrorMessages.MISSING_PARAMETER;
            logger.warn("No files specified!");
        } else {
            String[] fileNames = filesParam.split(",");
            List<File> list = new ArrayList<File>();
            File jobDir = new File(seriesDir, jobStr);
            for (String fileName : fileNames) {
                if (fileName.equals(Util.sanitizeSubPath(fileName))) {
                    list.add(new File(jobDir, fileName));
                } else {
                    logger.warn("Invalid file "+fileName);
                    continue;
                }
            }
            if (list.size() == 0) {
                errorString = ErrorMessages.INVALID_FILENAME;
            } else {
                logger.debug("Deleting "+list.size()+" files");
                try {
                    jobManager.deleteFiles(list.toArray(
                                new File[list.size()]));
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
     * Sends the contents of one or more files to the client.
     * If a single file is requested its content is sent to the client while
     * multiple files are archived and sent as ZIP.
     *
     * @param request The servlet request including a files parameter with
     *                the filenames separated by comma
     * @param response The servlet response receiving the data
     *
     * @return null on success or the default view with an error parameter on
     *         failure.
     */
    public ModelAndView downloadFiles(HttpServletRequest request,
                                      HttpServletResponse response) {

        final ServiceInterface si = getGrisuService(request);
        File seriesDir = (File)request.getSession().getAttribute("seriesDir");
        final String jobStr = request.getParameter("job");
        final String remoteStr = request.getParameter("remote");
        final String filesParam = request.getParameter("files");
        final boolean remote = (remoteStr != null && remoteStr.length() > 0);
        String errorString = null;
        VRLJob job = null;

        if (jobStr != null) {
            try {
                long jobId = Long.parseLong(jobStr);
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
        } else if (filesParam == null) {
            errorString = ErrorMessages.MISSING_PARAMETER;
            logger.warn("No files specified!");
        } else {
            String[] fileNames = filesParam.split(",");
            File jobDir = new File(seriesDir, jobStr);
            // single file is sent raw
            if (fileNames.length == 1) {
                String fileName = fileNames[0];
                File f = null;

                if (!fileName.equals(Util.sanitizeSubPath(fileName))) {
                    errorString = ErrorMessages.INVALID_FILENAME;
                    logger.warn("Invalid file " + fileName + " requested!");
                } else {
                    if (remote) {
                        try {
                            String url = job.getOutputUrl() + "/" + fileName;
                            f = GrisuRegistryManager.getDefault(si)
                                .getFileManager().downloadFile(url);
                        } catch (Exception e) {
                            errorString = ErrorMessages.INTERNAL_ERROR;
                            logger.error(e.getMessage(), e);
                        }
                    } else {
                        f = new File(jobDir, fileName);
                        if (!f.canRead()) {
                            errorString = ErrorMessages.INTERNAL_ERROR;
                            logger.error("File "+f.getPath()+" not readable!");
                        }
                    }
                }

                if (errorString == null) {
                    logger.debug("Download "+f.getPath());
                    try {
                        sendSingleFile(f, response);
                        return null;
                    } catch (IOException e) {
                        errorString = ErrorMessages.INTERNAL_ERROR;
                        logger.error(e.getMessage(), e);
                    }
                }
            } else { // multiple files are put in an archive
                logger.debug("Archiving " + fileNames.length + " file(s)");

                response.setContentType("application/zip");
                response.setHeader("Content-Disposition",
                        "attachment; filename=\"jobfiles.zip\"");

                try {
                    boolean readOneOrMoreFiles = false;
                    ZipOutputStream zout = new ZipOutputStream(
                            response.getOutputStream());
                    for (String fileName : fileNames) {
                        if (!fileName.equals(Util.sanitizeSubPath(fileName))) {
                            logger.warn("Invalid file "+fileName+" requested!");
                            continue;
                        }
                        File f = null;
                        if (remote) {
                            String url = job.getOutputUrl() + "/" + fileName;
                            f = GrisuRegistryManager.getDefault(si)
                                .getFileManager().downloadFile(url);
                        } else {
                            f = new File(jobDir, fileName);
                        }
                        if (!f.canRead()) {
                            // if a file could not be read we go ahead and try
                            // the next one.
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
                        zout.finish();
                        errorString = ErrorMessages.INTERNAL_ERROR;
                        logger.error(errorString);
                    }

                } catch (Exception e) {
                    errorString = ErrorMessages.INTERNAL_ERROR;
                    logger.error(e.getMessage(), e);
                }
            }
        }

        // We only end up here in case of an error so return the message.
        // TODO: This may not actually work if the ZipOutputStream has been
        // opened already
        return new ModelAndView(viewName, "error", errorString);
    }

    /**
     * Sends the contents of a job file to the client.
     *
     * @param request The servlet request including a filename parameter
     * @param response The servlet response
     *
     * @return A JSON object with a sourceText attribute holding the file
     *         contents. If there is no active series or the file cannot
     *         be read the JSON object will contain an error attribute
     *         indicating the error.
     */
    public ModelAndView getFileContents(HttpServletRequest request,
                                        HttpServletResponse response) {

        File seriesDir = (File)request.getSession().getAttribute("seriesDir");
        final String jobStr = request.getParameter("job");
        final String fileName = request.getParameter("filename");
        final ModelAndView mav = new ModelAndView("jsonView");
        String errorString = null;
        VRLJob job = null;
        String sourceText = null;

        if (jobStr != null) {
            try {
                long jobId = Long.parseLong(jobStr);
                job = jobManager.getJobById(jobId);
            } catch (NumberFormatException e) {
                logger.warn("Error parsing job ID " + jobStr);
            }
        }

        if (getGrisuService(request) == null) {
            errorString = ErrorMessages.SESSION_EXPIRED;
            logger.warn("ServiceInterface is null!");
        } else if (seriesDir == null) {
            errorString = ErrorMessages.NO_SERIES;
            logger.warn(errorString);
        } else if (job == null) {
            errorString = ErrorMessages.INVALID_JOB;
            logger.warn("job is null!");
        } else if (fileName == null) {
            errorString = ErrorMessages.MISSING_PARAMETER;
            logger.warn("No filename specified!");
        } else if (!fileName.equals(Util.sanitizeSubPath(fileName))) {
            errorString = ErrorMessages.INVALID_FILENAME;
            logger.warn("Invalid file " + fileName + " requested!");
        } else {
            try {
                File f = new File(new File(seriesDir, jobStr), fileName);
                if (!f.canRead() || !f.isFile()) {
                    errorString = ErrorMessages.INTERNAL_ERROR;
                    logger.error("File "+f.getPath()+" not readable!");
                } else if (f.length() > MAX_FILE_SIZE_FOR_DISPLAY) {
                    errorString = ErrorMessages.FILE_TOO_BIG;
                    logger.warn("File "+f.getPath()+" too big for display!");
                } else {
                    logger.debug("Reading "+f.getPath());
                    BufferedReader input = new BufferedReader(
                            new FileReader(f));
                    StringBuffer contents = new StringBuffer();
                    String line = null;
                    while ((line = input.readLine()) != null) {
                        contents.append(line).append(
                                System.getProperty("line.separator"));
                    }
                    input.close();
                    sourceText = contents.toString();
                }
            } catch (IOException e) {
                errorString = ErrorMessages.INTERNAL_ERROR;
                logger.error(e.getMessage(), e);
            }
        }

        if (errorString != null) {
            mav.addObject("error", errorString);
            mav.addObject("success", false);
        } else {
            mav.addObject("fileName", fileName);
            mav.addObject("sourceText", sourceText);
            mav.addObject("success", true);
        }

        return mav;
    }

    /**
     * Returns a JSON object containing a boolean which indicates whether the
     * current series has modifications.
     *
     * @param request The servlet request
     * @param response The servlet response
     *
     * @return A JSON object with a modified attribute.
     *         If there is no active series the JSON object will contain
     *         an error attribute indicating the error.
     */
    public ModelAndView isModified(HttpServletRequest request,
                                   HttpServletResponse response) {

        File seriesDir = (File)request.getSession().getAttribute("seriesDir");
        final ModelAndView mav = new ModelAndView("jsonView");
        String errorString = null;

        if (getGrisuService(request) == null) {
            errorString = ErrorMessages.SESSION_EXPIRED;
            logger.warn("ServiceInterface is null!");
        } else if (seriesDir == null) {
            errorString = ErrorMessages.NO_SERIES;
            logger.warn(errorString);
        } else {
            try {
                logger.debug("Checking for local modifications.");
                mav.addObject("modified",
                        jobManager.hasModifications(seriesDir));
            } catch (Exception e) {
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
     * Returns a JSON object containing an array of filenames for the active
     * series.
     *
     * @param request The servlet request
     * @param response The servlet response
     *
     * @return A JSON object with a files attribute which is an array of
     *         FileInformation objects. If there is no active series
     *         the JSON object will contain an error attribute indicating
     *         the error.
     */
    public ModelAndView listFiles(HttpServletRequest request,
                                  HttpServletResponse response) {

        final ServiceInterface si = getGrisuService(request);
        File seriesDir = (File)request.getSession().getAttribute("seriesDir");
        Long seriesId = (Long)request.getSession().getAttribute("seriesId");
        final String jobStr = request.getParameter("job");
        final String remoteStr = request.getParameter("remote");
        final boolean remote = (remoteStr != null && remoteStr.length() > 0);
        final ModelAndView mav = new ModelAndView("jsonView");
        String errorString = null;
        VRLJob job = null;

        if (jobStr != null) {
            try {
                long jobId = Long.parseLong(jobStr);
                job = jobManager.getJobById(jobId);
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
        } else {
            List<FileInformation> fileList = new ArrayList<FileInformation>();

            try {
                // check if the request is for a remote or local listing
                if (remote) {
                    if (job.getOutputUrl() != null
                            && job.getOutputUrl().length() > 0) {
                        logger.debug("Retrieving remote file list for job "
                                + jobStr + " of series "
                                + seriesId.toString());
                        DtoFolder folder = si.ls(job.getOutputUrl(), 1);
                        for (DtoFile file : folder.getChildrenFiles()) {
                            fileList.add(new FileInformation(
                                    file.getName(), file.getSize(), ""));
                        }
                    }
                } else {
                    logger.debug("Generating file list for job " + jobStr
                            + " of series " + seriesId.toString());
                    File jobDir = new File(seriesDir, jobStr);
                    FileInformation[] files = jobManager.listFiles(jobDir);
                    fileList = Arrays.asList(files);
                }
            } catch (Exception e) {
                errorString = ErrorMessages.INTERNAL_ERROR;
                logger.error(e.getMessage(), e);
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
     * Reverts one or more files from the active series to the original state.
     *
     * @param request The servlet request
     * @param response The servlet response
     *
     * @return A JSON object
     */
    public ModelAndView revertFiles(HttpServletRequest request,
                                    HttpServletResponse response) {

        File seriesDir = (File)request.getSession().getAttribute("seriesDir");
        final String jobStr = request.getParameter("job");
        final String filesParam = request.getParameter("files");
        final ModelAndView mav = new ModelAndView("jsonView");
        String errorString = null;
        VRLJob job = null;

        if (jobStr != null) {
            try {
                long jobId = Long.parseLong(jobStr);
                job = jobManager.getJobById(jobId);
            } catch (NumberFormatException e) {
                logger.warn("Error parsing job ID " + jobStr);
            }
        }

        if (getGrisuService(request) == null) {
            errorString = ErrorMessages.SESSION_EXPIRED;
            logger.warn("ServiceInterface is null!");
        } else if (seriesDir == null) {
            errorString = ErrorMessages.NO_SERIES;
            logger.warn(errorString);
        } else if (job == null) {
            errorString = ErrorMessages.INVALID_JOB;
            logger.warn("job is null!");
        } else if (filesParam == null) {
            errorString = ErrorMessages.MISSING_PARAMETER;
            logger.warn("No files specified!");
        } else {
            String[] fileNames = filesParam.split(",");
            List<File> list = new ArrayList<File>();
            File jobDir = new File(seriesDir, jobStr);
            for (String fileName : fileNames) {
                if (fileName.equals(Util.sanitizeSubPath(fileName))) {
                    list.add(new File(jobDir, fileName));
                } else {
                    logger.warn("Invalid file "+fileName);
                    continue;
                }
            }
            if (list.size() == 0) {
                errorString = ErrorMessages.INVALID_FILENAME;
            } else {
                logger.debug("Reverting "+list.size()+" files");
                try {
                    jobManager.revertFiles(list.toArray(
                                new File[list.size()]));
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
     * Saves the contents of a file for the active series.
     *
     * @param request The servlet request including a filename parameter and a
     *                contents parameter
     * @param response The servlet response
     *
     * @return A JSON object
     */
    public ModelAndView saveFileContents(HttpServletRequest request,
                                         HttpServletResponse response) {

        File seriesDir = (File)request.getSession().getAttribute("seriesDir");
        final String jobStr = request.getParameter("job");
        final String fileName = request.getParameter("filename");
        final String contents = request.getParameter("contents");
        final ModelAndView mav = new ModelAndView("jsonView");
        String errorString = null;
        VRLJob job = null;

        if (jobStr != null) {
            try {
                long jobId = Long.parseLong(jobStr);
                job = jobManager.getJobById(jobId);
            } catch (NumberFormatException e) {
                logger.warn("Error parsing job ID " + jobStr);
            }
        }

        if (getGrisuService(request) == null) {
            errorString = ErrorMessages.SESSION_EXPIRED;
            logger.warn("ServiceInterface is null!");
        } else if (seriesDir == null) {
            errorString = ErrorMessages.NO_SERIES;
            logger.warn(errorString);
        } else if (job == null) {
            errorString = ErrorMessages.INVALID_JOB;
            logger.warn("job is null!");
        } else if (fileName == null || contents == null) {
            errorString = ErrorMessages.MISSING_PARAMETER;
            logger.warn("No filename/contents provided!");
        } else if (!fileName.equals(Util.sanitizeSubPath(fileName)) ||
                fileName.startsWith(".")) {
            errorString = ErrorMessages.MISSING_PARAMETER;
            logger.warn("Invalid filename " + fileName + "!");
        } else {
            logger.debug("Saving file contents "+jobStr+"/"+fileName);
            try {
                File destFile = new File(new File(seriesDir, jobStr),
                        fileName);
                if (destFile.isDirectory()) {
                    errorString = ErrorMessages.INVALID_FILENAME;
                    logger.warn("Tried to overwrite directory "
                            + destFile.getPath());
                } else {
                    boolean newFile = !destFile.exists();
                    BufferedWriter output = new BufferedWriter(
                        new FileWriter(destFile));
                    output.write(contents);
                    output.close();
                    if (newFile) {
                        try {
                            jobManager.addFile(destFile);
                        } catch (Exception e) {
                            errorString = ErrorMessages.INTERNAL_ERROR;
                            logger.error(e.getMessage(), e);
                        }
                    }
                }
            } catch (IOException e) {
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

        File seriesDir = (File)request.getSession().getAttribute("seriesDir");
        final String jobStr = request.getParameter("job");
        String errorString = null;
        FileInformation fileInfo = null;
        VRLJob job = null;

        if (jobStr != null) {
            try {
                long jobId = Long.parseLong(jobStr);
                job = jobManager.getJobById(jobId);
            } catch (NumberFormatException e) {
                logger.warn("Error parsing job ID " + jobStr);
            }
        }

        if (getGrisuService(request) == null) {
            errorString = ErrorMessages.SESSION_EXPIRED;
            logger.warn("ServiceInterface is null!");
        } else if (seriesDir == null) {
            errorString = ErrorMessages.NO_SERIES;
            logger.warn(errorString);
        } else if (job == null) {
            errorString = ErrorMessages.INVALID_JOB;
            logger.warn("job is null!");
        } else {
            MultipartHttpServletRequest mfReq =
                (MultipartHttpServletRequest) request;

            MultipartFile f = mfReq.getFile("file");
            if (f == null) {
                errorString = ErrorMessages.MISSING_PARAMETER;
                logger.warn("No file provided!");
            } else if (!f.getOriginalFilename().equals(
                        Util.sanitizeSubPath(f.getOriginalFilename()))) {
                errorString = ErrorMessages.INVALID_FILENAME;
                logger.warn("Invalid filename " + f.getOriginalFilename());
            } else {
                logger.info("Saving uploaded file "+f.getOriginalFilename());
                File destination = new File(new File(seriesDir, jobStr),
                        f.getOriginalFilename());
                if (destination.isDirectory()) {
                    errorString = ErrorMessages.INVALID_FILENAME;
                    logger.warn("Tried to overwrite directory " +
                            destination.getPath());
                } else {
                    boolean newFile = !destination.exists();
                    if (!newFile) {
                        logger.debug("Overwriting existing file.");
                    }
                    try {
                        f.transferTo(destination);
                    } catch (IOException e) {
                        errorString = ErrorMessages.INTERNAL_ERROR;
                        logger.error(e.getMessage(), e);
                    }
                    if (newFile) {
                        try {
                            jobManager.addFile(destination);
                            fileInfo = new FileInformation(
                                    f.getOriginalFilename(), f.getSize(), "A");
                        } catch (Exception e) {
                            errorString = ErrorMessages.INTERNAL_ERROR;
                            logger.error(e.getMessage(), e);
                        }
                    }
                }
            }
        }

        // We cannot use jsonView here since this is a file upload request and
        // ExtJS uses a hidden iframe which receives the response.
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        try {
            PrintWriter pw = response.getWriter();
            if (errorString != null) {
                pw.print("{success:false,error:'"+errorString+"'}");
            } else {
                pw.print("{success:true,name:'" + fileInfo.getName()
                        + "',size:" + fileInfo.getSize()
                        + ",state:'" + fileInfo.getState() + "'}");
            }
            pw.flush();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    private void sendSingleFile(File file, HttpServletResponse response)
            throws IOException {

        String fileName = file.getName();
        if (fileName.endsWith(".py")) {
            response.setContentType("application/x-python");
        } else if (fileName.endsWith(".txt")
                || fileName.endsWith(".vtk")
                || fileName.endsWith(".vtu")
                || fileName.endsWith(".xml")) {
            response.setContentType("text/plain");
        } else if (fileName.endsWith(".tex")
                || fileName.endsWith(".sty")
                || fileName.endsWith(".cls")) {
            response.setContentType("application/x-tex");
        } else if (fileName.endsWith(".jpg")) {
            response.setContentType("image/jpeg");
        } else if (fileName.endsWith(".png")) {
            response.setContentType("image/png");
        } else {
            response.setContentType("application/octet-stream");
        }
        response.setHeader("Content-Disposition",
                "attachment; filename=\""+fileName+"\"");

        byte[] buffer = new byte[16384];
        int count = 0;
        OutputStream out = response.getOutputStream();
        FileInputStream fin = new FileInputStream(file);
        while ((count = fin.read(buffer)) != -1) {
            out.write(buffer, 0, count);
        }
        out.flush();
    }
}

