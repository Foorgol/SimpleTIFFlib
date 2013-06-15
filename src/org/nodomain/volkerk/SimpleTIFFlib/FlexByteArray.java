/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nodomain.volkerk.SimpleTIFFlib;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author volker
 */
public class FlexByteArray {
    
    protected byte[] data;
    
    protected boolean swapBytes = false;
    
    public FlexByteArray(byte[] inData)
    {
        if (inData == null) throw new IllegalArgumentException();
        data = inData.clone();
    }
    
    /**
     * Dump the data to a file. EXISTING FILES WILL BE OVERWRITTEN!
     * 
     * @param fName is a String with the name of the file to write to
     */
    public void dumpToFile(String fName)
    {
        Path p = Paths.get(fName);

        try
        {
            Files.write(p, data);
        }
        catch (IOException ex)
        {
            throw new IllegalArgumentException("Could not write to file: " + ex.getMessage());
        }
    }
    
    /**
     * This class assumed all data in little endian (LSB at lower array index). If data is in MSB, you need to set swapBytes to true
     * 
     * @param doSwapBytes set to true to process big endian data (MSB first)
     */
    public void setSwap(boolean doSwapBytes)
    {
        swapBytes = doSwapBytes;
    }
    
    public boolean getSwap()
    {
        return swapBytes;
    }
    
    /**
     * Checks whether "count" bytes starting at 0-based offset "offset" are available in the array
     * 
     * Directly throws an exception
     * 
     * @param offset the 0-based starting position
     * @param count the number of bytes to check for
     */
    protected void checkBoundary(int offset, int count)
    {
        if ((offset < 0) || (count < 0)) throw new IllegalArgumentException();
        
        long lastByteOffset = offset + count - 1;
        
        if (lastByteOffset >= data.length) throw new IllegalArgumentException("Requested offset " + lastByteOffset + " is beyond the file end at " + data.length);
    }
    
    protected byte[] getSwappedSlice(int offset, int count)
    {
        checkBoundary(offset, count);
        byte[] result = new byte[count];
        
        // copy the data incl. possible swapping
        for (int n=0; n < count; n++)
        {
            if (swapBytes)
            {
                result[count - n - 1] = data[offset + n];
            }
            else
            {
                result[n] = data[offset + n];
            }
        }
        
        return result;
        
    }
    
    public int getByte(int offset)
    {
        checkBoundary(offset, 1);
        return (data[offset] & 0xff);
    }
    
    public void setByte(int offset, int newVal)
    {
        checkBoundary(offset, 1);
        
        newVal = newVal & 0xFF;
        byte b = (byte) newVal;
        data[offset] = b;
    }
    
    public byte getSignedByte(int offset)
    {
        checkBoundary(offset, 1);
        return data[offset];
    }
    
    public int getUint16(int offset)
    {
        checkBoundary(offset, 2);
        
        byte[] tmp = getSwappedSlice(offset, 2);
        
        return (tmp[0] & 0xff) + (tmp[1] & 0xff) *256;
    }
            
    public int getSint16(int offset)
    {
        checkBoundary(offset, 2);
        
        int u = getUint16(offset);
        String tmp = Integer.toBinaryString(u);
        short result = (short)Integer.parseInt(tmp, 2);
        return result;
    }
            
    public long getUint32(int offset)
    {
        checkBoundary(offset, 4);
        
        int lw = getUint16(offset);
        int hw = getUint16(offset + 2);
        
        return (long)hw * 65536 + lw;
    }
    
    public int getSint32(int offset)
    {
        checkBoundary(offset, 4);
        
        String lw = Integer.toBinaryString(getUint16(offset));
        String hw = Integer.toBinaryString(getUint16(offset+2));
        
        return Integer.parseInt(hw+lw, 2);
    }
    
    public int length()
    {
        return data.length;
    }
    
}
