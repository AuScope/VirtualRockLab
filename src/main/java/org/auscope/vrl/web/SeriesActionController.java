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
        final String user = request.getRemoteUser();
        String name = request.getParameter("name");
        String description = request.getParameter("description");
        File seriesDir = (File)request.getSession().getAttribute("seriesDir");
        Long seriesId = (Long)request.getSession().getAttribute("seriesId");
        String errorString = null;
        VRLSeries series = null;
        long newId = -1;
        ModelAndView mav = new ModelAndView("jsonView");

        if (seriesId != null) {
            series = jobManager.getSeriesById(seriesId.longValue());
        } else {
            logger.warn("No series ID specified!");
        }

        if (seriesDir == null || series == null) {
            errorString = new String("No active series in session");
            logger.error("seriesDir or series is null!");
        } else if (name == null || name.length() < 3) {
            errorString = new String("Invalid series name");
            logger.error(errorString);
        } else {
            // prevent duplicate names
            List<VRLSeries> uSeries = jobManager.getSeriesByUser(user);
            Iterator<VRLSeries> it = uSeries.listIterator();
            while (it.hasNext()) {
                if (name.equals(it.next().getName())) {
                    errorString = new String("A series by that name already exists");
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
                        series, user, name, description);
                newId = newSeries.getId();
            } catch (Exception e) {
                errorString = new String("Could not clone series");
                logger.error(errorString, e);
            }
        }

        if (errorString != null) {
            mav.addObject("error", errorString);
            mav.addObject("success", false);
        } else {
            mav.addObject("seriesId", newId);
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

        ModelAndView mav = new ModelAndView("jsonView");
        File seriesDir = (File)request.getSession().getAttribute("seriesDir");
        if (seriesDir != null) {
            logger.debug("Deleting work dir at "+seriesDir.getPath());
            Util.deleteFilesRecursive(seriesDir);
        }

        request.getSession().removeAttribute("seriesDir");
        request.getSession().removeAttribute("seriesId");

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
        String name = request.getParameter("name");
        String description = request.getParameter("description");
        long seriesId = -1;
        String errorString = null;
        ModelAndView mav = new ModelAndView("jsonView");

        if (name == null || name.length() < 3) {
            errorString = new String("Invalid series name");
            logger.error(errorString);
        } else {
            List<VRLSeries> series = jobManager.getSeriesByUser(user);
            Iterator<VRLSeries> it = series.listIterator();
            while (it.hasNext()) {
                if (name.equals(it.next().getName())) {
                    errorString = new String("A series by that name already exists");
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
                // we can now access the new ID
                seriesId = series.getId();
            } catch (Exception e) {
                errorString = new String("Could not create series");
                logger.error(errorString, e);
            }
        }

        if (errorString != null) {
            mav.addObject("error", errorString);
            mav.addObject("success", false);
        } else {
            mav.addObject("seriesId", seriesId);
            mav.addObject("success", true);
        }

        return mav;
    }

    /**
     * Returns a JSON object containing an array of series of the current user.
     *
     * @param request The servlet request including a node parameter
     * @param response The servlet response
     *
     * @return null on success or a JSON object with an error parameter on
     *         failure. The JSON result is directly written to the response.
     */
    public ModelAndView listSeries(HttpServletRequest request,
                                     HttpServletResponse response) {

        String errorString = null;
        final String node = request.getParameter("node");
        final boolean demo = Boolean.valueOf(request.getParameter("demo"));
        final String user = (demo == true ? "demo" : request.getRemoteUser());

        if (node == null) {
            logger.warn("No node specified!");
            errorString = new String("Invalid request");
        } else if (node.equals("series-root")) {
            List<VRLSeries> series = jobManager.getSeriesByUser(user);
            JSONArray jsSeries = (JSONArray)JSONSerializer.toJSON(series);
            Iterator it = jsSeries.listIterator();
            while (it.hasNext()) {
                JSONObject entry = (JSONObject)it.next();
                long seriesId = entry.getLong("id");
                entry.put("id", "series-"+entry.getString("id"));
                entry.put("date", entry.getLong("creationDate"));
                entry.put("leaf", true);
                entry.put("seriesId", seriesId);
                if (demo) {
                    entry.put("isExample", true);
                }
            }
            String json = jsSeries.toString();
            response.setContentType("application/json");
            try {
                response.getWriter().print(json);
            } catch (IOException e) {
                logger.error("Error writing response!");
            }
        } else {
            logger.warn("Invalid node "+node);
            errorString = new String("Invalid request");
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
     * Makes the given series active in the user's session and returns its
     * details.
     *
     * @param request The servlet request including a seriesId parameter
     * @param response The servlet response
     *
     * @return a JSON object with an error parameter on failure.
     */
    public ModelAndView openSeries(HttpServletRequest request,
                                    HttpServletResponse response) {

        final String user = request.getRemoteUser();
        String seriesIdStr = request.getParameter("seriesId");
        VRLSeries series = null;
        Long seriesId = null;
        ModelAndView mav = new ModelAndView("jsonView");
        String errorString = null;

        if (seriesIdStr != null) {
            try {
                seriesId = new Long(seriesIdStr);
                series = jobManager.getSeriesById(seriesId.longValue());
            } catch (NumberFormatException e) {
                logger.error("Error parsing series ID!");
            }
        } else {
            logger.warn("No series ID specified!");
        }

        if (series == null) {
            errorString = new String("The requested series does not exist");
            logger.error(errorString);
        } else if (!series.getUser().equals(user) &&
                    !series.getUser().equals("demo")) {
            errorString = new String(
                "You can only open your own series and examples");
            logger.warn(user+" tried to open "+series.getUser()+"'s series");
        } else {
            try {
                File seriesDir = jobManager.openSeries(
                        series.getUser(), seriesId.longValue());
                logger.debug("Opened series at "+seriesDir.getPath());
                File oldDir = (File)request.getSession()
                        .getAttribute("seriesDir");
                if (oldDir != null) {
                    logger.debug("Deleting old checkout "+oldDir.getPath());
                    Util.deleteFilesRecursive(oldDir);
                }

                request.getSession().setAttribute("seriesDir", seriesDir);
                request.getSession().setAttribute("seriesId", seriesId);

                if (series.getUser().equals("demo")) {
                    mav.addObject("isExample", true);
                }
                mav.addObject("seriesId", seriesId.longValue());
                mav.addObject("name", series.getName());
                mav.addObject("description", series.getDescription());
                mav.addObject("creationDate", series.getCreationDate());
            } catch (Exception e) {
                errorString = new String("Error accessing series files");
                logger.error(errorString, e);
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

