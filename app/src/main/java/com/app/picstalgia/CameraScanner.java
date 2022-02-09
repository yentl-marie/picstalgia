package com.app.picstalgia;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.YuvImage;
import android.hardware.display.DisplayManager;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.impl.ImageCaptureConfig;
import androidx.camera.core.internal.IoConfig;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.camera.view.RotationProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.jetbrains.annotations.NotNull;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static androidx.camera.core.CameraXThreads.TAG;

public class CameraScanner  extends AppCompatActivity {
    private PreviewView previewView;
    private ImageCapture imageCapture;
    private ImageAnalysis imageAnalysis;
    private PopupWindow popupWindow;
    private ProgressBar progressLoader;
    private ProcessCameraProvider cameraProvider;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private Executor cameraExecutor = Executors.newSingleThreadExecutor();
    private OrientationEventListener orientationEventListener;
    private DisplayManager.DisplayListener displayListener;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scanner_activity);

        previewView = findViewById(R.id.preview_view);
        MaterialButton captureButton = findViewById(R.id.capture_btn);
        ImageButton closeButton = findViewById(R.id.close_btn);
        progressLoader = findViewById(R.id.progress_loader);

        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressLoader.setVisibility(View.VISIBLE);

                captureImage();
            }
        });

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                orientationEventListener.disable();
                cameraProvider.unbindAll();
                goToMain();
                finish();
            }
        });


    }

    @Override
    public void onStop() {
        super.onStop();
        orientationEventListener.disable();
        cameraProvider.unbindAll();
    }

    @Override
    public void onPause() {
        super.onPause();
        orientationEventListener.disable();
        cameraProvider.unbindAll();
    }

    @Override
    public void onResume() {
        super.onResume();
        setupCamera();

        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");

        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
        }
    }

    private void setupCamera() {
        //camera preview
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindPreview(cameraProvider);

                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));


    }

    private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder().setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {

                image.close();
            }
        });
        OrientationEventListener orientationEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
//                textView.setText(Integer.toString(orientation));
            }
        };
        orientationEventListener.enable();
    }




    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        this.cameraProvider = cameraProvider;

        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        imageCapture = new ImageCapture.Builder()
                .build();

        imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        //orientation
        orientationEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                if(orientation == ORIENTATION_UNKNOWN) {
                    return;
                }else {
                    int rotation = 0;
                    if (orientation >= 45 && orientation < 135) {
                        rotation = Surface.ROTATION_270;
                    } else if (orientation >= 135 && orientation < 225) {
                        rotation = Surface.ROTATION_180;
                    } else if (orientation >= 225 && orientation < 315) {
                        rotation = Surface.ROTATION_90;
                    } else {
                        rotation = Surface.ROTATION_0;
                    }

                    imageAnalysis.setTargetRotation(rotation);
                    imageCapture.setTargetRotation(rotation);
                }
            }
        };
        orientationEventListener.enable();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, imageAnalysis, imageCapture, preview);
    }

    private void analyzeImage() {
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer() {
            @Override
            @SuppressLint("UnsafeOptInUsageError")
            public void analyze(@NonNull ImageProxy imageProxy) {
                System.out.println(imageProxy.getFormat()+" analyzer ===============================");


                imageProxy.close();


            }
        });
    }


    @SuppressLint("UnsafeOptInUsageError")
    private void captureImage() {
        imageCapture.takePicture(cameraExecutor, new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess (ImageProxy imageProxy) {
                        Image image = imageProxy.getImage();
                        System.out.println(imageProxy.getImageInfo().getRotationDegrees()+" ===============================");
                        Bitmap bitmap = rotateBitmap(toBitmap(image), imageProxy.getImageInfo().getRotationDegrees());
                        Bitmap croppedBitmap = cropImage(bitmap);
                        try {
                            Rectangle rect = detectRectangle(croppedBitmap);
                            if(rect != null) {
                                Bitmap skewCorrected = rotateBitmap(correctSkews(rect, croppedBitmap), 90);

                                CameraScanner.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        viewImage(skewCorrected);
                                    }
                                });
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        imageProxy.close();
                    }

                    @Override
                    public void onError (ImageCaptureException exception) {
                        exception.printStackTrace();
                    }
                }
        );
    }

    public static Bitmap rotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    private Bitmap toBitmap(Image image) {
        ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
        byteBuffer.rewind();
        byte[] bytes = new byte[byteBuffer.capacity()];
        byteBuffer.get(bytes);
        byte[] clonedBytes = bytes.clone();
        return BitmapFactory.decodeByteArray(clonedBytes, 0, clonedBytes.length);

    }

    public void goToMain() {
        Intent mainMenu = new Intent(CameraScanner.this, MainMenu.class);
        mainMenu.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(mainMenu);
    }

    //-----------------FINDING THE IMAGE----------------------------

    private List<Point> findCorners(Bitmap image) throws Exception {
        Mat imgMat = new Mat();
        Utils.bitmapToMat(image, imgMat);


        Mat gray = new Mat();
        Imgproc.cvtColor(imgMat, gray, Imgproc.COLOR_BGR2GRAY);

        Mat blurred = gray.clone();
        Imgproc.GaussianBlur(gray, blurred, new org.opencv.core.Size(11,11), 0);

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

        MatOfPoint2f approxCurve;

        double maxArea = 0;
        int maxId = -1;

        Mat result = new Mat();

        //CANNY ALGORITHM
        Mat edgeMat = new Mat();
        Imgproc.Canny(blurred, edgeMat, 10, 20);
        Imgproc.morphologyEx(edgeMat, result, Imgproc.MORPH_CLOSE,
                Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new org.opencv.core.Size(8, 8)));


        Mat hierarchy = Mat.zeros(new org.opencv.core.Size(5.0, 5.0), CvType.CV_8UC1);
        Imgproc.findContours(result, contours, hierarchy,
                    Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);



        for (MatOfPoint contour : contours) {
            MatOfPoint2f temp = new MatOfPoint2f(contour.toArray());

            double area = Imgproc.contourArea(contour);
            approxCurve = new MatOfPoint2f();
            Imgproc.approxPolyDP(temp, approxCurve,
                    Imgproc.arcLength(temp, true) * 0.02, true);



            if (approxCurve.total() == 4 && area >= maxArea) {
                Rectangle rect = contourToRectangle(approxCurve.toList());
                float rectWidth = rect.distance(rect.topLeft(), rect.topRight());
                float rectHeight = rect.distance(rect.topLeft(), rect.bottomLeft());
                if(Math.abs(rectHeight-rectWidth) < 100){ //detect largest square
                    maxArea = area;
                    maxId = contours.indexOf(contour);
                }

            }
        }

        if (maxId >= 0) {
            MatOfPoint2f temp = new MatOfPoint2f(contours.get(maxId).toArray());
            approxCurve = new MatOfPoint2f();
            Imgproc.approxPolyDP(temp, approxCurve,
                    Imgproc.arcLength(temp, true) * 0.02, true);

            System.out.println("========================= approx curve: "+approxCurve.toList());

            return approxCurve.toList();
        }

        return null;
    }

    private Bitmap cropImage(Bitmap bitmap) {
        int imageWidth = bitmap.getWidth();
        int x = imageWidth / 5;
        int y = (int) (bitmap.getHeight() /5);
        int cropWidth = imageWidth - (int) (imageWidth / 2.5);

        System.out.println("===================== bitWidth = " + imageWidth +" bitHeight = "+ bitmap.getHeight() + " cropWidth = "+ cropWidth + " x = "+x +" y = "+y);

        return Bitmap.createBitmap(bitmap, x, y, cropWidth, cropWidth);
    }
    private Rectangle detectRectangle(Bitmap bitmap) throws Exception {

        List<Point> corners = findCorners(bitmap);
        if(corners != null) {
            Rectangle rect = contourToRectangle(corners);
            System.out.println("=== tl "+ rect.topLeft()+"tr "+rect.topRight() + "bl "+rect.bottomLeft()+"br "+rect.bottomRight());

            System.out.println(" rect width: "+ (rect.distance(rect.topLeft(), rect.topRight()) + 20) +" rect height: "+ (rect.distance(rect.topLeft(), rect.bottomLeft()) + 20));

            return rect;
        }

        return null;
    }


    private Rectangle contourToRectangle(List<Point> points) {
        List<android.graphics.Point> temp = new ArrayList<>();
        temp.add(new android.graphics.Point((int)points.get(0).x, (int) points.get(0).y));
        temp.add(new android.graphics.Point((int) points.get(1).x, (int) points.get(1).y));
        temp.add(new android.graphics.Point((int) points.get(2).x, (int) points.get(2).y));
        temp.add(new android.graphics.Point((int) points.get(3).x, (int) points.get(3).y));

        return Rectangle.from(temp);

    }

    //----------------------------CORRECTING SKEWS-------------------------------
    private Bitmap correctSkews(Rectangle rect, Bitmap image) {
        float widthTop = rect.distance(rect.topLeft(), rect.topRight());
        float widthBottom = rect.distance(rect.bottomLeft(), rect.bottomRight());
        float heightLeft = rect.distance(rect.topLeft(), rect.bottomLeft());
        float heightRight = rect.distance(rect. topRight(), rect.bottomRight());

        float maxWidth = Math.max(widthTop, widthBottom);
        float maxHeight = Math.max(heightLeft, heightRight);
        float finalLen = Math.min(maxWidth, maxHeight);

        List<Point> dstData = new ArrayList<>();
        dstData.add(new Point(0, 0));
        dstData.add(new Point(0, finalLen));
        dstData.add(new Point(finalLen, 0));
        dstData.add(new Point(finalLen, finalLen));
        Mat dstPoints = Converters.vector_Point2f_to_Mat(dstData);

        Mat srcPoints = Converters.vector_Point2f_to_Mat(rect.points_opencv());
        Mat perspectiveTransform = Imgproc.getPerspectiveTransform(srcPoints, dstPoints);

        Mat srcMat = new Mat(image.getWidth(), image.getHeight(), CvType.CV_8UC1);
        Utils.bitmapToMat(image, srcMat);
        Mat dstMat = new Mat(Math.round(finalLen), Math.round(finalLen), CvType.CV_8UC1);

        Imgproc.warpPerspective(srcMat, dstMat, perspectiveTransform, new org.opencv.core.Size(finalLen, finalLen), Imgproc.INTER_CUBIC);

        Bitmap result = Bitmap.createBitmap(dstMat.cols(), dstMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(dstMat, result); //result is flipped vertically

        //flip image
        Matrix matrix = new Matrix();
        matrix.postScale(1, -1, result.getWidth()/2f, result.getHeight()/2f);

        return Bitmap.createBitmap(result, 0,0, result.getWidth(), result.getHeight(), matrix, true);
    }

    //==========================IMAGE VIEW====================================
    private void viewImage(Bitmap image) {
        View popupView = showPopupWindow(R.layout.popup_view_media);
        popupWindow.showAtLocation(getWindow().getDecorView(), Gravity.CENTER_HORIZONTAL, 0, 0);
        ImageView imageView = popupView.findViewById(R.id.image_view);
        MaterialButton closeButton = popupView.findViewById(R.id.close_btn);
        MaterialButton playButton = popupView.findViewById(R.id.media_preview);

        imageView.setImageBitmap(image);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(500, 500));

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressLoader.setVisibility(View.GONE);
                popupWindow.dismiss();
            }
        });

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = scanImage(image);
                popupWindow.dismiss();
                if(name != null) {
                    String type = name.split("_")[1];
                    if(type.equals("url")) {
                        playUrlMedia(name);  //play url from here it won't open the player
                    } else {
                        startMediaPlayer(name);
                    }
                } else {
                    Toast.makeText(CameraScanner.this, "Error: Media is not extracted.", Toast.LENGTH_SHORT).show();
                    progressLoader.setVisibility(View.GONE);
                }
            }
        });
    }

    private void startMediaPlayer(String name) {
        Intent mediaPlayer = new Intent(this, MediaPlayer.class);
        mediaPlayer.putExtra("filename", name);
        startActivity(mediaPlayer);
    }

    private void playUrlMedia(String name) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("media").document(user.getUid())
                .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull @NotNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot doc = task.getResult();
                    if (doc != null) {
                        String link = doc.getString(name);
                        if (!link.startsWith("http://") && !link.startsWith("https://")) {
                            link = "http://" + link;
                        }

                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                        startActivity(browserIntent);
                    }
                }
            }
        });
    }

    private String scanImage(Bitmap image) {
        BlindWatermark scanner = new BlindWatermark();
        Bitmap qr = scanner.scan(image);

        return scanner.readQRImage(qr);
    }

    public View showPopupWindow(int layout) {
        //Create a View object yourself through inflater
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(layout, null);

        //Specify the length and width through constants
        int width = WindowManager.LayoutParams.WRAP_CONTENT;
        int height = WindowManager.LayoutParams.WRAP_CONTENT;

        //Make Inactive Items Outside Of PopupWindow
        boolean focusable = true;

        //Create a window with our parameters
        popupWindow = new PopupWindow(popupView, width, height, focusable);

        //do not dismiss when clicked outside
        popupWindow.setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(motionEvent.getX() < 0 || motionEvent.getX() > view.getWidth()) return true;
                if(motionEvent.getY() < 0 || motionEvent.getY() >view.getHeight()) return true;

                return false;
            }
        });

        return popupView;
    }
}
