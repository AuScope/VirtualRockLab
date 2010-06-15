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

import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

/**
 * Controller for the Access Error page.
 *
 * @author Cihan Altinay
 */
public class AccessErrorController implements Controller {

    protected final Log logger = LogFactory.getLog(getClass());
    private MailSender mailSender;

    public void setMailSender(MailSender mailSender) {
        this.mailSender = mailSender;
    }

    public ModelAndView handleRequest(HttpServletRequest request,
                                      HttpServletResponse response) {

        if (request.getMethod().equalsIgnoreCase("POST")
                && "register".equals(request.getParameter("action"))) {
            logger.info("POST received from "+request.getRemoteUser());
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo("c.altinay@uq.edu.au");
            msg.setCc("d.weatherley@uq.edu.au");
            msg.setText("A user has requested to be registered with the VRL.\n"
                    +"\nUser Details:\n"
                    +"\nName: "+request.getHeader("Shib-Person-commonName")
                    +"\nEmail: "+request.getRemoteUser()
                    +"\nOrganisation: "+request.getHeader("Shib-EP-OrgDN")
                    +"\nAffiliation: "+request.getHeader("Shib-EP-Affiliation")
                    +"\nToken: "+request.getHeader("Shib-AuEduPerson-SharedToken")
                    +"\n"
                    );
            msg.setFrom("VRL Portal <webmaster@esscc.uq.edu.au>");
            msg.setSubject("VRL Registration Request");
            try {
                mailSender.send(msg);
            } catch (MailException e) {
                logger.error(e.getMessage(), e);
            }
        }

        logger.info("Returning access_error view.");
        return new ModelAndView("access_error");
    }
}

