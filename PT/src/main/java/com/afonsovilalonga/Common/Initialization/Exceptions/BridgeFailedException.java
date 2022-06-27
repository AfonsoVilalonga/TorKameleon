package com.afonsovilalonga.Common.Initialization.Exceptions;

public class BridgeFailedException extends Exception{
    
    public BridgeFailedException(){
        super();
    }

    public BridgeFailedException(String message){
        super(message);
    }
}
