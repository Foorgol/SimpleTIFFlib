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
 *
 * @author volker
 */
public class ImageFileDirectory {
    
    protected HashMap<Integer, IFD_Entry> entries;
    
    protected static final int IFD_ENTRY_SIZE = 12;
    
    protected long nextOffset;
    
    protected ImageFileDirectory parent = null;
    
    protected FlexByteArray data;
    
    protected int cfaPatternCols;
    protected int cfaPatternRows;
    protected int[] cfaPattern;
    
    public ImageFileDirectory(FlexByteArray _data, int offset)
    {
        this(_data, offset, null);
    }
    
    public ImageFileDirectory(FlexByteArray _data, int offset, ImageFileDirectory _parent)
    {
        data = _data;
        int entryCount = (int) data.getUint16(offset);
        entries = new HashMap<>();
        parent = _parent;
        
        //System.err.println("Found IFD with " + entryCount + " entries");
        
        for (int i = 0; i < entryCount; i++)
        {
            IFD_Entry tmp = new IFD_Entry(data, offset + 2 + i*IFD_ENTRY_SIZE);
            entries.put(tmp.tag, tmp);
        }
        
        // next offset of the next IFD
        nextOffset = data.getUint32(offset + 2 + entryCount * IFD_ENTRY_SIZE);
        
        // if we have CFA data, store some often used values
        if (photometricInterpretation() == TIFF_TAG.PHOTO_INTERPRETATION_CFA)
        {
            cfaPatternCols = cfaPatternDim()[0];
            cfaPatternRows = cfaPatternDim()[1];
            cfaPattern = cfaPatternGet();
        }
    }
    
    public long getNextDirectoryOffset()
    {
        return nextOffset;
    }
    
    public boolean hasTag(int t)
    {
        return entries.containsKey(t);
    }
    
    public IFD_Entry getEntry(int tag)
    {
        if (!(hasTag(tag))) throw new IllegalArgumentException("Unknow tag!");
        return entries.get(tag);
    }
    
    public int getFieldAsInt(int tag)
    {
        return getFieldAsInt(tag, 1);
    }
    
    public int getFieldAsInt(int tag, int num)
    {
        if (num != 1) throw new IllegalArgumentException("Not yet implemented!");
        
        if (!(hasTag(tag))) throw new IllegalArgumentException("Unknow tag!");
        
        return entries.get(tag).getInt();
    }
    
    public long getFieldAsLong(int tag)
    {
        return getFieldAsLong(tag, 1);
    }
    
    public long getFieldAsLong(int tag, int num)
    {
        if (num != 1) throw new IllegalArgumentException("Not yet implemented!");
        
        if (!(hasTag(tag))) throw new IllegalArgumentException("Unknow tag!");
        
        return entries.get(tag).getLong();
    }
    
    public long imgWidth()
    {
        IFD_Entry e = getEntry(TIFF_TAG.IMAGE_WIDTH);
        
        if (e.getType() == IFD_Entry.ENTRY_TYPE.LONG) return e.getLong();
        return e.getInt();
    }
    
    public long imgLen()
    {
        IFD_Entry e = getEntry(TIFF_TAG.IMAGE_LENGTH);
        
        if (e.getType() == IFD_Entry.ENTRY_TYPE.LONG) return e.getLong();
        return e.getInt();
    }
    
    public boolean isCompressed()
    {
        return (getEntry(TIFF_TAG.COMPRESSION).getInt() != 1);
    }
    
    public int samplesPerPixel()
    {
        return getEntry(TIFF_TAG.SAMPLES_PER_PIXEL).getInt();
    }
    
    public long newSubFileType()
    {
        return getEntry(TIFF_TAG.NEW_SUB_FILE_TYPE).getLong();
    }
    
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
    
    public String bitsPerSampleStr()
    {
        return arrayToString(bitsPerSample());
    }
    
    public int photometricInterpretation()
    {
        return getEntry(TIFF_TAG.PHOTOMETRIC_INTERPRETATION).getInt();
    }
    
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
    
    public int stripsPerImage()
    {
        return getEntry(TIFF_TAG.STRIP_OFFSETS).getNumVal();
    }
    
    public long RowsPerStrip()
    {
        IFD_Entry e = getEntry(TIFF_TAG.ROWS_PER_STRIP);
        
        if (e.getType() == IFD_Entry.ENTRY_TYPE.LONG) return e.getLong();
        return e.getInt();
    }
    
    public double xRes()
    {
        try
        {
            return getEntry(TIFF_TAG.X_RESOLUTION).getDouble();
        }
        catch (Exception e) {}
        
        return 0;
    }
    
    public double yRes()
    {
        try
        {
            return getEntry(TIFF_TAG.Y_RESOLUTION).getDouble();
        }
        catch (Exception e) {}
        
        return 0;
    }
    
    public int planarCfg()
    {
        return getEntry(TIFF_TAG.PLANAR_CONFIGURATION).getInt();
    }
    
    public int resolutionUnit()
    {
        try
        {
            return getEntry(TIFF_TAG.RESOLUTION_UNIT).getInt();
        }
        catch (Exception e) {}
        
        return TIFF_TAG.RES_UNIT_NONE;
    }
    
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
    
    public int[] cfaPatternDim()
    {
        int[] result = new int[] {-1,-1};
        
        if (photometricInterpretation() != TIFF_TAG.PHOTO_INTERPRETATION_CFA) return result;
        
        return getEntry(TIFF_TAG.CFA_REPEAT_PATTERN_DIM).getIntArray();
    }
    
    public int[] cfaPatternGet()
    {
        int[] result = new int[] {-1};
        
        if (photometricInterpretation() != TIFF_TAG.PHOTO_INTERPRETATION_CFA) return result;
        
        return getEntry(TIFF_TAG.CFA_PATTERN).getIntArray();
    }
    
    public long[] stripOffsets()
    {
        IFD_Entry e = getEntry(TIFF_TAG.STRIP_OFFSETS);
        if (e.type == IFD_Entry.ENTRY_TYPE.LONG) return e.getLongArray();
        
        int[] tmp = e.getIntArray();
        long[] result = new long[tmp.length];
        for (int i=0; i<tmp.length; i++) result[i] = tmp[i];
        
        return result;
    }
    
    public long[] stripByteCounts()
    {
        IFD_Entry e = getEntry(TIFF_TAG.STRIP_BYTE_COUNTS);
        if (e.type == IFD_Entry.ENTRY_TYPE.LONG) return e.getLongArray();
        
        int[] tmp = e.getIntArray();
        long[] result = new long[tmp.length];
        for (int i=0; i<tmp.length; i++) result[i] = tmp[i];
        
        return result;
    }
    
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
        System.err.println("CFA bits per pixel: " + CFA_getBitsPerPixel());
    }
    
    protected String arrayToString(int[] intArray)
    {
        String result = "";
        for (int i : intArray)
        {
            result += i + ", ";
        }
        return result.substring(0, result.length()-2);
    }

    protected String arrayToString(long[] longArray)
    {
        String result = "";
        for (long i : longArray)
        {
            result += i + ", ";
        }
        return result.substring(0, result.length()-2);
    }
    
    public int CFA_getBitsPerPixel()
    {
        // Calculate the number of bits in the first stripe and
        // divide that by the number of pixels in the first strip
        long numBits = stripByteCounts()[0] * 8;
        long pixelPerStrip = imgWidth() * RowsPerStrip();
        
        return (int)(numBits/pixelPerStrip);  // integer division; rounding down to account for padding bits
    }
    
    public int CFA_coord2color(int x, int y)
    {
        // determine the current pixel color
        int patternX = x % cfaPatternCols;
        int patternY = y % cfaPatternRows;
        return cfaPattern[patternY * cfaPatternCols + patternX];
    }
    
    public int[] CFA_col2rgb(int x, int y, int col)
    {
        int[] result = new int[]{0,0,0};
        
        int color = CFA_coord2color(x, y);
        
        result[color] = col;
        
        return result;
    }
    
    public int CFA_col2rgbInt(int x, int y, int col)
    {
        int[] rgb = CFA_col2rgb(x, y, col);
        
        return ((rgb[0] << 16) + (rgb[1] << 8) + rgb[2]);
    }
    
    public int CFA_getPixel(int x, int y)
    {
        return CFA_setPixel(x, y, -1);
    }
    
    public int CFA_setPixel(int x, int y, int newVal)
    {
        int w = (int) imgWidth();
        int h = (int) imgLen();
        int bpp = CFA_getBitsPerPixel();
        
        if ((x >= w) || (y >= h))
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
            assert (allBits.length() == bpp);
            return Integer.parseInt(allBits, 2);
        }
        
        // write operation
        
        // limit the new value to the available bits
        int maxVal = (1 << bpp) - 1;
        newVal = Math.min(newVal, maxVal);
        newVal = Math.max(0, newVal);
        
        // convert it into a bit string
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
        assert (allBits.length() == 24);
        
        // write data
        for (int i=0; i < 3; i++)
        {
            int b = Integer.parseInt(allBits.substring(i*8, (i+1)*8), 2);
            data.setByte(ptr + i, b);
        }
        
        // check for debugging
        assert (CFA_getPixel(x, y) == newVal);
        
        return newVal;
    }
    
    protected String getPaddedBitString(int ptr)
    {
        int b = data.getByte(ptr);
        String result = Integer.toBinaryString(b);
        while (result.length() != 8) result = "0" + result;
        
        return result;
    }
    
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
    
    public int[][] CFA_getPixelData()
    {
        int w = (int) imgWidth();
        int h = (int) imgLen();
        
        int[][] result = new int[w][h];
        
        int bpp = CFA_getBitsPerPixel();
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
                    int oldLen = bits.length();
                    
                    // get the substring with the first "bpp" bits from the left
                    String binPix = bits.substring(0, bpp);
                    assert (binPix.length() == bpp);

                    // remove the first "bpp" bits from the string
                    bits = bits.substring(bpp);
                    assert (bits.length() == (oldLen - bpp));
                    
                    //String tmp = "";
                    //for (int i=0; i<bpp; i++) tmp+= binPix.charAt(bpp-i-1);
                    //binPix = tmp;
                    
                    // convert the binary string into a value and store it
                    // As the array is initialized with all zeros, there is no need to set the other color components to zero
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
    
    public void CFA_raw2png(String destFileName, boolean useGrayscale)
    {
        int[][] rawData = CFA_getPixelData();
        
        // convert to 8-bit color depth, if necessary
        if (CFA_getBitsPerPixel() > 8)
        {
            int bitDiff = CFA_getBitsPerPixel() - 8;
            
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
    
    public void CFA_primitiveDemosaic(String destFileName)
    {
        int[][] rawData = CFA_getPixelData();
        int w = (int) imgWidth();
        int h = (int) imgLen();
        
        double[][][] demosData = new double[w][h][3];
        
        // create a new buffered image
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        
        // de-mosaic by averaging one "RGGB"-tile (four pixel) into one pixel
        double maxR = -1;
        double maxG = -1;
        double maxB = -1;
        for (int y=0; y < h; y++)
        {
            for (int x=0; x < w; x++)
            {
                // get the color type
                int color = CFA_coord2color(x, y);
                
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
        
        // normalize to 8-bit
        double maxVal = Math.max(maxR, maxG);
        maxVal = Math.max(maxVal, maxB);
        maxVal *= 0.4;
        
        maxR *= 0.75 * 0.5;
        maxG *= 1.15 * 0.5;
        maxB *= 1.1 * 0.5;
        
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
