package org.auscope.vrl.web;

import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ScriptBuilderController extends AbstractController {

    protected final Log logger = LogFactory.getLog(getClass());
    
    public ModelAndView handleRequestInternal(HttpServletRequest request,
                                              HttpServletResponse response)
            throws Exception {

        String action = request.getParameter("scriptaction");
        String script = request.getParameter("sourcetext");

        if (action != null && script != null) {
            String scriptName = request.getParameter("scriptname");
            if (scriptName == null)
                scriptName = "particle_script";

            if (action.equals("download")) {
                logger.info("User requested script download");
                response.setContentType("text/plain");
                response.setContentLength(script.length());
                response.setHeader("Content-Disposition",
                        "attachment; filename=\""+scriptName+".py\"");

                PrintWriter writer = response.getWriter();
                writer.print(script);
                writer.close();
                return null;

            } else if (action.equals("use")) {
                logger.info("User requested script use");

                try {
                    String tempDir = System.getProperty("java.io.tmpdir");
                    File scriptFile = new File(
                            tempDir+File.separator+scriptName+".py");
                    scriptFile.deleteOnExit();
                    PrintWriter writer = new PrintWriter(scriptFile);
                    writer.print(script);
                    writer.close();

                } catch (IOException e) {
                    logger.error("Could not create temp file: " + e.getMessage());
                }

                ModelAndView mav =  new ModelAndView(
                        new RedirectView("gridsubmit.html"));
                mav.addObject("newscript", scriptName);
                return mav;
            }
        }

        logger.info("Returning scriptbuilder view");
        return new ModelAndView("scriptbuilder");
    }
}

