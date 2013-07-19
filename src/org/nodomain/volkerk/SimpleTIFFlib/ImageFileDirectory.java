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

import java.awt.image.*;
import java.io.File;
import java.util.*;
import javax.imageio.*;

/**
 * A class representing an "Image File Directory" in a TIFF file.
 * Allows also for some manipulation of the picture data associated with the IFD
 */
public class ImageFileDirectory extends Generic_CFA_PixBuf {
    
    /**
     * A hashmap which stores all IFD entries for this directory, accessible via entry tag
     */
    protected HashMap<Integer, IFD_Entry> entries;
    
    protected static final int IFD_ENTRY_SIZE = 12;
    
    /**
     * The offset of the next IFD in the TIFF file
     */
    protected long nextOffset;
    
    /**
     * A pointer to a potential parent IFD (if we are a Sub-IFD)
     */
    protected ImageFileDirectory parent = null;
    
    /**
     * The number of columns for the pattern of the color filter array
     */
    protected int cfaPatternCols;
    
    /**
     * The number of rows for the pattern of the color filter array
     */
    protected int cfaPatternRows;
    
    /**
     * The color filter array pattern. Has patternCols x patternRows entries
     */
    protected int[] cfaPattern;
    
    
    /**
     * Stores the Bits-Per-Sample value, because it's often used
     */
    protected int[] bps;
    
    /**
     * Constructor for a "root"-IFD without parent
     * 
     * @param _data the TIFF file as FlexByteArray
     * @param offset the first byte of the IFD within the data block
     */
    public ImageFileDirectory(FlexByteArray _data, int offset)
    {
        this(_data, offset, null);
    }
    
    /**
     * Constructor for a general IFD which could have a parent
     * 
     * @param _data the TIFF file as FlexByteArray
     * @param offset the first byte of the IFD within the data block
     * @param _parent pointer to the parent IFD, if any
     */
    public ImageFileDirectory(FlexByteArray _data, int offset, ImageFileDirectory _parent)
    {
        super(_data);
        
        // store the parent
        parent = _parent;
        
        // prepare an empty hashmap for the directory entries
        // and read the number of entries from the first two bytes in the data block
        entries = new HashMap();
        int entryCount = (int) data.getUint16(offset);
        
        // read all entries; they follow in 12-byte-block after the first two bytes
        // with the entry count
        for (int i = 0; i < entryCount; i++)
        {
            IFD_Entry tmp = new IFD_Entry(data, offset + 2 + i*IFD_ENTRY_SIZE);
            entries.put(tmp.tag, tmp);
        }
        
        // the last two bytes of the IFD contain the pointer to the next IFD
        nextOffset = data.getUint32(offset + 2 + entryCount * IFD_ENTRY_SIZE);
        
        // if we have CFA data, store some often used values
        if (photometricInterpretation() == TIFF_TAG.PHOTO_INTERPRETATION_CFA)
        {
            cfaPatternCols = cfaPatternDim()[0];
            cfaPatternRows = cfaPatternDim()[1];
            cfaPattern = cfaPatternGet();
        }
        
        // store the bits-per-pixel... it's often used
        bps = bitsPerSample();
    }
    
    /**
     * Returns the offset of the next IFD
     * 
     * @return the offset of the next IFD within the TIFF file
     */
    public long getNextDirectoryOffset()
    {
        return nextOffset;
    }
    
    /**
     * Checks whether this IFD has Sub-IFDs
     * 
     * @return true if subIFDs exist
     */
    public boolean hasSubDirs()
    {
        if (!(hasTag(TIFF_TAG.SUB_IFDs))) return false;
        IFD_Entry e = getEntry(TIFF_TAG.SUB_IFDs);

        if (e.getNumVal() < 1) return false;
        
        return true;
    }
    
    /**
     * Creates IFD instances for all sub-directories and returns an array of
     * these instances
     * 
     * @return array of ImageFileDirectories or null if there are no subDirs
     */
    public ImageFileDirectory[] getSubIFDs()
    {
        if (!(hasSubDirs())) return null;
        IFD_Entry e = getEntry(TIFF_TAG.SUB_IFDs);
        
        int numSubs = e.getNumVal();
        ImageFileDirectory[] result = new ImageFileDirectory[numSubs];
        
        int cnt = 0;
        for (long ptr : e.getLongArray())
        {
            result[cnt] = new ImageFileDirectory(data, (int) ptr, this);
            cnt++;
        }
        
        return result;
    }
    
    
    /**
     * Checks whether an entry with a specific ID exists in the directory or not
     * 
     * @param t the tag / ID of the entry
     * 
     * @return true if the entry exists, false otherwise
     */
    public boolean hasTag(int t)
    {
        return entries.containsKey(t);
    }
    
    /**
     * Retrieves the IFD_Entry object for an with a specific ID
     * Throws an exception if the entry doesn't exist
     * 
     * @param tag the entry's ID
     * 
     * @return the IFD_Entry object for the specific ID
     */
    public IFD_Entry getEntry(int tag)
    {
        if (!(hasTag(tag))) throw new IllegalArgumentException("Unknown tag!");
        return entries.get(tag);
    }
    
    /**
     * The image width in pixels, which can be either stored as SHORT or LONG
     * 
     * @return the width of the image in pixel
     */
    public int imgWidth()
    {
        IFD_Entry e = getEntry(TIFF_TAG.IMAGE_WIDTH);
        
        if (e.getType() == IFD_Entry.ENTRY_TYPE.LONG) return (int) e.getLong();
        return e.getInt();
    }
    
    /**
     * The image height in pixels, which can be either stored as SHORT or LONG
     * 
     * @return the height of the image in pixel
     */
    public int imgHeight()
    {
        IFD_Entry e = getEntry(TIFF_TAG.IMAGE_LENGTH);
        
        if (e.getType() == IFD_Entry.ENTRY_TYPE.LONG) return (int) e.getLong();
        return e.getInt();
    }
    
    /**
     * The compression flag for the image data
     * 
     * @return true if the image data is compressed, false otherwise
     */
    public boolean isCompressed()
    {
        return (getEntry(TIFF_TAG.COMPRESSION).getInt() != 1);
    }
    
    /**
     * The number of samples per pixel, e. g. "3" for R, G, B
     * 
     * @return the number of samples per pixel
     */
    public int samplesPerPixel()
    {
        return getEntry(TIFF_TAG.SAMPLES_PER_PIXEL).getInt();
    }
    
    /**
     * Return "1" if the image is a thumbnail, "0" otherwise
     * 
     * @return 0 for a regular image, 1 for a thumbnail
     */
    public long newSubFileType()
    {
        return getEntry(TIFF_TAG.NEW_SUB_FILE_TYPE).getLong();
    }
    
    /**
     * The number of bits per sample
     * 
     * @return array with the number of bits per sample for each sample
     */
    public int[] bitsPerSample()
    {
        IFD_Entry e = getEntry(TIFF_TAG.BITS_PER_SAMPLE);
        
        // make a plausibility check
        // the number of values for "bits per sample" should be
        // equivalent to the content of samplesPerPixel
        if (e.getNumVal() != samplesPerPixel())
        {
            throw new IllegalStateException("TIFF Data inconsistent");
        }
        
        return e.getIntArray();
    }
    
    /**
     * Converts the array with the bits per sample to a string for logging purposes
     * 
     * @return a string with a comma-separated list of the bits per sample
     */
    public String bitsPerSampleStr()
    {
        return arrayToString(bps);
    }
    
    /**
     * The photometric interpretation of the image data, e. g. CFA or RGB
     * 
     * @return the ID of the interpretation type according to the TIFF standard
     */
    public int photometricInterpretation()
    {
        return getEntry(TIFF_TAG.PHOTOMETRIC_INTERPRETATION).getInt();
    }
    
    /**
     * Converts the photometric interpretation tag into a string for logging purposes
     * 
     * @return a string with the photometric interpretation, e. g. "YCbCr"
     */
    public String photometricInterpretationStr()
    {
        switch (photometricInterpretation())
        {
            case TIFF_TAG.PHOTO_INTERPRETATION_CFA: return "CFA";
            case TIFF_TAG.PHOTO_INTERPRETATION_RGB: return "RGB";
            case TIFF_TAG.PHOTO_INTERPRETATION_GRAY: return "Grayscale";
            case TIFF_TAG.PHOTO_INTERPRETATION_YCBCR: return "YCbCr";
        }
        
        return "Undefined";
    }
    
    /**
     * The "Make"-field of the directory. If it is not present, the make field
     * of the parent directory is returned. If that's not possible, an empty
     * String is returned.
     * 
     * @return the value of the "make" field of this IFD, the parent's IFD or ""
     */
    public String make()
    {
        try
        {
            return getEntry(TIFF_TAG.MAKE).getString();
        }
        catch (Exception e)
        {
            if (parent != null) return parent.make();
        }
        return "";
    }
    
    /**
     * The "Model"-field of the directory. If it is not present, the model field
     * of the parent directory is returned. If that's not possible, an empty
     * String is returned.
     * 
     * @return the value of the "model" field of this IFD, the parent's IFD or ""
     */
    public String model()
    {
        try
        {
            return getEntry(TIFF_TAG.MODEL).getString();
        }
        catch (Exception e)
        {
            if (parent != null) return parent.model();
        }
        return "";
    }
    
    /**
     * The number strips per image, for images stored in strips
     * 
     * @return the number of strips per image
     */
    public int stripsPerImage()
    {
        return getEntry(TIFF_TAG.STRIP_OFFSETS).getNumVal();
    }
    
    /**
     * The number rows per strip, for images stored in strips
     * Can be either stored as LONG or SHORT
     * 
     * @return the number of rows per strip
     */
    public long RowsPerStrip()
    {
        IFD_Entry e = getEntry(TIFF_TAG.ROWS_PER_STRIP);
        
        if (e.getType() == IFD_Entry.ENTRY_TYPE.LONG) return e.getLong();
        return e.getInt();
    }
    
    /**
     * The resolution in x-direction or 0, if not present
     * 
     * @return The resolution in x-direction or 0, if not present
     */
    public double xRes()
    {
        try
        {
            return getEntry(TIFF_TAG.X_RESOLUTION).getDouble();
        }
        catch (Exception e) {}
        
        return 0;
    }
    
    /**
     * The resolution in y-direction or 0, if not present
     * 
     * @return The resolution in y-direction or 0, if not present
     */
    public double yRes()
    {
        try
        {
            return getEntry(TIFF_TAG.Y_RESOLUTION).getDouble();
        }
        catch (Exception e) {}
        
        return 0;
    }
    
    /**
     * The planar configuration of the image data:
     * "1" means chunky with e. g. RGB values stored continuously
     * "2" means planar with image data stored in separate planes for each color component
     * 
     * @return "1" for chunky data or "2" for planar image data
     */
    public int planarCfg()
    {
        return getEntry(TIFF_TAG.PLANAR_CONFIGURATION).getInt();
    }
    
    /**
     * The resolution unit, which can be inch ("2"), cm ("3") or none ("1")
     * 
     * @return 1, 2 or 3 for none, inch or cm as resolution unit
     */
    public int resolutionUnit()
    {
        try
        {
            return getEntry(TIFF_TAG.RESOLUTION_UNIT).getInt();
        }
        catch (Exception e) {}
        
        return TIFF_TAG.RES_UNIT_NONE;
    }
    
    /**
     * The value of the software-field in the IFD. If it's not present in this
     * IFD, the parents value or "" is returned.
     * 
     * @return the value of the software-field in the IFD
     */
    public String software()
    {
        try
        {
            return getEntry(TIFF_TAG.SOFTWARE).getString();
        }
        catch (Exception e)
        {
            if (parent != null) return parent.software();
        }
        return "";
    
    }
    
    /**
     * The value of the datetime-field in the IFD. If it's not present "" is returned.
     * 
     * @return the value of the datetime-field in the IFD or ""
     */
    public String datetime()
    {
        try
        {
            return getEntry(TIFF_TAG.DATETIME).getString();
        }
        catch (Exception e)
        {
            if (parent != null) return parent.datetime();
        }
        return "";
    
    }
    
    /**
     * The number of rows and columns in the CFA pattern
     * 
     * @return array with the number of pattern rows at index 0 and columns at index 1
     */
    public int[] cfaPatternDim()
    {
        int[] result = new int[] {-1,-1};
        
        if (photometricInterpretation() != TIFF_TAG.PHOTO_INTERPRETATION_CFA) return result;
        
        return getEntry(TIFF_TAG.CFA_REPEAT_PATTERN_DIM).getIntArray();
    }
    
    /**
     * The CFA pattern as array of 0 (R), 1 (G) or 2 (B)
     * 
     * @return The CFA pattern as array of 0 (R), 1 (G) or 2 (B)
     */
    public int[] cfaPatternGet()
    {
        int[] result = new int[] {-1};
        
        if (photometricInterpretation() != TIFF_TAG.PHOTO_INTERPRETATION_CFA) return result;
        
        return getEntry(TIFF_TAG.CFA_PATTERN).getIntArray();
    }
    
    /**
     * The offset within the TIFF data block where the strips of the image start
     * 
     * @return an array of longs with the byte offset of each strip in the image
     */
    public long[] stripOffsets()
    {
        IFD_Entry e = getEntry(TIFF_TAG.STRIP_OFFSETS);
        if (e.type == IFD_Entry.ENTRY_TYPE.LONG) return e.getLongArray();
        
        // values stored as SHORT
        // can't directly cast an int-array to long-array,
        // therefore I have to manually copy the values
        int[] tmp = e.getIntArray();
        long[] result = new long[tmp.length];
        for (int i=0; i<tmp.length; i++) result[i] = tmp[i];
        
        return result;
    }
    
    /**
     * The number of bytes in each image strip
     * 
     * @return an array of longs with the byte size of each strip
     */
    public long[] stripByteCounts()
    {
        IFD_Entry e = getEntry(TIFF_TAG.STRIP_BYTE_COUNTS);
        if (e.type == IFD_Entry.ENTRY_TYPE.LONG) return e.getLongArray();
        
        // values stored as SHORT
        // can't directly cast an int-array to long-array,
        // therefore I have to manually copy the values
        int[] tmp = e.getIntArray();
        long[] result = new long[tmp.length];
        for (int i=0; i<tmp.length; i++) result[i] = tmp[i];
        
        return result;
    }
    
    /**
     * The DNG-tag "Active Area" which denotes the actually used part of the image / sensor
     * 
     * @return x0, y0, x1, y1 coordinates of the active image area
     */
    public long[] DNG_ActiveArea()
    {
        // Default: the whole image is the active area
        if (!(hasTag(TIFF_TAG.DNG_ACTIVE_AREA))) return new long[] {0, 0, imgWidth(), imgHeight()};
        
        IFD_Entry e = getEntry(TIFF_TAG.DNG_ACTIVE_AREA);
        long[] tmpLong = new long[] {0, 0, 0, 0};
        
        // the active area might be stored as long or short
        // we always return long, so we need to copy between the arrays manually
        if (e.type == IFD_Entry.ENTRY_TYPE.SHORT)
        {
            int[] tmpInt = e.getIntArray();
            for (int i=0; i<4; i++) tmpLong[i] = tmpInt[i];
        }
        else tmpLong = e.getLongArray();
        
        // the IFD entry reports y-coordinates first; we want to change that
        return new long[] {tmpLong[1], tmpLong[0], tmpLong[3], tmpLong[2]};
    }
    
    /**
     * The DNG-tag "Default Crop Origin" which denotes the top left coordinate of a crop window within the active area
     * 
     * @return x, y coordinates of the top-left corner of the crop window; coordinates are relative to the active area
     */
    public long[] DNG_DefaultCropOrigin()
    {
        // Default: no cropping
        if (!(hasTag(TIFF_TAG.DNG_DEFAULT_CROP_ORIGIN))) return new long[] {0, 0};
        
        IFD_Entry e = getEntry(TIFF_TAG.DNG_DEFAULT_CROP_ORIGIN);
        
        // values stores as LONG: direct return
        if (e.type == IFD_Entry.ENTRY_TYPE.LONG) return e.getLongArray();
        
        // values stored in SHORT: copy manually to long
        long[] tmpLong = new long[] {0, 0};
        if (e.type == IFD_Entry.ENTRY_TYPE.SHORT)
        {
            int[] tmpInt = e.getIntArray();
            for (int i=0; i<2; i++) tmpLong[i] = tmpInt[i];
        }
        
        // values stored as RATIONAL: not yet supported
        return tmpLong;
    }
    
    /**
     * The DNG-tag "Default Crop Size" which denotes the size of a crop window within the active area
     * 
     * @return width, height of the crop window
     */
    public long[] DNG_DefaultCropSize()
    {
        // Default: no cropping
        if (!(hasTag(TIFF_TAG.DNG_DEFAULT_CROP_SIZE))) return new long[] {imgWidth(), imgHeight()};
        
        IFD_Entry e = getEntry(TIFF_TAG.DNG_DEFAULT_CROP_SIZE);
        
        // values stores as LONG: direct return
        if (e.type == IFD_Entry.ENTRY_TYPE.LONG) return e.getLongArray();
        
        // values stored in SHORT: copy manually to long
        long[] tmpLong = new long[] {0, 0};        
        if (e.type == IFD_Entry.ENTRY_TYPE.SHORT)
        {
            int[] tmpInt = e.getIntArray();
            for (int i=0; i<2; i++) tmpLong[i] = tmpInt[i];
            return tmpLong;
        }
        
        // values stored as RATIONAL: not yet supported
        return new long[] {imgWidth(), imgHeight()};
    }
    
    /**
     * The TIIF tag "Default Fill Order" which denotes the sequence in which bits are stored in bytes
     * 
     * @return 1 (pixels with lower column values are stored in the higher-order bits of the byte) or 2 (pixels with lower column values are stored in the lower-order bits of the byte)
     */
    public int fillOrder()
    {
        if (!(hasTag(TIFF_TAG.FILLORDER))) return 1; // Default value according to spec
        IFD_Entry e = getEntry(TIFF_TAG.FILLORDER);
        return e.getInt();
    }
    
    /**
     * Print some info about this image to stderr
     */
    public void dumpInfo()
    {
        System.err.println("--------------------- TIFF tags ---------------------");
        System.err.println("Image size: " + imgWidth() + " x " + imgHeight());
        System.err.println("Image is compressed: " + isCompressed());
        System.err.println("Samples per pixel: " + samplesPerPixel());
        System.err.println("Bits per sample: " + bitsPerSampleStr());
        System.err.println("Fill order: " + fillOrder());
        System.err.println("Photometric Interpretation: " + photometricInterpretationStr());
        System.err.println("Make: " + make());
        System.err.println("Model: " + model());
        System.err.println("Strips per Image: " + stripsPerImage());
        System.err.println("Strip offsets: " + arrayToString(stripOffsets()));
        System.err.println("Strip byte counts: " + arrayToString(stripByteCounts()));
        System.err.println("Rows per Strip: " + RowsPerStrip());
        System.err.println("X resolution: " + xRes());
        System.err.println("Y resolution: " + yRes());
        System.err.println("Planar configuration: " + planarCfg());
        System.err.println("Resolution unit: " + resolutionUnit());
        System.err.println("Software: " + software());
        System.err.println("Date and time: " + datetime());
        System.err.println("CFA repeat pattern dimension: " + arrayToString(cfaPatternDim()));
        System.err.println("CFA pattern: " + arrayToString(cfaPatternGet()));
        System.err.println("--------------------- DNG tags ---------------------");
        System.err.println("Active area: " + arrayToString(DNG_ActiveArea()));
        System.err.println("Default crop origin: " + arrayToString(DNG_DefaultCropOrigin()));
        System.err.println("Default crop size: " + arrayToString(DNG_DefaultCropSize()));
    }
    
    /**
     * Converts an array of ints to a string with comma-separated values for beautiful logging
     * 
     * @param intArray the array with the ints for conversion
     * 
     * @return a string with comma-separated ints
     */
    protected String arrayToString(int[] intArray)
    {
        String result = "";
        for (int i : intArray)
        {
            result += i + ", ";
        }
        return result.substring(0, result.length()-2);
    }

    /**
     * Converts an array of longs to a string with comma-separated values for beautiful logging
     * 
     * @param longArray the array with the longs for conversion
     * 
     * @return a string with comma-separated longs
     */
    protected String arrayToString(long[] longArray)
    {
        String result = "";
        for (long i : longArray)
        {
            result += i + ", ";
        }
        return result.substring(0, result.length()-2);
    }
        
    /**
     * Dumps some debug info about a specific pixel to stderr
     * 
     * @param x the 0-based x-coordinate of the pixel
     * @param y the 0-based y-coordinate of the pixel
     */
    public void CFA_printPixelInfo(int x, int y)
    {
        String[] colName = new String[] {"red", "green", "blue"};
        
        String out = "Pixel at " + x + ", " + y + " is: " + colName[CFA_coord2color(x, y)];
        out += "\n\n";
        
        out += "Surrounding pixels of the same color:\n";
        
        for (int r = -4; r <= 4; r+=2)
        {
            for (int c = -4; c <= 4; c+=2)
            {
                out += CFA_getPixel(x + c, y + r) + "  ";
            }
            out += "\n";
        }
        
        out += "\n\n";
        
        System.err.println(out);
    }
    
    /**
     * Resets all pixels in an CFA pixel to either fully black or fully white.
     * Works only for CFA-images stored in strips.
     * 
     * @param bit true for setting everything to 1, false for setting everything to 0
     */
    public void CFA_clearAllBits(boolean bit)
    {
        long[] offsets = stripOffsets();
        long[] counts = stripByteCounts();
        
        int val = bit ? 255 : 0;
        
        for (int s=0; s < stripsPerImage(); s++)
        {
            int ptr = (int) (offsets[s]);
            
            for (int cnt=0; cnt < ((int) (counts[s])); cnt++) data.setByte(ptr+cnt, val);
        }
    }
        
    /**
     * Calculates the offset of the first byte in the data buffer which contains the first bit of the requested pixel
     * 
     * @param x the 0-based x-coordinate of the pixel
     * @param y the 0-based y-coordinate of the pixel
     * 
     * @return the offset of the first byte in "data" that contains pixel bits
     */
    protected long CFA_getPixOffsetInBuffer(int x, int y)
    {
        int w = (int) imgWidth();
        int h = (int) imgHeight();

        if ((x >= w) || (y >= h) || (x < 0) || (y < 0))
        {
            throw new IllegalArgumentException("Invalid coordinates: " + x + ", " + y);
        }
        
        // calculate the strip base address
        int stripNum = y / (int) RowsPerStrip();
        long ptr = stripOffsets()[stripNum];
        
        // If we have multiple pixels in one byte (color depth unequal 8, 16
        // or 32), we need a special method to calc the offset
        int bpp = CFA_getBitsPerPixel();
        if (((bpp % 8) != 0) || (bpp == 24))
        {
            // calculate the offset of the row within the strip
            // use the ceil()-function to account for the byte-padding at the end of each row
            int bytesPerRow = (int) Math.ceil(w * bpp / 8.0);
            ptr += (y % (int) RowsPerStrip()) * bytesPerRow;

            // find the first byte containing the first bit of the pixel
            ptr += x * bpp / 8;
            
            return ptr;
        }
        
        //
        // if we reach this point, bpp is either 8, 16 or 32
        //
        
        int bytesPerPixel = bpp / 8;
        
        // calculate the offset of the row within the strip
        ptr += (y % (int) RowsPerStrip()) * w * bytesPerPixel;
        
        // find the base address of the pixel
        ptr += x * bytesPerPixel;

        return ptr;
    }

    @Override
    public int CFA_getBitsPerPixel() {
        return bps[0];
    }

    @Override
    public int[] CFA_getActiveArea() {
        long[] x = DNG_ActiveArea();
        
        return new int[] {(int) x[0], (int) x[1], (int) x[2], (int) x[3]};
    }

    @Override
    public int[] CFA_getCropInfo() {
        int[] result = new int[4];
        
        result[0] = (int) DNG_DefaultCropOrigin()[0];
        result[1] = (int) DNG_DefaultCropOrigin()[1];
        result[2] = (int) DNG_DefaultCropSize()[0];
        result[3] = (int) DNG_DefaultCropSize()[1];
        
        return result;
    }

    @Override
    public int[] CFA_getPatternDim() {
        return new int[] {cfaPatternCols, cfaPatternRows};
    }

    @Override
    public int[] CFA_getPattern() {
        return cfaPattern;
    }
}
