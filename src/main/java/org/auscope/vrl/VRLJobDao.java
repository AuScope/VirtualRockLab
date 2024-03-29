/*
 * This file is part of the AuScope Virtual Rock Lab (VRL) project.
 * Copyright (c) 2010 The University of Queensland, ESSCC
 *
 * Licensed under the terms of the GNU Lesser General Public License.
 */
package org.auscope.vrl;

import java.sql.SQLException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * A Hibernate-backed VRLJob data object
 *
 * @author Cihan Altinay
 */
public class VRLJobDao extends HibernateDaoSupport {

    protected final Log logger = LogFactory.getLog(getClass());

    /**
     * Retrieves jobs that are grouped under given series
     *
     * @param seriesID the ID of the series
     *
     * @return list of <code>VRLJob</code> objects belonging to given series
     */
    public List<VRLJob> getJobsBySeries(final long seriesId) {
        return (List<VRLJob>) getHibernateTemplate()
            .findByNamedParam("from VRLJob j where j.seriesId=:searchId",
                    "searchId", seriesId);
    }

    /**
     * Retrieves jobs that belong to a specific user
     *
     * @param user the user whose jobs are to be retrieved
     *
     * @return list of <code>VRLJob</code> objects belonging to given user
     */
    public List<VRLJob> getJobsByUser(final String user) {
        return (List<VRLJob>) getHibernateTemplate()
            .findByNamedParam("from VRLJob j where j.user=:searchUser",
                    "searchUser", user);
    }

    /**
     * Deletes the given job.
     */
    public void deleteJob(final VRLJob job) {
        getHibernateTemplate().delete(job);
    }

    /**
     * Retrieves the job with given ID.
     *
     * @return <code>VRLJob</code> object with given ID.
     */
    public VRLJob getJobById(final long id) {
        return (VRLJob) getHibernateTemplate().get(VRLJob.class, id);
    }

    /**
     * Saves or updates the given job.
     */
    public void saveJob(final VRLJob job) {
        getHibernateTemplate().saveOrUpdate(job);
    }
}

