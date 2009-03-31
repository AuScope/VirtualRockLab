package org.auscope.vrl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.Validator;
import org.springframework.validation.Errors;


public class VRLJobValidator implements Validator {

    /** Logger for this class and subclasses */
    protected final Log logger = LogFactory.getLog(getClass());

    public boolean supports(Class clazz) {
        return VRLJob.class.equals(clazz);
    }

    public void validate(Object obj, Errors errors) {
        VRLJob job = (VRLJob) obj;
        if (job == null) {
            errors.rejectValue("name", "error.not-specified", null, "Please provide a job name.");
        } else {
            if (job.getName().length() < 3) {
                errors.rejectValue("name", "error.not-specified", null,
                        "Please provide a descriptive job name.");
            }
            if (job.getScriptFile().length() < 3) {
                errors.rejectValue("scriptFile", "error.not-specified", null,
                        "Please provide a script filename.");
            }
        }
    }
}

