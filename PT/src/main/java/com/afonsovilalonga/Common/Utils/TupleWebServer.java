package com.afonsovilalonga.Common.Utils;

import java.io.PipedOutputStream;

public class TupleWebServer {
    private PipedOutputStream pipe;
    private String window_id;

    public TupleWebServer(PipedOutputStream pipe, String window_id){
        this.pipe = pipe;
        this.window_id = window_id;
    }

    public PipedOutputStream getPipe(){
        return this.pipe;
    }

    public String getWindownId(){
        return this.window_id;
    }
}
