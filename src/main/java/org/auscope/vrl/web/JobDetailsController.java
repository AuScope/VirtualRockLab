package org.auscope.vrl.web;

import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.auscope.vrl.GridAccessController;
import org.auscope.vrl.UserJob;
import org.auscope.vrl.UserJobManager;

public class JobDetailsController implements Controller {

    protected final Log logger = LogFactory.getLog(getClass());

    private GridAccessController gridAccess;
    private UserJobManager userJobManager;

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        //TODO: Check credentials before doing anything -> redirect to login
        // page if no valid proxy found
        //if (!gridAccess.validProxy()) {}

        //String reqAction = request.getParameter("action");
        //String reqJob = request.getParameter("ref");
            /*
        ModelAndView mav = super.handleRequest(request, response);
        Map<String, Object> model = mav.getModel();

        UserJob job = (UserJob)model.get("job");

        if (job == null) {
            logger.info("Job was null, returning to main page");
            return new ModelAndView("monitor");
        }

        logger.info("Request: "+job.getReference());
        */
/*
        if (reqAction != null && reqJob != null) {
            logger.info("User request: "+reqAction + "; job: "+reqJob);
            UserJob job = userJobManager.getUserJobByRef("testUser", reqJob);
            if (job == null) {
                logger.error("Requested job not in job manager!");
            } else { 
                if (reqAction.equals("kill")) {
                    String newState = gridAccess.killJob(reqJob);
                    if (newState == null)
                        newState = "Failed";
                    logger.info("New job state: "+newState);
                    job.setStatus(newState);
                    userJobManager.saveUserJob(job);
                }
            }
        }
*/
        logger.info("Getting details of user job");

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

        logger.info("Returning user job details view");

        return new ModelAndView("jobdetails", "model", myModel);
    }

    public void setGridAccess(GridAccessController gridAccess) {
        this.gridAccess = gridAccess;
    }

    public void setUserJobManager(UserJobManager userJobManager) {
        this.userJobManager = userJobManager;
    }
}

