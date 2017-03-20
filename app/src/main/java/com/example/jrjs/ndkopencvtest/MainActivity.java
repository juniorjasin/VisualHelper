  package com.example.jrjs.ndkopencvtest;


import android.content.DialogInterface;
import android.graphics.Color;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.imgproc.Imgproc.COLOR_RGB2GRAY;
import static org.opencv.imgproc.Imgproc.resize;

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
      public int xFaceCenter = -1;

      // index que me dice la opcion que esta iluminada (a la que se esta apuntando con la cara)
      public int index = -1;

      // medidas de la pantalla
      public int srcHeight;
      public int srcWidth;

      // objetos manejadores de la ventana (mas adelante podria ser un vector o una lista)
      public ImageView im1;
      public ImageView im2;
      public ImageView im3;
      public ImageView im4;

      public List<ImageView> imList = new ArrayList<ImageView>();

      // contador de sonrisas, para seleccionar una opcion luego de que detecte
      // este numero consecutivo de sonrisas
      public int smileCounter = 40;


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

        // obtengo medidas de la pantalla
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        srcHeight = displayMetrics.heightPixels;
        srcWidth = displayMetrics.widthPixels;

        im1 = (ImageView) findViewById(R.id.img1);
        im2 = (ImageView) findViewById(R.id.img2);
        im3 = (ImageView) findViewById(R.id.img3);
        im4 = (ImageView) findViewById(R.id.img4);

        // seteo en tiempo de ejecucion las imagenes
        // uso Background y no setSrc porque lei que con src
        // se superpone la imagen encima de la otra, pero no se como comprobar si es cierto
        im1.setImageResource(R.drawable.hola);
        im2.setImageResource(R.drawable.tengoquieroestoy);
        im3.setImageResource(R.drawable.familia);
        im4.setImageResource(R.drawable.responder);


        // cargo las imagenes en la lista
        imList.add(im1);
        imList.add(im2);
        imList.add(im3);
        imList.add(im4);

        //*
        // hago la accion para cuando se presione el boton calibrar (ponerlo en un metodo)
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

                    if(isCalibrated()){
                        // se la comente porque me parece que es molesto pero la otra si es importante
                        //dialogoAviso("Calibracion", "Calibracion exitosa");
                    }else{
                        dialogoAviso("Calibracion", "Fallo calibracion");
                    }
                }
            }
        });
        //*/

    }

      // muestro AlertDialog con mensaje
      public void dialogoAviso(String title, String message){

          // Muestro un cartelito que diga que esta calibrado
          new AlertDialog.Builder(MainActivity.this)
                  .setTitle(title)
                  .setMessage(message)
                  .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                      public void onClick(DialogInterface dialog, int which) {
                          // continue with delete
                      }
                  })
                  .setIcon(android.R.drawable.ic_dialog_alert)
                  .show();
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
          Core.flip(mRgba,mRgba,1); // invierto los pixeles asi nos vemos como si fuera una espejo

          Mat rz = new Mat();
          resize(mRgba, rz, new Size(srcWidth, srcHeight));

          if (!isCalibrated()){

              // hago esto dentro y no fuera porque consumia recursos y crasheaba
              // agrando la imagen para analizarla (no seria necesario agrandarlo al tamaño de la pantalla
              // podria hacerla un poco mas chica si ncesito mejor performance)

              calibrar(rz);
          }

          if(xFaceCenter != -1){
              // ya esta calibrado (xFaceCenter tiene un valor)

              // obtengo el index para saber que imagen seleccionar
              index = OpencvNativeClass.getIndex(rz.getNativeObjAddr(),xFaceCenter,size);
          }

          // TODO: segun la opcion index que se eligio, saltar a otra opcion
          // 1) hacer una prueba sencilla que cuando se detecte sonrisa cambie algo
          // lo hice
          // 2) ver la forma de utilizar arbol ottaa.xml -> creo que deberia hacer un selector
          // no me va a servir porque el selector hace cambios en un solo widget (imagen en este caso)
          // y no es practico hacer uno por cada imagen
          // 3) acomodar las imagenes para que esten centradas segun el tamaño de la pantalla

          // TODO: tomar muchos frames de sonrisa para que se seleccione la opcion

          // si se obtuvo un indice valido que se resalte la opcion
          if(index != -1){
              resaltarOpcion(index);

              int sd = OpencvNativeClass.smileDetection(rz.getNativeObjAddr());
              Log.d("smileDet", Integer.toString(sd));
              if( sd != -1){
                  // TODO: encontro sonrisa hago mi accion
                  smileCounter++;
                  if(smileCounter >= 4){ // cuando esta de frente detecta sonrisas por mas que no sonria con 4

                      Imgproc.cvtColor(mRgba,mRgba,COLOR_RGB2GRAY);
                      Log.d("smileDet Gray", Integer.toString(sd));

                  }
              } else smileCounter = 0;
          }


          /* funcionamiento del programa en QT:
             1)En el main se crea un objeto internface, que este (constructor) crea un Graph, y luego
             lo inicializa y le pide el nodo inicial al grafo
             2) Primero con la clase XMLreader levanta el xml y lo parsea, busca en las etiquetas <node>
             los datos que necesita
             3) con esto crea un objeto de la clase Node, que tiene los atributos (id, title, img.png, etc)
             y los agrega un QVector de nodes
             4)


          */




          // siempre tengo que hacerle relese a los Mat porque sino se me acumulan en la memoria
          rz.release();
          // retorno null si no quiero que se vea la imagen pero que se tome cada frame de la camara
          return mRgba;
      }


      // switch para elegir opcion en index (solo para no tener tanto codigo en onCameraFrame)
      public void resaltarOpcion(int index){

          switch (index){
              case 0:
                  destacarImagen(index);
                  break;
              case 1:
                  destacarImagen(index);
                  break;
              case 2:
                  destacarImagen(index);
                  break;
              case 3:
                  destacarImagen(index);
                  break;
          }
      }

      // metodo que necesito porque si no corre en original thread no funciona
      // no puede modificar cosas de la vista fuera de este thread, solo con listener, oncreate, etc
      public void destacarImagen(final int idx){
          runOnUiThread(new Runnable() {
              @Override
              public void run() {
                  resaltarOpcionApuntada(idx);
                  oscurecerOtrasOpciones(idx);
              }
          });
      }

      // resalto la opcion que se mira
      public void resaltarOpcionApuntada(int opcion){
          ImageView iv = null;
          try {
              iv = imList.get(opcion);
              iv.clearColorFilter();

              // con esto hago que se cambie segun lo que setee en el selector.xml
              // iv.setActivated(true);


          }catch (IndexOutOfBoundsException e){
              Log.d("error", e.toString());
          }
      }

      // oscurezco (filtro) las opciones restantes
      public void oscurecerOtrasOpciones(int opcion){
          // agrego filtro oscuro a las imagenes que no estoy mirando
          for (int i = 0; i < imList.size(); i++) {
              if(i != opcion){
                  ImageView ivClear = null;
                  try{
                      ivClear = imList.get(i);
                      ivClear.setColorFilter(Color.argb(100, 0, 0, 0));
                  }catch (IndexOutOfBoundsException e){
                      Log.d("error", e.toString());
                  }
              }
          }
      }

      // si el boton para empezar a calibrar no esta presionado y ya esta calibrado retorna true
      public boolean isCalibrated(){
          return calibrated && !startCalibration;
      }

      // se llama si no esta calibrado y cuando se haya presionado el boton calibrar
      // una vez obtenida una sonrisa (desde C++) se la almacena en xFaceCenter
      // y la utilizo para calcular el index
      public boolean calibrar(Mat mRgba){

          // si startCalibration esta en true ahi podemos calibrar y se retornara el centro de la cara
          // para que luego se detecte las variaciones
          if(startCalibration == true){
              // faceDetection deberia retornar el centro de la cara
              int xCentro = OpencvNativeClass.smileDetection(mRgba.getNativeObjAddr());
              if ( xCentro != -1 ) {
                  // si entro es que detecto al menos una sonrisa y la puedo utilizar como referencia

                  xFaceCenter = xCentro;
                  calibrated = true; // en principio no voy a dejar que se descalibre
              }
          }
          return calibrated;
      }

  }
