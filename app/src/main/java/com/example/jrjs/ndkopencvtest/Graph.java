package com.example.jrjs.ndkopencvtest;

import android.util.Log;

import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jrjs on 21/03/17.
 */

public class Graph {

    // grafo igual al que se crea en el xmlreader
    private List graph = new ArrayList();

    private Nodo currentNode;

    public List getFirstChilds(){
        // obtengo el primer hijo
        Nodo firstNode = (Nodo) this.graph.get(0);
        List<String> ch = firstNode.getChildren();
        currentNode = firstNode;
        return ch;
    }

    public void restarGraph(){
        //this.currentNode = (Nodo) this.graph.get(0);
    }

    // recorre la lista graph y busca si algun noodo tiene un id igual al que le pasan por parametro
    public Nodo getNodo(String id){
        Nodo n;
        for(int i = 0; i < graph.size(); i++){
            n = (Nodo) graph.get(i);
            //Log.d("n", n.id);
            //Log.d("param", id);
            if(n.id.equals(id)){
                //Log.d("igualdad", id);
                return n;
            }
        }

        return null;
    }

    // metodos accesores
    public void setCurrentNodo(Nodo cn){this.currentNode = cn; }
    public Nodo getCurrentNodo(){return this.currentNode; }
    public void setGraph(List graph){
        this.graph = graph;
    }
    public List getGraph(){
        return this.graph;
    }
}
