package org.auscope.vrl.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;
import org.springframework.web.servlet.view.RedirectView;

/**
 *
 */
public class ScriptBuilderController extends MultiActionController {

    protected final Log logger = LogFactory.getLog(getClass());
    
    private String scriptName;
    private String scriptText;
    
    protected ModelAndView handleNoSuchRequestHandlingMethod(
            NoSuchRequestHandlingMethodException ex,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        logger.info("No/invalid action parameter. Returning scriptbuilder view");
        scriptName = null;
        scriptText = null;
        String scriptFile = request.getParameter("usescript");
        if (scriptFile != null) {
            logger.info("Script source to edit provided.");
            String tempDir = System.getProperty("java.io.tmpdir");
            try {
                BufferedReader input = new BufferedReader(
                    new FileReader(tempDir+File.separator+scriptFile));
                StringBuffer contents = new StringBuffer();
                String line = null;
                while ((line = input.readLine()) != null) {
                    contents.append(line).append(
                            System.getProperty("line.separator"));
                }
                input.close();
                if (scriptFile.lastIndexOf(".py") > 0) {
                    scriptName = scriptFile.substring(0,
                            scriptFile.lastIndexOf(".py"));
                } else {
                    scriptName = scriptFile;
                }
                scriptText = contents.toString();

            } catch (IOException e) {
                logger.error("Error reading file.");
            }
        }

        return new ModelAndView("scriptbuilder");
    }

    public ModelAndView downloadScript(HttpServletRequest request,
                                       HttpServletResponse response)
            throws Exception {

        logger.info("User requested script download");
        String script = request.getParameter("sourcetext");
        if (script != null) {
            String scriptName = request.getParameter("scriptname");
            if (scriptName == null) {
                scriptName = "particle_script";
            }
            response.setContentType("text/plain");
            response.setContentLength(script.length());
            response.setHeader("Content-Disposition",
                    "attachment; filename=\""+scriptName+".py\"");

            PrintWriter writer = response.getWriter();
            writer.print(script);
            writer.close();
            return null;
        }
        logger.info("No source text provided. Returning scriptbuilder view.");
        return new ModelAndView("scriptbuilder");
    }

    public ModelAndView useScript(HttpServletRequest request,
                                  HttpServletResponse response)
            throws Exception {

        logger.info("User requested script use");
        String script = request.getParameter("sourcetext");
        if (script != null) {
            String scriptName = request.getParameter("scriptname");
            if (scriptName == null) {
                scriptName = "particle_script";
            }

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
        logger.info("No source text provided. Returning scriptbuilder view.");
        return new ModelAndView("scriptbuilder");
    }

    public ModelAndView getScriptText(HttpServletRequest request,
                                      HttpServletResponse response)
            throws Exception {

        logger.info("Script source requested.");
        ModelAndView mav = new ModelAndView("jsonView");
        if (scriptText != null) {
            mav.addObject("scriptName", scriptName);
            mav.addObject("scriptText", scriptText);
        }
        return mav;
    }
}

