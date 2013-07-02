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
        TIFFhandler h = new TIFFhandler(Paths.get(testInputDataDir(), "M26-1615_00000.dng"));
        ImageFileDirectory ifd = h.getFirstIFDwithCFA();
        ifd.dumpInfo();
        ifd.CFA_raw2png("/tmp/tst.png", false);
        fail();
    }
}