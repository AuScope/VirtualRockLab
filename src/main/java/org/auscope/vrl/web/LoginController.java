package org.auscope.vrl.web;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
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

    private static final String SLCS_URL = "https://slcs1.arcs.org.au/SLCS/";
    private static final String HOST_KEY_FILE = "/etc/shibboleth/hostkey.pem";
    private GridAccessController gridAccess;
    private String authToken;

    public void setGridAccess(GridAccessController gridAccess) {
        this.gridAccess = gridAccess;
    }

    /**
     * Parses the request data and uses the information to create a certificate
     * request.
     *
     * @return A <code>CertificateRequest</code> object.
     */
    private CertificateRequest createRequest(CertificateKeys keys,
                                             final String requestData) {
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(
                    URLDecoder.decode(requestData).trim()));

        CertificateRequest req = null;

        try {
            DocumentBuilder builder =
                DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(is);
            authToken = doc.getElementsByTagName("AuthorizationToken")
                .item(0).getFirstChild().getNodeValue();
            String dn = doc.getElementsByTagName("Subject")
                .item(0).getFirstChild().getNodeValue();

            // parse and add extensions
            List extensions = new ArrayList<CertificateExtension>();
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

    private String extractSlcsResponse(HttpServletRequest request)
            throws GeneralSecurityException, IOException {
        String responseXML = null;
        String certReqDataHex = request.getParameter("CertificateRequestData");
        String sessionKeyHex = request.getParameter("SessionKey");
        if (certReqDataHex == null || sessionKeyHex == null) {
            logger.error("CertificateRequestData or SessionKey empty!");
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
    
    public ModelAndView handleRequest(HttpServletRequest request,
                                      HttpServletResponse response) {

        if (request.getMethod().equalsIgnoreCase("GET")) {
            //logger.error(request.getHeader("Shib-AuEduPerson-SharedToken"));
            if (gridAccess.initProxy("vrluser", "pwd0815!")) {
                return new ModelAndView(new RedirectView(
                            "joblist.html", true, false, false));
            }

            try {
                String serviceUrl = "https://" +
                    InetAddress.getLocalHost().getCanonicalHostName() +
                    "/vrl/login.html";
                logger.info("Redirecting to SLCS. ServiceUrl= "+serviceUrl);
                return new ModelAndView(
                        new RedirectView(SLCS_URL+"token?service="+serviceUrl));
            } catch (UnknownHostException e) {
                logger.error(e);
            }
        } else if (request.getMethod().equalsIgnoreCase("POST")) {
            try {
                String slcsResponse = extractSlcsResponse(request);
                logger.debug("SLCSResponse:\n"+slcsResponse);

                CertificateKeys certKeys =
                    new CertificateKeys(2048, new char[0]);

                logger.debug("Parsing CertRequestData...");
                CertificateRequest req = createRequest(certKeys, slcsResponse);

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
                    String certStr = doc.getElementsByTagName("Certificate")
                        .item(0).getFirstChild().getNodeValue();
                    InputStream in = new ByteArrayInputStream(
                            certStr.getBytes());
                    X509Certificate certificate = CertUtil.loadCertificate(in);

                    if (gridAccess.initProxy(
                                certKeys.getPrivate(), certificate)) {
                        return new ModelAndView(new RedirectView(
                                    "joblist.html", true, false, false));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Could not acquire SLCS certificate: "+e.getMessage());
            }
        }

        //FIXME: grid access did not work so redirect to a page showing what
        //happened, maybe giving option of MyProxy details entry
        return new ModelAndView(new RedirectView(
                    "/Shibboleth.sso/Logout", true, false, false));
    }
}

