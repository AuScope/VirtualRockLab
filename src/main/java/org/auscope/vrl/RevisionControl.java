/*
 * This file is part of the AuScope Virtual Rock Lab (VRL) project.
 * Copyright (c) 2010 The University of Queensland, ESSCC
 *
 * Licensed under the terms of the GNU Lesser General Public License.
 */
package org.auscope.vrl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.auscope.vrl.FileInformation;
import org.auscope.vrl.Util;

import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNDirEntry;
import org.tigris.subversion.svnclientadapter.ISVNLogMessage;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.SVNClientAdapterFactory;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNStatusKind;
import org.tigris.subversion.svnclientadapter.SVNUrl;
import org.tigris.subversion.svnclientadapter.commandline.CmdLineClientAdapterFactory;
import org.tigris.subversion.svnclientadapter.javahl.JhlClientAdapterFactory;

/**
 * Provides revision control functionality for VRL series.
 */
public class RevisionControl {

    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir")
        + File.separator + "vrl" + File.separator;

    /** Logger for this class */
    private final Log logger = LogFactory.getLog(getClass());
    
    private String password;
    private String username;
    private String svnroot;
    private ISVNClientAdapter svnClient;

    public void setPassword(String password) {
        this.password = password;
        svnClient.setPassword(password);
    }

    public void setSvnroot(String svnroot) {
        this.svnroot = svnroot;
    }

    public void setUsername(String username) {
        this.username = username;
        svnClient.setUsername(username);
    }

    /**
     * Default constructor which creates an instance of the underlying
     * subversion client.
     */
    public RevisionControl() throws SVNClientException {
        try {
            logger.info("Trying JavaHL client");
            JhlClientAdapterFactory.setup();
            svnClient = SVNClientAdapterFactory.createSVNClient(
                    JhlClientAdapterFactory.JAVAHL_CLIENT);
        } catch (SVNClientException e) {
            logger.debug(JhlClientAdapterFactory.getLibraryLoadErrors());
        }
        if (svnClient == null) {
            logger.info("Trying Commandline client");
            CmdLineClientAdapterFactory.setup();
            svnClient = SVNClientAdapterFactory.createSVNClient(
                    CmdLineClientAdapterFactory.COMMANDLINE_CLIENT);
        }
    }

    /**
     * Performs a checkout
     *
     * @param user Name of the series owner
     * @param seriesId Identifier of the series
     * @param revision Revision to check out
     *
     * @return <code>File</code> object of checked out series
     */
    public File checkoutRevision(String user, long seriesId,
                                 String revision) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String targetDir = TEMP_DIR + user + File.separator
            + sdf.format(new Date());
        File path = new File(targetDir);

        SVNUrl url = new SVNUrl(
                svnroot + File.separator
                + user + File.separator
                + seriesId);
        SVNRevision rev = SVNRevision.getRevision(revision);

        // first checkout a fresh copy of HEAD
        svnClient.checkout(url, path, SVNRevision.HEAD, true);
        // now reverse-merge changes from requested revision to be able to
        // make changes and commit back
        svnClient.merge(url, SVNRevision.HEAD, url, rev, path, true, true);

        return path;
    }

    /**
     * Returns an array of revision logs for given series.
     *
     * @param user Name of the series owner
     * @param projectId Identifier of the series
     *
     * @return Array of <code>RevisionLog</code> objects.
     */
    public RevisionLog[] getSeriesLogs(String user, long seriesId,
            String revisionStart, String revisionEnd) {
        RevisionLog[] result = null;
        try {
            SVNUrl url = new SVNUrl(
                    svnroot + File.separator
                    + user + File.separator
                    + seriesId);
            SVNRevision revStart, revEnd;
            try {
                revStart = SVNRevision.getRevision(revisionStart);
            } catch (ParseException e) {
                revStart = SVNRevision.START;
            }
            try {
                revEnd = SVNRevision.getRevision(revisionEnd);
            } catch (ParseException e) {
                revEnd = SVNRevision.HEAD;
            }

            ISVNLogMessage[] msgs = svnClient.getLogMessages(
                    url, revStart, revEnd);
            result = new RevisionLog[msgs.length];
            for (int i=0; i<msgs.length; i++) {
                ISVNLogMessage msg = msgs[i];
                RevisionLog log = new RevisionLog(
                        msg.getRevision().getNumber(), msg.getDate(),
                        msg.getMessage());
                result[i] = log;
            }
        } catch (Exception e) {
            logger.error(e);
        }

        return result;
    }

    /**
     * Returns information about a file or directory within a series.
     *
     * @param user Name of the series owner
     * @param projectId Identifier of the series
     * @param subPath Path to the entry within the series
     * @param revision Revision to retrieve (e.g. "HEAD" or "42")
     *
     * @return <code>RevisionEntry</code> object for requested entry or null
     *         on error.
     */
    public RevisionEntry getEntry(String user, long seriesId, String subPath,
                                  String revision) throws Exception {
        RevisionEntry result = null;
        SVNUrl url = new SVNUrl(
                svnroot + File.separator
                + user + File.separator
                + seriesId + File.separator
                + Util.sanitizeSubPath(subPath));
        logger.debug("url="+url.toString());
        ISVNDirEntry entry = svnClient.getDirEntry(url,
                SVNRevision.getRevision(revision));
        result = new RevisionEntry(entry.getPath(),
                entry.getLastChangedRevision().getNumber(),
                entry.getLastChangedDate(),
                entry.getSize());
        return result;
    }

    /**
     * Clones an existing series revision.
     *
     * @param srcUser Name of the source series owner
     * @param srcId Identifier of the source series
     * @param revision Revision to clone
     * @param destUser Name of the destination series owner
     * @param destId Identifier of the destination series
     * @param message Commit message for the copy
     */
    public void cloneSeries(String srcUser, long srcId, String revision,
                            String destUser, long destId, Map jobIdMap,
                            String message)
            throws Exception {

        // 1. export a clean copy of source series
        SVNUrl srcUrl = new SVNUrl(
                svnroot + File.separator
                + srcUser + File.separator
                + srcId);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String targetDir = TEMP_DIR + sdf.format(new Date());
        File destPath = new File(targetDir);
        svnClient.doExport(
                srcUrl, destPath, SVNRevision.getRevision(revision), true);

        // 2. update job directory names
        Iterator it = jobIdMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            File src = new File(destPath, (String)pair.getKey());
            File dst = new File(destPath, (String)pair.getValue());
            if (!src.renameTo(dst)) {
                logger.error("Unable to move "+src.getPath()+" to "
                        +dst.getPath());
                // Ignore or throw? -> we ignore for now...
            }
        }

        // 3. import the modified series directory
        SVNUrl destUrl = new SVNUrl(
                svnroot + File.separator
                + destUser + File.separator
                + destId);
        svnClient.doImport(destPath, destUrl, message, true);
        Util.deleteFilesRecursive(destPath);
    }

    /**
     * Creates a new empty series in the revision control system.
     *
     * @param user Name of the series owner
     * @param seriesId Identifier of the series
     */
    public void createSeries(String user, long seriesId, String message)
            throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String sourceDir = TEMP_DIR + user + File.separator
            + sdf.format(new Date());
        File path = new File(sourceDir);
        if (path.mkdirs()) {
            // create a Documentation subdirectory
            //File subPath = new File(path, "Documentation");
            //subPath.mkdir();
            SVNUrl url = new SVNUrl(
                    svnroot + File.separator
                    + user + File.separator
                    + seriesId);
            logger.debug("Importing "+sourceDir);
            svnClient.doImport(path, url, message, true);
            Util.deleteFilesRecursive(path);
        } else {
            throw new IOException("Could not create import directory");
        }
    }

    // **********
    // ** Methods dealing with checked out series follow
    // **********

    /**
     * Adds a series file to the repository.
     *
     * @param file File to add
     */
    public void addFile(File file) throws Exception {
        svnClient.addFile(file);
    }

    /**
     * Commits changes to the revision control system.
     *
     * @param path Path to commit
     * @param message Commit message
     */
    public long commitChanges(File path, String message) throws Exception {
        long revision = svnClient.commit(new File[] {path}, message, true);
        return revision;
    }

    /**
     * Returns whether a checkout has local modifications.
     *
     * @param file Path to checkout
     */
    public boolean hasModifications(File path) throws Exception {
        ISVNStatus[] states = svnClient.getStatus(path, true, false);
        /*
        for (ISVNStatus status : states) {
            String name = status.getFile().getName();
            long size = status.getFile().length();
            String textStatus = status.getTextStatus().toString();
            logger.debug(name+" - "+textStatus);
        }
        */
        return (states.length > 0);
    }

    /**
     * Returns the contents of a checked-out directory as FileInformation
     * objects.
     *
     * @param path Path to checkout
     */
    public FileInformation[] listFilesWithStatus(File path) throws Exception {
        ISVNStatus[] states = svnClient.getStatus(path, false, true);
        List<FileInformation> files = new ArrayList<FileInformation>(
                states.length-1);
        for (ISVNStatus status : states) {
            // skip top-level directory
            if (path.equals(status.getFile())) {
                continue;
            }
            String name = status.getFile().getName();
            long size = status.getFile().length();
            String state;
            SVNStatusKind textStatus = status.getTextStatus();
            if (textStatus.equals(SVNStatusKind.ADDED)) {
                state = "A";
            } else if (textStatus.equals(SVNStatusKind.DELETED)) {
                state = "D";
            } else if (textStatus.equals(SVNStatusKind.MODIFIED)
                    || textStatus.equals(SVNStatusKind.REPLACED)) {
                state = "M";
            } else if (textStatus.equals(SVNStatusKind.NORMAL)) {
                state = "N";
            } else {
                // unknown status
                state = textStatus.toString();
            }
            files.add(new FileInformation(name, size, state));
        }
        return files.toArray(new FileInformation[0]);
    }

    /**
     * Marks series files for deletion.
     *
     * @param files Array of files to delete
     */
    public void removeFiles(File[] files) throws Exception {
        svnClient.remove(files, true);
    }

    /**
     * Reverts series files to their original state - if a file was marked
     * for addition then it is deleted.
     *
     * @param files Array of files to revert
     */
    public void revertFiles(File[] files) throws Exception {
        for (final File file : files) {
            svnClient.revert(file, false);
            if (svnClient.getSingleStatus(file).getTextStatus().equals(
                        SVNStatusKind.UNVERSIONED)) {
                file.delete();
            }
        }
    }
}

