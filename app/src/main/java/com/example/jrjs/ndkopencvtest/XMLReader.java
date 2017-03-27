package com.example.jrjs.ndkopencvtest;

import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by jrjs on 21/03/17.
 */

public class XMLReader {

    // We don't use namespaces
    private static final String ns = null;

    //
    public List grafo;

    // en InputStream le paso la direccion de ottaa.xml
    public List parse(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            Log.d("parse", "1");
            return readNodes(parser);
        } finally {
            in.close();
        }
    }

    private List readNodes(XmlPullParser parser) throws XmlPullParserException, IOException {
        grafo = new ArrayList();
        Log.d("readNodes", "1");
        parser.require(XmlPullParser.START_TAG, ns, "nodes");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            Log.d("name readNodes", name);
            // Starts by looking for the entry tag
            if (name.equals("node")) {
                grafo.add(readNode(parser));
                Log.d("list", Integer.toString(grafo.size()));
            } else {

            }
        }
        return grafo;
    }


    // Parses the contents of an entry. If it encounters a id, icon, or title tag, hands them off
// to their respective "read" methods for processing. Otherwise, skips the tag.
    private Nodo readNode(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "node");
        String id = null;
        String title= null;
        String icon = null;
        List <String> children = null;
        String text = null;


        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            Log.d("namereadNode", name);
            if (name.equals("id")) {
                id = readId(parser);
            } else if (name.equals("title")) {
                title = readTitle(parser);
            } else if (name.equals("icon")) {
                icon = readIcon(parser);
            } else if (name.equals("children")) {
                children = readChildren(parser);
            } else if (name.equals("text")){
                    text = readTextTag(parser);
            }else{
                Log.d("readNode", "no encontro alguno de los tags");
                // encuentra <text> y viene aca y no retorna nada por eso lee hasta aca,
                // pero no entiendo porque encuentra <text> recien en el 4to node
                // y antes no los encuentra
            }
        }
        return new Nodo(id, title, icon, children, text);
    }

    private String readTextTag(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "text");
        String text = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "text");
        return text;
    }


    // Processes title tags in the feed.
    private String readId(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "id");
        String id = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "id");
        return id;
    }

    // Processes id tags in the feed.
    private String readTitle(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "title");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "title");
        return title;
    }

    // Processes icon tags in the feed.
    private String readIcon(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "icon");
        String icon = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "icon");
        return icon;
    }

    private List <String> readChildren(XmlPullParser parser) throws IOException, XmlPullParserException{
        parser.require(XmlPullParser.START_TAG, ns, "children");
        List <String> children = readTextChildren(parser);
        parser.require(XmlPullParser.END_TAG, ns, "children");
        return children;
    }

    private List<String> readTextChildren(XmlPullParser parser) throws IOException, XmlPullParserException {
        List <String> listText = new ArrayList<String>();
        String result = readText(parser);

        String[] arr = result.split(",");
        for(int i = 0; i < arr.length; i++){
            listText.add(arr[i]);
        }

        return listText;
    }

    // extra los valores (texto) de los metodos read
    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

}
