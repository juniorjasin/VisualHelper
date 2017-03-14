//
// Created by jrjs on 14/03/17.
//

#ifndef VISUALHELPER_MATPROCESSOR_H
#define VISUALHELPER_MATPROCESSOR_H

#include <opencv2/opencv.hpp>
using namespace cv;

class MatProcessor {


    int xFaceCenter;

    // guardo caras y sonrisas que se encuentrem
    std::vector <Rect> faces;
    std::vector <Rect> smiles;

    // guardo caras y sonrisas actuales
    Rect currentFace;
    Rect currentSmile;

public:

    MatProcessor():xFaceCenter(-1){

    }

    int getXFaceCenter(){ return xFaceCenter;}

    // retorna true si detecta una cara
    bool detectFace(Mat &frame) {

        // los LBP funcionan mejor que los haarcascade
        // direccion para crear el Classifier y levantar los xml
        String facePath = "/storage/emulated/0/data/lbpcascade_frontalface.xml";
        CascadeClassifier face_cascade;

        // levanto y valido que sea correctos
        if (!face_cascade.load(facePath)) {
            printf("--(!)Error loading\n");
            return false;
        };

        // no se porque lo convierte a gris y luego lo detecta
        Mat frame_gray;
        cvtColor(frame, frame_gray, CV_BGR2GRAY); // porque lo cambia a gris ??
        equalizeHist(frame_gray, frame_gray);

        //-- Detect faces // con estos valores se ve mas fluido
        face_cascade.detectMultiScale(frame_gray, faces, 1.1, 2, 0 | CV_HAAR_SCALE_IMAGE, Size(300, 300));

        // verifico que haya una cara
        if (faces.size() > 0) {
            currentFace = faces.at(0);
            xFaceCenter = faces.at(0).x + faces.at(0).width / 2;
            return true;
        }
        return false;
    }

    bool detectSmile(Mat &frame) {

        if (detectFace(frame)){

            // los LBP funcionan mejor que los haarcascade
            // direccion para crear el Classifier y levantar los xml
            String smilePath = "/storage/emulated/0/data/haarcascade_smile.xml";
            CascadeClassifier smile_cascade;

            // levanto y valido que sea correctos
            if( !smile_cascade.load( smilePath ) ){ printf("--(!)Error loading\n"); return false; };

            // no se porque lo convierte a gris y luego lo detecta
            Mat frame_gray;
            cvtColor( frame, frame_gray, CV_BGR2GRAY ); // porque lo cambia a gris ??
            equalizeHist( frame_gray, frame_gray );

            // creo Mat con cara y detecto sonrisa
            Mat face(frame_gray, Rect(faces[0].height, faces[0].width, faces[0].x,faces[0].y));
            smile_cascade.detectMultiScale(face, smiles, 1.1, 2, 0 | CASCADE_SCALE_IMAGE, Size(300, 300));

            // verifico que haya sonrisas
            if(smiles.size() > 0){
                // si encontre al menos una sonrisa, la almaceno y retorno true
                currentSmile = smiles.at(0);
                return true;
            } else{
                // si no hay sonrisas retorna false.
                return false;
            }
        } else{
            // si no hay cara no se va a encontrar sonrisa
            return false;
        }
    }

};


#endif //VISUALHELPER_MATPROCESSOR_H
