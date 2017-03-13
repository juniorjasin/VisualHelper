package com.example.jrjs.ndkopencvtest;

/**
 * Created by jrjs on 07/03/17.
 */

// recordatorio: todavia no descubri como hacer que retorne Object,
// no se que hay que retornar del lado de C++

public class OpencvNativeClass {
    public native static int convertGray(long matAddrRgba, long matAddrGray);
    public native static int faceDetection(long addrRgba, long size, Object extra);
}
