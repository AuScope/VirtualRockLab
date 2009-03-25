package org.auscope.vrl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Singleton class that provides utility methods like copying files.
 * 
 * @author Cihan Altinay
 */
public class Util
{
    /** Logger for this class and subclasses */
    private static Log logger = LogFactory.getLog(Util.class.getName());

    /**
     * Private constructor to prevent instantiation.
     */
    private Util() { }

    /**
     * Copies a file from source to destination.
     * 
     * @return true if file was successfully copied, false otherwise
     */
    public static boolean copyFile(File source, File destination) {
        boolean success = false;
        logger.info(source.getPath()+" -> "+destination.getPath());
        try {
            BufferedReader input = new BufferedReader(
                    new FileReader(source));
            BufferedWriter output = new BufferedWriter(
                    new FileWriter(destination));
            String line = null;
            while ((line = input.readLine()) != null) {
                output.write(line);
                output.newLine();
            }
            input.close();
            output.close();
            success = true;

        } catch (IOException e) {
            logger.warn("Could not copy file: "+e.getMessage());
        }
        return success;
    }

    /**
     * Moves a file from source to destination.
     * 
     * @return true if file was successfully moved, false otherwise
     */
    public static boolean moveFile(File source, File destination) {
        boolean success = copyFile(source, destination);
        if (success) {
            source.delete();
        }
        return success;
    }
}

