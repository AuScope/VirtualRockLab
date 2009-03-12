package org.auscope.vrl.web;

import org.springframework.web.servlet.mvc.multiaction.MultiActionController;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.auscope.vrl.GridAccessController;
import org.auscope.vrl.UserJob;
import org.auscope.vrl.UserJobManager;

public class JobMonitorController extends MultiActionController {

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

        // Return default view
        return invokeNamedMethod("showJobs", request, response);
    }

    public ModelAndView killJob(HttpServletRequest request,
                                HttpServletResponse response) {

        String jobRef = request.getParameter("ref");
        logger.info("Cancelling job "+jobRef);

        UserJob job = userJobManager.getUserJobByRef("testUser", jobRef);
        if (job == null) {
            logger.error("Requested job not in job manager! Not cancelling.");

        } else {
            String newState = gridAccess.killJob(jobRef);
            if (newState == null)
                newState = "Failed";
            logger.info("New job state: "+newState);

            job.setStatus(newState);
            userJobManager.saveUserJob(job);
        }

        return showJobs(request, response);
    }

    public ModelAndView jobDetails(HttpServletRequest request,
                                   HttpServletResponse response) {

        String jobRef = request.getParameter("ref");
        logger.info("Getting details of job "+jobRef);

        UserJob job = userJobManager.getUserJobByRef("testUser", jobRef);
        if (job == null) {
            logger.error("Requested job not in job manager!");

        } else {
            Map<String, Object> myModel = new HashMap<String, Object>();
            myModel.put("ref", jobRef);

            File dir = new File(job.getOutputDir());
            File[] files = dir.listFiles();
            if (files != null) {
                Arrays.sort(files);

                FileInfo[] fileDetails = new FileInfo[files.length];
                for (int i=0; i<files.length; i++) {
                    fileDetails[i] = new FileInfo(files[i].getName(), files[i].length());
                }

                myModel.put("files", fileDetails);
            }

            logger.info("Returning user job details view");
            return new ModelAndView("jobdetails", "model", myModel);
        }

        return showJobs(request, response);
    }

    public ModelAndView downloadFile(HttpServletRequest request,
                                     HttpServletResponse response) {

        String jobRef = request.getParameter("ref");
        String fileName = request.getParameter("filename");
        logger.info("Download "+fileName+" of job "+jobRef);

        UserJob job = userJobManager.getUserJobByRef("testUser", jobRef);
        if (job == null) {
            logger.error("Invalid job specified!");
        } else if (fileName == null) {
            logger.error("No filename provided!");
        } else {
            File f = new File(job.getOutputDir()+File.separator+fileName);
            if (!f.canRead()) {
                logger.error("File "+f.getPath()+" not readable!");
            } else {
                response.setContentType("application/octet-stream");
                //response.setContentLength((int)f.length());
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
                } catch (IOException e) {
                    logger.error("Could not send file: "+e.getMessage());
                }

                return null;
            }
        }

        return showJobs(request, response);
    }

    public ModelAndView showJobs(HttpServletRequest request,
                                 HttpServletResponse response) {

        //TODO: Check credentials before doing anything -> redirect to login
        // page if no valid proxy found
        //if (!gridAccess.validProxy()) {}

        logger.info("Updating status of user jobs");

        List<UserJob> userJobs = userJobManager.getUserJobs("testUser");
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

        Map<String, Object> myModel = new HashMap<String, Object>();

        if (!userJobs.isEmpty()) {
            myModel.put("jobs", userJobs);
        }

        logger.info("Returning user job view");
        return new ModelAndView("monitor", "model", myModel);
    }

    public void setGridAccess(GridAccessController gridAccess) {
        this.gridAccess = gridAccess;
    }

    public void setUserJobManager(UserJobManager userJobManager) {
        this.userJobManager = userJobManager;
    }
}

