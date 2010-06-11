/*
 * This file is part of the AuScope Virtual Rock Lab (VRL) project.
 * Copyright (c) 2010 The University of Queensland, ESSCC
 *
 * Licensed under the terms of the GNU Lesser General Public License.
 */
package org.auscope.vrl.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Main controller class for the Virtual Rock Lab.
 *
 * @author Cihan Altinay
 */
public class HomeController implements Controller {

    /** Logger for this class */
    private final Log logger = LogFactory.getLog(getClass());

    public ModelAndView handleRequest(HttpServletRequest request,
                                      HttpServletResponse response) {

        // Ensure Grid Service is initialized
        if (request.getSession().getAttribute("grisuService") != null) {
            return new ModelAndView("home");
        } else {
            request.getSession().setAttribute(
                    "redirectAfterLogin", "/home.html");
            logger.debug("ServiceInterface not initialized. Redirecting to login page.");
            return new ModelAndView(
                    new RedirectView("/login.html", true, false, false));
        }
    }
}

