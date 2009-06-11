package org.auscope.vrl.web.security;

import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.security.ui.FilterChainOrder;
import org.springframework.security.ui.preauth.AbstractPreAuthenticatedProcessingFilter;

/**
 * This AbstractPreAuthenticatedProcessingFilter implementation obtains the
 * username from request headers populated by an external Shibboleth
 * authentication system.
 * 
 * @author san218
 */
public class ShibPreAuthenticatedProcessingFilter
        extends AbstractPreAuthenticatedProcessingFilter {

    protected final Log logger = LogFactory.getLog(getClass());

    protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
        if (logger.isDebugEnabled()) {
            StringBuffer buf = new StringBuffer();
            Enumeration eHeaders = request.getHeaderNames();
            while (eHeaders.hasMoreElements()) {
                String name = (String) eHeaders.nextElement();
                if ((name.matches("Shib-.*") || name.matches("shib-.*")) &&
                    !name.equals("Shib-Attributes"))
                {
                    String value = request.getHeader(name).toString();
                    buf.append(name + " = " + value + "\n");
                }
            }
            logger.debug("Shibboleth attributes:\n"+buf.toString());
        }
        logger.info("Returning "+request.getHeader("Shib-Mail"));
        return request.getHeader("Shib-Mail");
    }
    
    protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
       // no password - user is already authenticated
       return "NONE";
    }
    
    public int getOrder() {
       return FilterChainOrder.PRE_AUTH_FILTER;
    }   
}

