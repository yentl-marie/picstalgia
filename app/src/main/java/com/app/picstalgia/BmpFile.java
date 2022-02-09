package com.app.picstalgia;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class BmpFile {
    // Private constants
    private final static int BITMAPFILEHEADER_SIZE = 14;
    private final static int BITMAPINFOHEADER_SIZE = 40;
    // Private variable declaration
    // Bitmap file header
    private byte bitmapFileHeader[] = new byte[14];
    private byte bfType[] = {'B', 'M'};
    private int bfSize = 0;
    private int bfReserved1 = 0;
    private int bfReserved2 = 0;
    private int bfOffBits = BITMAPFILEHEADER_SIZE + BITMAPINFOHEADER_SIZE;
    // Bitmap info header
    private byte bitmapInfoHeader[] = new byte[40];
    private int biSize = BITMAPINFOHEADER_SIZE;
    private int biWidth = 0;
    private int biHeight = 0;
    private int biPlanes = 1;
    private int biBitCount = 24;
    private int biCompression = 0;
    private int biSizeImage = 0x030000;
    private int biXPelsPerMeter = 0x0;
    private int biYPelsPerMeter = 0x0;
    private int biClrUsed = 0;
    private int biClrImportant = 0;
    // Bitmap raw data
    private int pixels[];
    // File section
    private ByteBuffer buffer = null;
    private OutputStream outputStream;

    // Default constructor
    public BmpFile() {
    }

    public File saveBitmap(Bitmap bitmap, String name) {
        try {
            File file = new File(Environment.getExternalStorageDirectory()+"/Picstalgia/images/"+name+".bmp");
            if(!file.exists()){
                file.getParentFile().mkdirs();
            } else {
                return file;
            }
            outputStream = new FileOutputStream(file.getPath());
            save(bitmap);
            outputStream.close();
            return file;
        } catch (Exception e) {
            Log.e("Exception", e.getMessage());
            return null;
        }
    }

    public File saveBitmap(Bitmap bitmap) {
        try {
            String timeStamp = ZonedDateTime.now( ZoneId.systemDefault() )
                    .format( DateTimeFormatter.ofPattern( "uuuuMMddHHmmss" ) );
            File file = new File(Environment.getExternalStorageDirectory()+"/Picstalgia/images/picstalgia"+timeStamp+".bmp");
            if(!file.exists()){
                file.getParentFile().mkdirs();
            }
            outputStream = new FileOutputStream(file.getPath());
            save(bitmap);
            outputStream.close();
            return file;
        } catch (Exception e) {
            Log.e("Exception", e.getMessage());
            return null;
        }
    }

    public void saveBitmap(Bitmap bitmap,OutputStream outputStream) {
        this.outputStream = outputStream;
        save(bitmap);
    }

    /*
     *  The saveMethod is the main method of the process. This method
     *  will call the convertImage method to convert the memory image to
     *  a byte array; method writeBitmapFileHeader creates and writes
     *  the bitmap file header; writeBitmapInfoHeader creates the
     *  information header; and writeBitmap writes the image.
     */
    private void save(Bitmap bitmap) {
        try {
            convertImage(bitmap);
            writeBitmapFileHeader();
            writeBitmapInfoHeader();
            writeBitmap();
            // write to output stream
            outputStream.write(buffer.array());
        } catch (Exception e) {
            Log.e("Exception", e.getMessage());
        }
    }

    /*
     * convertImage converts the memory image to the bitmap format (BRG).
     * It also computes some information for the bitmap info header.
     */
    private boolean convertImage(Bitmap bitmap) {
        int pad;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        pixels = new int[width * height];

        bitmap.getPixels(
                pixels,
                0,
                width,
                0,
                0,
                width,
                height);

        pad = (4 - ((width * 3) % 4)) * height;
        biSizeImage = ((width * height) * 3) + pad;
        bfSize = biSizeImage + BITMAPFILEHEADER_SIZE +
                BITMAPINFOHEADER_SIZE;

        buffer = ByteBuffer.allocate(bfSize);
        biWidth = width;
        biHeight = height;
        return (true);
    }

    /*
     * writeBitmap converts the image returned from the pixel grabber to
     * the format required. Remember: scan lines are inverted in
     * a bitmap file!
     * Each scan line must be padded to an even 4-byte boundary.
     */
    private void writeBitmap() {
        int size;
        int value;
        int j;
        int i;
        int rowCount;
        int rowIndex;
        int lastRowIndex;
        int pad;
        int padCount;
        byte rgb[] = new byte[3];
        size = (biWidth * biHeight) - 1;
        pad = 4 - ((biWidth * 3) % 4);
        if (pad == 4)   // Bug correction
            pad = 0;    // Bug correction
        rowCount = 1;
        padCount = 0;
        rowIndex = size - biWidth;
        lastRowIndex = rowIndex;
        try {
            for (j = 0; j < size; j++) {
                value = pixels[rowIndex];
                rgb[0] = (byte) (value & 0xFF);
                rgb[1] = (byte) ((value >> 8) & 0xFF);
                rgb[2] = (byte) ((value >> 16) & 0xFF);
                buffer.put(rgb);
                if (rowCount == biWidth) {
                    padCount += pad;
                    for (i = 1; i <= pad; i++) {
                        buffer.put((byte) 0x00);
                    }
                    rowCount = 1;
                    rowIndex = lastRowIndex - biWidth;
                    lastRowIndex = rowIndex;
                } else
                    rowCount++;
                rowIndex++;
            }
            // Update the size of the file
            bfSize += padCount - pad;
            biSizeImage += padCount - pad;
        } catch (Exception e) {
            Log.e("Exception", e.getMessage());
        }
    }

    /*
     * writeBitmapFileHeader writes the bitmap file header to the file.
     */
    private void writeBitmapFileHeader() {
        try {
            buffer.put(bfType);
            buffer.put(intToDWord(bfSize));
            buffer.put(intToWord(bfReserved1));
            buffer.put(intToWord(bfReserved2));
            buffer.put(intToDWord(bfOffBits));
        } catch (Exception e) {
            Log.e("Exception", e.getMessage());
        }
    }

    /*
     * writeBitmapInfoHeader writes the bitmap information header
     * to the file.
     */
    private void writeBitmapInfoHeader() {
        try {
            buffer.put(intToDWord(biSize));
            buffer.put(intToDWord(biWidth));
            buffer.put(intToDWord(biHeight));
            buffer.put(intToWord(biPlanes));
            buffer.put(intToWord(biBitCount));
            buffer.put(intToDWord(biCompression));
            buffer.put(intToDWord(biSizeImage));
            buffer.put(intToDWord(biXPelsPerMeter));
            buffer.put(intToDWord(biYPelsPerMeter));
            buffer.put(intToDWord(biClrUsed));
            buffer.put(intToDWord(biClrImportant));
        } catch (Exception e) {
            Log.e("Exception", e.getMessage());
        }
    }

    /*
     * intToWord converts an int to a word, where the return
     * value is stored in a 2-byte array.
     */
    private byte[] intToWord(int parValue) {
        byte retValue[] = new byte[2];
        retValue[0] = (byte) (parValue & 0x00FF);
        retValue[1] = (byte) ((parValue >> 8) & 0x00FF);
        return (retValue);
    }

    /*
     * intToDWord converts an int to a double word, where the return
     * value is stored in a 4-byte array.
     */
    private byte[] intToDWord(int parValue) {
        byte retValue[] = new byte[4];
        retValue[0] = (byte) (parValue & 0x00FF);
        retValue[1] = (byte) ((parValue >> 8) & 0x000000FF);
        retValue[2] = (byte) ((parValue >> 16) & 0x000000FF);
        retValue[3] = (byte) ((parValue >> 24) & 0x000000FF);
        return (retValue);
    }
}


//source: https://stackoverflow.com/questions/22909429/android-save-a-bitmap-to-bmp-file-format