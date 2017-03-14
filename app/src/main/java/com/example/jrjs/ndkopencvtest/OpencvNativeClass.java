package com.example.jrjs.ndkopencvtest;

/**
 * Created by jrjs on 07/03/17.
 */

// recordatorio: todavia no descubri como hacer que retorne Object,
// no se que hay que retornar del lado de C++

public class OpencvNativeClass {

    public native static int smileDetection(long addrRgba);
    public native static int getIndex(long addrRgba, int xFaceCenter ,int size);
}
