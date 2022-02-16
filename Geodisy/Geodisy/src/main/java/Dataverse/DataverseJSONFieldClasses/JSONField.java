package Dataverse.DataverseJSONFieldClasses;

import BaseFiles.GeoLogger;
import Crosswalking.JSONParsing.DataverseParser;

import java.net.URL;


public abstract class JSONField {
    protected GeoLogger logger = new GeoLogger(DataverseParser.class.toString());

    /**
     * @param value String that hopefully is a properly formated URL
     * @return Either a validly formated URL or an empty String
     */
    protected String filterURL(String value) {
        try {
            new URL(value).toURI();
            return value;
        }

        // If there was an Exception
        // while creating URL object
        catch (Exception e) {
            return "";
        }
    }








    public abstract String getField(String fieldName);

    public void errorParsing(String className, String fieldName){
        logger.error("Something wrong parsing "+ className + ". Title is " + fieldName);
    }

    public void errorGettingValue(String className, String fieldName){
        logger.error("Something wrong getting field from "+ className + ". Field name is " + fieldName);
    }

    protected String stringed(float val) {
        return String.valueOf(val);
    }
    protected String stringed(int val){
        return String.valueOf(val);
    }

}
