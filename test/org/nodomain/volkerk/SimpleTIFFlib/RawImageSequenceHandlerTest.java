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
public class RawImageSequenceHandlerTest extends TstBaseClass {
    
    @Test
    public void testSomeMethod()
    {
        // just for internal purposes....
        RawImageSequenceHandler r = new RawImageSequenceHandler("/home/volker/data/130602_MagicLantern/RawMovie/TestData/M1/M15-0006.RAW");
        r.dumpInfo();
    }
}