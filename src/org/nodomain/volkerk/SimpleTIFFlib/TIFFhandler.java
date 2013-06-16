/*
 * Copyright © 2013 Volker Knollmann
 * 
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file or visit
 * http://www.wtfpl.net/ for more details.
 * 
 * This program comes without any warranty. Use it at your own risk or
 * don't use it at all.
 */

package org.nodomain.volkerk.SimpleTIFFlib;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;

/**
 *
 * @author volker
 */
public class TIFFhandler {
    protected static final int MIN_FILE_SIZE = 20;
    
    /**
     * The Path for the TIFF input file
     */
    protected Path inFilePath;
    
    /**
     * A copy of the complete input file
     */
    protected FlexByteArray fData = null;
    
    /**
     * a list of all image file directories in the file
     */
    ArrayList<ImageFileDirectory> ifdList;
    
    /**
     * Constructor. Takes the input file name and reads all bytes into an array
     * 
     * @param fName the path to the input file
     * @throws IOException 
     */
    public TIFFhandler(String fName) throws IOException
    {
        this(Paths.get(fName));
    }
    
    /**
     * Constructor. Takes the input file name and reads all bytes into an array.
     * Checks the magic bytes of a TIFF file and initializes the IFDs
     * 
     * @param fPath Path-object for the input file
     * @throws IOException 
     */
    public TIFFhandler(Path fPath) throws IOException
    {
        inFilePath = fPath;
        
        fData = new FlexByteArray(Files.readAllBytes(inFilePath));
        
        // check some parameters
        if (fData.length() < MIN_FILE_SIZE)
        {
            throw new IllegalArgumentException(fPath.toString() + " is not a TIFF file");
        }
        
        // check header to see if we have a TIFF
        int firstTwoBytes = fData.getUint16(0);
        if (firstTwoBytes == 0x4949)
        {
            fData.setSwap(false);  // 0x4949 indicates little endian
        }
        else if (firstTwoBytes == 0x4d4d)
        {
            fData.setSwap(true); // 0x4d4d indicates big endian
        }
        else throw new IllegalArgumentException("First two bytes in file invalid!");
        
        if (fData.getUint16(2) != 42) throw new IllegalArgumentException("Missing 42-tag in header!");
        
        // if we've reached this point, we can be pretty sure to have a valid TIFF file
        
        // get a pointer to the first IFD and read all IFDs
        int firstDirOffset = (int) fData.getUint32(4);        
        initDirectories(firstDirOffset);
    }
    
    /**
     * Create a flat list of all (sub-)IFDs in the file. The list is stored
     * in the member variable ifdList, which is reset in this function
     * 
     * @param firstDirectoryOffset the index of the first byte of the first IFD in the file
     */
    protected void initDirectories(int firstDirectoryOffset)
    {
        ifdList = new ArrayList();
        
        int nextOffset = firstDirectoryOffset;
        while (nextOffset != 0)
        {
            ImageFileDirectory d = new ImageFileDirectory(fData, nextOffset);
            ifdList.add(d);
            
            nextOffset = (int) d.getNextDirectoryOffset();
            
            // Sub-IFD? If yes, add them to the list. Assumption:
            // only one level of sub-dirs, so that we don't need to search recursively
            if (d.hasSubDirs())
            {
                for (ImageFileDirectory sub : d.getSubIFDs()) ifdList.add(sub);
            }
        }
    }
    
    /**
     * Takes the first raw image in the file and dumps it into a PNG file
     * 
     * @param destFileName name of the file to write the PNG to
     */
    public void dumpRawToPng(String destFileName)
    {
        ImageFileDirectory ifd = getFirstIFDwithCFA();
        if (ifd != null) ifd.CFA_raw2png(destFileName, false);
        else System.err.println("dumpRawToPng: no RAW data found in image!");
    }
    
    /**
     * Returns the IFD-object for the first image with RAW data or null
     * 
     * @return the IFD-object for the first image with RAW data or null
     */
    public ImageFileDirectory getFirstIFDwithCFA()
    {
        for (ImageFileDirectory ifd : ifdList)
        {
            if (ifd.photometricInterpretation() != TIFF_TAG.PHOTO_INTERPRETATION_CFA) continue;
            return ifd;
        }
        return null;
    }
    
    /**
     * Takes the first raw image in the file and demosaics it into a PNG file
     * 
     * @param destFileName name of the file to write the PNG to
     */
    public void primitiveDemosaic(String destFileName)
    {
        ImageFileDirectory ifd = getFirstIFDwithCFA();
        if (ifd != null) ifd.CFA_primitiveDemosaic(destFileName);
        else System.err.println("dumpRawToPng: no RAW data found in image!");
    }
    
    /**
     * Write the whole TIFF data to a file. Existing files will be overwritten
     * 
     * @param dstPath Path-instance pointing to the file to write to
     */
    public void saveAs(Path dstPath)
    {
        saveAs(dstPath.toString());
    }
    
    /**
     * Write the whole TIFF data to a file. Existing files will be overwritten
     * 
     * @param fname name of the file to write to
     */
    public void saveAs(String fname)
    {
        fData.dumpToFile(fname);
    }
    
}
