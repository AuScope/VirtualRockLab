package org.auscope.vrl.web;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.auscope.vrl.GridAccessController;

public class LoginController implements Controller {

    protected final Log logger = LogFactory.getLog(getClass());

    private GridAccessController gridAccess;


    public void setGridAccess(GridAccessController gridAccess) {
        this.gridAccess = gridAccess;
    }

    public ModelAndView handleRequest(HttpServletRequest request,
                                      HttpServletResponse response)
            throws Exception {

        HttpSession session = request.getSession();
        String user;
        String pass;

        user = request.getParameter("username");

        if (user != null) {
            // if the user just logged in store username and password in
            // the session object
            pass = request.getParameter("password");
            if (pass == null) {
                pass = "";
            }

            logger.info("Trying to initialize proxy with new login details");
            if (gridAccess.initProxy(user, pass)) {
                session.setAttribute("myProxyUser", user);
                session.setAttribute("myProxyPass", pass);
                return new ModelAndView(
                        new RedirectView("joblist.html", true, false, false));
            } else {
                logger.info("Proxy init failed. Resetting session attributes.");
                session.removeAttribute("myProxyUser");
                session.removeAttribute("myProxyPass");
            }

        } else if ((user = (String)session.getAttribute("myProxyUser")) != null) {
            logger.info("User is already logged in. Re-initializing proxy.");
            pass = (String)session.getAttribute("myProxyPass");
            if (gridAccess.initProxy(user, pass)) {
                return new ModelAndView(
                        new RedirectView("joblist.html", true, false, false));
            }
        }
        
        return new ModelAndView("login");
    }
}

