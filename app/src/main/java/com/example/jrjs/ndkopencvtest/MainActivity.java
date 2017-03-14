  package com.example.jrjs.ndkopencvtest;


import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

  public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2{

      // string de control
    public static String TAG = "MainActivity";
      // Mat que utilizo para guardar los frames que obtengo y tambien los proceso
    Mat mRgba, imgGrey, imgCanny;
      // objeto con el que se levanta la camara
    JavaCameraView javaCameraView;
      // buton para calibrar
      Button calibrar;

      // flag que cambia si se presiona el button calibrar para que comience la calibracion
      public boolean startCalibration = false;

      // flag que dice si ya fue calibrado o no
      public boolean calibrated = false;

      // int que contiene la cantidad de opciones
      public int size = 4;

      // guardo el centro de la cara
      public int xFaceCenter = 0;

      // index que me dice la opcion que esta iluminada (a la que se esta apuntando con la cara)
      public int index = -1;

      // cargo la libreria (se hace en tiempo de ejecucion)
      static {
          // segun lo que lei: en android 4.2 por un problema tiene problemas para encontrar
          // las librerias, por eso agrego la primera linea
          System.loadLibrary("opencv_java3");
          System.loadLibrary("MyOpencvLibs");
      }


    BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case BaseLoaderCallback.SUCCESS:
                    javaCameraView.enableView();
                    break;

                default:
                    super.onManagerConnected(status);
                    break;
            }

        }
    };

      // metodo donde comienza la ejecucion (seria como el main)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // para que se vea en full screen (me esconde las notificaciones)
        // pero para que se vea toda la camara hay que agregar en el manifest:
        // <application android:theme="@style/Theme.AppCompat.NoActionBar" >
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
         WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Log.d("VISUAL HELPER", "onCreate");

        setContentView(R.layout.activity_main);

        // apuntamos nuestro objeto al que añadimos en activity_main.xml
        javaCameraView = (JavaCameraView) findViewById(R.id.java_camera_view);
        javaCameraView.setVisibility(View.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);


        //*
        calibrar = (Button) findViewById(R.id.iButtonCalibrar);
        calibrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!startCalibration) {
                    startCalibration = true;
                    calibrated = false;
                    calibrar.setText("fin calibrado");
                }
                else {
                    calibrar.setText("Calibrar");
                    startCalibration = false;
                    // cuando presiona fin calibrado ya sabemos que lo calibro
                    calibrated = true;
                }
            }
        });
        //*/

    }


      @Override
      protected void onPause(){
          super.onPause();
          if(javaCameraView != null)
              javaCameraView.disableView();
      }


      @Override
      protected void onDestroy(){
          super.onDestroy();
          if(javaCameraView != null)
              javaCameraView.disableView();
      }

      @Override
      protected void onResume(){
          super.onResume();

          if(OpenCVLoader.initDebug()){
              Log.i(TAG,"Opencv cargado correctamente");
              mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
          }else {
              Log.i(TAG,"Opencv no cargado correctamente");
              OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
          }
      }



      // inicializo los Mat que cree arriba con los parametros que se obtiene de la camara
      // pueden variara si el <<widget>> de la camara (en el xml) es mas grande o mas chico
      @Override
      public void onCameraViewStarted(int width, int height) {

          mRgba = new Mat(height,width, CvType.CV_8UC4);
          imgGrey = new Mat(height,width, CvType.CV_8UC1);
          imgCanny = new Mat(height,width, CvType.CV_8UC1);
      }

      @Override
      public void onCameraViewStopped() {
          mRgba.release();
      }

      // aca se recibe cada uno de los frames que se va a procesar
      @Override
      public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
          mRgba = inputFrame.rgba();

          //*
          if (isCalibrated(mRgba)){

              Log.d("isCalibrated", "esta calibrado ;)");
              Log.d("xFacecenter", Integer.toString(xFaceCenter));

          }

          //*/






          // retorno null si no quiero que se vea la imagen pero que se tome cada frame de la camara
          return mRgba;
      }

      public boolean isCalibrated(Mat mRgba){

          if(calibrated && !startCalibration) return true;
          // si startCalibration esta en true ahi podemos calibrar y se retornara el centro de la cara
          // para que luego se detecte las variaciones
          if(startCalibration == true){
              // faceDetection deberia retornar el centro de la cara
              int xCentro = OpencvNativeClass.smileDetection(mRgba.getNativeObjAddr());
              if ( xCentro != -1 )
                  // si entro es que detecto al menos una sonrisa y la puedo utilizar como referencia

                  xFaceCenter = xCentro;
              calibrated = true; // en principio no voy a dejar que se descalibre

              // TODO: podria tambien (mas adelante) pedir que se calibre mas en el centro de la pantalla
          }
          if(!startCalibration){
              if(xFaceCenter == -1){

                  // TODO: crear un AlertDialog que indique que no se ha encontrado sonrisa
              }
          }

          Log.d("xFaceCenter", Integer.toString(xFaceCenter));

          return calibrated;
      }
  }
