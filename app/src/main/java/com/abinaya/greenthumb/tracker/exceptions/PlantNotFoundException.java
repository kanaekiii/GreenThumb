package com.abinaya.greenthumb.tracker.exceptions;


public class PlantNotFoundException extends RuntimeException {
    public PlantNotFoundException(String message)   {
        super(message);
    }
}
