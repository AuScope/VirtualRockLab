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
        String user = request.getParameter("username");

        // the user tried to login
        if (user != null) {
            String pass = request.getParameter("password");
            if (pass == null) {
                pass = "";
            }

            if (gridAccess.initProxy(user, pass)) {
                session.setAttribute("myProxyUser", user);
                session.setAttribute("myProxyPass", pass);
                return new ModelAndView(
                        new RedirectView("joblist.html", true, false, false));
            }
        }

        // check if user is already logged in
        if (session.getAttribute("myProxyUser") != null) {
            return new ModelAndView(
                    new RedirectView("joblist.html", true, false, false));
        }
        
        return new ModelAndView("login");
    }
}

