/*
 * This file is part of the AuScope Virtual Rock Lab (VRL) project.
 * Copyright (c) 2010 The University of Queensland, ESSCC
 *
 * Licensed under the terms of the GNU Lesser General Public License.
 */
package org.auscope.vrl.web;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.ssl.PKCS8Key;

import org.auscope.vrl.GridAccessController;

// see http://www.hpc.jcu.edu.au/projects/archer-data-activities/browser/security/current/slcs-common/src/
// for source code of these classes
import org.glite.slcs.pki.CertificateExtension;
import org.glite.slcs.pki.CertificateExtensionFactory;
import org.glite.slcs.pki.CertificateKeys;
import org.glite.slcs.pki.CertificateRequest;

import org.globus.gsi.CertUtil;

import org.springframework.security.context.SecurityContextHolder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.view.RedirectView;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Controller that handles Grid proxy initialisation using either a SLCS
 * certificate or MyProxy details entered by the user.
 * If the IdP releases a Shared Token attribute it is forwarded to the ARCS
 * SLCS service to retrieve a certificate which is subsequently used to
 * generate a Grid proxy. Otherwise the user is presented with a
 * login form to enter MyProxy details which in turn are used to access
 * the Grid. If login attempts fail three times the session
 * is destroyed and the user is redirected to the Shibboleth logout page.
 *
 * @author Cihan Altinay
 */
public class LoginController implements Controller {

    protected final Log logger = LogFactory.getLog(getClass());

    private static final String SLCS_URL = "https://slcs1.arcs.org.au/SLCS/";
    private static final String HOST_KEY_FILE = "/etc/shibboleth/hostkey.pem";
    /** Lifetime of the generated proxy in seconds (10 days) */
    private static final int PROXY_LIFETIME = 10*24*60*60;
    /** maximum number of login failures */
    private static final int MAX_ATTEMPTS = 3;

    private GridAccessController gridAccess;

    private class RequestData {
        public String authToken;
        public String certDN;
        public List certExtensions;
    }

    /**
     * Sets the <code>GridAccessController</code> to be used for proxy checking
     * and initialisation.
     *
     * @param gridAccess the GridAccessController to use
     */
    public void setGridAccess(GridAccessController gridAccess) {
        this.gridAccess = gridAccess;
    }

    /**
     * Main entry point which decides where to redirect to.
     * If this is a GET request and the current grid proxy is not valid then
     * a redirect to the SLCS server is performed. A POST request is handled
     * as being a response from the SLCS server so the certificate is extracted
     * and a grid proxy is generated.
     *
     * @return an appropriate ModelAndView
     */
    public ModelAndView handleRequest(HttpServletRequest request,
                                      HttpServletResponse response) {

        Integer loginAttempts = (Integer)request.getSession()
            .getAttribute("loginAttempt");
        String errorMessage = null;
        
        if (loginAttempts == null) {
            loginAttempts = new Integer(1);
        }

        if (loginAttempts.intValue() <= MAX_ATTEMPTS) {
            logger.debug(request.getRemoteUser() + "'s login attempt #"
                    + loginAttempts.toString());
            try {
                // CASE 1: MyProxy login attempt
                if (request.getParameter("proxyuser") != null) {
                    logger.debug("Handling MyProxy login.");
                    handleMyProxyLogin(request);
                    return redirectToTarget(request);

                // CASE 2: No shared token -> show login form
                } else if (request
                        .getHeader("Shib-AuEduPerson-SharedToken") == null) {
                    logger.debug("No shared token.");

                // CASE 3: valid shared token and GET request
                } else if (request.getMethod().equalsIgnoreCase("GET")) {
                    logger.debug("Handling GET request.");

                    // CASE 3.1: Grid proxy valid -> nothing to do
                    Object credential = request.getSession().getAttribute("userCred");
                    if (gridAccess.isProxyValid(credential)) {
                        logger.debug("Valid proxy found.");
                        return redirectToTarget(request);

                    // CASE 3.2: No Grid proxy -> request SLCS cert
                    } else {
                        final String serviceUrl = "https://"
                            + request.getServerName() + "/vrl/login.html";
                        return redirectToSlcs(serviceUrl);
                    }

                // CASE 4: valid shared token and POST request (from SLCS)
                } else if (request.getMethod().equalsIgnoreCase("POST")) {
                    logger.debug("Handling POST request.");
                    processSlcsResponse(request);
                    return redirectToTarget(request);
                }
            } catch (Exception e) {
                errorMessage = new String(e.getMessage());
                logger.error(errorMessage, e);
                loginAttempts = new Integer(loginAttempts.intValue()+1);
                request.getSession().setAttribute(
                        "loginAttempt", loginAttempts);
            }
        } else {
            logger.warn("Too many failed login attempts. Killing session.");
            return doLogout(request);
        }

        logger.debug("Returning login view.");
        return new ModelAndView("login", "error", errorMessage);
    }

    /**
     * Invalidates the security context and the session and returns a redirect
     * to Shibboleth logout.
     */
    private ModelAndView doLogout(HttpServletRequest request) {
        request.getSession(false).invalidate();
        SecurityContextHolder.clearContext();
        return new ModelAndView(new RedirectView(
                    "/Shibboleth.sso/Logout", false, false, false));
    }

    /**
     * Checks MyProxy details submitted and tries to retrieve Grid proxy
     * using the details.
     */
    private void handleMyProxyLogin(HttpServletRequest request)
            throws Exception {

        String user = request.getParameter("proxyuser");
        char[] pass;

        if (user != null) {
            if (request.getParameter("proxypass") == null) {
                pass = new char[0];
            } else {
                pass = request.getParameter("proxypass").toCharArray();
            }

            logger.info("Initializing Grid proxy with MyProxy details.");
            Object credential = gridAccess.initProxy(
                    user, pass, PROXY_LIFETIME);
            if (credential != null) {
                logger.info("Storing credentials in session.");
                request.getSession().setAttribute("userCred", credential);
            } else {
                logger.info("Proxy initialisation failed.");
                throw new Exception("Proxy initialisation failed.");
            }
        } else {
            throw new Exception("Empty user name!");
        }
    }

    /**
     * Returns a {@link ModelAndView} object for a redirect to the SLCS server.
     *
     * @return A prepared <code>ModelAndView</code> to redirect to SLCS.
     */
    private ModelAndView redirectToSlcs(final String serviceUrl) {
        logger.info("Redirecting to SLCS. ServiceUrl= "+serviceUrl);
        return new ModelAndView(
                new RedirectView(SLCS_URL+"token?service="+serviceUrl));
    }

    /**
     * Returns a {@link ModelAndView} object which is a redirect either to a
     * page requested prior to login or the default view.
     * 
     * @return The <code>ModelAndView</code> of the proper destination page.
     */
    private ModelAndView redirectToTarget(HttpServletRequest request) {
        Object target = request.getSession().getAttribute("redirectAfterLogin");
        // clear login attempt counter
        request.getSession().removeAttribute("loginAttempt");
        if (target != null) {
            logger.debug("Redirecting to "+target.toString());
            request.getSession().removeAttribute("redirectAfterLogin");
            return new ModelAndView(new RedirectView(
                        target.toString(), true, false, false));
        }
        logger.debug("Redirecting to joblist.");
        return new ModelAndView(new RedirectView(
                    "/joblist.html", true, false, false));
    }

    /**
     * Parses the request data and sets attributes accordingly.
     *
     * @param requestData the data to parse
     */
    private RequestData parseRequestData(final String requestData) {
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(
                    URLDecoder.decode(requestData).trim()));

        RequestData rd = new RequestData();
        try {
            DocumentBuilder builder =
                DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(is);
            rd.authToken = doc.getElementsByTagName("AuthorizationToken")
                .item(0).getFirstChild().getNodeValue();
            rd.certDN = doc.getElementsByTagName("Subject")
                .item(0).getFirstChild().getNodeValue();

            // parse and add extensions
            rd.certExtensions = new ArrayList<CertificateExtension>();
            NodeList certExt = doc.getElementsByTagName("CertificateExtension");
            for (int i=0; i < certExt.getLength(); i++) {
                String name = ((Element) certExt.item(i)).getAttribute("name");
                String value = certExt.item(i).getFirstChild().getNodeValue();
                CertificateExtension ext = CertificateExtensionFactory
                    .createCertificateExtension(name, value);
                rd.certExtensions.add(ext);
            }

        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return rd;
    }

    /**
     * Converts a hex string into a byte array.
     */
    private byte[] unhexlify(final String hexString) {
        byte[] array = new byte[hexString.length()/2];
        
        for (int i=0; i<hexString.length()/2; i++) {
            String s = hexString.substring(i*2, i*2+2);
            array[i] = (byte) Integer.parseInt(s, 16);
        }

        return array;
    }

    /**
     * Uses a cipher to decrypt data from an input stream.
     */
    private String decryptString(InputStream in, Cipher cipher)
            throws GeneralSecurityException, IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int blockSize = cipher.getBlockSize();
        int outputSize = cipher.getOutputSize(blockSize);
        byte[] inBlock = new byte[blockSize];
        byte[] outBlock = new byte[outputSize];
        int bytesRead;
        do {
            bytesRead = in.read(inBlock);
            if (bytesRead == blockSize) {
                int len = cipher.update(inBlock, 0, blockSize, outBlock);
                output.write(outBlock, 0, len);
            }
        } while (bytesRead == blockSize);

        if (bytesRead > 0) {
            outBlock = cipher.doFinal(inBlock, 0, bytesRead);
        } else {
            outBlock = cipher.doFinal();
        }
        output.write(outBlock);
        return output.toString();
    }

    /**
     * Extracts and decrypts the XML response received from the SLCS server.
     *
     * @return the decrypted XML response
     */
    private String extractSlcsResponse(HttpServletRequest request)
            throws GeneralSecurityException, IOException {
        String responseXML = null;
        String certReqDataHex = request.getParameter("CertificateRequestData");
        String sessionKeyHex = request.getParameter("SessionKey");
        if (certReqDataHex == null || sessionKeyHex == null) {
            throw new GeneralSecurityException("Invalid Request.");
        } else {
            // load host key
            FileInputStream in = new FileInputStream(HOST_KEY_FILE);
            PKCS8Key pem = new PKCS8Key(in, null);
            Key privateKey = pem.getPrivateKey();
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.UNWRAP_MODE, privateKey);

            // unwrap session key and decrypt request data
            byte[] wrappedKey = unhexlify(sessionKeyHex);
            ByteArrayInputStream certReqDataEnc =
               new ByteArrayInputStream(unhexlify(certReqDataHex));
            Key key = cipher.unwrap(wrappedKey, "AES", Cipher.SECRET_KEY);
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            responseXML = decryptString(certReqDataEnc, cipher);
        }
        return responseXML;
    }
 
    /**
     * Processes the SLCS response and tries to generate a grid proxy from
     * the extracted certificate and key.
     */
    private void processSlcsResponse(HttpServletRequest request)
            throws GeneralSecurityException, Exception {
            
        String slcsResponse = extractSlcsResponse(request);
        logger.debug("SLCSResponse:\n"+slcsResponse);
        RequestData rd = parseRequestData(slcsResponse);

        String certCN = rd.certDN.split("CN=")[1];
        String shibCN = request.getHeader("Shib-Person-commonName") + " "
                + request.getHeader("Shib-AuEduPerson-SharedToken");
        if (!certCN.equals(shibCN)) {
            logger.error(certCN+" != "+shibCN);
            throw new GeneralSecurityException(
                    "Certificate is not for current user!");
        }
 
        CertificateKeys certKeys = new CertificateKeys(2048, new char[0]);
        CertificateRequest req = new CertificateRequest(
                certKeys, rd.certDN, rd.certExtensions);

        logger.info("Requesting signed certificate...");
        URL certRespURL = new URL(SLCS_URL +
                "certificate?AuthorizationToken=" + rd.authToken +
                "&CertificateSigningRequest=" +
                URLEncoder.encode(req.getPEMEncoded(), "UTF-8"));
        BufferedReader certRespReader = new BufferedReader(
                new InputStreamReader(certRespURL.openStream()));
        StringBuffer certResp = new StringBuffer();

        String inputLine;
        while ((inputLine = certRespReader.readLine()) != null) {
            certResp.append(inputLine);
            certResp.append('\n');
        }
        certRespReader.close();

        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(certResp.toString().trim()));
        DocumentBuilder builder = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder();
        Document doc = builder.parse(is);
        String status = doc.getElementsByTagName("Status")
            .item(0).getFirstChild().getNodeValue();

        logger.info("Response status: "+status);
        if (!status.equals("Error")) {
            String certStr = doc.getElementsByTagName("Certificate")
                .item(0).getFirstChild().getNodeValue();
            InputStream in = new ByteArrayInputStream(certStr.getBytes());
            X509Certificate certificate = CertUtil.loadCertificate(in);

            Object credential = gridAccess.initProxy(
                    certKeys.getPrivate(), certificate, PROXY_LIFETIME);
            if (credential == null) {
                throw new Exception("Proxy generation failed");
            } else {
                logger.info("Storing credentials in session.");
                request.getSession().setAttribute("userCred", credential);
            }
        } else {
            throw new Exception("Error retrieving SLCS certificate!");
        }
    }
}

