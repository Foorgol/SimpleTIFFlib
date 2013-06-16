/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nodomain.volkerk.SimpleTIFFlib;

/**
 * A class with constants from the TIFF-specification
 */
public class TIFF_TAG {
    
    public static final int NEW_SUB_FILE_TYPE = 0x00FE;
    public static final int IMAGE_WIDTH = 0x0100;
    public static final int IMAGE_LENGTH = 0x0101;
    public static final int BITS_PER_SAMPLE = 0x0102;
    public static final int COMPRESSION = 0x103;
    public static final int PHOTOMETRIC_INTERPRETATION = 0x106;
    public static final int IMAGE_DESCRIPTION = 0x10E;
    public static final int MAKE = 0x010F;
    public static final int MODEL = 0x0110;
    public static final int STRIP_OFFSETS = 0x0111;
    public static final int ORIENTATION = 0x0112;
    public static final int SAMPLES_PER_PIXEL = 0x0115;
    public static final int ROWS_PER_STRIP = 0x0116;
    public static final int STRIP_BYTE_COUNTS = 0x0117;
    public static final int X_RESOLUTION = 0x011A;
    public static final int Y_RESOLUTION = 0x011B;
    public static final int PLANAR_CONFIGURATION = 0x011C;
    public static final int RESOLUTION_UNIT = 0x0128;
    public static final int SOFTWARE = 0x0131;
    public static final int DATETIME = 0x0132;
    public static final int SUB_IFDs = 0x014A;
    
    public static final int CFA_REPEAT_PATTERN_DIM = 0x828D;
    public static final int CFA_PATTERN = 0x828E;
    
//----------------------------------------------------------------------------		
    
    public static final int PHOTO_INTERPRETATION_GRAY = 1;
    public static final int PHOTO_INTERPRETATION_RGB = 2;
    public static final int PHOTO_INTERPRETATION_YCBCR = 6;
    public static final int PHOTO_INTERPRETATION_CFA = 32803;
        
//----------------------------------------------------------------------------		
    
    public static final int PLANAR_CFG_CHUNKY = 1;
    public static final int PLANAR_CFG_PLANAR = 2;
        
//----------------------------------------------------------------------------		
    
    public static final int RES_UNIT_NONE = 1;
    public static final int RES_UNIT_INCH = 2;
    public static final int RES_UNIT_CM = 3;
}
