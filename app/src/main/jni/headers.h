//
// Created by jrjs on 13/03/17.
//

#ifndef VISUALHELPER_HEADERS_H
#define VISUALHELPER_HEADERS_H

#include <opencv2/opencv.hpp>
#include "MatProcessor.h"


using namespace std;
using namespace cv;

// cabeceras de mis metodos
void Calibration(Rect currentFace);
int toGray(Mat img, Mat &gray);
int detect(Mat &frame, int size);
int getIndex(int size, Rect calibration, int xFaceCenter);

// variables globales
bool calibrated = false;

// lo que devuelve el metodo faceDetection
int xCentroCara = -1;


#endif //VISUALHELPER_HEADERS_H
