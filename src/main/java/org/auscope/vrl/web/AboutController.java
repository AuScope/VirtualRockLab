package org.auscope.vrl.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

/**
 * Controller for the About popup window.
 */
public class AboutController implements Controller {

    protected final Log logger = LogFactory.getLog(getClass());

    public ModelAndView handleRequest(HttpServletRequest request,
                                      HttpServletResponse response) {

        String appServerHome = request.getSession().getServletContext()
            .getRealPath("/");
        File manifestFile = new File(appServerHome, "META-INF/MANIFEST.MF");
        Manifest mf = new Manifest();
        ModelAndView mav = new ModelAndView("about");

        try {
            mf.read(new FileInputStream(manifestFile));
            Attributes atts = mf.getMainAttributes();
            if (mf != null) {
                mav.addObject("specificationTitle", atts.getValue("Specification-Title"));
                mav.addObject("implementationVersion", atts.getValue("Implementation-Version"));
                mav.addObject("implementationBuild", atts.getValue("Implementation-Build"));
                mav.addObject("buildDate", atts.getValue("buildDate"));
                mav.addObject("buildJdk", atts.getValue("Build-Jdk"));
                mav.addObject("javaVendor", atts.getValue("javaVendor"));
                mav.addObject("builtBy", atts.getValue("Built-By"));
                mav.addObject("osName", atts.getValue("osName"));
                mav.addObject("osVersion", atts.getValue("osVersion"));
                            
                mav.addObject("serverName", request.getServerName());
                mav.addObject("serverInfo", request.getSession().getServletContext().getServerInfo());
                mav.addObject("serverJavaVersion", System.getProperty("java.version"));
                mav.addObject("serverJavaVendor", System.getProperty("java.vendor"));
                mav.addObject("javaHome", System.getProperty("java.home"));
                mav.addObject("serverOsArch", System.getProperty("os.arch"));
                mav.addObject("serverOsName", System.getProperty("os.name"));
                mav.addObject("serverOsVersion", System.getProperty("os.version"));
            } else {
                logger.error("Error reading manifest file.");
            }
        } catch (IOException e) {
            /* ignore, since we'll just leave an empty form */
            e.printStackTrace();
        }
        return mav;
    }
}

