/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nodomain.volkerk.SimpleTIFFlib;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author volker
 */
public class ImageFileDirectoryTest extends TstBaseClass {
    
    @Test
    public void testConstructor() throws IOException {
        // use a test file with a-priori known offsets for the IFDs
        byte[] allBytes = Files.readAllBytes(Paths.get(testInputDataDir(), "000000.dng"));
        FlexByteArray a = new FlexByteArray(allBytes);
        ImageFileDirectory ifd1 = new ImageFileDirectory(a, 8);    // main IFD
        ImageFileDirectory ifd2 = new ImageFileDirectory(a, 398, ifd1);  // sub IFD
        
        ifd1.dumpInfo();
        assert(ifd2.imgWidth() == 1280);
        assert(ifd2.imgLen() == 720);
        assertTrue(ifd2.samplesPerPixel() == 1);
        assertTrue(ifd2.bitsPerSample()[0] == 14);
        assertTrue(ifd2.bitsPerSample().length == 1);
        assertTrue(ifd2.photometricInterpretation() == 32803);
        assertTrue(ifd2.stripsPerImage() == 1);
        assertTrue(ifd2.RowsPerStrip() == 720);
        assertTrue(ifd2.planarCfg() == 1);
        assertTrue(ifd2.make().equals("Canon "));
        assertTrue(ifd2.cfaPatternGet()[0] == 0); // CFA image
        assertTrue(ifd2.cfaPatternGet()[1] == 1); // CFA image
        assertTrue(ifd2.cfaPatternGet()[2] == 1); // CFA image
        assertTrue(ifd2.cfaPatternGet()[3] == 2); // CFA image
        assertTrue(ifd2.cfaPatternGet().length == 4); // CFA image
        assertTrue(ifd2.cfaPatternDim()[0] == 2); // CFA image
        assertTrue(ifd2.cfaPatternDim()[1] == 2); // CFA image
        assertTrue(ifd2.cfaPatternDim().length == 2); // CFA image
        
        assertTrue(ifd1.imgWidth() == 128);
        assertTrue(ifd1.imgLen() == 96);
        assertTrue(ifd1.samplesPerPixel() == 3);
        assertTrue(ifd1.bitsPerSample()[0] == 8);
        assertTrue(ifd1.bitsPerSample()[1] == 8);
        assertTrue(ifd1.bitsPerSample()[2] == 8);
        assertTrue(ifd1.bitsPerSample().length == 3);
        assertTrue(ifd1.photometricInterpretation() == 2);
        assertTrue(ifd1.stripsPerImage() == 1);
        assertTrue(ifd1.RowsPerStrip() == 96);
        assertTrue(ifd1.planarCfg() == 1);
        assertTrue(ifd1.make().equals("Canon "));
        assertTrue(ifd1.cfaPatternGet()[0] == -1); // not a CFA image
        assertTrue(ifd1.cfaPatternGet().length == 1); // not a CFA image
        assertTrue(ifd1.cfaPatternDim()[0] == -1); // not a CFA image
        assertTrue(ifd1.cfaPatternDim()[1] == -1); // not a CFA image
        assertTrue(ifd1.cfaPatternDim().length == 2); // not a CFA image

    }
    
    @Test
    public void testSubDir() throws IOException
    {
        // use a test file with a-priori known offsets for the IFDs
        byte[] allBytes = Files.readAllBytes(Paths.get(testInputDataDir(), "000000.dng"));
        FlexByteArray a = new FlexByteArray(allBytes);
        ImageFileDirectory ifd1 = new ImageFileDirectory(a, 8);    // main IFD
        
        assertTrue(ifd1.hasSubDirs());
        assertTrue(ifd1.getSubIFDs().length == 1);
        ImageFileDirectory ifd2 = ifd1.getSubIFDs()[0];
        assert(ifd2.imgWidth() == 1280);
        assert(ifd2.imgLen() == 720);
        assertFalse(ifd2.hasSubDirs());
        assertNull(ifd2.getSubIFDs());
        
    }
}