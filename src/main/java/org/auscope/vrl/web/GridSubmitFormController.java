package org.auscope.vrl.web;

import org.springframework.validation.BindException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.io.File;
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
        String jobTempDir = inTransfers[0];

        for (int i=0;; i++) {
            MultipartFile f = mfReq.getFile("file"+i);
            if (f == null) {
                break;
            }
            logger.info("Saving uploaded file "+f.getOriginalFilename());
            f.transferTo(new File(jobTempDir+f.getOriginalFilename()));
        }

        // add server part to local stageIn
        inTransfers[0] = gridAccess.getLocalGridFtpServer() + jobTempDir;
        job.setInTransfers(inTransfers);

        logger.info("Submitting job with name " + job.getName() + " to " +
                job.getSite());

        // ...and ACTION!
        String submitEPR = gridAccess.submitJob(job);

        if (submitEPR == null) {
            errors.reject("error.not-submitted");
            return showForm(request, response, errors);

        } else {
            logger.info("Resulting EPR: "+submitEPR);

            String[] eprs = new String[] { submitEPR };
            String[] status = gridAccess.retrieveJobStatus(eprs);
            UserJob userJob = new UserJob("testUser", job.getName(),
                    submitEPR, new Date().toString(), status[0]);
            userJobManager.saveUserJob(userJob);
            logger.info("Returning to " + getSuccessView());
        }

        return super.onSubmit(request, response, command, errors);
    }

    protected Object formBackingObject(HttpServletRequest request)
            throws ServletException {
        final String site = "ESSCC";
        final String name = "gridjob";
        final String email = "";
        final String code = CODE_NAME;
        final String jobType = "mpi";
        final String maxWallTime = "5";
        final String maxMemory = "1024";
        final String cpuCount = "2";
        final String stdInput = "";
        final String stdOutput = "stdOutput.txt";
        final String stdError = "stdError.txt";
        String[] arguments = { "script.py" };
        String[] inTransfers;
        final String[] outTransfers = { gridAccess.getLocalGridFtpServer() +
            gridAccess.getLocalGridFtpStageOutDir()
        };
        final String version =
            gridAccess.retrieveCodeVersionsAtSite(site, code)[0];
        final String queue = gridAccess.retrieveQueueNamesAtSite(site)[0];

        // Create a new directory to put all files for this job into.
        // This directory will always be the first stageIn directive.
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String dateFmt = sdf.format(new Date());
        String jobTempDir = gridAccess.getLocalGridFtpStageInDir() +
            "testUser" + "-" + dateFmt + "/";
        boolean success = (new File(jobTempDir)).mkdir();

        if (!success) {
            logger.error("Could not create directory "+jobTempDir);
            jobTempDir = gridAccess.getLocalGridFtpStageInDir();
        }

        inTransfers = new String[] { jobTempDir };

        // Check if ScriptBuilder was used. If so, there is a file in the
        // system temp directory which needs to be staged in.
        String newScript = request.getParameter("newscript");
        if (newScript != null) {
            logger.info("Adding "+newScript+" to stageIn directory");

            File scriptFile = new File(System.getProperty("java.io.tmpdir") +
                    File.separator+newScript+".py");
            success = scriptFile.renameTo(
                    new File(jobTempDir, scriptFile.getName()));

            if (success) {
                logger.info("Moved "+newScript+" to stageIn directory");
                arguments[0] = newScript+".py";
            } else {
                logger.error("Could not move "+newScript+" to stageIn!");
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

