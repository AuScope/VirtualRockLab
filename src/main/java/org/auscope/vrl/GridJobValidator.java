package org.auscope.vrl;

import org.springframework.validation.Validator;
import org.springframework.validation.Errors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.auscope.gridtools.GridJob;

public class GridJobValidator implements Validator {

    /** Logger for this class and subclasses */
    protected final Log logger = LogFactory.getLog(getClass());

    public boolean supports(Class clazz) {
        return GridJob.class.equals(clazz);
    }

    public void validate(Object obj, Errors errors) {
        GridJob job = (GridJob) obj;
        if (job == null) {
            errors.rejectValue("name", "error.not-specified", null, "Please provide a job name.");
        } else {
            if (job.getName().length() < 3) {
                errors.rejectValue("name", "error.not-specified", null,
                        "Please provide a descriptive job name.");
            }
            if (job.getArguments()[0].length() < 3) {
                errors.rejectValue("arguments", "error.not-specified", null,
                        "Please provide a script filename.");
            }
        }
    }
}

