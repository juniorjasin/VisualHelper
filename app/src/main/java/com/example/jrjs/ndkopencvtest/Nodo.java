package com.example.jrjs.ndkopencvtest;

import java.util.List;

/**
 * Created by jrjs on 21/03/17.
 */

public class Nodo {

    public final String id;
    public final String title;
    public final String icon;
    public final List<String> children;
    public final String text;

    public List<String> getChildren(){
        return this.children;
    }

    Nodo(String id, String icon, String title, List<String> children, String text) {
        this.id = id;
        this.icon = icon;
        this.title = title;
        this.children = children;
        this.text = text;
    }

    public String NodoToString(){

        String result = "";
        /*
        result.concat("[id:" + id);
        result.concat(" - title:" + title);
        result.concat(" - icon:" + icon);
        result.concat(" - children:" + children);
        result.concat(" - text:" + text + "]");
        */
        return result;
    }

}
