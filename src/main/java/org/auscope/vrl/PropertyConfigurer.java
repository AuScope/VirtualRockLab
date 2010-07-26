/*
 * This file is part of the AuScope Virtual Rock Lab (VRL) project.
 * Copyright (c) 2010 The University of Queensland, ESSCC
 *
 * Licensed under the terms of the GNU Lesser General Public License.
 */
package org.auscope.vrl;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

/**
 * Identical to PropertyPlaceholderConfigurer except it allows resolving
 * properties from within portal classes.
 *
 * @author Cihan Altinay
 */
public class PropertyConfigurer extends PropertyPlaceholderConfigurer {

    protected final Log logger = LogFactory.getLog(getClass());

    private Properties mergedProps;

    public String resolvePlaceholder(String placeholder) {
        if (mergedProps == null) {
            try {
                mergedProps = mergeProperties();
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }

        if (mergedProps != null) {
            return resolvePlaceholder(placeholder, mergedProps);
        } else {
            return "";
        }
    }
}

