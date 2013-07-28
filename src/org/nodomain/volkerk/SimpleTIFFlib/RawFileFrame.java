/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nodomain.volkerk.SimpleTIFFlib;

/**
 *
 * @author volker
 */
public class RawFileFrame extends Generic_CFA_PixBuf {
    
    /**
     * The image width
     */
    protected int width;
    
    /**
     * The image height
     */
    protected int height;
    
    /**
     * The bits per CFA pixel
     */
    protected int bpp;
    
    /**
     * x0, y0, x1, y1 of the active area of the image
     */
    protected int[] activeArea;
    
    /**
     * x0, y0, width, height of a crop window within the active area
     */
    protected int[] crop;
    
    /**
     * Constructor with the full data set
     * 
     * @param _buf FlexByteArray containing the CFA data of the frame
     * @param _w width of the frame in pixels
     * @param _h height of the frame in pixels
     * @param _bpp bits per pixel
     * @param aa x0, y0, x1, y1 of the active area within the frame
     * @param crop x0, y0, w, h of an crop area within the active area
     */
    public RawFileFrame(FlexByteArray _buf, int _w, int _h, int _bpp, int[] aa, int[] _crop)
    {
        super(_buf);
        
        width = _w;
        height = _h;
        bpp = _bpp;
        activeArea = aa;
        crop = _crop;
    }
    
    /**
     * Alternative constructor which sets Active Area and Default Crop to the whole image
     * 
     * @param _buf FlexByteArray containing the CFA data of the frame
     * @param _w width of the frame in pixels
     * @param _h height of the frame in pixels
     * @param _bpp bits per pixel
     */
    public RawFileFrame(FlexByteArray _buf, int _w, int _h, int _bpp)
    {
        this(_buf, _w, _h, _bpp, new int[]{0, 0, _w, _h}, new int[]{0, 0, _w, _h});
    }
    
    public RawFileFrame getCopy()
    {
        return new RawFileFrame(new FlexByteArray(data.getDataClone()), width, height, bpp, activeArea, crop);
    }

    @Override
    public int CFA_getBitsPerPixel() {
        return bpp;
    }

    @Override
    public int[] CFA_getActiveArea() {
        return activeArea;
    }

    @Override
    public int[] CFA_getCropInfo() {
        return crop;
    }

    @Override
    public int[] CFA_getPatternDim() {
        return new int[] {2,2};   // constant for RAW files
    }

    @Override
    public int[] CFA_getPattern() {
        return new int[] {0,1,1,2};  // constant for RAW files
    }

    /**
     * Returns the offset within the data block of the first byte of a row
     * 
     * @param row the 0-based y-coordinate of the pixel
     * 
     * @return the offset of the first byte in "data" that contains pixel bits
     */
    @Override
    protected long CFA_getRowOffsetInBuffer(int row)
    {
        // calculate the number of bytes in a row
        // use the ceil()-function to account for the byte-padding at the end of each row
        int bytesPerRow = (int) Math.ceil(width * CFA_getBitsPerPixel() / 8.0);
        
        return (long)(row * bytesPerRow);
    }

    @Override
    public int imgWidth() {
        return width;
    }

    @Override
    public int imgHeight() {
        return height;
    }
    
    /**
     * Returns a copy of the raw pixel data (e. g. for writing it back to the file)
     * 
     * @return a byte array with the raw pixel data
     */
    public byte[] getFrameData()
    {
        return data.getDataClone();
    }
    
}
