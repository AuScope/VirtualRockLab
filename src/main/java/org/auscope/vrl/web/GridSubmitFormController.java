package org.auscope.vrl.web;

import org.springframework.validation.BindException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.Exception;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.auscope.gridtools.GridJob;
import org.auscope.vrl.GridAccessController;
import org.auscope.vrl.UserJob;
import org.auscope.vrl.UserJobManager;

public class GridSubmitFormController extends SimpleFormController {

    /** Logger for this class and subclasses */
    protected final Log logger = LogFactory.getLog(getClass());

    private static final String CODE_NAME = "esys_particle";
    private GridAccessController gridAccess;
    private UserJobManager userJobManager;

    public ModelAndView handleRequestInternal(HttpServletRequest request,
                                              HttpServletResponse response)
            throws ServletException, Exception {

        ModelAndView mav = super.handleRequestInternal(request, response);

        List<String[]> versionsAtSite = new ArrayList<String[]>();

        String[] particleSites = gridAccess.
                retrieveSitesWithSoftwareAndVersion(CODE_NAME, "");
        for (String s : particleSites) {
            String[] siteVersions = gridAccess.
                    retrieveCodeVersionsAtSite(s, CODE_NAME);
            versionsAtSite.add(siteVersions);
        }
        mav.addObject("sites", particleSites);
        mav.addObject("versions", versionsAtSite);

        return mav;
    }

    protected ModelAndView onSubmit(HttpServletRequest request,
                                    HttpServletResponse response,
                                    Object command,
                                    BindException errors)
            throws Exception {

        MultipartHttpServletRequest mfReq = (MultipartHttpServletRequest)request;
        GridJob job = (GridJob) command;

        // JavaScript returns one string for the transfers delimited by commas
        String transfersString = job.getInTransfers()[0];
        String[] inTransfers = transfersString.split(",");

        // see formBackingObject() - first element is local stageIn dir
        String jobInputDir = inTransfers[0];

        // Get all uploaded files through parameters file$i and save in
        // temporary job directory
        for (int i=0;; i++) {
            MultipartFile f = mfReq.getFile("file"+i);
            if (f == null) {
                break;
            }
            logger.info("Saving uploaded file "+f.getOriginalFilename());
            f.transferTo(new File(jobInputDir+f.getOriginalFilename()));
        }

        // Add server part to local stageIn
        inTransfers[0] = gridAccess.getLocalGridFtpServer() + jobInputDir;
        job.setInTransfers(inTransfers);

        // Create a new directory for the output files of this job
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String dateFmt = sdf.format(new Date());
        String jobID = "testUser" + "-" + job.getName() + "-" + dateFmt +
            File.separator;
        String jobOutputDir = gridAccess.getLocalGridFtpStageOutDir() + jobID;
        boolean success = (new File(jobOutputDir)).mkdir();

        if (!success) {
            logger.error("Could not create directory "+jobOutputDir);
            jobOutputDir = gridAccess.getLocalGridFtpStageOutDir();
        }
        job.setOutTransfers(new String[] { gridAccess.getLocalGridFtpServer() +
            jobOutputDir });

        logger.info("Submitting job with name " + job.getName() + " to " +
                job.getSite());

        // ...and ACTION!
        String submitEPR = gridAccess.submitJob(job);

        if (submitEPR == null) {
            errors.reject("error.not-submitted");
            return showForm(request, response, errors);

        } else {
            logger.info("Resulting EPR: "+submitEPR);

            String status = gridAccess.retrieveJobStatus(submitEPR);
            UserJob userJob = new UserJob("testUser", job.getName(),
                    jobOutputDir, submitEPR, job.getArguments()[0], status, new Date().toString());
            userJobManager.saveUserJob(userJob);
            logger.info("Returning to " + getSuccessView());
        }

        return new ModelAndView(new RedirectView(getSuccessView(), true, false, false));
    }

    private boolean moveFile(File source, File destination) {
        boolean success = false;
        logger.info(source.getPath()+" -> "+destination.getPath());
        try {
            BufferedReader input = new BufferedReader(
                    new FileReader(source));
            BufferedWriter output = new BufferedWriter(
                    new FileWriter(destination));
            String line = null;
            while ((line = input.readLine()) != null) {
                output.write(line);
                output.newLine();
            }
            input.close();
            output.close();
            source.delete();
            success = true;
        } catch (IOException e) {
            logger.warn("Could not move file: "+e.getMessage());
        }
        return success;
    }

    protected Object formBackingObject(HttpServletRequest request)
            throws ServletException {
        final String site = "ESSCC";
        final String name = "gridjob";
        final String email = "";
        final String code = CODE_NAME;
        final String jobType = "mpi";
        final String maxWallTime = "2";
        final String maxMemory = "1024";
        final String stdInput = "";
        final String stdOutput = "stdOutput.txt";
        final String stdError = "stdError.txt";
        String cpuCount = "2";
        String[] arguments = { "script.py" };
        String[] inTransfers;
        final String[] outTransfers = new String[0];
        String version = "";
        String queue = "";

        String[] allVersions = gridAccess.retrieveCodeVersionsAtSite(site, code);
        if (allVersions.length > 0)
            version = allVersions[0];
        
        String[] allQueues = gridAccess.retrieveQueueNamesAtSite(site);
        if (allQueues.length > 0)
            queue = allQueues[0];

        // Create a new directory to put all files for this job into.
        // This directory will always be the first stageIn directive.
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String dateFmt = sdf.format(new Date());
        String jobID = "testUser" + "-" + dateFmt + File.separator;
        String jobInputDir = gridAccess.getLocalGridFtpStageInDir() + jobID;

        boolean success = (new File(jobInputDir)).mkdir();

        if (!success) {
            logger.error("Could not create directory "+jobInputDir);
            jobInputDir = gridAccess.getLocalGridFtpStageInDir();
        }

        // The server part will be added before submission
        inTransfers = new String[] { jobInputDir };

        // Check if the ScriptBuilder was used. If so, there is a file in the
        // system temp directory which needs to be staged in.
        String newScript = request.getParameter("newscript");
        if (newScript != null) {
            logger.info("Adding "+newScript+" to stageIn directory");
            File tmpScriptFile = new File(System.getProperty("java.io.tmpdir") +
                    File.separator+newScript+".py");
            File newScriptFile = new File(jobInputDir, tmpScriptFile.getName());
            success = moveFile(tmpScriptFile, newScriptFile);
            if (success) {
                logger.info("Moved "+newScript+" to stageIn directory");
                arguments[0] = newScript+".py";

                // Now look for a string like
                // sim = LsmMpi( numWorkerProcesses = 2, ...)
                // to extract the number of CPUs
                try {
                    BufferedReader input = new BufferedReader(
                            new FileReader(newScriptFile));
                    String line = null;
                    while ((line = input.readLine()) != null) {
                        int sidx;
                        if ((sidx = line.indexOf("numWorkerProcesses")) != -1) {
                            sidx = line.indexOf('=', sidx);
                            if (sidx == -1) {
                                break;
                            }
                            sidx++;
                            int eidx = line.indexOf(',', sidx);
                            if (eidx == -1) {
                                break;
                            }
                            try {
                                int iCount = Integer.parseInt(line.substring(
                                            sidx, eidx).trim());
                                cpuCount = String.valueOf(iCount+1);
                                logger.info("Number of CPUs from script: "+cpuCount);
                            } catch (NumberFormatException e) {
                                logger.warn("Error parsing number of CPUs.");
                            }

                            break;
                        }
                    }
                    input.close();
                } catch (IOException e) {
                    logger.warn("Could not open script file to extract number of CPUs: "+e.getMessage());
                }

            } else {
                logger.warn("Could not move "+newScript+" to stageIn!");
            }
        }

        logger.info("Creating new GridJob instance");

        GridJob guiJob = new GridJob(site, name, email, code, version,
            arguments, queue, jobType, maxWallTime, maxMemory, cpuCount,
            inTransfers, outTransfers, stdInput, stdOutput, stdError);

        return guiJob;
    }

    public void setGridAccess(GridAccessController gridAccess) {
        this.gridAccess = gridAccess;
    }

    public GridAccessController getGridAccess() {
        return gridAccess;
    }

    public void setUserJobManager(UserJobManager userJobManager) {
        this.userJobManager = userJobManager;
    }

    public UserJobManager getUserJobManager() {
        return userJobManager;
    }

}

