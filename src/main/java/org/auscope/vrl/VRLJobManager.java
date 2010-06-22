/*
 * This file is part of the AuScope Virtual Rock Lab (VRL) project.
 * Copyright (c) 2010 The University of Queensland, ESSCC
 *
 * Licensed under the terms of the GNU Lesser General Public License.
 */
package org.auscope.vrl;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Class that talks to the data objects to retrieve or save data
 *
 * @author Cihan Altinay
 */
public class VRLJobManager {
    protected final Log logger = LogFactory.getLog(getClass());

    private RevisionControl revisionCtl;
    private VRLJobDao vrlJobDao;
    private VRLSeriesDao vrlSeriesDao;

    public void setRevisionCtl(RevisionControl revisionCtl) {
        this.revisionCtl = revisionCtl;
    }

    public void setVRLJobDao(VRLJobDao vrlJobDao) {
        this.vrlJobDao = vrlJobDao;
    }

    public void setVRLSeriesDao(VRLSeriesDao vrlSeriesDao) {
        this.vrlSeriesDao = vrlSeriesDao;
    }

    public List<VRLSeries> querySeries(String user, String name, String desc) {
        return vrlSeriesDao.query(user, name, desc);
    }

    public VRLSeries getSeriesById(long seriesId) {
        return vrlSeriesDao.getSeriesById(seriesId);
    }

    public List<VRLSeries> getSeriesByUser(final String user) {
        return vrlSeriesDao.getSeriesByUser(user);
    }

    public List<VRLJob> getJobsBySeries(long seriesId) {
        return vrlJobDao.getJobsBySeries(seriesId);
    }

    public VRLJob getJobById(long jobId) {
        return vrlJobDao.getJobById(jobId);
    }

    public void deleteJob(VRLJob job) {
        vrlJobDao.deleteJob(job);
    }

    public void saveJob(VRLJob vrlJob) {
        vrlJobDao.saveJob(vrlJob);
    }

    public void saveSeries(VRLSeries series) {
        vrlSeriesDao.saveSeries(series);
    }

    public VRLSeries cloneSeries(VRLSeries series, String revision,
                                 String user, String name,
                                 String description) throws Exception {
        VRLSeries newSeries = new VRLSeries();
        newSeries.setDescription(description);
        newSeries.setName(name);
        newSeries.setUser(user);
        newSeries.setCreationDate((new Date()).getTime());
        saveSeries(newSeries);
        long newId = newSeries.getId();

        // copy jobs - jobIdMap holds the mapping of old jobId to new jobId
        List jobs = getJobsBySeries(series.getId().longValue());
        Map jobIdMap = new HashMap(jobs.size());
        Iterator it = jobs.listIterator();
        while (it.hasNext()) {
            VRLJob job = (VRLJob)it.next();
            VRLJob newJob = new VRLJob(job.getName(), job.getDescription(),
                    job.getScriptFile(), newId);
            saveJob(newJob);
            jobIdMap.put(job.getId().toString(), newJob.getId().toString());
        }

        // copy series files
        String message = new String("Copy of '" + series.getName()
                + "' at revision " + revision);
        revisionCtl.cloneSeries(series.getUser(), series.getId(), revision,
                user, newSeries.getId(), jobIdMap, message);
        return newSeries;
    }

    public VRLSeries createSeries(String user, String name,
                                  String description) throws Exception {
        VRLSeries series = new VRLSeries();
        series.setDescription(description);
        series.setName(name);
        series.setUser(user);
        series.setCreationDate((new Date()).getTime());
        saveSeries(series);
        String message = new String("Initial revision of '"
                + series.getName() + "'");
        try {
            revisionCtl.createSeries(series.getUser(), series.getId(), message);
        } catch (Exception e) {
            //TODO: deleteSeries(series.getId());
            throw(e);
        }
        return series;
    }

    public RevisionEntry getLastSeriesRevision(String user, long seriesId)
            throws Exception {
        return revisionCtl.getEntry(user, seriesId, "", "HEAD");
    }

    public RevisionLog getRevisionLog(String user, long seriesId,
                                      String revision) {
        RevisionLog[] res = revisionCtl.getSeriesLogs(user, seriesId,
                revision, revision);
        if (res != null && res.length > 0) {
            return res[0];
        } else {
            logger.debug("Revision "+revision+" of series "+seriesId
                    +" by user "+user+" not found.");
            return null;
        }
    }

    public RevisionLog[] getRevisionsBySeries(String user, long seriesId) {
        return revisionCtl.getSeriesLogs(user, seriesId, "0", "HEAD");
    }

    public File checkoutSeriesRevision(String user, long seriesId,
                                       String revision) throws Exception {
        return revisionCtl.checkoutRevision(user, seriesId, revision);
    }

    public long saveRevision(File path, String message) throws Exception {
        return revisionCtl.commitChanges(path, message);
    }

    public void addFile(File file) throws Exception {
        revisionCtl.addFile(file);
    }

    public void deleteFiles(File[] files) throws Exception {
        revisionCtl.removeFiles(files);
    }

    public boolean hasModifications(File path) throws Exception {
        return revisionCtl.hasModifications(path);
    }

    public FileInformation[] listFiles(File path) throws Exception {
        return revisionCtl.listFilesWithStatus(path);
    }

    public void revertFiles(File[] files) throws Exception {
        revisionCtl.revertFiles(files);
    }
}

