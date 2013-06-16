/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nodomain.volkerk.SimpleTIFFlib;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author volker
 */
public class IFD_EntryTest {
    
    public IFD_EntryTest() {
    }
    
    protected FlexByteArray buildEntry(int tag, int type, int count, long value, int offset, int totalByteSize)
    {
        byte[] buf = new byte[totalByteSize];
        FlexByteArray result = new FlexByteArray(buf);
        
        result.setUint16(offset, tag);
        result.setUint16(offset + 2, type);
        result.setUint32(offset + 4, count);
        result.setUint32(offset + 8, value);
        
        return result;
    }

    protected IFD_Entry getEntry(int tag, int type, int count, long value, int offset, int totalByteSize)
    {
        return new IFD_Entry(buildEntry(tag, type, count, value, offset, totalByteSize), offset);
    }
    
    @Test
    public void testGetTag() {
        IFD_Entry e = getEntry(0x10, 0x01, 0, 0, 0, 42);
        
        assertTrue(e.getTag() == 0x10);
        assertTrue(e.getTagHex().equals("10"));
    }
    
    @Test
    public void testGetNumVal() {
        IFD_Entry e = getEntry(0x10, 0x01, 0, 0, 0, 42);
        assertTrue(e.getNumVal() == 0);
        
        e = getEntry(0x10, 0x01, 42, 0, 0, 42);
        assertTrue(e.getNumVal() == 42);
    }

    @Test
    public void testGetInt() {
        
        // #1: BYTE, make sure that unused bits are ignore
        IFD_Entry e = getEntry(0x10, 0x01, 0x01, 0xFAAAAAF0, 0, 42);
        assertTrue(e.getInt() == 0xF0);
        
        // #2: SHORT, make sure that unused bits are ignored
        e = getEntry(0x10, 0x03, 0x01, 0xFAAABB10, 0, 42);
        assertTrue(e.getInt() == 0xBB10);
        
        // #3: SBYTE, make sure that unused bits are ignored
        e = getEntry(0x10, 0x06, 0x01, 0xFFFFFFFF, 0, 42);
        assertTrue(e.getInt() == -1);
        
        // #4: SSHORT, make sure that unused bits are ignored
        e = getEntry(0x10, 0x08, 0x01, 0xFFFFFF00, 0, 42);
        assertTrue(e.getInt() == -256);
        
        // #4: SLONG
        e = getEntry(0x10, 0x09, 0x01, 0x80000001, 0, 42);
        assertTrue(e.getInt() == -0x7FFFFFFF);
    }

    @Test
    public void testGetInt_externalData() {
        
        // Test a signed long stored outside the block
        FlexByteArray a = buildEntry(0x10, 0x09, 1, 20, 0, 100);
        a.setUint32(20, 0x80000001);
        IFD_Entry e = new IFD_Entry(a, 0);
        assertTrue(e.getInt(false, 0) == -0x7FFFFFFF);
        
        // Test a SSHORT stored outside the block
        a = buildEntry(0x10, 0x09, 1, 20, 0, 100);
        a.setUint32(20, 0xFFFFFF00);
        e = new IFD_Entry(a, 0);
        assertTrue(e.getInt(false, 0) == -256);
    }

    @Test
    public void testGetLong() {
        // Test an internally stored LONG
        IFD_Entry e = getEntry(0x10, 0x04, 0x01, 0xFABCDEFF, 0, 42);
        assertTrue(e.getLong()== 0xFABCDEFF);
        
        // Test a long stored outside the block
        // use a wrong base pointer to create the illusion of multiple values
        FlexByteArray a = buildEntry(0x10, 0x09, 2, 20, 0, 100);
        a.setUint32(24, 0xFABCDEFF);
        e = new IFD_Entry(a, 0);
        assertTrue(e.getInt(false, 1) == 0xFABCDEFF);
    }


    @Test
    public void testGetString() {
        // Test an internally stored string
        IFD_Entry e = getEntry(0x10, 0x02, 0x04, 0x44434241, 0, 42);
        assertTrue(e.getString().equals("ABCD"));
        
        // Test a long stored outside the block
        FlexByteArray a = buildEntry(0x10, 0x02, 5, 20, 0, 100);
        a.setUint32(20, 0x44434241);
        a.setByte(24, 0x45);
        e = new IFD_Entry(a, 0);
        assertTrue(e.getString().equals("ABCDE"));
    }

    @Test
    public void testGetDouble() {
        
        // Test a RATIONAL stored outside the block
        FlexByteArray a = buildEntry(0x10, 0x05, 5, 20, 0, 100);
        a.setUint32(20, 100);
        a.setUint32(24, 4);
        IFD_Entry e = new IFD_Entry(a, 0);
        assertTrue(e.getDouble() == 25.0);
        
        // Test a RATIONAL stored outside the block in an array
        a = buildEntry(0x10, 0x05, 5, 20, 0, 100);
        a.setUint32(28, 50);
        a.setUint32(32, 4);
        e = new IFD_Entry(a, 0);
        assertTrue(e.getDouble(1) == 12.5);
    }

    @Test
    public void testGetIntArray() {
        // Test an internally stored byte array
        IFD_Entry e = getEntry(0x10, 0x01, 0x03, 0x44434241, 0, 42);
        int[] b = e.getIntArray();
        assert(b.length == 3);
        assert(b[0] == 0x41);
        assert(b[1] == 0x42);
        assert(b[2] == 0x43);
        
        // Test SSHORT array stored outside the block
        FlexByteArray a = buildEntry(0x10, 0x08, 3, 40, 0, 100);
        a.setUint16(40, 0xFF00);
        a.setUint16(42, 0x42);
        a.setUint16(44, 0x100);
        e = new IFD_Entry(a, 0);
        b = e.getIntArray();
        assert(b.length == 3);
        assert(b[0] == -256);
        assert(b[1] == 0x42);
        assert(b[2] == 0x100);
    }

    @Test
    public void testGetLongArray() {
        // Test an externally stored LONG array
        IFD_Entry e = getEntry(0x10, 0x04, 1, 0x8090A0B0, 0, 100);
        
        long[] b = e.getLongArray();
        assert(b.length == 1);
        assert(b[0] == 0x8090A0B0);
        
        // Test an externally stored LONG array
        FlexByteArray a = buildEntry(0x10, 0x04, 3, 40, 0, 100);
        a.setUint32(40, 60);
        a.setUint32(44, 70);
        a.setUint32(48, 80);
        e = new IFD_Entry(a, 0);
        
        b = e.getLongArray();
        assert(b.length == 3);
        assert(b[0] == 60);
        assert(b[1] == 70);
        assert(b[2] == 80);
        
    }
}