package Dataverse.FindingBoundingBoxes;

import BaseFiles.GeoLogger;
import Dataverse.FindingBoundingBoxes.LocationTypes.BoundingBox;
import org.json.JSONObject;
import org.json.XML;

public class GeonamesJSON {
    JSONObject jo;
    GeoLogger logger = new GeoLogger(this.getClass());

    public GeonamesJSON(JSONObject jo) {
        this.jo = jo;
    }

    public GeonamesJSON(String geonameString){
        if(geonameString.isEmpty())
           jo = new JSONObject();
        else{
             if (geonameString.contains("<totalResultsCount>")) {
                 if (geonameString.contains("<totalResultsCount>0</totalResultsCount>"))
                    jo = new JSONObject();
                 else {
                    jo = (JSONObject) XML.toJSONObject(geonameString).get("geonames");
                    jo = (JSONObject) jo.get("geoname");
                }
             } else {
                jo = XML.toJSONObject(geonameString);
                if (jo.has("country"))
                    jo = jo.getJSONObject("country");
                else if (jo.has("givenCountry"))
                    jo = jo.getJSONObject("givenCountry");
                else
                    logger.error("Something was wrong with the country XML" + geonameString);
             }
        }
    }
    public GeonamesJSON(){
        jo = new JSONObject();
    }

    public void setJSONObject(String json){
        jo = XML.toJSONObject(json);
    }
    public JSONObject getRecordByName(String name){
            if(jo.get("countryName").toString().equalsIgnoreCase(name) || jo.get("altName").toString().toLowerCase().contains(name.toLowerCase()))
                return jo;
            return null;
    }

    public boolean hasGeoRecord(){
        return !jo.isEmpty();
    }

    public BoundingBox getBBFromGeonamesBBElementString(){
        BoundingBox bb =  new BoundingBox();
        if(jo.has("givenCountry"))
            jo = jo.getJSONObject("givenCountry");
        if(!jo.has("south"))
            return bb;
        bb.setLongWest(getDoubleLatLongVal(jo,"west"));
        bb.setLatNorth(getDoubleLatLongVal(jo,"north"));
        bb.setLongEast(getDoubleLatLongVal(jo,"east"));
        bb.setLatSouth(getDoubleLatLongVal(jo,"south"));
        return bb;
    }

    public BoundingBox getBBFromGeonamesJSON(){
        BoundingBox bb =  new BoundingBox();
        if(!jo.has("bbox"))
            return bb;
        JSONObject bbox = (JSONObject) jo.get("bbox");
        bb.setLongWest(getDoubleLatLongVal(bbox,"west"));
        bb.setLatNorth(getDoubleLatLongVal(bbox,"north"));
        bb.setLongEast(getDoubleLatLongVal(bbox,"east"));
        bb.setLatSouth(getDoubleLatLongVal(bbox,"south"));
        if(!bb.hasBoundingBox())
            return new BoundingBox();
        return bb;
    }

    public String getAltNames(){
        return getVal("alternateNames");
    }

    public String getCommonCountryName(){
        return getVal("countryName");
    }
    //TODO check the label is correct
    public String getCommonCityName(){
        return getVal("name");
    }
    //TODO check the label is correct
    public String getCommonStateName(){
        return getVal("name");
    }

    public String getCountryCode(){
        return getVal("countryCode");
    }

    private String getVal(String label) {
        return jo.has(label)? jo.getString(label):"";
    }

    private double getDoubleLatLongVal(JSONObject ob, String label){
        return ob.has(label)? ob.getDouble(label):361;
    }
}
