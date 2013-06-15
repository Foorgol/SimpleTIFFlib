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
public class FlexByteArrayTest {
    
    protected FlexByteArray getTestArray()
    {
        byte[] result = new byte[10]; // {0x01, 0x02, 0x03, 0x04, 0xFF, 0xFE, 0xFD, 0xFC, 0x00};
        
        result[0] = 1; //Byte.parseByte("1", 2);        // 0x01
        result[1] = 2;  //Byte.parseByte("10", 2);       // 0x02
        result[2] = 3; //Byte.parseByte("11", 2);       // 0x03
        result[3] = 4; //Byte.parseByte("100", 2);      // 0x04
        result[4] = -1; //Byte.parseByte("11111111", 2); // 0xFF
        result[5] = -2; //Byte.parseByte("11111110", 2); // 0xFE
        result[6] = -3; //Byte.parseByte("11111101", 2); // 0xFD
        result[7] = -4; //Byte.parseByte("11111100", 2); // 0xFC
        result[8] = -5; //Byte.parseByte("11111011", 2); // 0xFB
        result[9] = 0; //Byte.parseByte("0", 2);         // 0x00
        
        return new FlexByteArray(result);
    }
    
    @Test
    public void testGetSwappedSlice() {
        FlexByteArray a = getTestArray();
        
        // Default: LSB
        byte[] tst = a.getSwappedSlice(1, 2);
        assertTrue(tst.length == 2);
        assertTrue(tst[0] == 2);
        assertTrue(tst[1] == 3);
        
        // Now: MSB
        a.setSwap(true);
        tst = a.getSwappedSlice(1, 2);
        assertTrue(tst.length == 2);
        assertTrue(tst[0] == 3);
        assertTrue(tst[1] == 2);
    }

    @Test
    public void testGetByte() {
        FlexByteArray a = getTestArray();
        
        assertTrue(a.getByte(0) == 1);
        assertTrue(a.getByte(4) == 255);
        assertTrue(a.getByte(8) == 0xFB);
        assertTrue(a.getByte(9) == 0x00);
    }

    @Test
    public void testSetByte() {
        FlexByteArray a = getTestArray();
        
        assertTrue(a.getByte(0) == 1);
        a.setByte(0, 0);
        assertTrue(a.getByte(0) == 0);
        a.setByte(0, 255);  // the new value is always interprezted as unsigned
        assertTrue(a.getByte(0) == 255);
        assertTrue(a.getSignedByte(0) == -1);
    }

    @Test
    public void testGetSignedByte() {
        FlexByteArray a = getTestArray();
        
        assertTrue(a.getSignedByte(0) == 1);
        assertTrue(a.getSignedByte(4) == -1);
    }

    @Test
    public void testGetUint16() {
        FlexByteArray a = getTestArray();
        
        // little endian, LSB first
        assertTrue(a.getUint16(0) == 0x0201);
        assertTrue(a.getUint16(3) == 0xFF04);
        assertTrue(a.getUint16(8) == 0x00FB);
        
        // big endian, MSB first
        a.setSwap(true);
        assertTrue(a.getUint16(0) == 0x0102);
        assertTrue(a.getUint16(3) == 0x04FF);
        assertTrue(a.getUint16(8) == 0xFB00);
    }

    @Test
    public void testGetSint16() {
        FlexByteArray a = getTestArray();
        
        // little endian, LSB first
        assertTrue(a.getSint16(0) == 0x0201);
        assertTrue(a.getSint16(3) < 0);  // 0xFF04, first bit set, should be neg.
        assertTrue(a.getSint16(8) == 0x00FB);
        
        // big endian, MSB first
        a.setSwap(true);
        assertTrue(a.getSint16(0) == 0x0102);
        assertTrue(a.getSint16(3) == 0x04FF);
        assertTrue(a.getSint16(8) < 0);  // 0xFB00, first bit set, should be neg.
    }

    @Test
    public void testGetUint32() {
        FlexByteArray a = getTestArray();
        
        // little endian, LSB first
        assertTrue(a.getUint32(0) == 0x04030201);
        assertTrue(a.getUint32(2) == 0xFEFF0403);
        assertTrue(a.getUint32(6) == 0x00FBFCFD);
        
        // big endian, MSB first
        a.setSwap(true);
        assertTrue(a.getUint32(0) == 0x01020304);
        assertTrue(a.getUint32(2) == 0x0304FFFE);
        assertTrue(a.getUint32(6) == 0xFDFCFB00);
    }

    @Test
    public void testGetSint32() {
        FlexByteArray a = getTestArray();
        
        // little endian, LSB first
        assertTrue(a.getSint32(0) == 0x04030201);
        assertTrue(a.getSint32(2) < 0);
        assertTrue(a.getSint32(6) == 0x00FBFCFD);
        
        // big endian, MSB first
        a.setSwap(true);
        assertTrue(a.getSint32(0) == 0x01020304);
        assertTrue(a.getSint32(2) == 0x0304FFFE);
        assertTrue(a.getSint32(6) < 0);
    }

    @Test
    public void testLength() {
        FlexByteArray a = getTestArray();
        assertTrue(a.length() == 10);
    }
}