/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nodomain.volkerk.SimpleTIFFlib;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 *
 * @author volker
 */
public abstract class Generic_CFA_PixBuf {
    
    /**
     * The CFA image or even more data
     */
    protected FlexByteArray data;
    
    public Generic_CFA_PixBuf(FlexByteArray _buf)
    {
        data = _buf;
    }
    
    /**
     * The number of bits per pixel
     * 
     * @return array with the number of bits per pixel for each sample
     */
    abstract public int CFA_getBitsPerPixel();
    
    /**
     * The "Active Area" which denotes the actually used part of the image / sensor
     * 
     * @return x0, y0, x1, y1 coordinates of the active image area
     */
    abstract public int[] CFA_getActiveArea();
    
    /**
     * The "Default Crop" which denotes the top left coordinate and the width and height of a crop window within the active area
     * 
     * @return x, y, w, h coordinates of the top-left corner and width and height of the crop window; coordinates are relative to the active area
     */
    abstract public int[] CFA_getCropInfo();
    
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
        int nCols = CFA_getPatternDim()[0];
        
        // determine the current pixel color
        int patternX = x % nCols;
        int patternY = y % CFA_getPatternDim()[1];
        return CFA_getPattern()[patternY * nCols + patternX];
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
     * Sets the intensity value of a pixel in an CFA image
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
        int h = (int) imgHeight();

        if ((x >= w) || (y >= h) || (x < 0) || (y < 0))
        {
            throw new IllegalArgumentException("Invalid coordinates: " + x + ", " + y);
        }
        
        // If we have multiple pixels in one byte (color depth unequal 8, 16
        // or 32), we need a special method to puzzle the bits for one pixel together
        int bpp = CFA_getBitsPerPixel();
        if (((bpp % 8) != 0) || (bpp == 24)) return CFA_setPixel_BitPuzzle(x, y, newVal);
                
        // find the base address of the pixel
        int ptr = (int) CFA_getPixOffsetInBuffer(x, y);
        
        // is it only a getPixel-call?
        if (newVal < 0)
        {
            if (bpp == 8) return data.getByte(ptr);
            else if (bpp == 16) return data.getUint16(ptr);
            return (int) (data.getUint32(ptr));  // Will fail for 32 bpp, because Uint32 does fit into int
        }
        
        if (bpp == 8) data.setByte(ptr, newVal);
        else if (bpp == 16) data.setUint16(ptr, newVal);
        else data.setUint32(ptr, newVal);  // will fail for 32 bpp
        
        return newVal;
    }
    
    /**
     * Gets or sets the intensity value of a pixel in an CFA image
     * 
     * Designed for bits per pixel != 8, 16 or 32
     * 
     * @param x the 0-based x-coordinate of the pixel
     * @param y the 0-based y-coordinate of the pixel
     * @param newVal the new intensity value for the pixel; set to -1 to return the current value
     * 
     * @return the intensity of the pixel as int, if newVal=-1
     */
    protected int CFA_setPixel_BitPuzzle(int x, int y, int newVal)
    {
        int bpp = CFA_getBitsPerPixel();
                
        // find the first byte containing the first bit of the pixel
        int ptr = (int) CFA_getPixOffsetInBuffer(x, y);
        
        // assumption: one pixel is never spread across more than four bytes
        // read all bits into a long, in the sequence they are stored in the file.
        // This means a "Big Endian" interpreation of the bytes, but only for
        // one DWORD read
        boolean oldSwapStatus = data.getSwap();
        data.setSwap(true);
        long allBits = data.getUint32(ptr);
        data.setSwap(oldSwapStatus);
        
        // find the position of the first pixel within the first byte / string
        int firstBitIndex = (x * bpp) % 8;   // 0-based index of the first bit in the first byte, with index ZERO BEING THE MSB
        firstBitIndex = 7 - firstBitIndex; // 0-based index of the first bit in the first byte, with index ZERO BEING THE LSB, as normal
        firstBitIndex += 24;  // plus three bytes to get the 0-based index of the first bit within the DWORD
        
        // find the 0-based index of the last bit
        int lastBitIndex = firstBitIndex - bpp + 1;

        // create a bit mask for the first bpp bits
        long mask = (1L << bpp) - 1;

        // is it a read or write operation?
        if (newVal < 0) // read
        {
            // make the last pixel bit the LSB within the word
            allBits = allBits >> lastBitIndex;
            
            // mask out the first bpp bits to retrieve the pixel value
            return (int) (allBits & mask);        
        }
        
        // write operation
        
        // limit the new value to the available bits
        long nv = newVal & mask;
        
        // shift new bits and mask to the correct position in the DWORD
        nv = nv << lastBitIndex;
        mask = mask << lastBitIndex;
        
        // invert the mask... now everything is 1 except for the pixel values
        mask = ((1L << 32) - 1) ^ mask;
        
        // clear all pixel bits first and then set the bits
        allBits = allBits & mask;
        allBits = allBits | nv;
        
        // write back the DWORK
        data.setSwap(true);
        data.setUint32(ptr, allBits);
        data.setSwap(oldSwapStatus);
                
        return newVal;
    }
    
    /**
     * Returns the CFA image data into an 2-dim array of ints,
     * which each int representing the intensity of one pixel
     * 
     * @return a 2-dim int array with color intensities for each pixel in the image
     */
    public int[][] CFA_getPixelData()
    {
        int bpp = CFA_getBitsPerPixel();
        
        // If we have multiple pixels in one byte (color depth unequal 8, 16
        // or 32), we need a special method to puzzle the bits for one pixel together
        if (((bpp % 8) != 0) || (bpp == 24)) return CFA_getPixelData_BitPuzzle();
        
        int w = (int) imgWidth();
        int h = (int) imgHeight();
        
        int[][] result = new int[w][h];
        
        int bytesPerSample = bpp / 8;   // we have either 8, 16 or 32 bpp; so this is fine
        
        int row = 0;
        
        for (int y = 0; y < h; y++)
        {
            // get the base address of the row
            int basePtr = (int) CFA_getPixOffsetInBuffer(0, y);
            
            for (int x=0; x < w; x++)
            {
                int ptr = basePtr + x*bytesPerSample;
                
                if (bytesPerSample == 1) result[x][y] = data.getByte(ptr);
                else if (bytesPerSample == 2) result[x][y] = data.getUint16(ptr);
                else result[x][y] = (int) data.getUint32(ptr);  // THIS WILL FAIL FOR 32 BPP!!
            }
        }
        
        return result;
    }
    
    /**
     * Returns the CFA image data into an 2-dim array of ints,
     * which each int representing the intensity of one pixel
     * 
     * Designed for images with bits-per-sample != 8, 16, 32
     * 
     * Currently implemented as "brute-force-get-one-pixel-after-the-other" and leaves much room for improvement
     * 
     * @return a 2-dim int array with color intensities for each pixel in the image
     */
    protected int[][] CFA_getPixelData_BitPuzzle()
    {
        int w = (int) imgWidth();
        int h = (int) imgHeight();
        
        int[][] result = new int[w][h];
        
        for (int y=0; y < h; y++)
        {
            for (int x=0; x < w; x++) result[x][y] = CFA_getPixel(x, y);
        }
        
        return result;
    }
    
    /**
     * Writes the CFA data into a PNG without demosaicing. If the CFA has more
     * than 8 bits per pixel, all intensity values are scaled down to 8 bits
     * 
     * @param destFileName the filename to write the PNG to
     * @param useGrayscale if true, the data is stored as grayscale (R=G=B for each pixel)
     */
    public void CFA_raw2png(String destFileName, boolean useGrayscale)
    {
        int[][] rawData = CFA_getPixelData();
        
        // convert to 8-bit color depth, if necessary
        int bpp = CFA_getBitsPerPixel();
        if (bpp > 8)
        {
            int bitDiff = bpp - 8;
            
            for (int y=0; y < imgHeight(); y++)
            {
                for (int x=0; x < imgWidth(); x++)
                {
                    rawData[x][y] = rawData[x][y] >> bitDiff;
                }
            }
        }
        
        // create a new buffered image
        BufferedImage img = new BufferedImage((int)imgWidth(), (int)imgHeight(), BufferedImage.TYPE_INT_RGB);
        
        // write all pixels
        for (int y=0; y < imgHeight(); y++)
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
     * The number of rows and columns in the CFA pattern
     * 
     * @return array with the number of pattern rows at index 0 and columns at index 1
     */
    
    abstract public int[] CFA_getPatternDim();
    
    /**
     * The CFA pattern as array of 0 (R), 1 (G) or 2 (B)
     * 
     * @return The CFA pattern as array of 0 (R), 1 (G) or 2 (B)
     */
    abstract public int[] CFA_getPattern();
    
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
        
        long ptr = CFA_getRowOffsetInBuffer(y);
        ptr += x * CFA_getBitsPerPixel() / 8;
        
        return ptr;
    }
    
    /**
     * Returns the offset within the data block of the first byte of a row
     * 
     * @param row the 0-based y-coordinate of the pixel
     * 
     * @return the offset of the first byte in "data" that contains pixel bits
     */
    abstract protected long CFA_getRowOffsetInBuffer(int row);
    
    abstract public int imgWidth();
    abstract public int imgHeight();
    
    
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
        int h = (int) imgHeight();
        
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
