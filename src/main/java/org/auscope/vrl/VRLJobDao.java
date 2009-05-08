package org.auscope.vrl;

import java.sql.SQLException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

public class VRLJobDao extends HibernateDaoSupport {

    protected final Log logger = LogFactory.getLog(getClass());

    public List<VRLJob> getJobsOfSeries(final int seriesID) {
        return (List<VRLJob>) getHibernateTemplate()
            .findByNamedParam("from VRLJob j where j.seriesId=:searchID",
                    "searchID", seriesID);
    }

    public List<VRLJob> getJobsByUser(final String user) {
        return (List<VRLJob>) getHibernateTemplate()
            .findByNamedParam("from VRLJob j where j.user=:searchUser",
                    "searchUser", user);
        /*
        return sessionFactory.getCurrentSession()
            .createQuery("from jobs j where j.user=:searchUser")
            .setString("searchUser", user)
            .list();
        */
    }

    public VRLJob get(final int id) {
        return (VRLJob) getHibernateTemplate().get(VRLJob.class, id);
        /*
        return (VRLJob) sessionFactory.getCurrentSession()
            .load(VRLJob.class, id);
        */
    }

    public void save(final VRLJob job) {
        getHibernateTemplate().saveOrUpdate(job);
        //sessionFactory.getCurrentSession().saveOrUpdate(job);
    }
}

