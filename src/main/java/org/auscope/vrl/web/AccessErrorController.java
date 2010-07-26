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

import org.auscope.vrl.PropertyConfigurer;

import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

import org.springframework.security.AuthenticationException;
import org.springframework.security.DisabledException;
import org.springframework.security.userdetails.UsernameNotFoundException;

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
    private PropertyConfigurer propertyConfigurer;

    public void setMailSender(MailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void setPropertyConfigurer(PropertyConfigurer propertyConfigurer) {
        this.propertyConfigurer = propertyConfigurer;
    }

    public ModelAndView handleRequest(HttpServletRequest request,
                                      HttpServletResponse response) {

        ModelAndView mav = new ModelAndView("access_error");
        final String user = request.getRemoteUser();
        final String shibCN = propertyConfigurer.resolvePlaceholder(
                "shib.commonName");
        final String shibOrg = propertyConfigurer.resolvePlaceholder(
                "shib.organisation");
        final String shibAffil = propertyConfigurer.resolvePlaceholder(
                "shib.affiliation");
        final String shibToken = propertyConfigurer.resolvePlaceholder(
                "shib.sharedToken");
        final String emailCc = propertyConfigurer.resolvePlaceholder(
                "registration.emailCc");
        AuthenticationException ex = (AuthenticationException) request
            .getSession().getAttribute("SPRING_SECURITY_LAST_EXCEPTION");

        if (request.getMethod().equalsIgnoreCase("POST")
                && "register".equals(request.getParameter("action"))) {

            logger.info("POST received from "+user);
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(propertyConfigurer.resolvePlaceholder(
                    "registration.emailTo"));
            if (emailCc != null && !emailCc.isEmpty()) {
                msg.setCc(emailCc);
            }
            msg.setText("A user has requested to be registered with the VRL.\n"
                    +"\nUser Details:\n"
                    +"\nName: "+request.getHeader(shibCN)
                    +"\nEmail: "+user
                    +"\nOrganisation: "+request.getHeader(shibOrg)
                    +"\nAffiliation: "+request.getHeader(shibAffil)
                    +"\nToken: "+request.getHeader(shibToken)
                    +"\n"
                    );
            msg.setFrom(request.getHeader(shibCN)+" <"+user+">");
            msg.setSubject("VRL Registration Request");

            try {
                mailSender.send(msg);
                mav.addObject("show", "messageSent");
            } catch (MailException e) {
                logger.error(e.getMessage(), e);
                mav.addObject("show", "messageSendError");
                mav.addObject("notify", msg.getTo());
            }

        } else if (ex instanceof DisabledException) {
            mav.addObject("show", "accountDisabled");

        } else if (ex instanceof UsernameNotFoundException) {
            mav.addObject("show", "notRegistered");
            mav.addObject("commonName", request.getHeader(shibCN));
            mav.addObject("organisation", request.getHeader(shibOrg));
            mav.addObject("affiliation", request.getHeader(shibAffil));
            mav.addObject("sharedToken", request.getHeader(shibToken));
            mav.addObject("email", user);
            mav.addObject("notify", propertyConfigurer.resolvePlaceholder(
                    "registration.emailTo"));
        }

        logger.info("Returning access_error view.");
        return mav;
    }
}

