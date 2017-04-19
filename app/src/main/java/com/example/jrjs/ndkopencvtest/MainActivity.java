  package com.example.jrjs.ndkopencvtest;


import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Handler;

import static org.opencv.imgproc.Imgproc.COLOR_RGB2GRAY;
import static org.opencv.imgproc.Imgproc.resize;

  public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, TextToSpeech.OnInitListener {

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
      private int srcHeight;
      private int srcWidth;

      // medidas de las imagenes
      private int imgsWidth = 0;
      private int imgsHeigth = 0;

      // objetos manejadores de la ventana (mas adelante podria ser un vector o una lista)
      public ImageView im1;
      public ImageView im2;
      public ImageView im3;
      public ImageView im4;

      public List<ImageView> imList = new ArrayList<ImageView>();

      // contador de ojos, para seleccionar una opcion luego de que detecte
      // este numero consecutivo de ojos
      public int eyeCounter = 0;

      // var para verificar si se apuntaba al mismo index en la vuelta anterior
      private int lastIndex = 0;

      // contador para que luego de n veces seguidas apuntando al mismo index
      // se pueda seleccionar la opcion
      int indexCounter = 0;

      // clase para leer ottaa.xml
      XMLReader xmlReader = new XMLReader();

      // List de Nodos que conforman el grafo
      Graph gr;

      // cantidad de imagenes cargadas (que se ven). arranca con 4 imagenes (0 al 3)
      public int imageCounter = 3 ;

      // ruta donde voy a almacenar archivos de mi app
      public static final String LOCAL_STORAGE = "/storage/emulated/0/visualhelper";

      // donde se vera la tvFrase de cada imagen
      private TextView tvFrase;

      private String frase = "";

      // TextToSpeech, objeto sintetizador de voz
      TextToSpeech tts;

      // indica cuando finaliza una vuelta de ciclo de pictogramas
      private boolean finCiclo = false;

      // contador de sonrisas para reiniciar la app
      int smileCounter = 0;

      // boolean para saber cuando se termina de reproducir la frase
      private boolean ttsFinished = false;

      private final String[] xmlNames = {"haarcascade_eye_tree_eyeglasses",
              "haarcascade_frontalface_alt",
              "haarcascade_smile",
              "lbpcascade_frontalface",
              "ottaa"};

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
        setContentView(R.layout.activity_main);

        initScrenSize();
        initCamera();
        createGraph();
        initFiles();

        tvFrase = (TextView) findViewById(R.id.frase);
        tvFrase.setTextSize(25);

        // hago la accion para cuando se presione el boton calibrar (ponerlo en un metodo)
        calibrar = (Button) findViewById(R.id.iButtonCalibrar);
        calibrar.requestLayout();

        calibrarCara(calibrar);

         tts = new TextToSpeech(this, this);
    }

    // apuntamos, iniciamos camara y le damos medidas segun tamaño de la pantalla
      private void initCamera() {
          // apuntamos nuestro objeto al que añadimos en activity_main.xml
          javaCameraView = (JavaCameraView) findViewById(R.id.java_camera_view);
          javaCameraView.setVisibility(View.VISIBLE);
          javaCameraView.setCvCameraViewListener(this);
          javaCameraView.requestLayout();

          javaCameraView.getLayoutParams().height = srcHeight/2;
          javaCameraView.getLayoutParams().width = srcWidth/2;
      }

      // creo carpeta y archivos si no existen
      private void initFiles() {
          try {
              createFolder();
          } catch (IOException e) {
              e.printStackTrace();
              Log.d("folder", "No se pudo crear");
          }
      }

      // metodo que crea la carpeta visualhelper en LOCAL_STORAGE, si es que no existe
      // y crea los archivos necesarios dentro si no existia la carpeta.
      private void createFolder() throws IOException {
          File folder = new File(LOCAL_STORAGE);
          //Log.d("success", Environment.getDataDirectory().toString());
          boolean success = true;
          if (!folder.exists()) {
              success = folder.mkdir(); // si ya existe la carpeta, retorna falso.
          }

          if (success) {
              // copia todas las imagenes en la carpeta /virtualhelper
              createAllFiles(xmlNames, LOCAL_STORAGE, ".xml");
          }
      }

      // crea todos las imagenes
      private void createAllFiles(String[] fileNames, String path ,String extension) throws IOException {

          InputStream initialStream = null;
          int i = 0;
          for(String fileName : fileNames){
              int idResource = getResources().getIdentifier(fileName, "raw", getPackageName());
              Log.d("22 id", String.valueOf(idResource));
              initialStream = this.getResources().openRawResource(idResource);
              createSingleFile(initialStream, path, fileName, extension);
              i++;
          }
      }

      // metodo que crea un archivo con la ubicacion, nombre y el contenido que se pasan por parametro
      private void createSingleFile(InputStream initialStream, String pathDestination, String fileName, String extension) throws IOException {
          // copia a buffer lo que tiene el initialStream
          byte[] buffer = new byte[initialStream.available()];
          initialStream.read(buffer);

          // crea un nuevo archivo en la direccion y con lo que contiene initialStream
          File targetFile = new File(pathDestination + "/" + fileName + extension);
          OutputStream outStream = new FileOutputStream(targetFile);
          outStream.write(buffer);
      }

      // parsea xml, carga nodos y grafo
      // luego se cargan las primeras 4 imagenes
      private void createGraph() {

          gr = new Graph();

          // obtengo el grafo de los recursos
          InputStream in = this.getResources().openRawResource(R.raw.ottaa);
          // xmlreader
          try {
              gr.setGraph(xmlReader.parse(in));
          } catch (XmlPullParserException e) {
              Log.d("xmlReader1", e.toString());
              e.printStackTrace();
          } catch (IOException e) {
              e.printStackTrace();
              Log.d("xmlReader", "exception 2");
          }

          // ya tengo el graph en la clase grafo.

          // carga el primer nodo y retorna los hijos (nombre de las imagenes)
          // y estas se cargan en la vista
          // luego, segun el nodo que se este viendo, hago lo mismo.
          List<String> childs = gr.getFirstChilds();

          // ahora inicializo las imagenes segun los hijos del nodo inicial (que no tiene nada)
          initImages(childs);
      }

      // inicializa srcHeight y srcWidth para calcular el tamaño de las imagnes y camara
      public void initScrenSize(){
          // obtengo medidas de la pantalla
          DisplayMetrics displayMetrics = new DisplayMetrics();
          getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
          srcHeight = displayMetrics.heightPixels;
          srcWidth = displayMetrics.widthPixels;

      }

      // apunto imagenes, cargo source inicial y las agrego a la lista
      public void initImages(List<String> images){

          // apunto imagenes y las cargo en List
          List<ImageView> ivList = pointImgToWidgets();

          // lo seteo aca tambien porque cuando reinicio la app queda guardado el ultimo numero
          // y no puedo aceptar las opciones
          imageCounter = imList.size();

          imgsWidth = srcWidth/imageCounter;
          imgsHeigth = imgsWidth;

          setImagesSizes(ivList, imgsWidth, imgsHeigth);
          setImagesNamesAsResource(images);
      }

      // seteo las imagenes con los nombres que vienen en images
      private void setImagesNamesAsResource(List<String> images) {
          int i = 0;
          for(String imgName : images){
              int idResource = getResources().getIdentifier(imgName, "drawable", getPackageName());
              imList.get(i).setImageResource(idResource);
              i++;
          }
      }

      // seteo los tamaños de las imagenes en la Lista, segun los tamaños de la pantalla
      private void setImagesSizes(List<ImageView> l, int w, int h) {

          for(ImageView i : l){
              i.requestLayout();
              i.getLayoutParams().width = w;
              i.getLayoutParams().height = h;
          }
      }

      // apunto mis atibutos a las ImageView
      private List<ImageView> pointImgToWidgets() {
          // apunto las imagenes a los widgets
          im1 = (ImageView) findViewById(R.id.img1);
          im2 = (ImageView) findViewById(R.id.img2);
          im3 = (ImageView) findViewById(R.id.img3);
          im4 = (ImageView) findViewById(R.id.img4);

          // cargo las imagenes en la lista
          imList.add(im1);
          imList.add(im2);
          imList.add(im3);
          imList.add(im4);

          return imList;
      }

      // funcionamiento de buton para calibrar la cara
      public void calibrarCara(final Button buttonCalibrar){
          buttonCalibrar.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                  if(!startCalibration) {
                      startCalibration = true;
                      calibrated = false;
                      buttonCalibrar.setText("fin calibrado");

                  }
                  else {
                      buttonCalibrar.setText("Calibrar");
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
      }

      // muestro AlertDialog con mensaje
      public void dialogoAviso(final String title, final String message){

          runOnUiThread(new Runnable() {
              @Override
              public void run() {
                  // Muestro un cartelito que diga que esta calibrado
                  new AlertDialog.Builder(MainActivity.this)
                          .setTitle(title)
                          .setMessage(message)
                          .setPositiveButton("restart", new DialogInterface.OnClickListener() {
                              public void onClick(DialogInterface dialog, int which) {
                                  // usuario acepto
                                  restartApp();
                              }
                          })

                          .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                              public void onClick(DialogInterface dialog, int id) {
                                  // User cancelled the dialog

                              }
                          })
                          .setIcon(android.R.drawable.ic_dialog_alert)
                          .show();
              }
          });

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

          Log.d("scrWidth", String.valueOf(srcWidth)); // 2560
          Log.d("scrHeight", String.valueOf(srcHeight)); // 1440

          Log.d("filas", String.valueOf(mRgba.rows() * 2)); // 720 // (1.1) 792 // (1.2) 864
          Log.d("columnas", String.valueOf(mRgba.cols() * 2)); // 960 // (1.1) 1056 // (1.2) 1152
          // (1.5) 1080 // .4 1007 //
          // (1.5) 1440 // .4 1344 //

          // agrando la imagen para analizarla (no seria necesario agrandarlo al tamaño de la pantalla
          // podria hacerla un poco mas chica si ncesito mejor performance)
          Mat rz = new Mat();
          resize(mRgba, rz, new Size(mRgba.cols()*1.5, mRgba.rows()*1.5));

          if (!isCalibrated()){
              // hago esto dentro y no fuera porque consumia recursos y crasheaba
              calibrar(rz);
          }

          if(xFaceCenter != -1){
              // ya esta calibrado (xFaceCenter tiene un valor)
              // obtengo el index para saber que imagen seleccionar
              index = OpencvNativeClass.getIndex(rz.getNativeObjAddr(),xFaceCenter,size);
          }

          // si se obtuvo un indice valido que se resalte la opcion
          if(index != -1){

              resaltarOpcion(index);
              //int sd = OpencvNativeClass.smileDetection(rz.getNativeObjAddr());
              int sd = OpencvNativeClass.eyeDetection(rz.getNativeObjAddr());

              //Log.d("smileDet", Integer.toString(sd));
              //Log.d("eyeCounter", String.valueOf(eyeCounter));
              //Log.d("Sonrisa", String.valueOf(sd));

              if( sd != -1){

                  Log.d("EYE", "ojo NO encontrado");


                  eyeCounter++;

                  if(lastIndex == index) indexCounter++;
                  else indexCounter = 0;

                  /*
                  Log.d("index a", String.valueOf(index));
                  Log.d("indexCounter a", String.valueOf(indexCounter));
                  Log.d("ind eyeCounter a", String.valueOf(eyeCounter));
                  Log.d("ind imageCounter a", String.valueOf(imageCounter));
                  */

                  Log.d("ind eyeCounter a", String.valueOf(eyeCounter));

                  // conmigo 4,7 funcionan mas o menos bien
                  if(index >= 0 && index < imageCounter && eyeCounter > 1 && indexCounter > 1) {
                      // cuando esta de frente detecta sonrisas por mas que no sonria con 4
                      Imgproc.cvtColor(mRgba,mRgba,COLOR_RGB2GRAY);
                      /*
                      Log.d("smileDet Gray", Integer.toString(sd));
                      Log.d("index", String.valueOf(index));
                      Log.d("indexCounter", String.valueOf(indexCounter));
                      Log.d("ind eyeCounter", String.valueOf(eyeCounter));
                      Log.d("ind imageCounter", String.valueOf(imageCounter));
                      */

                      // reinicio el contador para q en la proxima vuelta
                      // no siga detectando sonrisas/indexapuntados
                      indexCounter = 0;
                      eyeCounter = 0;

                      String imgName = gr.getCurrentNodo().children.get(index);
                      Nodo n = gr.getNodo(imgName);

                      if(n != null){
                        //  Log.d("NODO", "DISTINTO DE NULL");

                          String text = n.text;
                          if(!(text == null)) {
                              frase += n.text + " ";
                              updateFrase(frase);
                          }
                          gr.setCurrentNodo(n);
                          List<String> ch = gr.getCurrentNodo().children;
                          updateImages(ch);

                          //Log.d("children", n.children.get(0));
                          // llego al final, entonces reproduzco la frase
                          if(n.children.get(0).equals("none")){

                              reproducirFrase(frase, "es");

                              //dialogoAviso("final", "presione restart para iniciar nuevamente");
                          }
                      }
                  }
                  lastIndex = index;
              } else {
                  Log.d("EYE", "ojo encontrado");
                  eyeCounter = 0;
              }
          }

          // si termino de reproducir el texto, que recien ahi pueda reiniciar la app
          if(ttsFinished == true){
              String toastText = "espere unos segundos para reiniciar...";
              sendToastMessage(toastText, Toast.LENGTH_LONG);
              setTimerToRestart(5000, 2000);
              ttsFinished = false;
          }

          // siempre tengo que hacerle relese a los Mat porque sino se me acumulan en la memoria
          rz.release();
          // retorno null si no quiero que se vea la imagen pero que se tome cada frame de la camara
          return mRgba;
      }

      // seteo un contador de tiempo tiempoEspera para que se reinicie la app
      private void setTimerToRestart(final int tiempoEspera, final int tiempoIntervalo) {

          runOnUiThread(new Runnable() {
              @Override
              public void run() {

                  new CountDownTimer(tiempoEspera, tiempoIntervalo) {
                      public void onTick(long millisUntilFinished) {
                          // los LENGTH_SHORT tardan mas de un segundo y cuando muestra los ultimos ya termino
                          //sendToastMessage("restart en "+ millisUntilFinished / 1000,Toast.LENGTH_SHORT);
                      }

                      public void onFinish() {
                          //mTextField.setText("done!");
                          restartApp();
                      }
                  }.start();
              }
          });
      }

      // muestra toast con el string por parametro y con el tiempo por parametro
      private void sendToastMessage(final String toastText, int lengthLong) {
          runOnUiThread(new Runnable() {
              @Override
              public void run() {
                  Toast toast = Toast.makeText(getApplicationContext(), toastText, Toast.LENGTH_LONG);
                  toast.show();
              }
          });
      }

      // reproduce la frase con el codigo de idioma especificado
      private Boolean reproducirFrase(String frase, String codigoIdioma) {

          activarTTSListener();

          Locale loc = new Locale(codigoIdioma, "", "");
          if(tts.isLanguageAvailable(loc) >= TextToSpeech.LANG_AVAILABLE){
              tts.setLanguage(loc);
          }

          String utteranceId= this.hashCode() + "";
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
              tts.speak(frase, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
              Log.d("frase", "if (nueva version)");
          }else{
              Log.d("frase", "else (vieja version)");
              tts.speak(frase, TextToSpeech.QUEUE_FLUSH, null);
          }
          return true;
      }

      private void activarTTSListener(){
          tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
              @Override
              public void onDone(String utteranceId) {
                  Log.d("MainActivity", "TTS finished");
                  ttsFinished = true;
              }

              @Override
              public void onError(String utteranceId) {
              }

              @Override
              public void onStart(String utteranceId) {
                  Log.d("MainActivity", "TTS comienza");
              }
          });
      }

      private void restartApp() {
          runOnUiThread(new Runnable() {
              @Override
              public void run() {
                  frase = "";
                  tvFrase.setText("");
                  imList.clear();
                  imageCounter = 0;
                  createGraph();
              }
          });

      }

      // le añade a la frase en el textView la cadena del nodo actual
      private void updateFrase(final String s) {
          //if(!s.isEmpty() && s.equals(null) && s.equals("") && s == "null" && s.length() == 0)
          //    return;

          runOnUiThread(new Runnable() {
              @Override
              public void run() {
                  tvFrase.setText(s);
              }
          });
      }

      // actualizo las imagenes que se muestran
      private void updateImages(final List<String> nextImages) {

          runOnUiThread(new Runnable() {
              @Override
              public void run() {

                  // limpio todas las imagenes cargadas
                  for(ImageView im : imList) {
                      im.setImageDrawable(null);
                  }

                  // seteo las imagnes que vienen en la list nextImages
                  imageCounter = 0;
                  for(String imgName : nextImages){
                      int idResource = getResources().getIdentifier(imgName, "drawable", getPackageName());
                      imList.get(imageCounter).setImageResource(idResource);
                      imageCounter++;
                  }

              }
          });
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

                  if (startCalibration){
                      calibrar.setText("calibrar");
                      startCalibration = false;
                  }

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

      @Override
      public void onInit(int status) {


      }
  }
