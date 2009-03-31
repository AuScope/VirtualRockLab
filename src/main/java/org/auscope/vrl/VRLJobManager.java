package org.auscope.vrl;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class VRLJobManager {
    protected final Log logger = LogFactory.getLog(getClass());

    private VRLJobDao vrlJobDao;

    public List<VRLJob> getJobsByUser(String user) {
        return vrlJobDao.getJobListByUser(user);
    }

    public VRLJob getJobByUserAndRef(String user, String reference) {
        return vrlJobDao.getJobByUserAndRef(user, reference);
    }

    public void saveJob(VRLJob vrlJob) {
        vrlJobDao.saveVRLJob(vrlJob);
    }

    public void setVRLJobDao(VRLJobDao vrlJobDao) {
        this.vrlJobDao = vrlJobDao;
    }
}

