package Dataverse.FindingBoundingBoxes;

import BaseFiles.GeoLogger;
import Dataverse.DataverseJavaObject;
import Dataverse.FindingBoundingBoxes.LocationTypes.BoundingBox;

import java.io.*;


public abstract class FindBoundBox {
    abstract BoundingBox getDVBoundingBox(String country);
    abstract BoundingBox getDVBoundingBox(String country, String province) throws IOException;
    abstract BoundingBox getDVBoundingBox(String country, String province, String city);

    GeoLogger logger = new GeoLogger(FindBoundBox.class);

    protected BoundingBox readResponse(String responseString, String doi, DataverseJavaObject djo){
        return parseXMLString(responseString, doi, djo);
    }

    protected String readCoverageCountry(String responseString, String doi, DataverseJavaObject djo){
        int tooFar = responseString.indexOf("</geoname>");
        int start = responseString.indexOf("<countryName>");
        if(start==-1||start>tooFar) {
            logger.info("Record with PERSISTENT_ID of "+ doi + "could not have information found  in geonames. Please doublecheck the geospatial coverage field values", djo);
            return "";
        }
        int end = responseString.indexOf("</countryName>");
        return responseString.substring(start+14,end);
    }

    protected  BoundingBox parseXMLString(String responseString, String doi, DataverseJavaObject djo){
        BoundingBox box = new BoundingBox();
        int tooFar = responseString.indexOf("</geoname>");
        int start = responseString.indexOf("<west>");
        if(start==-1||start>tooFar) {
            logger.info("Record with PERSISTENT_ID of "+ doi + "could not have a bounding box found in geonames. Please doublecheck the geospatial coverage field values", djo);
            return box;
        }
        int end = responseString.indexOf("</west>");
        box.setLongWest(responseString.substring(start+6, end));
        start = responseString.indexOf("<east>");
        end = responseString.indexOf("</east>");
        box.setLongEast(responseString.substring(start+6, end));
        start = responseString.indexOf("<north>");
        end = responseString.indexOf("</north>");
        box.setLatNorth(responseString.substring(start+7, end));
        start = responseString.indexOf("<south>");
        end = responseString.indexOf("</south>");
        box.setLatSouth(responseString.substring(start+7, end));
        box.setGenerated(true);

        return box;
    }

    protected String addDelimiter(String country, String secondParam) {
        return country + "zzz"+ secondParam;
    }

    protected String addDelimiter(String country, String secondParam, String thirdParam) {
        return country + "zzz"+ secondParam + "zzz" + thirdParam;
    }

}