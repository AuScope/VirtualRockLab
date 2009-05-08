package org.auscope.vrl.web;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
//import java.security.KeyPair;
//import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.auscope.vrl.GridAccessController;

import org.glite.slcs.pki.CertificateExtension;
import org.glite.slcs.pki.CertificateExtensionFactory;
import org.glite.slcs.pki.CertificateKeys;
import org.glite.slcs.pki.CertificateRequest;

//import org.globus.gsi.CertUtil;
//import org.globus.gsi.bc.BouncyCastleCertProcessingFactory;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.view.RedirectView;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Controller that forwards shibboleth token to SLCS to retrieve a certificate
 * which can subsequently be used to access grid resources.
 */
public class LoginController implements Controller {

    protected final Log logger = LogFactory.getLog(getClass());

    private static final String SLCS_URL = "https://slcstest.arcs.org.au/SLCS/";
    private static final String SERVICE_URL =
        "https://shake75.quakes.uq.edu.au/vrl/login.html";
    private GridAccessController gridAccess;
    private String authToken;

    public void setGridAccess(GridAccessController gridAccess) {
        this.gridAccess = gridAccess;
    }

    /**
     *
     */
    private CertificateRequest createRequest(CertificateKeys keys,
                                             final String requestData) {
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(
                    URLDecoder.decode(requestData).trim()));

        CertificateRequest req = null;

        try {
            String dn;
            List<CertificateExtension> extensions;
            DocumentBuilder builder =
                DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(is);
            authToken = doc.getElementsByTagName("AuthorizationToken")
                .item(0).getFirstChild().getNodeValue();
            dn = doc.getElementsByTagName("Subject")
                .item(0).getFirstChild().getNodeValue();

            // parse and add extensions
            extensions = new ArrayList<CertificateExtension>();

            NodeList certExt = doc.getElementsByTagName("CertificateExtension");
            for (int i=0; i < certExt.getLength(); i++) {
                String name = ((Element) certExt.item(i)).getAttribute("name");
                String value = certExt.item(i).getFirstChild().getNodeValue();
                CertificateExtension ext = CertificateExtensionFactory
                    .createCertificateExtension(name, value);
                extensions.add(ext);
            }
            req = new CertificateRequest(keys, dn, extensions);

        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        //KeyPair certKeys = CertUtil.generateKeyPair("RSA", 2048);
        //byte[] certReq = BouncyCastleCertProcessingFactory.getDefault()
        //    .createCertificateRequest(dn, key);
        //Principal principal = new X509NameUtil().createX509Name(dn);

        return req;
    }

    public ModelAndView handleRequest(HttpServletRequest request,
                                      HttpServletResponse response)
            throws Exception {

        if (request.getMethod().equalsIgnoreCase("GET")) {
            logger.info("Redirecting to SLCS...");
            return new ModelAndView(
                    new RedirectView(SLCS_URL+"token?service="+SERVICE_URL));

        } else if (request.getMethod().equalsIgnoreCase("POST")) {
            String certReqData = request.getParameter("CertificateRequestData");
            if (certReqData == null) {
                logger.error("POST without CertificateRequestData!");
            } else {
                CertificateKeys certKeys =
                    new CertificateKeys(2048, new char[0]);

                logger.info("Parsing CertRequestData...");
                CertificateRequest req = createRequest(certKeys, certReqData);

                logger.info("Requesting signed certificate...");
                URL certRespURL = new URL(SLCS_URL +
                        "certificate?AuthorizationToken=" + authToken +
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
                is.setCharacterStream(new StringReader(
                            certResp.toString().trim()));
                DocumentBuilder builder =
                    DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document doc = builder.parse(is);
                String status = doc.getElementsByTagName("Status")
                    .item(0).getFirstChild().getNodeValue();

                logger.info("Response status: "+status);
                if (!status.equals("Error")) {
                    String certificate = doc.getElementsByTagName("Certificate")
                        .item(0).getFirstChild().getNodeValue();

                    if (gridAccess.initProxy(
                                certKeys.getPrivate(), certificate)) {
                        return new ModelAndView(new RedirectView(
                                    "joblist.html", true, false, false));
                    }
                }
            }
        }

        //FIXME: grid access did not work so redirect to a page showing what
        //happened
        return new ModelAndView("joblist");
    }
}

