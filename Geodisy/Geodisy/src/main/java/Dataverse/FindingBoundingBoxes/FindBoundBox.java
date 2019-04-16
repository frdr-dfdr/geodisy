package Dataverse.FindingBoundingBoxes;

import Dataverse.FindingBoundingBoxes.LocationTypes.BoundingBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;


public abstract class FindBoundBox {
    abstract BoundingBox getDVBoundingBox(String country);
    abstract BoundingBox getDVBoundingBox(String country, String state) throws IOException;
    abstract BoundingBox getDVBoundingBox(String country, String state, String city);
    abstract BoundingBox getDVBoundingBoxOther(String country, String other);
    abstract BoundingBox getDVBoundingBoxOther(String country,String state, String other);
    abstract String getJSONString(String search);
    Logger logger = LogManager.getLogger(FindBoundBox.class.getName());

    protected BoundingBox readResponse(String responseString, String doi){
        return parseXMLString(responseString, doi);
    }

    protected  BoundingBox parseXMLString(String responseString, String doi){
        BoundingBox box = new BoundingBox();
        int tooFar = responseString.indexOf("</geoname>");
        int start = responseString.indexOf("<west>");
        if(start==-1||start>tooFar) {
            logger.info("Record with DOI of "+ doi + "could not have a bounding box found in geonames. Please doublecheck the geospatial coverage field values");
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

        return box;
    }

    protected String addDelimiter(String country, String secondParam) {
        return country + "zzz"+ secondParam;
    }

    protected String addDelimiter(String country, String secondParam, String thirdParam) {
        return country + "zzz"+ secondParam + "zzz" + thirdParam;
    }

}