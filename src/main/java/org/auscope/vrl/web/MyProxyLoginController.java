package org.auscope.vrl.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.auscope.vrl.GridAccessController;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.view.RedirectView;

public class MyProxyLoginController implements Controller {

    protected final Log logger = LogFactory.getLog(getClass());
    private static final int PROXY_LIFETIME = 6*60*60;
    private GridAccessController gridAccess;

    public void setGridAccess(GridAccessController gridAccess) {
        this.gridAccess = gridAccess;
    }

    public ModelAndView handleRequest(HttpServletRequest request,
                                      HttpServletResponse response) {

        String user;
        String pass;
        String error = null;

        user = request.getParameter("username");

        if (user != null) {
            pass = request.getParameter("password");
            if (pass == null) {
                pass = "";
            }

            logger.info("Trying to initialize proxy with MyProxy details");
            if (gridAccess.initProxy(user, pass, PROXY_LIFETIME)) {
                return new ModelAndView(
                        new RedirectView("joblist.html", true, false, false));
            } else {
                logger.info("Proxy initialisation failed.");
                error = new String("Could not initialize grid proxy with entered MyProxy details!");
            }

        }

        return new ModelAndView("myproxylogin", "error", error);
    }
}

