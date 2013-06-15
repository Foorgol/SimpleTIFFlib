/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nodomain.volkerk.SimpleTIFFlib;

/**
 *
 * @author volker
 */
public class IFD_Entry {
    
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
    
    protected static final int VALUE_STORED_IN_ENTRY = -1;
    
    protected int tag;
    protected ENTRY_TYPE type;
    protected int numVal;
    protected FlexByteArray data;
    protected int offset;
    
    public IFD_Entry(FlexByteArray _data, int _offset)
    {        
        data = _data;
        offset = _offset;
        
        //System.err.println("  IFD Entry at offset " + offset);
        
        // get the tag ID
        tag = data.getUint16(offset);
        
        // get the tag type
        int rawType = data.getUint16(offset + 2);
        if (!(determineType(rawType))) throw new IllegalArgumentException("Unknown tag type " + rawType);
        
        // get the number of values for the entry
        numVal = data.getUint16(offset + 4);
        
        //System.err.println("    Found tag 0x" + getTagHex());
        //System.err.println("    Found tag type " + type);
        //System.err.println("    Has " + numVal + " values stored");
        if (numVal == 1)
        {
            try
            {
                assertType(ENTRY_TYPE.BYTE, ENTRY_TYPE.SBYTE, ENTRY_TYPE.SHORT, ENTRY_TYPE.SSHORT, ENTRY_TYPE.SLONG);
                //System.err.println("    Value = " + getInt());
            }
            catch (Exception e)
            {}
            
            //if (type == ENTRY_TYPE.LONG) System.err.println("    Value = " + getLong());
        }
    }
    
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
    
    public int getTag()
    {
        return tag;
    }
    
    public String getTagHex()
    {
        return Integer.toHexString(tag);
    }
    
    public int getNumVal()
    {
        return numVal;
    }
    
    protected void assertType(ENTRY_TYPE ... allowedType)
    {
        for (ENTRY_TYPE e : allowedType)
        {
            if (type == e) return;
        }
        
        throw new IllegalArgumentException("Type " + type + " not allowed!");

    }
    
    /**
     * Return a field value as integer, for values that are directly stored in the IFD entry
     * 
     * @return the value as integer
     */
    public int getInt()
    {
        return getInt(true, 0);
    }
    
    public int getInt(boolean inEntryValue, int cnt)
    {
        assertType(ENTRY_TYPE.BYTE, ENTRY_TYPE.SBYTE, ENTRY_TYPE.SHORT, ENTRY_TYPE.SSHORT, ENTRY_TYPE.SLONG);
        
        int ptr = offset+8;
        if (!inEntryValue) ptr = (int) getLong();
        
        if (type == ENTRY_TYPE.BYTE) return data.getByte(ptr + cnt);
        else if (type == ENTRY_TYPE.SHORT) return data.getUint16(ptr + 2*cnt);
        else if (type == ENTRY_TYPE.SBYTE) return data.getSignedByte(ptr + cnt);
        else if (type == ENTRY_TYPE.SSHORT) return data.getSint16(ptr + 2*cnt);
        else if (type == ENTRY_TYPE.SLONG) return data.getSint32(ptr + 4*cnt);
        
        throw new IllegalStateException("We should never reach this point...");
    }
    
    public long getLong()
    {
        return getLong(true, 0);
    }
    
    public long getLong(boolean inEntryValue, int cnt)
    {
        //assertType(ENTRY_TYPE.LONG);
        
        int ptr = offset+8;
        if (!inEntryValue) ptr = (int) getLong();
        
        return data.getUint32(ptr + 4*cnt);
    }
    
    public ENTRY_TYPE getType()
    {
        return type;
    }
    
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
            
            // String can contain sub-string, terminated by zero
            // I read everything into one string, separated by " "
            if (c == 0) result += " ";
            else result += (char) c;
        }
        
        return result;
    }
    
    public double getDouble()
    {
        return getDouble(0);
    }
    
    public double getDouble(int cnt)
    {
        assertType(ENTRY_TYPE.RATIONAL);
        int ptr = offset + 8;
        
        if (type == ENTRY_TYPE.RATIONAL)
        {
            ptr = (int) getLong() + cnt*8;
            long numerator = data.getUint32(ptr);
            long denom = data.getUint32(ptr + 4);
            
            return ((double) numerator) / ((double) denom);
        }
        
        return -1;
    }
    
    public int[] getIntArray()
    {
        int byteSize = getNumVal();
        if ((type == ENTRY_TYPE.SHORT) || (type == ENTRY_TYPE.SSHORT)) byteSize *= 2;
        if ((type == ENTRY_TYPE.SLONG)) byteSize *= 4;
        
        boolean inEntryArray = (byteSize < 5);
        
        int[] result = new int[getNumVal()];
        
        for (int i=0; i < getNumVal(); i++)
        {
            result[i] = getInt(inEntryArray, i);
        }
        
        return result;
    }
    
    public long[] getLongArray()
    {
        boolean inEntryArray = (getNumVal() < 2);
        
        long[] result = new long[getNumVal()];
        
        for (int i=0; i < getNumVal(); i++)
        {
            result[i] = getLong(inEntryArray, i);
        }
        
        return result;
    }
}
