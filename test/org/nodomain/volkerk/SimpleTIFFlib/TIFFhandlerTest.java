/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nodomain.volkerk.SimpleTIFFlib;

import org.nodomain.volkerk.SimpleTIFFlib.ImageFileDirectory;
import org.nodomain.volkerk.SimpleTIFFlib.TIFFhandler;
import java.io.IOException;
import java.nio.file.Paths;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author volker
 */
public class TIFFhandlerTest extends TstBaseClass {
    
    @Test
    public void testConstructor() throws IOException
    {
        TIFFhandler h = new TIFFhandler(Paths.get(testInputDataDir(), "000083.dng"));
        
        //h.dumpRawToPng("/tmp/rawTest.png");
        
        ImageFileDirectory ifd = h.getFirstIFDwithCFA();
        
        for (int x = 511; x < 770; x+=8)
        {
            int newVal = (ifd.CFA_getPixel(x-2, 213) + ifd.CFA_getPixel(x+2, 213))/2;
            ifd.CFA_setPixel(x, 213, newVal);
        }
        h.primitiveDemosaic("/tmp/rawTest1.png");
        h.dumpRawToPng("/tmp/rawTest.png");
    }
}