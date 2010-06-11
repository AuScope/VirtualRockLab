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
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Class that talks to the data objects to retrieve or save data
 *
 * @author Cihan Altinay
 */
public class VRLJobManager {
    protected final Log logger = LogFactory.getLog(getClass());

    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir")
        + File.separator + "vrl" + File.separator;

    private VRLJobDao vrlJobDao;
    private VRLSeriesDao vrlSeriesDao;

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

    public VRLSeries cloneSeries(VRLSeries series, String user, String name,
                                 String description) throws Exception {
        VRLSeries newSeries = new VRLSeries();
        newSeries.setDescription(description);
        newSeries.setName(name);
        newSeries.setUser(user);
        newSeries.setCreationDate((new Date()).getTime());
        saveSeries(newSeries);
        long newId = newSeries.getId();

        // copy jobs
        List jobs = getJobsBySeries(series.getId().longValue());
        Iterator it = jobs.listIterator();
        while (it.hasNext()) {
            VRLJob job = (VRLJob)it.next();
            VRLJob newJob = new VRLJob(job.getName(), job.getDescription(),
                    job.getScriptFile(), job.getOutputDir(),
                    newId);
            saveJob(newJob);
        }

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
        return series;
    }

    public File openSeries(String user, long seriesId) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String targetDir = TEMP_DIR + user + File.separator
            + sdf.format(new Date());
        File path = new File(targetDir);
        path.mkdirs();
        List jobs = getJobsBySeries(seriesId);
        Iterator it = jobs.listIterator();
        while (it.hasNext()) {
            VRLJob job = (VRLJob)it.next();
            File jobDir = new File(path, job.getId().toString());
            jobDir.mkdir();
        }
        return path;
    }
}

