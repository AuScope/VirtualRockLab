package org.auscope.vrl;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class UserJobManager {
    protected final Log logger = LogFactory.getLog(getClass());

    private UserJobDao userJobDao;

    public List<UserJob> getUserJobs(String userId) {
        return userJobDao.getUserJobList(userId);
    }

    public void saveUserJob(UserJob userJob) {
        userJobDao.saveUserJob(userJob);
    }

    public void setUserJobDao(UserJobDao userJobDao) {
        this.userJobDao = userJobDao;
    }
}

