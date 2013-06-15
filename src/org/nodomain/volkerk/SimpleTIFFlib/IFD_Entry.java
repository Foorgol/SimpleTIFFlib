/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nodomain.volkerk.SimpleTIFFlib;

/**
 * A class representing a single entry in an Image File Directory
 */
public class IFD_Entry {
    
    /**
     * Entry types according to TIFF data format specification
     */
    public enum ENTRY_TYPE
    {
        BYTE,
        ASCII,
        SHORT,
        LONG,
        RATIONAL,
        SBYTE,
        UNDEFINED,
        SSHORT,
        SLONG,
        SRATIONAL,
        FLOAT,
        DOUBLE
    }
    
    /**
     * A tag, indicating whether the value of this entry is stored in the
     * IFD entry itself or somewhere else. The latter happens if the data
     * size exceeds four bytes
     */
    protected static final int VALUE_STORED_IN_ENTRY = -1;
    
    /**
     * The entry's tag ID
     */
    protected int tag;
    
    /**
     * The entry's data data
     */
    protected ENTRY_TYPE type;
    
    /**
     * The number of values associated with this tag
     */
    protected int numVal;
    
    /**
     * The data block in which this tag is somewhere stored ("data" is more
     * than just the 12-bytes for the entry!).
     */
    protected FlexByteArray data;
    
    /**
     * The offset to the first byte of the entry within the data block
     */
    protected int offset;
    
    /**
     * Constructor. Determines ID and value of the tag
     * 
     * @param _data the data block in which the entry is stored
     * @param _offset the 0-based of the first byte of the entry
     */
    public IFD_Entry(FlexByteArray _data, int _offset)
    {        
        // store the datablock for later
        data = _data;
        offset = _offset;
        
        // get the tag ID from the first two bytes of the entry
        tag = data.getUint16(offset);
        
        // get the tag type from the next two bytes
        // and make sure it's valid
        int rawType = data.getUint16(offset + 2);
        if (!(determineType(rawType))) throw new IllegalArgumentException("Unknown tag type " + rawType);
        
        // get the number of values for the entry
        numVal = data.getUint16(offset + 4);        
    }
    
    /**
     * Resolves the numeric "type"-flag in the entry into one of the internal ENUMs
     * and sets the member variable "type" accordingly
     * 
     * @param rawType the type number according to the TIFF spec
     * 
     * @return true if the type could be determined, false otherwise
     */
    protected boolean determineType(int rawType)
    {
        if (rawType == 1) type = ENTRY_TYPE.BYTE;
        else if (rawType == 2) type = ENTRY_TYPE.ASCII;
        else if (rawType == 3) type = ENTRY_TYPE.SHORT;
        else if (rawType == 4) type = ENTRY_TYPE.LONG;
        else if (rawType == 5) type = ENTRY_TYPE.RATIONAL;
        else if (rawType == 6) type = ENTRY_TYPE.SBYTE;
        else if (rawType == 7) type = ENTRY_TYPE.UNDEFINED;
        else if (rawType == 8) type = ENTRY_TYPE.SSHORT;
        else if (rawType == 9) type = ENTRY_TYPE.SLONG;
        else if (rawType == 10) type = ENTRY_TYPE.SRATIONAL;
        else if (rawType == 11) type = ENTRY_TYPE.FLOAT;
        else if (rawType == 12) type = ENTRY_TYPE.DOUBLE;
        else return false;
        
        return true;
    }
    
    /**
     * Returns the entry's tag ID
     * 
     * @return the entry's tag ID
     */
    public int getTag()
    {
        return tag;
    }
    
    /**
     * Returns the entry's tag ID as a hex string for log messages
     * 
     * @return the entry's tag ID as a hex string for log messages
     */
    public String getTagHex()
    {
        return Integer.toHexString(tag);
    }
    
    /**
     * Returns the number of values associated with this entry
     * 
     * @return the number of values associated with this entry
     */
    public int getNumVal()
    {
        return numVal;
    }
    
    /**
     * Makes sure that the actual entry type is of the types listed in the arguments.
     * Throws an exception if not
     * 
     * @param allowedType is list of ENTRY_TYPE values against which the actual entry type is checked
     */
    protected void assertType(ENTRY_TYPE ... allowedType)
    {
        for (ENTRY_TYPE e : allowedType)
        {
            if (type == e) return;
        }
        
        throw new IllegalArgumentException("Type " + type + " not allowed!");
    }
    
    /**
     * Return a single field value as integer for values that are directly stored in the IFD entry
     * 
     * @return the value as integer
     */
    public int getInt()
    {
        return getInt(true, 0);
    }
    
    /**
     * Retrieves the n-th value of the entry, if the value can be expressed as an int
     * 
     * @param inEntryValue if set to true, the value is read from the entry data block; if false, the "value"-section of the entry is interpreted as pointer to the actual data
     * @param cnt the 0-based index of the value to receive
     * @return the value as integer
     */
    public int getInt(boolean inEntryValue, int cnt)
    {
        assertType(ENTRY_TYPE.BYTE, ENTRY_TYPE.SBYTE, ENTRY_TYPE.SHORT, ENTRY_TYPE.SSHORT, ENTRY_TYPE.SLONG);
        
        // check if cnt is valid
        if (cnt >= getNumVal())
        {
            throw new IllegalArgumentException("Illegal index number " + cnt + ", the IFD has only " + getNumVal() + "value(s) stored!");
        }
        
        // determine the base adress of the data. It's either the entry itself
        // our an external data block. In the later case, the address of the
        // external block is stored in the entry
        int ptr = offset+8;
        if (!inEntryValue) ptr = (int) getLong();
        
        if (type == ENTRY_TYPE.BYTE) return data.getByte(ptr + cnt);
        else if (type == ENTRY_TYPE.SHORT) return data.getUint16(ptr + 2*cnt);
        else if (type == ENTRY_TYPE.SBYTE) return data.getSignedByte(ptr + cnt);
        else if (type == ENTRY_TYPE.SSHORT) return data.getSint16(ptr + 2*cnt);
        else if (type == ENTRY_TYPE.SLONG) return data.getSint32(ptr + 4*cnt);
        
        throw new IllegalStateException("We should never reach this point...");
    }
    
    /**
     * Return a single field value as long integer for values that are directly stored in the IFD entry
     * 
     * @return the value as integer
     */
    public long getLong()
    {
        return getLong(true, 0);
    }
    
    /**
     * Retrieves the n-th value of the entry, if the value can be expressed as an long
     * 
     * @param inEntryValue if set to true, the value is read from the entry data block; if false, the "value"-section of the entry is interpreted as pointer to the actual data
     * @param cnt the 0-based index of the value to receive
     * @return the value as long
     */
    public long getLong(boolean inEntryValue, int cnt)
    {
        //assertType(ENTRY_TYPE.LONG);
        
        int ptr = offset+8;
        if (!inEntryValue) ptr = (int) getLong();
        
        return data.getUint32(ptr + 4*cnt);
    }
    
    /**
     * Returns the value type associated with this entry
     * 
     * @return the value type associated with this entry
     */
    public ENTRY_TYPE getType()
    {
        return type;
    }
    
    /**
     * Interprets the entry as string(s) with terminating zero.
     * In the rare case that one entry contains multiple strings,
     * the strings are merged into one string with a " " in between.
     * 
     * @return the data as string
     */
    public String getString()
    {
        int ptr = offset + 8;
        
        // if we have more than four characters incl. the terminating zero,
        // the string is stored outside the entry
        if (getNumVal() > 4) ptr = (int) getLong();
        
        String result = "";
        for (int i=0; i<getNumVal(); i++)
        {
            int c = data.getByte(ptr + i);
            
            // Strings can contain sub-string, terminated by zero
            // I read everything into one string, separated by " "
            if (c == 0) result += " ";
            else result += (char) c;
        }
        
        return result;
    }
    
    /**
     * Return a single field value as double for values that are directly stored in the IFD entry
     * So far, only values of type RATIONAL are supported, no true doubles or floats
     * 
     * @return the value as integer
     */
    public double getDouble()
    {
        return getDouble(0);
    }
    
    /**
     * Retrieves the n-th value of the entry, if the value can be expressed as a double
     * So far, only values of type RATIONAL are supported, no true doubles or floats
     * 
     * @param cnt the 0-based index of the value to receive
     * @return the value as double
     */
    public double getDouble(int cnt)
    {
        assertType(ENTRY_TYPE.RATIONAL);
        int ptr = offset + 8;
        
        if (type == ENTRY_TYPE.RATIONAL)
        {
            // a RATIONAL entry need 8 bytes, therefore it is always stored outside the entry
            ptr = (int) getLong() + cnt*8;
            long numerator = data.getUint32(ptr);
            long denom = data.getUint32(ptr + 4);
            
            return ((double) numerator) / ((double) denom);
        }
        
        return -1; // we should never reach this
    }
    
    /**
     * Returns all values stored in the entry as an array of integers. Works for
     * SHORT, SSHORT and SLONG
     * 
     * @return an array of integers with getNumVal() entries
     */
    public int[] getIntArray()
    {
        // calculate the total size of the array to see if it fits into the
        // entry or if it's stored outside
        int byteSize = getNumVal();
        if ((type == ENTRY_TYPE.SHORT) || (type == ENTRY_TYPE.SSHORT)) byteSize *= 2;
        if ((type == ENTRY_TYPE.SLONG)) byteSize *= 4;
        
        boolean inEntryArray = (byteSize < 5);
        
        // copy all values into an array
        int[] result = new int[getNumVal()];
        
        for (int i=0; i < getNumVal(); i++)
        {
            result[i] = getInt(inEntryArray, i);
        }
        
        return result;
    }
    
    /**
     * Returns all values stored in the entry as an array of longs. Works for type LONG
     * 
     * @return an array of longs with getNumVal() entries
     */
    public long[] getLongArray()
    {
        // if the entry has 0 or 1 entry, the array fits into the entry
        boolean inEntryArray = (getNumVal() < 2);
        
        long[] result = new long[getNumVal()];
        
        for (int i=0; i < getNumVal(); i++)
        {
            result[i] = getLong(inEntryArray, i);
        }
        
        return result;
    }
}
