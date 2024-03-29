/*
 * This file is part of the AuScope Virtual Rock Lab (VRL) project.
 * Copyright (c) 2010 The University of Queensland, ESSCC
 *
 * Licensed under the terms of the GNU Lesser General Public License.
 */
package org.auscope.vrl.web;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.auscope.vrl.RevisionEntry;
import org.auscope.vrl.RevisionLog;
import org.auscope.vrl.VRLJob;
import org.auscope.vrl.VRLJobManager;
import org.auscope.vrl.VRLSeries;
import org.auscope.vrl.Util;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controller for series related actions.
 *
 * @author Cihan Altinay
 */
public class SeriesActionController extends MultiActionController {

    /** Logger for this class */
    private final Log logger = LogFactory.getLog(getClass());

    private VRLJobManager jobManager;

    /**
     * Sets the {@link VRLJobManager} to be used to retrieve and store series
     * and job details.
     *
     * @param jobManager the <code>VRLJobManager</code> to use
     */
    public void setJobManager(VRLJobManager jobManager) {
        this.jobManager = jobManager;
    }

    protected ModelAndView handleNoSuchRequestHandlingMethod(
            NoSuchRequestHandlingMethodException ex,
            HttpServletRequest request,
            HttpServletResponse response) {

        logger.warn(ex.getMessage());
        String referer = request.getHeader("referer");
        if (referer != null && referer.contains(request.getServerName())) {
            ModelAndView mav = new ModelAndView("jsonView");
            mav.addObject("error", ex.getMessage());
            mav.addObject("success", false);
            return mav;
        } else {
            return new ModelAndView(
                    new RedirectView("/login.html", true, false, false));
        }
    }

    private boolean isLoggedIn(HttpServletRequest request) {
        return (request.getSession().getAttribute("grisuService") != null);
    }

    ///////////////////////////////////////////////////////////////////////
    ////////////////////////////// ACTIONS ////////////////////////////////
    ///////////////////////////////////////////////////////////////////////

    /**
     * Creates a copy of the active series for the current user.
     *
     * @param request The servlet request
     * @param response The servlet response
     *
     * @return A JSON object
     */
    public ModelAndView cloneSeries(HttpServletRequest request,
                                     HttpServletResponse response) {

        Long seriesId = (Long)request.getSession().getAttribute("seriesId");
        Long seriesRev = (Long)request.getSession().getAttribute("seriesRev");
        final String user = request.getRemoteUser();
        final String name = request.getParameter("name");
        String description = request.getParameter("description");
        final ModelAndView mav = new ModelAndView("jsonView");
        String errorString = null;
        VRLSeries series = null;
        long newRevision = -1;
        long newId = -1;

        if (seriesId != null) {
            series = jobManager.getSeriesById(seriesId.longValue());
        }

        if (!isLoggedIn(request)) {
            errorString = ErrorMessages.SESSION_EXPIRED;
            logger.warn("ServiceInterface is null!");
        } else if (series == null) {
            errorString = ErrorMessages.NO_SERIES;
            logger.warn(errorString);
        } else if (name == null || name.length() < 3) {
            errorString = ErrorMessages.MISSING_PARAMETER;
            logger.warn("No/Invalid series name specified!");
        } else {
            // prevent duplicate names
            List<VRLSeries> uSeries = jobManager.getSeriesByUser(user);
            Iterator<VRLSeries> it = uSeries.listIterator();
            while (it.hasNext()) {
                if (name.equals(it.next().getName())) {
                    errorString = ErrorMessages.SERIES_EXISTS;
                    logger.warn(errorString);
                    break;
                }
            }
        }

        if (errorString == null) {
            try {
                if (description == null) {
                    description = "";
                }
                VRLSeries newSeries = jobManager.cloneSeries(
                        series, seriesRev.toString(), user, name, description);
                newId = newSeries.getId();

                RevisionEntry rev = jobManager.getLastSeriesRevision(
                        user, newId);
                newRevision = rev.getRevision();
            } catch (Exception e) {
                errorString = ErrorMessages.INTERNAL_ERROR;
                logger.error(e.getMessage(), e);
            }
        }

        if (errorString != null) {
            mav.addObject("error", errorString);
            mav.addObject("success", false);
        } else {
            mav.addObject("seriesId", newId);
            mav.addObject("revision", newRevision);
            mav.addObject("success", true);
        }

        return mav;
    }

    /**
     * Cleans up temporary files and session attributes used by the active
     * series.
     *
     * @param request The servlet request
     * @param response The servlet response
     *
     * @return a JSON object with a success attribute.
     */
    public ModelAndView closeSeries(HttpServletRequest request,
                                     HttpServletResponse response) {

        File seriesDir = (File)request.getSession().getAttribute("seriesDir");
        final ModelAndView mav = new ModelAndView("jsonView");

        if (seriesDir != null) {
            logger.debug("Deleting checkout at "+seriesDir.getPath());
            Util.deleteFilesRecursive(seriesDir);
        }

        request.getSession().removeAttribute("seriesDir");
        request.getSession().removeAttribute("seriesId");
        request.getSession().removeAttribute("seriesRev");
        mav.addObject("success", true);
        return mav;
    }

    /**
     * Creates a new series for the current user.
     *
     * @param request The servlet request
     * @param response The servlet response
     *
     * @return A JSON object
     */
    public ModelAndView createSeries(HttpServletRequest request,
                                      HttpServletResponse response) {

        final String user = request.getRemoteUser();
        final String name = request.getParameter("name");
        String description = request.getParameter("description");
        final ModelAndView mav = new ModelAndView("jsonView");
        String errorString = null;
        long seriesId = -1;
        long seriesRevision = -1;

        if (!isLoggedIn(request)) {
            errorString = ErrorMessages.SESSION_EXPIRED;
            logger.warn("ServiceInterface is null!");
        } else if (name == null || name.length() < 3) {
            errorString = ErrorMessages.MISSING_PARAMETER;
            logger.warn("No/Invalid series name specified!");
        } else {
            List<VRLSeries> series = jobManager.getSeriesByUser(user);
            Iterator<VRLSeries> it = series.listIterator();
            while (it.hasNext()) {
                if (name.equals(it.next().getName())) {
                    errorString = ErrorMessages.SERIES_EXISTS;
                    logger.warn(errorString);
                    break;
                }
            }
        }

        if (errorString == null) {
            try {
                logger.debug("Creating new series '"+name+"'.");
                if (description == null) {
                    description = "";
                }
                VRLSeries series = jobManager.createSeries(
                        user, name, description);
                // we can now access the new ID and revision
                seriesId = series.getId();
                RevisionEntry rev = jobManager.getLastSeriesRevision(
                        user, seriesId);
                seriesRevision = rev.getRevision();
            } catch (Exception e) {
                errorString = ErrorMessages.INTERNAL_ERROR;
                logger.error(e.getMessage(), e);
            }
        }

        if (errorString != null) {
            mav.addObject("error", errorString);
            mav.addObject("success", false);
        } else {
            mav.addObject("seriesId", seriesId);
            mav.addObject("revision", seriesRevision);
            mav.addObject("success", true);
        }

        return mav;
    }

    /**
     * Returns a JSON object containing an array of revisions of the given
     * series or an array of series of the current user if the special
     * series-root node is specified.
     *
     * @param request The servlet request including a node parameter
     * @param response The servlet response
     *
     * @return null on success or a JSON object with an error parameter on
     *         failure. The JSON result is directly written to the response.
     */
    public ModelAndView listSeries(HttpServletRequest request,
                                   HttpServletResponse response) {

        final boolean demo = Boolean.valueOf(request.getParameter("demo"));
        final String user = (demo == true ? "demo" : request.getRemoteUser());
        final String node = request.getParameter("node");
        String errorString = null;

        if (!isLoggedIn(request)) {
            errorString = ErrorMessages.SESSION_EXPIRED;
            logger.warn("ServiceInterface is null!");
        } else if (node == null) {
            errorString = ErrorMessages.MISSING_PARAMETER;
            logger.warn("No node specified!");
        } else if (node.equals("series-root")) {
            List<VRLSeries> series = jobManager.getSeriesByUser(user);
            JSONArray jsSeries = (JSONArray)JSONSerializer.toJSON(series);
            Iterator it = jsSeries.listIterator();
            while (it.hasNext()) {
                JSONObject entry = (JSONObject)it.next();
                try {
                    long seriesId = entry.getLong("id");
                    RevisionEntry rev = jobManager.getLastSeriesRevision(
                            user, seriesId);
                    entry.put("id", "series-"+entry.getString("id"));
                    entry.put("date", rev.getDate().getTime());
                    entry.put("seriesId", seriesId);
                    entry.put("revision", String.valueOf(rev.getRevision()));
                    if (demo) {
                        entry.put("isExample", true);
                    }
                } catch (Exception e) {
                    logger.error("Error retrieving series revision for user "
                            + user, e);
                }
            }
            String json = jsSeries.toString();
            response.setContentType("application/json");
            try {
                response.getWriter().print(json);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        } else if (node.startsWith("series-")) {
            String idStr = node.substring(7);
            try {
                long seriesId = Long.parseLong(idStr);
                logger.debug("Retrieving logs for series "+idStr);
                RevisionLog[] logs = jobManager.getRevisionsBySeries(
                        user, seriesId);
                JSONArray jsLogs = new JSONArray();
                for (RevisionLog log : logs) {
                    JSONObject jsEntry = new JSONObject();
                    jsEntry.put("date", log.getDate().getTime());
                    jsEntry.put("description", log.getMessage());
                    jsEntry.put("leaf", true);
                    jsEntry.put("name", String.valueOf(log.getRevision()));
                    jsEntry.put("seriesId", seriesId);
                    jsEntry.put("revision", log.getRevision());
                    if (demo) {
                        jsEntry.put("isExample", true);
                    }
                    jsLogs.add(jsEntry);
                }
                response.setContentType("application/json");
                response.getWriter().print(jsLogs.toString());
            } catch (NumberFormatException e) {
                errorString = ErrorMessages.MISSING_PARAMETER;
                logger.warn("Error parsing series ID " + idStr);
            } catch (IOException e) {
                errorString = ErrorMessages.INTERNAL_ERROR;
                logger.error(e.getMessage(), e);
            } catch (Exception e) {
                errorString = ErrorMessages.INTERNAL_ERROR;
                logger.error(e.getMessage(), e);
            }
        } else {
            errorString = ErrorMessages.MISSING_PARAMETER;
            logger.warn("Invalid node " + node);
        }

        if (errorString == null) {
            return null;
        } else {
            ModelAndView mav = new ModelAndView("jsonView");
            mav.addObject("success", false);
            mav.addObject("error", errorString);
            return mav;
        }
    }

    /**
     * Performs a checkout of given series and revision making it active in
     * the user's session and returns details about the series.
     *
     * @param request The servlet request including a seriesId and revision
     *                parameter
     * @param response The servlet response
     *
     * @return a JSON object with an error parameter on failure.
     */
    public ModelAndView openSeries(HttpServletRequest request,
                                   HttpServletResponse response) {

        final String user = request.getRemoteUser();
        final String seriesIdStr = request.getParameter("seriesId");
        final String revisionStr = request.getParameter("revision");
        final ModelAndView mav = new ModelAndView("jsonView");
        String errorString = null;
        VRLSeries series = null;
        long revision = -1;
        Long seriesId = null;

        if (seriesIdStr != null) {
            try {
                seriesId = new Long(seriesIdStr);
                series = jobManager.getSeriesById(seriesId.longValue());
            } catch (NumberFormatException e) {
                logger.warn("Error parsing series ID " + seriesIdStr);
            }
        }

        if (revisionStr != null) {
            try {
                revision = Long.parseLong(revisionStr);
            } catch (NumberFormatException e) {
                logger.warn("Error parsing revision number " + revisionStr);
            }
        }

        if (!isLoggedIn(request)) {
            errorString = ErrorMessages.SESSION_EXPIRED;
            logger.warn("ServiceInterface is null!");
        } else if (series == null || revision < 0) {
            errorString = ErrorMessages.MISSING_PARAMETER;
            logger.warn(errorString);
        } else if (!series.getUser().equals(user) &&
                    !series.getUser().equals("demo")) {
            errorString = ErrorMessages.NOT_AUTHORIZED;
            logger.warn(user+" tried to open "+series.getUser()+"'s series");
        } else {
            try {
                File seriesDir = jobManager.checkoutSeriesRevision(
                        series.getUser(), seriesId.longValue(), revisionStr);
                logger.debug("Checked out series to "+seriesDir.getPath());
                File oldDir = (File)request.getSession()
                        .getAttribute("seriesDir");
                if (oldDir != null) {
                    logger.debug("Deleting old checkout "+oldDir.getPath());
                    Util.deleteFilesRecursive(oldDir);
                }

                request.getSession().setAttribute("seriesDir", seriesDir);
                request.getSession().setAttribute("seriesId", seriesId);
                request.getSession().setAttribute("seriesRev",
                        new Long(revision));

                if (series.getUser().equals("demo")) {
                    mav.addObject("isExample", true);
                }
                mav.addObject("seriesId", seriesId.longValue());
                mav.addObject("name", series.getName());
                mav.addObject("description", series.getDescription());
                mav.addObject("creationDate", series.getCreationDate());
                RevisionEntry entry = jobManager.getLastSeriesRevision(
                        series.getUser(), seriesId.longValue());
                mav.addObject("lastModified", entry.getDate().getTime());
                mav.addObject("latestRevision", entry.getRevision());
                RevisionLog log = jobManager.getRevisionLog(
                        series.getUser(), seriesId.longValue(), revisionStr);
                mav.addObject("revision", revision);
                mav.addObject("revisionLog", log.getMessage());
            } catch (Exception e) {
                errorString = ErrorMessages.INTERNAL_ERROR;
                logger.error(e.getMessage(), e);
            }
        }

        if (errorString != null) {
            mav.addObject("error", errorString);
            mav.addObject("success", false);
        } else {
            mav.addObject("success", true);
        }

        return mav;
    }

    /**
     * Commits modified files of the active series to the revision control
     * system.
     *
     * @param request The servlet request including a message parameter
     * @param response The servlet response
     *
     * @return a JSON object with an error parameter on failure.
     */
    public ModelAndView saveSeries(HttpServletRequest request,
                                   HttpServletResponse response) {

        File seriesDir = (File)request.getSession().getAttribute("seriesDir");
        Long seriesId = (Long)request.getSession().getAttribute("seriesId");
        final String user = request.getRemoteUser();
        final String message = request.getParameter("message");
        final ModelAndView mav = new ModelAndView("jsonView");
        String errorString = null;
        VRLSeries series = null;
        long revision = -1;

        if (seriesId != null) {
            series = jobManager.getSeriesById(seriesId.longValue());
        }

        if (!isLoggedIn(request)) {
            errorString = ErrorMessages.SESSION_EXPIRED;
            logger.warn("ServiceInterface is null!");
        } else if (series == null) {
            errorString = ErrorMessages.NO_SERIES;
            logger.warn(errorString);
        } else if (!series.getUser().equals(user)) {
            errorString = ErrorMessages.NOT_AUTHORIZED;
            logger.warn(user+" tried to save "+series.getUser()+"'s series.");
        } else if (message == null) {
            errorString = ErrorMessages.MISSING_PARAMETER;
            logger.warn(errorString);
        } else {
            logger.debug("Committing changes in "+seriesDir.getPath());
            try {
                revision = jobManager.saveRevision(seriesDir, message);
                mav.addObject("revision", revision);
                if (revision >= 0) {
                    RevisionEntry entry = jobManager.getLastSeriesRevision(
                            user, seriesId.longValue());
                    mav.addObject("lastModified", entry.getDate().getTime());
                    mav.addObject("latestRevision", revision);
                    mav.addObject("revisionLog", message);
                }
            } catch (Exception e) {
                errorString = ErrorMessages.INTERNAL_ERROR;
                logger.error(e.getMessage(), e);
            }
        }

        if (errorString != null) {
            mav.addObject("error", errorString);
            mav.addObject("success", false);
        } else {
            mav.addObject("success", true);
        }

        return mav;
    }
}

