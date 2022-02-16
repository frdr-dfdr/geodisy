package BaseFiles;/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */



import Dataverse.SourceJavaObject;
import GeoServer.GeoServerAPI;
import java.util.List;



/**
 * This is the main activity class of the tests middleware.
 * From here the calls for harvesting of Dataverse and exporting to Geoserver happen.
 * @author pdante
 */
public class Geodisy {
    /**
     * Harvesting metadata from FRDR Harvester
     * @return
     */
    public List<SourceJavaObject> harvestFRDRMetadata(){
        FRDRGeodisy frdrGeodisy = new FRDRGeodisy();
        return frdrGeodisy.harvestFRDRMetadata();
    }

    /**
     * Backside of middleware, this is the part that sends the processed data/metadata to Geoserver
     */
    public void exportToGeoserver(SourceJavaObject sjo){
        GeoServerAPI geoServerAPI = new GeoServerAPI(sjo);

    }
}
