package org.auscope.vrl.web;

import org.springframework.web.servlet.mvc.multiaction.MultiActionController;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.auscope.vrl.GridAccessController;
import org.auscope.vrl.UserJob;
import org.auscope.vrl.UserJobManager;

public class JobListController extends MultiActionController {

    public class FileInfo {
        private String name;
        private String readableSize;
        private long size;
        FileInfo(String n, long s) { name = n; setSize(s); }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public long getSize() { return size; }
        public String getReadableSize() { return readableSize; }
        public void setSize(long size) {
            this.size = size;
            readableSize = NumberFormat.getInstance().format(size);
        }
    };

    protected final Log logger = LogFactory.getLog(getClass());

    private GridAccessController gridAccess;
    private UserJobManager userJobManager;

    protected ModelAndView handleNoSuchRequestHandlingMethod(
            NoSuchRequestHandlingMethodException ex,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        logger.info("No/invalid action parameter; returning default view");
        // Return default view
        return new ModelAndView("joblist");
    }

    /**
     * Kills the job given by its reference.
     * 
     * @param request The servlet request including a ref parameter
     * @param response The servlet response
     *
     * @return A JSON object with a success attribute and an error attribute
     *         in case the job was not found in the job manager.
     */
    public ModelAndView killJob(HttpServletRequest request,
                                HttpServletResponse response) {

        String jobRef = request.getParameter("ref");
        logger.info("Cancelling job "+jobRef);
        UserJob job = userJobManager.getUserJobByRef("testUser", jobRef);
        Map<String, Object> myModel = new HashMap<String, Object>();

        if (job == null) {
            final String errorString = "Requested job not in job manager! Not cancelling.";
            logger.error(errorString);
            myModel.put("error", errorString);
            myModel.put("success", false);

        } else {
            String newState = gridAccess.killJob(jobRef);
            if (newState == null)
                newState = "Unknown";
            logger.info("New job state: "+newState);

            job.setStatus(newState);
            userJobManager.saveUserJob(job);
            myModel.put("success", true);
        }

        return new ModelAndView("jsonView", "model", myModel);
    }

    /**
     * Returns a JSON object containing an array of the files belonging to a
     * given job.
     * 
     * @param request The servlet request including a ref parameter
     * @param response The servlet response
     *
     * @return A JSON object with a files attribute which is an array of
     *         FileInfo objects. If the job was not found in the job manager
     *         the JSON object will contain an error attribute indicating the
     *         error.
     */
    public ModelAndView jobFiles(HttpServletRequest request,
                                 HttpServletResponse response) {

        String jobRef = request.getParameter("ref");
        logger.info("Getting file list for job "+jobRef);
        UserJob job = userJobManager.getUserJobByRef("testUser", jobRef);
        Map<String, Object> myModel = new HashMap<String, Object>();

        if (job == null) {
            final String errorString = "Requested job not in job manager!";
            logger.error(errorString);
            myModel.put("error", errorString);

        } else {
            FileInfo[] fileDetails = null;

            File dir = new File(job.getOutputDir());
            File[] files = dir.listFiles();
            if (files != null) {
                Arrays.sort(files);

                fileDetails = new FileInfo[files.length];
                for (int i=0; i<files.length; i++) {
                    fileDetails[i] = new FileInfo(files[i].getName(), files[i].length());
                }
            }

            myModel.put("ref", jobRef);
            myModel.put("files", fileDetails);
        }

        return new ModelAndView("jsonView", "model", myModel);
    }

    /**
     * Sends the contents of a job file to the client.
     * 
     * @param request The servlet request including a ref parameter and a
     *                filename parameter
     * @param response The servlet response receiving the data
     *
     * @return null on success or the joblist Model and View with an error
     *         parameter on failure
     */
    public ModelAndView downloadFile(HttpServletRequest request,
                                     HttpServletResponse response) {

        String jobRef = request.getParameter("ref");
        String fileName = request.getParameter("filename");
        UserJob job = userJobManager.getUserJobByRef("testUser", jobRef);
        Map<String, Object> myModel = new HashMap<String, Object>();
        String errorString = null;

        logger.info("Download "+fileName+" of job "+jobRef);

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
        return new ModelAndView("joblist", "model", myModel);
    }

    /**
     * Sends the contents of multiple job files as a ZIP archive to the client.
     * 
     * @param request The servlet request including a ref parameter and a
     *                files parameter with the filenames separated by comma
     * @param response The servlet response receiving the data
     *
     * @return null on success or the joblist Model and View with an error
     *         parameter on failure
     */
    public ModelAndView downloadMulti(HttpServletRequest request,
                                      HttpServletResponse response) {

        String jobRef = request.getParameter("ref");
        String filesParam = request.getParameter("files");
        UserJob job = userJobManager.getUserJobByRef("testUser", jobRef);
        Map<String, Object> myModel = new HashMap<String, Object>();
        String errorString = null;

        if (job != null && filesParam != null) {
            String[] fileNames = filesParam.split(",");
            logger.info("Archiving "+fileNames.length+" files of job "+jobRef);

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
        return new ModelAndView("joblist", "model", myModel);
    }

    /**
     * Returns a JSON object containing an array of jobs for the current user.
     * 
     * @param request The servlet request
     * @param response The servlet response
     *
     * @return A JSON object with a jobs attribute which is an array of
     *         UserJob objects.
     */
    public ModelAndView listJobs(HttpServletRequest request,
                                 HttpServletResponse response) {

        //TODO: Check credentials before doing anything -> redirect to login
        // page if no valid proxy found
        //if (!gridAccess.validProxy()) {}
        logger.info("EPPN = " + request.getHeader("eppn"));

        logger.info("Updating status of user jobs");
        List<UserJob> userJobs = userJobManager.getUserJobs("testUser");
        Map<String, Object> myModel = new HashMap<String, Object>();

        for (UserJob j : userJobs) {
            String state = j.getStatus();
            if (!state.equals("Done") && !state.equals("Failed")) {
                String newState = gridAccess.retrieveJobStatus(j.getReference());
                if (newState != null) {
                    j.setStatus(newState);
                    userJobManager.saveUserJob(j);
                }
            }
            //gridAccess.getJobByReference(j.getReference());
        }

        myModel.put("jobs", userJobs);

        logger.info("Returning user jobs");
        return new ModelAndView("jsonView", "model", myModel);
    }

    /**
     * Re-submits a job 
     * 
     * @param request The servlet request including a ref parameter
     * @param response The servlet response
     *
     * @return The gridsubmit model and view prepared to resubmit the job or
     *         the joblist model and view with an error parameter if the job
     *         was not found.
     */
    public ModelAndView resubmitJob(HttpServletRequest request,
                                    HttpServletResponse response) {

        String jobRef = request.getParameter("ref");
        logger.info("Re-submitting job "+jobRef);
        UserJob job = userJobManager.getUserJobByRef("testUser", jobRef);

        if (job == null) {
            Map<String, Object> myModel = new HashMap<String, Object>();
            final String errorString = "Requested job not in job manager!";
            logger.error(errorString);
            myModel.put("error", errorString);
            return new ModelAndView("joblist", "model", myModel);
        }

        ModelAndView mav =  new ModelAndView(
                new RedirectView("gridsubmit.html"));
        mav.addObject("resubmitRef", jobRef);
        return mav;
    }


    public void setGridAccess(GridAccessController gridAccess) {
        this.gridAccess = gridAccess;
    }

    public void setUserJobManager(UserJobManager userJobManager) {
        this.userJobManager = userJobManager;
    }
}

