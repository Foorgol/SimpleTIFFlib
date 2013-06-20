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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.nodomain.volkerk.LoggingLib.LoggingClass;

/**
 * A class for flexible access to an array of bytes
 */
public class FlexByteArray extends LoggingClass {
    
    /**
     * The data which is maintained by this class
     */
    protected byte[] data;
    
    /**
     * A flag whether the byte order should be swapped when reading multi-byte values
     * The default for this class -- without swapping -- is little endian (LSB at lower array index)
     */
    protected boolean swapBytes = false;
    
    /**
     * Constructor. Copies / clones an existing byte array
     * 
     * @param inData array of bytes containing the data for this class.
     */
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
        preLog(LVL_DEBUG, "Trying to instanciate Path for ", fName);
        Path p = Paths.get(fName);
        if (p == null) resultLog((LOG_FAIL));
        else resultLog(LOG_OK);

        try
        {
            preLog(LVL_DEBUG, "Calling Files.write()");
            Files.write(p, data);
            resultLog(LOG_OK);
        }
        catch (IOException ex)
        {
            resultLog(LOG_FAIL);
            dbg("Exception while writing to the disk: ", ex.getMessage());
            throw new IllegalArgumentException("Could not write to file: " + ex.getMessage());
        }
    }
    
    /**
     * This class assumes all data in little endian (LSB at lower array index). If data is in MSB, you need to set swapBytes to true
     * 
     * @param doSwapBytes set to true to process big endian data (MSB first)
     */
    public void setSwap(boolean doSwapBytes)
    {
        swapBytes = doSwapBytes;
    }
    
    /**
     * Returns the current endianess
     * 
     * @return true if bytes are interpreted as big endian (MSB first), false for LSB first
     */
    public boolean getSwap()
    {
        return swapBytes;
    }
    
    /**
     * Checks whether "count" bytes starting at 0-based offset "offset" are available in the array
     * 
     * Directly throws an exception if bounds are exceed
     * 
     * @param offset the 0-based starting position
     * @param count the number of bytes to check for
     */
    protected void assertArrayBoundary(int offset, int count)
    {
        if ((offset < 0) || (count < 0)) throw new IllegalArgumentException();
        
        long lastByteOffset = offset + count - 1;
        
        if (lastByteOffset >= data.length) throw new IllegalArgumentException("Requested offset " + lastByteOffset + " is beyond the file end at " + data.length);
    }
    
    /**
     * Takes a slice of data, inverses the byte sequence, if necessary, and returns it
     * 
     * Throws an exception if the requested data slice exceeds the array boundaries
     * 
     * @param offset the offset of the first byte of the slice (0-based)
     * @param count the number of bytes to retrieve
     * @return count bytes from the array; if necessary in inversed order, depending on the swap status
     */
    public byte[] getSwappedSlice(int offset, int count)
    {
        assertArrayBoundary(offset, count);
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
    
    /**
     * Take an array of bytes in LSB order and writes it into the data block
     * and swaps the byte sequence, if necessary
     * 
     * @param buf is the array with the LSB data
     * @param offset is the 0-based start address for the write access
     */
    public void writeSwappedSlice(byte[] buf, int offset)
    {
        assertArrayBoundary(offset, buf.length);
        
        // copy the data incl. possible swapping
        int count = buf.length;
        
        for (int n=0; n < count; n++)
        {
            if (swapBytes)
            {
                data[offset + count - n - 1] = buf[n];
            }
            else
            {
                data[offset + n] = buf[n];
            }
        }
    }
    
    /**
     * Retrieves an 8-bit unsigned integer from the array
     * 
     * Note: a normal "byte" in java is a SIGNED 8-bit value (-127...128), thus we have to return the result as int (not byte) and apply the 2s-complement
     * 
     * @param offset the position of the byte in the array
     * @return a value between 0...255, according to the byte value
     */
    public int getByte(int offset)
    {
        assertArrayBoundary(offset, 1);
        return (data[offset] & 0xff);
    }
    
    /**
     * Stores a an 8-bit unsigned integer in the array
     * 
     * @param offset the position of the byte in the data array
     * @param newVal the value to write to the bit
     */
    public void setByte(int offset, int newVal)
    {
        assertArrayBoundary(offset, 1);
        
        // make sure that the value is between 0...255
        newVal = newVal & 0xFF;
        
        // cast and store
        byte b = (byte) newVal;
        data[offset] = b;
    }
    
    /**
     * Retrieves an 8-bit signed integer from the array
     * 
     * @param offset the position of the byte in the array
     * @return a value between -128...127, according to the byte value
     */
    public byte getSignedByte(int offset)
    {
        assertArrayBoundary(offset, 1);
        return data[offset];
    }
    
    /**
     * Retrieves an 16-bit unsigned integer from the array
     * 
     * @param offset the position of the first byte in the array
     * @return a value between 0...65535, according to the byte values
     */
    public int getUint16(int offset)
    {
        assertArrayBoundary(offset, 2);
        
        // adjust byte order, if necessary
        byte[] tmp = getSwappedSlice(offset, 2);
        
        // compute the 16-bit value
        return (tmp[0] & 0xff) + (tmp[1] & 0xff) *256;
    }
    
    /**
     * Stores an unsigned 16-bit value in the data block, taking the
     * current byte order into account
     * 
     * @param offset 0-based index of the first byte
     * @param newVal the 16-bit value to write
     */
    public void setUint16(int offset, int newVal)
    {
        // range-limit the new value
        newVal &= 0xFFFF;
        
        // convert the int into a byte array with LSB first
        byte[] buf = new byte[2];
        buf[0] = (byte) (newVal % 256);
        buf[1] = (byte) (newVal / 256);
        
        // write the data incl. a possible byte swap
        writeSwappedSlice(buf, offset);
    }
            
    /**
     * Stores an unsigned 32-bit value in the data block, taking the
     * current byte order into account
     * 
     * @param offset 0-based index of the first byte
     * @param newVal the 32-bit value to write
     */
    public void setUint32(int offset, long newVal)
    {
        // range-limit the new value
        newVal &= 0xFFFFFFFF;
        
        // convert the int into a byte array with LSB first
        byte[] buf = new byte[4];
        buf[0] = (byte) (newVal % 256);
        buf[1] = (byte) ((newVal >> 8) & 0xff);
        buf[2] = (byte) ((newVal >> 16) & 0xff);
        buf[3] = (byte) ((newVal >> 24) & 0xff);
        
        // write the data incl. a possible byte swap
        writeSwappedSlice(buf, offset);
    }
    
    /**
     * Retrieves an 16-bit signed integer from the array
     * 
     * @param offset the position of the first byte in the array
     * @return a value between -32768...32767, according to the byte values
     */
    public int getSint16(int offset)
    {
        assertArrayBoundary(offset, 2);
        
        // retrieve the "pure data" as unsigned word
        // and convert it into a bit string
        int u = getUint16(offset);
        String tmp = Integer.toBinaryString(u);
        
        // interpret the bit string as a signed 16-bit integer
        short result = (short)Integer.parseInt(tmp, 2);
        
        return result;
    }
            
    /**
     * Retrieves an 32-bit unsigned integer from the array
     * 
     * Note: because a normal "int" in Java is a signed 32-bit value, the result
     * has to be returned as a long
     * 
     * @param offset the position of the first byte in the array
     * @return a value between 0 ... 2^32 - 1, according to the byte values
     */
    public long getUint32(int offset)
    {
        assertArrayBoundary(offset, 4);
        
        // construct the result from four (swapped) bytes
        byte[] tmp = getSwappedSlice(offset, 4);
        long result = (tmp[0] & 0xff);
        result += (tmp[1] & 0xff) << 8;
        result += (tmp[2] & 0xff) << 16;
        result += (tmp[3] & 0xff) << 24;
        
        return result;
    }
    
    /**
     * Retrieves an 32-bit signed integer from the array
     * 
     * @param offset the position of the first byte in the array
     * @return a value between -2^31 ... 2^31-1, according to the byte values
     */
    public int getSint32(int offset)
    {
        assertArrayBoundary(offset, 4);
        
        // get the raw, unsigend data, convert it into to in
        long u = getUint32(offset);
        return (int) u;
    }
    
    /**
     * Get the number of bytes stored in the array
     * 
     * @return the array size in bytes
     */
    public int length()
    {
        return data.length;
    }
    
}
