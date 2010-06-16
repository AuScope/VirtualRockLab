/*
 * This file is part of the AuScope Virtual Rock Lab (VRL) project.
 * Copyright (c) 2010 The University of Queensland, ESSCC
 *
 * Licensed under the terms of the GNU Lesser General Public License.
 */
package org.auscope.vrl.web;

/**
 * Class that holds error messages for web clients.
 *
 * @author Cihan Altinay
 */
public class ErrorMessages {
    public static final String FILE_TOO_BIG = "File size exceeds limit for inline display";
    public static final String INTERNAL_ERROR = "The server reported an error while performing the last operation";
    public static final String INVALID_FILENAME = "Invalid filename(s) supplied";
    public static final String INVALID_SCRIPTFILE = "Script filename must end in '.py'";
    public static final String INVALID_JOB = "Invalid job specified";
    public static final String JOB_EXISTS = "A job by that name already exists";
    public static final String JOB_IS_RUNNING = "Cannot delete running job";
    public static final String MISSING_PARAMETER = "Invalid or missing parameter";
    public static final String NO_SERIES = "No active series in session";
    public static final String NOT_AUTHORIZED = "You are not authorized to perform this operation";
    public static final String NULL_HANDLE = "Job has not been submitted";
    public static final String SCRIPT_NOT_FOUND = "The script file does not exist";
    public static final String SERIES_EXISTS = "A series by that name already exists";
    public static final String SESSION_EXPIRED = "You are not logged in or your session has expired";
}
