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

public class JobMonitorController implements Controller {

    protected final Log logger = LogFactory.getLog(getClass());

    private GridAccessController gridAccess;
    private UserJobManager userJobManager;

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        List<UserJob> userJobs = this.userJobManager.getUserJobs("testUser");

        logger.info("Updating status of user jobs");

        for (UserJob j : userJobs) {
            String state = j.getStatus();
            if (!state.equals("Done") && !state.equals("Failed")) {
                String[] eprs = new String[] { j.getReference() };
                String[] newState = gridAccess.retrieveJobStatus(eprs);
                if (newState[0] != null) {
                    j.setStatus(newState[0]);
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

        return new ModelAndView("index", "model", myModel);
    }

    public void setGridAccess(GridAccessController gridAccess) {
        this.gridAccess = gridAccess;
    }

    public void setUserJobManager(UserJobManager userJobManager) {
        this.userJobManager = userJobManager;
    }
}

