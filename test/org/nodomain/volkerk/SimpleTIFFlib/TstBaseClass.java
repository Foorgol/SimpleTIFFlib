/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nodomain.volkerk.SimpleTIFFlib;

import java.util.Date;
import java.io.File;
import java.io.IOException;
import org.junit.Before;
import java.net.URL;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/** Constants for the test execution
 * 
 */
interface TEST_CONSTANTS
{
    public static final String OUTDIR = "testOut";
    public static final String INPUT_RESOURCE_DIR = "res";
}

/**
 *
 * A base class for all other test fixtures
 */
public class TstBaseClass {
    /**
     * If set to false, temp. file from previous tests are NOT deleted
     * 
     * sometimes, files created during test case execution
     * should not be deleted to allow
     * for later offline debugging.
     */
    protected boolean doCleanup;
    
    /**
     * Default constructor. Initializes the class to deletae all temp files
     */
    public TstBaseClass() {
        // default: delete all files after the last test case
        doCleanup = true;
    }
    
//----------------------------------------------------------------------------		

    /**
     * Returns the path of currently executed JAR file
     */
    protected String jarDir()
    {
        return this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
    }
    
//----------------------------------------------------------------------------		

    /**
     * Returns the path for temporary files
     */
    protected String outDir()
    {
        return new File(jarDir(), TEST_CONSTANTS.OUTDIR).toString();
    }
		
//----------------------------------------------------------------------------		
    
    /**
     * return the path for test input files (e. g. sample databases)
     */
    protected String testInputDataDir()
    {
        return this.getClass().getResource(TEST_CONSTANTS.INPUT_RESOURCE_DIR).getPath();
    }

//----------------------------------------------------------------------------		

    /**
     * Initialize the test environment (check files, cleanup, etc)
     * 
     * initialize the test environment, which basically means
     * to check for the existence of all required files and paths
     * and cleaning up possible left-over files from previous
     * test case execution
     */
    @Before
    public void setUp() {
        // check for the directory with the test input files
        File inDir = new File(testInputDataDir());
        assertTrue(inDir.exists());
        
        // create the output directory, if not existing
	File fOutDir = new File(outDir());
        if (!(fOutDir.exists()))
        {
            fOutDir.mkdirs();
        }
        // if already existing, remove all stale files from it
        else
        {
            cleanupOutDir();
        }
        
        assertTrue(fOutDir.exists());
    }
		
//----------------------------------------------------------------------------		

    /** remove stale, temporary files from the output directory
     * 
     */
    public void cleanupOutDir()
    {
        File fOutDir = new File(outDir());
        if (!(fOutDir.exists())) return;
        
        for (File f : fOutDir.listFiles())
        {
            f.delete();
        }
    }
		
//----------------------------------------------------------------------------		

    /** deactivates the deletion of temp files after test execution
     * 
     */
    public void suppressCleanup()
    {
        doCleanup = false;
    }
		
//----------------------------------------------------------------------------		
    /**
     * Is called after the last test case is executed and removes temp files
     * 
     * Is called after the last test case is executed
     * Removes all old files from the output directory,
     * if not suppressed by setting doCleanup to false
     */
    @After
    public void cleanupTestEnvironment()
    {
        if (doCleanup) cleanupOutDir();
    }
    
}
