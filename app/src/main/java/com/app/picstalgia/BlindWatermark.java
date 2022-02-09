package com.app.picstalgia;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Environment;

import com.google.android.material.button.MaterialButton;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
//import com.watermark.androidwm.WatermarkBuilder;
//import com.watermark.androidwm.listener.BuildFinishListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class BlindWatermark {
    private int[][] qTable = {
            {16, 11, 10, 16, 24, 40, 51, 61},
            {12, 12, 14, 19, 26, 58, 60, 55},
            {14, 13, 16, 24, 40, 57, 69, 56},
            {14, 17, 22, 29, 51, 87, 80, 62},
            {18, 22, 37, 56, 68, 109, 103, 77},
            {24, 35, 55, 64, 81, 104, 113, 92},
            {49, 64, 78, 87, 103, 121, 120, 101},
            {72, 92, 95, 98, 112, 100, 103, 99}
    };

    private final double w0 = 0.5;
    private final double w1 = -0.5;
    private final double s0 = 0.5;
    private final double s1 = 0.5;

    private double[] cbComponent;
    private double[] crComponent;
    double[][] yPixels;
    int[][] originalPixels;
    int origWidth;
    int origHeight;

    private File photoFile;
    private String mediaUrl;

    public BlindWatermark(File photofile, String mediaUrl) {
        this.photoFile = photofile;
        this.mediaUrl = mediaUrl;
    }

    public BlindWatermark() {
    }

    public Bitmap scan(Bitmap scannedBitmap) {
        Bitmap img = null;
        if(scannedBitmap.getHeight() != scannedBitmap.getWidth()) {
            img = getImage(scannedBitmap); //image is from file
        } else {
            img = scannedBitmap;        //image is from camera
        }

        yPixels = getYComponent(img, "pic");
        double[][] dwt = dwt2D(yPixels);

        double[][] subbandHH = getHHSubband(dwt);
        Bitmap qr = extractQR(subbandHH);
        return qr;
    }

    public String readQRImage(Bitmap bMap) {
        String contents = null;

        int[] intArray = new int[bMap.getWidth()*bMap.getHeight()];
        //copy pixel data from the Bitmap into the 'intArray' array
        bMap.getPixels(intArray, 0, bMap.getWidth(), 0, 0, bMap.getWidth(), bMap.getHeight());

        LuminanceSource source = new RGBLuminanceSource(bMap.getWidth(), bMap.getHeight(), intArray);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        Reader reader = new MultiFormatReader();// use this otherwise ChecksumException
        try {
            Result result = reader.decode(bitmap);
            contents = result.getText();
        } catch (NotFoundException e) { e.printStackTrace(); }
        catch (ChecksumException e) { e.printStackTrace(); }
        catch (FormatException e) { e.printStackTrace(); }
        return contents;
    }

    public Bitmap watermark(Bitmap polaroid) {
        Bitmap image = getImage(polaroid);

        yPixels = getYComponent(image, "pic");
        double[][] dwt = dwt2D(yPixels);

        double[][] subbandLL = getLLSubband(dwt);
        double[][] quantizedLL = quantize(subbandLL);
        Bitmap qrCode = generateQRCode();
        embedQR(qrCode, quantizedLL);
        replaceHH(dwt, quantizedLL);


        double[][] idwt = idwt2d(dwt);
        int[] colors = getRGB(idwt);
        int[] out = backToPolaroid(colors);

        return Bitmap.createBitmap(out, origWidth, origHeight, Bitmap.Config.ARGB_8888);
    }

    public int[] backToPolaroid(int[] image) {
        int[] polaroid = new int[origWidth*origHeight];

        for(int i=0; i<origHeight; i++) {
            for (int j = 0; j < origWidth; j++) {
                if (j > 31 && j < 731 && i > 31 && i < 731) {
                    originalPixels[i][j] = image[((i-32)*700)+(j-32)];
                }
                polaroid[(i*origWidth) + j] = originalPixels[i][j];
            }
        }
        return polaroid;
    }

    public Bitmap getImage(Bitmap polaroid) {
        origWidth = polaroid.getWidth();
        origHeight = polaroid.getHeight();
        originalPixels = new int[origHeight][origWidth];
        int[] imageColors = new int[700*700];  //700x700 is the size assigned previously

        for(int i=0; i<origHeight; i++) {
            for(int j=0; j<origWidth; j++) {
                int color = polaroid.getPixel(j,i);
                if(j > 31 && j <= 731 && i > 31 && i <= 731) {
                    imageColors[((i-32)*700)+(j-32)] = color;
                } else {
                    originalPixels[i][j] = color;  //save this for going back to polaroid
                }
            }
        }

        return Bitmap.createBitmap(imageColors, 700, 700, Bitmap.Config.ARGB_8888);

    }



    public double[][] getYComponent(Bitmap bitmap, String bitType) {
        int len = bitmap.getWidth();

        System.out.format("===width %s \n", len);
        double[][] yComponent = new double[len][len];

        if(bitType.equals("pic")) {
            cbComponent = new double[len*len];
            crComponent = new double[len*len];

            for(int i=0; i<len; i++){
                for(int j=0; j<len; j++) {
                    int color = bitmap.getPixel(j,i);
                    int r = Color.red(color);
                    int g = Color.green(color);
                    int b = Color.blue(color);

                    //adjust the light colors because the code is showing
                    int[] px = adjustColor(r, g, b);
                    r = px[0];
                    g = px[1];
                    b = px[2];

                    yComponent[i][j] = r *  0.301 + g *  0.586 + b *  0.113;  // formula from https://stackoverflow.com/questions/17892346/how-to-convert-rgb-yuv-rgb-both-ways
                    cbComponent[(len*i)+j] = r * -0.168 + g * -0.332 + b *  0.500 + 128;
                    crComponent[len*i+j] = r *  0.500 + g * -0.417 + b * -0.082 + 128;
                }
                System.out.println();
            }
        } else {
            for(int i=0; i<len; i++){
                for(int j=0; j<len; j++) {
                    int color = bitmap.getPixel(j,i);
                    int r = Color.red(color);
                    int g = Color.green(color);
                    int b = Color.blue(color);

                    yComponent[i][j] = r *  0.301 + g *  0.586 + b *  0.113;  // formula from https://stackoverflow.com/questions/17892346/how-to-convert-rgb-yuv-rgb-both-ways

                }
            }
        }

        return yComponent;
    }

    public int[] adjustColor(int r, int g, int b) {
        int max = 252;
        int min = 10;
        if(r > max) {
            int offset = r-max;
            r = r-offset;
        } else if(g > max) {
            int offset = g-max;
            g = g-offset;
        } else if(b > max) {
            int offset = b-max;
            b = b-offset;
        }

        return new int[] {r, g, b};
    }

    public int[] getRGB(double[][] input) {
        int len = input.length;
        double[] y = new double[len*len];
        int[] colors = new int[len*len];
        int[] r = new int[len*len];
        int[] g = new int[len*len];
        int[] b = new int[len*len];

        for(int i=0; i<len; i++) {
            for(int j=0; j<len; j++) {
                y[len*i+j] = input[i][j];
            }
        }

        for(int i=0; i<len*len; i++) {
            r[i] = (int) (y[i]                                     + (crComponent[i] - 128) * 1.40200 );
            g[i] = (int) (y[i] + (cbComponent[i] - 128) * -0.34414 + (crComponent[i] - 128) * -0.71414 );
            b[i] = (int) (y[i] + (cbComponent[i] - 128) * 1.77200);

            colors[i] = Color.rgb(r[i], g[i], b[i]);

        }
        return colors;

    }

    public double[] dwt1D(double[] data) {
        double[] result = new double[data.length];

        int h = data.length/2;
        for (int i=0; i<h; i++){
            int k = i*2;
            result[i] = data[k]*s0 + data[k+1]*s1;
            result[i+h] = data[k]*w0 + data[k+1]*w1;
        }

        return result;
    }

    public double[][] dwt2D(double[][] yPixels) {       //source: https://stackoverflow.com/questions/29805608/implementing-discrete-wavelet-transformation-on-android
        int width = yPixels[0].length;
        int height = yPixels.length;
        double[][] scaledY = new double[height][width];
        double col[] = new double[height];
        double[][] temp = new double[height][width];

        //dwt row wise
        for(int i=0; i<height; i++) {
            temp[i] = dwt1D(yPixels[i]);
        }

        //dwt col wise
        for(int i=0; i<width; i++) {
            for(int j=0; j<height; j++) {
                col[j] = temp[j][i];
            }
            double[] result = dwt1D(col);
            for(int j=0; j<height; j++) {
                temp[j][i] = result[j];
            }
        }

        return temp;
    }

    public double[] idwt1D(double[] data) {
        double[] result = new double[data.length];

        int h = data.length/2;
        for (int i=0; i<h; i++){
            int k = i*2;
            result[k] = (data[i] * s0 + data[i + h] * w0) / w0;
            result[k + 1] = (data[i] * s1 + data[i + h] * w1) / s0;
        }

        return result;

    }

    public double[][] idwt2d(double[][] pix){
        int len = pix.length;
        double col[] = new double[len];
        double[][] temp = new double[len][len];

        //inverse dwt col wise
        for(int i=0; i<len; i++) {
            for(int j=0; j<len; j++) {
                col[j] = pix[j][i];
            }
            double[] result = idwt1D(col);
            for(int j=0; j<len; j++) {
                temp[j][i] = result[j];
            }
        }

        //inverse dwt row wise
        for(int i=0; i<len; i++) {
            temp[i] = idwt1D(temp[i]);
        }

        return temp;
    }

    public double[][] getLLSubband(double[][] dwt) {
        int length = yPixels.length/2;
        double[][] subbandLL = new double[length][length];

        for(int i=0; i<length;i++){
            for(int j=0; j<length;j++) {
                subbandLL[i][j] = dwt[i][j];
            }
        }
        return subbandLL;
    }

    public double[][] getHHSubband(double[][] dwt) {
        int len;
        if(dwt.length % 2 == 0) {
            len = dwt.length/2;
        }else {
            len = dwt.length/2 + 1;
        }
        double[][] hh = new double[dwt.length/2][dwt.length/2];

        for(int i=len; i<dwt.length; i++) {
            for (int j = len; j < dwt.length; j++) {
                hh[i-len][j-len] = dwt[i][j];
            }
        }
        return hh;
    }

    public double[][] quantize(double[][] ll) {
        int len = ll.length;
        double[][] q = new double[len][len];

        int x = 0;
        int y = 0;
        for(int i=0; i<len; i++) {
            for(int j=0; j<len; j++) {
                q[i][j] = ll[i][j] / qTable[x][y];
                y++;
                if(y == 8) {
                    y = 0;
                }
            }
            x++;
            if(x == 8) {
                x = 0;
            }
        }

        return q;
    }

    public void replaceHH(double[][] dwt, double[][] qLL) {
        int len;
        if(dwt.length % 2 == 0) {
            len = dwt.length/2;
        }else {
            len = dwt.length/2 + 1;
        }

        for(int i=len; i<dwt.length; i++) {
            for(int j=len; j<dwt.length; j++) {
                double val = qLL[i-len][j-len];
                if(val < 0){
                    val = 0;
                }
                if(val > 255){
                    val = 255;
                }
                dwt[i][j] = val;
            }
        }

    }

    public Bitmap generateQRCode() {
        int dimen = yPixels.length / 2;
        Bitmap bitmap;
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = qrCodeWriter.encode(mediaUrl, BarcodeFormat.QR_CODE, dimen, dimen);
            bitmap = Bitmap.createBitmap(dimen, dimen, Bitmap.Config.ARGB_8888);
            for (int x = 0; x<dimen; x++){
                for (int y=0; y<dimen; y++){
                    if(bitMatrix.get(x, y)) {
                        bitmap.setPixel(x, y, Color.BLACK);
                    } else {
                        bitmap.setPixel(x,y,Color.WHITE);
                    }
                }
            }
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void embedQR(Bitmap qr, double[][] ll) {
        double k = ll.length;
        double[][] yqr = getYComponent(qr, "qr");

        System.out.println("========embed========");
        for(int i=0; i<qr.getWidth(); i++) {
            for(int j=0; j<qr.getWidth(); j++) {
                ll[i][j] = yqr[i][j]/k;
            }
            System.out.println();
        }
    }

    public Bitmap extractQR(double[][] hh) {
        int len = hh.length;
        int k = len;
        int[][] qr = new int[len][len];
        Bitmap qrCode = Bitmap.createBitmap(len, len, Bitmap.Config.ARGB_8888);
        System.out.println("========extract========");
        int[] temp = new int[len];
        int[] col = new int[len];

        for(int i=0; i<len; i++) {
            for(int j=0; j<len; j++) {
                double val = Math.round(hh[i][j]) * k;

                if(val > 0) {
                    qr[i][j] = 1;
                } else if (val == 0){
                    qr[i][j] = 0;
                }
            }
            temp = cleanQRImage(qr[i]);
            for(int j=0; j<len; j++) {
                if(temp[j] == 0) {
                    qrCode.setPixel(j,i,Color.BLACK);
                } else {
                    qrCode.setPixel(j,i,Color.WHITE);
                }
            }

        }
        return qrCode;
    }

    public int[] cleanQRImage(int[] qr) {
        int[] temp = new int[qr.length];
        int len = qr.length;
        int x = 0;

        for(int i=0; i< qr.length; i++) {
            if(i<=(int)len/9|| i>=(int)(len-len/9)){
                temp[i] = 1; //white
            }else {
                if(qr[i] == 0){ //if black
                    x = x+1;
                }else {
                    temp[i] = 1;
                    if(x >= (int) len/45){
                        for(int j=1; j<x;j++){
                            temp[i-j] = 0;  //black
                        }
                    }else{
                        for(int j=1; j<x+1;j++){
                            temp[i-j] = 1;  //white
                        }
                    }
                    x=0;
                }
            }
        }
        return temp;
    }
}
