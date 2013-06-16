/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
public class ImageFileDirectory {
    
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
     * The complete TIFF file
     */
    protected FlexByteArray data;
    
    /**
     * The number of columns for the pattern of the color filter array
     */
    protected int cfaPatternCols;
    
    /**
     * The number of rows for the pattern of the color filter array
     */
    protected int cfaPatternRows;
    
    /**
     * The column filter array pattern. Has patternCols x patternRows entries
     */
    protected int[] cfaPattern;
    
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
        // store the data block
        data = _data;
        parent = _parent;
        
        // prepare an empty hashmap for the directory entries
        // and read the number of entries from the first two bytes in the data block
        entries = new HashMap<>();
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
    public long imgWidth()
    {
        IFD_Entry e = getEntry(TIFF_TAG.IMAGE_WIDTH);
        
        if (e.getType() == IFD_Entry.ENTRY_TYPE.LONG) return e.getLong();
        return e.getInt();
    }
    
    /**
     * The image height in pixels, which can be either stored as SHORT or LONG
     * 
     * @return the height of the image in pixel
     */
    public long imgLen()
    {
        IFD_Entry e = getEntry(TIFF_TAG.IMAGE_LENGTH);
        
        if (e.getType() == IFD_Entry.ENTRY_TYPE.LONG) return e.getLong();
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
        return arrayToString(bitsPerSample());
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
     * Print some info about this image to stderr
     */
    public void dumpInfo()
    {
        System.err.println("Image size: " + imgWidth() + " x " + imgLen());
        System.err.println("Image is compressed: " + isCompressed());
        System.err.println("Samples per pixel: " + samplesPerPixel());
        System.err.println("Bits per sample: " + bitsPerSampleStr());
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
     * @param intArray the array with the longs for conversion
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
     * Determines the color (red, green or blue) of a specific pixel in a CFA image
     * 
     * @param x the 0-based x-coordinate of the pixel
     * @param y the 0-based y-coordinate of the pixel
     * 
     * @return 0=R, 1=G, 2=B for the pixel color
     */
    public int CFA_coord2color(int x, int y)
    {
        // determine the current pixel color
        int patternX = x % cfaPatternCols;
        int patternY = y % cfaPatternRows;
        return cfaPattern[patternY * cfaPatternCols + patternX];
    }
    
    /**
     * Takes a single CFA-pixel intensity value and puts it into the right position
     * in an array of ints which represents RGB-colors
     * 
     * @param x the 0-based x-coordinate of the pixel
     * @param y the 0-based y-coordinate of the pixel
     * @param col the intensity value to store
     * 
     * @return an int-array with RGB-values at index 0, 1, 2
     */
    public int[] CFA_col2rgb(int x, int y, int col)
    {
        int[] result = new int[]{0,0,0};
        
        int color = CFA_coord2color(x, y);
        
        result[color] = col;
        
        return result;
    }
    
    /**
     * Converts an 8-bit intensity value of a pixel into a single
     * integer representing an RGB-value
     * 
     * @param x the 0-based x-coordinate of the pixel
     * @param y the 0-based y-coordinate of the pixel
     * @param col the intensity value to store
     * 
     * @return an int with B in the LSB, G in the second byte and R in the third
     */
    public int CFA_col2rgbInt(int x, int y, int col)
    {
        int[] rgb = CFA_col2rgb(x, y, col);
        
        return ((rgb[0] << 16) + (rgb[1] << 8) + rgb[2]);
    }
    
    /**
     * Returns the intensity value of a pixel in an CFA image
     * Works only for CFA-images stored in strips.
     * 
     * @param x the 0-based x-coordinate of the pixel
     * @param y the 0-based y-coordinate of the pixel
     * 
     * @return the intensity of the pixel as int
     */
    public int CFA_getPixel(int x, int y)
    {
        return CFA_setPixel(x, y, -1);
    }
    
    /**
     * Gets or sets the intensity value of a pixel in an CFA image
     * Works only for CFA-images stored in strips.
     * 
     * @param x the 0-based x-coordinate of the pixel
     * @param y the 0-based y-coordinate of the pixel
     * @param newVal the new intensity value for the pixel; set to -1 to return the current value
     * 
     * @return the intensity of the pixel as int, if newVal=-1
     */
    public int CFA_setPixel(int x, int y, int newVal)
    {
        int w = (int) imgWidth();
        int h = (int) imgLen();
        int bpp = bitsPerSample()[0];
        
        if ((x >= w) || (y >= h) || (x < 0) || (y < 0))
        {
            throw new IllegalArgumentException("Invalid coordinates: " + x + ", " + y);
        }
        
        // calculate the strip base address
        int stripNum = y / (int) RowsPerStrip();
        int ptr = (int) stripOffsets()[stripNum];
        
        // calculate the offset of the row within the strip
        // use the ceil()-function to account for the byte-padding at the end of each row
        int bytesPerRow = (int) Math.ceil(w * bpp / 8.0);
        ptr += (y % (int) RowsPerStrip()) * bytesPerRow;
        
        // find the first byte containing the first bit of the pixel
        ptr += x * bpp / 8;
        
        // assumption: one pixel is never spread across more than three bytes
        String allBits = "";
        for (int i=0; i < 3; i++)
        {
            allBits += getPaddedBitString(ptr + i);
        }
        
        // find the position of the first pixel within the first byte / string
        int startPos = (x * bpp) % 8;
        
        // is it a read or write operation?
        if (newVal < 0) // read
        {
            allBits = allBits.substring(startPos, startPos + bpp);
            //assert (allBits.length() == bpp);
            return Integer.parseInt(allBits, 2);
        }
        
        // write operation
        
        // limit the new value to the available bits
        int maxVal = (1 << bpp) - 1;
        newVal = Math.min(newVal, maxVal);
        newVal = Math.max(0, newVal);
        
        // convert it into a 0-padded bit string
        String newValBitString = Integer.toBinaryString(newVal);
        while (newValBitString.length() != bpp) newValBitString = "0" + newValBitString;
        
        // construct a new bitstring for all three bytes
        String head = "";
        String tail = "";
        if (startPos != 0)
        {
            head = allBits.substring(0, startPos);
        }
        if (startPos + bpp < allBits.length())
        {
            tail = allBits.substring(startPos + bpp);
        }
        allBits = head + newValBitString + tail;
        //assert (allBits.length() == 24);
        
        // write data
        for (int i=0; i < 3; i++)
        {
            int b = Integer.parseInt(allBits.substring(i*8, (i+1)*8), 2);
            data.setByte(ptr + i, b);
        }
        
        // check for debugging
        //assert (CFA_getPixel(x, y) == newVal);
        
        return newVal;
    }
    
    /**
     * Converts a single byte in the TIFF data block into binary string padded to 8 characters
     * 
     * @param ptr the address of the byte within the TIFF data block
     * 
     * @return A string of "0" and "1" with the bits of the byte, padded to 8 characters
     */
    protected String getPaddedBitString(int ptr)
    {
        int b = data.getByte(ptr);
        String result = Integer.toBinaryString(b);
        while (result.length() != 8) result = "0" + result;
        
        return result;
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
     * Returns the CFA image data into an 2-dim array of ints,
     * which each int representing the intensity of one pixel
     * 
     * Works only for CFA-images stored in strips.
     * 
     * @return a 2-dim int array with color intensities for each pixel in the image
     */
    public int[][] CFA_getPixelData()
    {
        int w = (int) imgWidth();
        int h = (int) imgLen();
        
        int[][] result = new int[w][h];
        
        int bpp = bitsPerSample()[0];
        int row = 0;
        
        for (int n=0; n < stripsPerImage(); n++)
        {
            int ptr = (int) stripOffsets()[n];
            int x = 0;
            int cnt = 0;
            String bits = "";
            
            while ((cnt < stripByteCounts()[n]) && (row < h))
            {
                // read the next byte in the data stream
                // and append the new bits to the old bits on the right end
                bits += getPaddedBitString(ptr + cnt);
                cnt++;
                
                // if we've reached one full pixel, read out the pixel
                if (bits.length() >= bpp)
                {
                    // get the substring with the first "bpp" bits from the left
                    String binPix = bits.substring(0, bpp);

                    // remove the first "bpp" bits from the string
                    bits = bits.substring(bpp);
                    
                    // convert the binary string into a value and store it
                    int newPixelValue = Integer.parseInt(binPix, 2);
                    result[x][row] = newPixelValue;

                    // start a new row, if necessary
                    x++;
                    if (x >= w)
                    {
                        x = 0;
                        row++;
                        bits = "";
                    }
                    
                }
            }
        }
        return result;
    }
    
    /**
     * Writes the CFA data into a PNG without demosaicing. If the CFA has more
     * than 8 bits per pixel, all intensity values are scaled down to 8 bits
     * 
     * Works only for CFA-images stored in strips.
     * 
     * @param destFileName the filename to write the PNG to
     * @param useGrayscale if true, the data is stored as grayscale (R=G=B for each pixel)
     */
    public void CFA_raw2png(String destFileName, boolean useGrayscale)
    {
        int[][] rawData = CFA_getPixelData();
        
        // convert to 8-bit color depth, if necessary
        int bpp = bitsPerSample()[0];
        if (bpp > 8)
        {
            int bitDiff = bpp - 8;
            
            for (int y=0; y < imgLen(); y++)
            {
                for (int x=0; x < imgWidth(); x++)
                {
                    rawData[x][y] = rawData[x][y] >> bitDiff;
                }
            }
        }
        
        // create a new buffered image
        BufferedImage img = new BufferedImage((int)imgWidth(), (int)imgLen(), BufferedImage.TYPE_INT_RGB);
        
        // write all pixels
        for (int y=0; y < imgLen(); y++)
        {
            for (int x=0; x < imgWidth(); x++)
            {
                int col;
                if (useGrayscale)
                {
                    col = rawData[x][y];
                    col += (col << 16) + (col << 8);
                }
                else
                {
                    col = CFA_col2rgbInt(x, y, rawData[x][y]);
                }
                img.setRGB(x, y, col);
            }
        }
        
        // write the file
        File outfile = new File(destFileName);
        
        try
        {
            ImageIO.write(img, "png", outfile);
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("Could not write image to " + destFileName);
        }
    }
    
    /**
     * A very primitive demosaicing for testing purposes. Results are written to a PNG file
     * 
     * Works only for CFA-images stored in strips.
     * 
     * @param destFileName the filename to write the PNG to
     */
    public void CFA_primitiveDemosaic(String destFileName)
    {
        int[][] rawData = CFA_getPixelData();
        int w = (int) imgWidth();
        int h = (int) imgLen();
        
        double[][][] demosData = new double[w][h][3];
        
        // create a new buffered image
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        
        // keep track of the overall maximum value for each color component
        double maxR = -1;
        double maxG = -1;
        double maxB = -1;
        
        // loop over all pixels and interpolate them one by one
        for (int y=0; y < h; y++)
        {
            for (int x=0; x < w; x++)
            {
                // get the color type
                int color = CFA_coord2color(x, y);
                
                // target variable for the RGB-values for this pixel
                double r=0;
                double g=0;
                double b=0;
                
                double tmp = 0;
                double cnt = 0;
                    
                // apply different interpolation algorithms, depending on the color
                if (color == 0) // red pixel, need to find blue and green
                {
                    r = rawData[x][y];
                    
                    // green: use the green pixel left, right, above and below the red pixel
                    if ((x-1) >= 0)
                    {
                        tmp += rawData[x-1][y];
                        cnt++;
                    }
                    if ((x+1) < w)
                    {
                        tmp += rawData[x+1][y];
                        cnt++;
                    }
                    if ((y-1) >= 0)
                    {
                        tmp += rawData[x][y-1];
                        cnt++;
                    }
                    if ((y+1) < h)
                    {
                        tmp += rawData[x][y+1];
                        cnt++;
                    }
                    
                    g = tmp / cnt;
                    
                    // blue: use the blue pixel at the top left, top right, bottom left, bottom right of the red pixel
                    tmp = 0;
                    cnt = 0;
                    if (((x-1) >= 0) && ((y-1) >= 0))
                    {
                        tmp += rawData[x-1][y-1];
                        cnt++;
                    }
                    if (((x+1) < w) && ((y-1) >= 0))
                    {
                        tmp += rawData[x+1][y-1];
                        cnt++;
                    }
                    if (((x-1) >= 0) && ((y+1) < h))
                    {
                        tmp += rawData[x-1][y+1];
                        cnt++;
                    }
                    if (((x+1) < w) && ((y+1) < h))
                    {
                        tmp += rawData[x+1][y+1];
                        cnt++;
                    }
                    
                    b = tmp / cnt;
                }
                
                else if (color == 1)  // green pixel: need to find red and blue
                {
                    g = rawData[x][y];
                    
                    // neighbor1: use the pixel left and right of the green pixel
                    if ((x-1) >= 0)
                    {
                        tmp += rawData[x-1][y];
                        cnt++;
                    }
                    if ((x+1) < w)
                    {
                        tmp += rawData[x+1][y];
                        cnt++;
                    }
                    
                    double n1 = tmp / cnt;
                    
                    // neighbor2: use the pixel above and below the green pixel
                    tmp = 0;
                    cnt = 0;
                    if ((y-1) >= 0)
                    {
                        tmp += rawData[x][y-1];
                        cnt++;
                    }
                    if ((y+1) < h)
                    {
                        tmp += rawData[x][y+1];
                        cnt++;
                    }
                    
                    double n2 = tmp / cnt;
                    
                    // in EVEN rows, n1 is red and n2 is blue;
                    // vice versa in ODD rows
                    if ((y % 2) == 0)
                    {
                        r = n1;
                        b = n2;
                    }
                    else
                    {
                        r = n2;
                        b = n1;
                    }
                }
                
                // blue pixel; need red and green
                else
                {
                    b = rawData[x][y];
                    
                    // green: use the green pixel left, right, above and below the blue pixel
                    if ((x-1) >= 0)
                    {
                        tmp += rawData[x-1][y];
                        cnt++;
                    }
                    if ((x+1) < w)
                    {
                        tmp += rawData[x+1][y];
                        cnt++;
                    }
                    if ((y-1) >= 0)
                    {
                        tmp += rawData[x][y-1];
                        cnt++;
                    }
                    if ((y+1) < h)
                    {
                        tmp += rawData[x][y+1];
                        cnt++;
                    }
                    
                    g = tmp / cnt;
                    
                    // red: use the red pixel at the top left, top right, bottom left, bottom right of the blue pixel
                    tmp = 0;
                    cnt = 0;
                    if (((x-1) >= 0) && ((y-1) >= 0))
                    {
                        tmp += rawData[x-1][y-1];
                        cnt++;
                    }
                    if (((x+1) < w) && ((y-1) >= 0))
                    {
                        tmp += rawData[x+1][y-1];
                        cnt++;
                    }
                    if (((x-1) >= 0) && ((y+1) < h))
                    {
                        tmp += rawData[x-1][y+1];
                        cnt++;
                    }
                    if (((x+1) < w) && ((y+1) < h))
                    {
                        tmp += rawData[x+1][y+1];
                        cnt++;
                    }
                    
                    r = tmp / cnt;
                }
                
                demosData[x][y][0] = r;
                demosData[x][y][1] = g;
                demosData[x][y][2] = b;
                
                if (r > maxR) maxR = r;
                if (g > maxG) maxG = g;
                if (b > maxB) maxB = b;
            }
        }
        
        // find the overall maximum intensity for a later
        // normalization to 8-bit
        double maxVal = Math.max(maxR, maxG);
        maxVal = Math.max(maxVal, maxB);
        
        // tweak the factors a bit. values empirically established.
        // I guess this is something like whitebalance-adjustment
        maxVal *= 0.4;
        maxR *= 0.75 * 0.5;
        maxG *= 1.15 * 0.5;
        maxB *= 1.1 * 0.5;
        
        // normalize all color values to 8 bit and store them in
        // the data structures for a PNG
        for (int y=0; y < h; y++)
        {
            for (int x=0; x < w; x++)
            {
                int r = (int) (demosData[x][y][0] / maxR * 255);
                int g = (int) (demosData[x][y][1] / maxG * 255);
                int b = (int) (demosData[x][y][2] / maxB * 255);
                
                r = Math.min(r, 255);
                g = Math.min(g, 255);
                b = Math.min(b, 255);
                
                int col = (r << 16) + (g << 8) + b;
                img.setRGB(x, y, col);
            }
        }

        
        // write the file
        File outfile = new File(destFileName);
        
        try
        {
            ImageIO.write(img, "png", outfile);
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("Could not write image to " + destFileName);
        }
  
    }
}
