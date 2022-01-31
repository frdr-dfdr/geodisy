package Crosswalking.GeoBlacklightJson;

import BaseFiles.GeoLogger;
import Dataverse.DataverseJSONFieldClasses.Fields.CitationSimpleJSONFields.Date;
import _Strings.GeodisyStrings;
import _Strings.DVFieldNameStrings;
import Dataverse.DataverseGeoRecordFile;
import Dataverse.DataverseJSONFieldClasses.Fields.CitationCompoundFields.Author;
import Dataverse.DataverseJSONFieldClasses.Fields.CitationCompoundFields.DateOfCollection;
import Dataverse.DataverseJSONFieldClasses.Fields.CitationCompoundFields.Description;
import Dataverse.DataverseJSONFieldClasses.Fields.DataverseJSONGeoFieldClasses.GeographicBoundingBox;
import Dataverse.DataverseJSONFieldClasses.Fields.DataverseJSONGeoFieldClasses.GeographicCoverage;
import Dataverse.DataverseJavaObject;
import Dataverse.DataverseRecordFile;
import Dataverse.FindingBoundingBoxes.LocationTypes.BoundingBox;
import netscape.javascript.JSObject;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;

import static _Strings.GeodisyStrings.*;
import static _Strings.GeoBlacklightStrings.*;
import static _Strings.GeoBlacklightStrings.FULL_LAYER_DESCRIPTION;
import static _Strings.XMLStrings.TEST_OPEN_METADATA_LOCAL_REPO;
import static _Strings.DVFieldNameStrings.*;

public class DataGBJSON extends GeoBlacklightJSON{
    GeoLogger logger;

    public DataGBJSON(DataverseJavaObject djo) {
        super();
        javaObject = djo;
        geoBlacklightJson = "";
        doi = djo.getPID();
        logger = new GeoLogger(this.getClass());
        geoFiles = djo.getGeoDataFiles();
        geoMeta = djo.getGeoDataMeta();
        recordLabel = djo.getSimpleFieldVal(RECORD_LABEL);
    }
    @Override
    protected JSONObject getRequiredFields(BoundingBox bb){

        jo.put("geoblacklight_version","1.0");
        jo.put("dc_identifier_s", GeodisyStrings.urlSlashes(javaObject.getSimpleFieldVal(RECORD_LABEL)));
        jo.put("layer_slug_s", doi);


        String name = javaObject.getSimpleFields().getField(TITLE);
        if(name.isEmpty()) {
            logger.error("Somehow creating a GBL json without a study title: " + doi);
            name = "Unknown Study Name";
        }
        jo.put("dc_title_s", name);
        String license = javaObject.getSimpleFields().getField(LICENSE);
        if(license.toLowerCase().equals("public")||license.isEmpty())
            jo.put("dc_rights_s","Public");
        else
            jo.put("dc_rights_s","Restricted");
        jo.put("dct_provenance_s",javaObject.getSimpleFields().getField(PUBLISHER));
        jo.put("dc_publisher_s",javaObject.getSimpleFields().getField(PUBLISHER));
        jo.put("solr_geom",determineGeomentry(bb));
        return jo;
    }

    private String determineGeomentry(BoundingBox bb) {
        if(bb.getLongWest()<bb.getLongEast())
            return "ENVELOPE(" + getBBString(bb) + ")";
        else
            return "MULTIPOLYGON(((" + addPolygon(bb.getLongWest(),180d,bb.getLatNorth(),bb.getLatSouth()) + ")), ((" + addPolygon(-180d, bb.getLongEast(), bb.getLatNorth(), bb.getLatSouth()) + ")))";
    }

    private String addPolygon(double west, double east, double north, double south) {
        return west + " " + south + ", " + west + " " + north + ", " + east + " " + north + ", " + east + " " + south + ", " + west + " " + south;
    }

    @Override
    protected JSONObject updateRequiredFields() {
        jo.put("dc_identifier_s", GeodisyStrings.urlSlashes(javaObject.getSimpleFieldVal(DVFieldNameStrings.RECORD_URL)));
        String origTitle = jo.getString("dc_title_s");
        String name = javaObject.getSimpleFields().getField(TITLE);
        if(origTitle.contains("(") &&  origTitle.contains(" of ") && (origTitle.lastIndexOf("(")>origTitle.lastIndexOf(" of "))){
            jo.put("dc_title_s", name + origTitle.substring(origTitle.lastIndexOf(" (")));
        } else
            jo.put("dc_title_s", name);
        String license = javaObject.getSimpleFields().getField(LICENSE);
        if(license.toLowerCase().equals("public")||license.isEmpty())
            jo.put("dc_rights_s","Public");
        else
            jo.put("dc_rights_s","Restricted");
        jo.put("dct_provenance_s",javaObject.getSimpleFields().getField(PUBLISHER));
        jo.put("dc_publisher_s",javaObject.getSimpleFields().getField(PUBLISHER));
        return jo;
    }
    private String padZeros(int number, int total){
        return padZeros(Integer.toString(number), total);
    }
    private String padZeros(String number, int total) {
        if(total>9) {
            if(number.length()<2)
                number = "0"+number;
        }
        if(total>99)
        {
            if(number.length()<3)
                number = "0"+number;
        }
        if(total>999)
        {
            if(number.length()<4)
                number = "0"+number;
        }
        if(total>9999)
        {
            if(number.length()<5)
                number = "0"+number;
        }
        return number;
    }

    private void addRecommendedFields(List<DataverseRecordFile> drfs) {
        JSONObject jfile = new JSONObject();
        JSONObject jfileID = new JSONObject();
        JSONArray jgeom = new JSONArray();
        for(DataverseRecordFile drf: drfs) {
            GeographicBoundingBox gbb = drf.getGBB();
            getFileType(drf);
            if (gbb.hasBB()) {
                jgeom.put(gbb.getField(GEOMETRY));
                jfileID.put("geo", gbb.getField(GEOMETRY));
                if (gbb.getField(GEOMETRY).equals(UNDETERMINED)) {
                    jfileID.put("title", "Metadata #" + drf.getGBBFileNumber());
                } else
                    jfileID.put("title", drf.getFileName());
                jfileID.put(("bbox"), determineGeomentry(gbb.getBB()));
                jfileID.put("height", gbb.getHeight());
                jfileID.put("width", gbb.getWidth());
                jfileID.put("projection", gbb.getProjection());
                jfileID.put("fileURL", gbb.getField(FILE_URL));
                addDataDownloadOptions(gbb,jfileID,gbb.isGeneratedFromGeoFile());
                jfile.put(drf.getGeoserverLabel(), jfileID);
            }
        }
        //layer_geom_type_sm is a customized layer_geom_s in GBL as we are now allowing more than one bbox per record
        jo.put("layer_geom_type_sm", jgeom);

        //layers_file_info_sm is a new custom nested field that holds the bbox, title, and geoserver label for each
        jo.put("layers_file_info_sm", jfile);
        getDSDescriptionSingle();

        JSONObject j = addBaseRecordInfo();

        jo.put(EXTERNAL_SERVICES, j.toString());
        jo.put("layer_id_s", doi);
    }

    private void updateRecommendedFields(){
        getDSDescriptionSingle();
    }

    private String getGeoserverLabel(GeographicBoundingBox gbb) {
        return gbb.getField(GEOSERVER_LABEL);
    }


    private String getBBString(BoundingBox bb){
        double west = bb.getLongWest();
        double east = bb.getLongEast();
        double north = bb.getLatNorth();
        double south = bb.getLatSouth();
        if(north==south && east!=west) {
            logger.warn("Found a line bounding box for record at: " + doi);
            if (north > -90.0)
                south = south - 0.1;
            else
                north = north + 0.1;
        }
        if(north!=south && east==west) {
            logger.warn("Found a line bounding box for record at: " + doi);
            if (west > -180.0)
                west = west - 0.1;
            else
                east = east + 0.1;
        }
        return west + ", " + east + ", " + north + ", " + south;
    }

    @Override
    protected JSONObject addDataDownloadOptions(GeographicBoundingBox gbb, JSONObject jo, boolean isOnGeoserver) {
        if(isOnGeoserver) {
            jo.put(WMS, GEOSERVER_WMS_LOCATION);
            if (gbb.getField(FILE_NAME).endsWith(".shp"))
                jo.put(WFS, GEOSERVER_WFS_LOCATION);
        }
        if(!gbb.getField(FILE_URL).isEmpty())
            jo.put(DIRECT_FILE_DOWNLOAD, gbb.getField(FILE_URL));
        return jo;
    }

    @Override
    protected JSONObject addBaseRecordInfo(){
        JSONObject jo = new JSONObject();
        jo.put(FULL_LAYER_DESCRIPTION,  GeodisyStrings.urlSlashes(javaObject.getSimpleFieldVal(DVFieldNameStrings.RECORD_URL)));
        jo.put(ISO_METADATA, END_XML_JSON_FILE_PATH + GeodisyStrings.urlSlashes((recordLabel) + "/" + ISO_METADATA_FILE_ZIP));
        return jo;
    }

    //TODO, check I am getting all the optional fields I should be
    @Override
    protected JSONObject getOptionalFields(List<DataverseRecordFile> drfs) {
        addRecommendedFields(drfs);
        getBoundingBoxes(drfs);
        getAuthors();
        getIssueDate();
        getLanguages();
        getPlaceNames();
        getSubjects();
        getType();
        getModifiedDate();
        getSolrYear();
        getTemporalRange();

        return jo;
    }

    private void getBoundingBoxes(List<DataverseRecordFile> drfs) {
        JSONArray bbs = new JSONArray();

        //Get BBoxes that were either entered or are from files
        for(DataverseRecordFile drf: drfs) {
            JSONObject o = new JSONObject();
            GeographicBoundingBox gbb = drf.getGBB();
            o.put("west",gbb.getWestLongitude());
            o.put("east", gbb.getEastLongitude());
            o.put("north", gbb.getNorthLatitude());
            o.put("south",gbb.getSouthLatitude());
            if(!gbb.getFileName().equals("")) {
                o.put("name", gbb.getFileName());
                o.put("geometry_type", gbb.getField(GEOMETRY));
                o.put("bbox_type","file");
            }else if(gbb.getLocation().isEmpty()) {
                o.put("bbox_type", "bounding box");
                o.put("name","NSEW: " + gbb.getNorthLatitude() + ", " + gbb.getSouthLatitude() + ", " + gbb.getEastLongitude() + ", " + gbb.getWestLongitude());
            }
            else{
                o.put("bbox_type","bounding box");
                o.put("name", gbb.getLocation());
            }
            bbs.put(o);

        }
    }

    @Override
    protected JSONObject updateOptionalFields(){
        updateRecommendedFields();
        getAuthors();
        getIssueDate();
        getLanguages();
        getSubjects();
        getType();
        getModifiedDate();
        getSolrYear();
        getTemporalRange();

        return jo;
    }

    private void getTemporalRange() {
        List<DateOfCollection> dates = javaObject.getCitationFields().getListField(DATE_OF_COLLECT);
        String dateRange = "";
        int count = 1;
        for(DateOfCollection d: dates){
            int start = d.getStartYear();
            int end = d.getEndYear();
            if(start==-111111){
                if (end != -111111) {
                    if (count == 1) {
                        dateRange += end;
                    } else {
                        dateRange += ", " + end;
                    }
                    count++;
                }
            }else{
                if(end == -111111){
                    if(count == 1) {
                        dateRange += start;
                        count++;
                    } else {
                        dateRange += ", " + start;
                    }
                } else{
                    String range = "";
                    if(start<end){
                        range = start + "-" + end;
                    }else if (start==end) {
                        range = start + "";
                    }else {
                        range = end + "_" + start;
                    }
                    if(count>1) {
                        dateRange += ", " + range;

                    }
                    else {
                        dateRange += range;
                    }
                    count++;
                }
            }

        }
        if(!dateRange.equals(""))
            jo.put("dct_temporal_sm",dateRange);

    }

    private void getSolrYear() {
        List<DateOfCollection> dates = javaObject.getCitationFields().getListField(DATE_OF_COLLECT);
        int date = 10000;
        for(DateOfCollection doc: dates){
            String dateString = doc.getStartDate();
            int current;
            if(dateString.contains("-"))
                current = Integer.valueOf(dateString.substring(0,dateString.indexOf("-")));
            else
                current = Integer.valueOf(dateString);

            if(date>current)
                date = current;

        }
        if(date!=10000)
            jo.put("solr_year_i",String.valueOf(date));

    }

    private void getModifiedDate() {
        String modDate = javaObject.getSimpleFieldVal(LAST_MOD_DATE);
        if(!modDate.isEmpty() && !modDate.equals("9999"))
            jo.put("layer_modified_dt",modDate);
    }

    private void getRelatedRecords(DataverseRecordFile drf, int total) {
        LinkedList<DataverseGeoRecordFile> geo = javaObject.getGeoDataFiles();
        LinkedList<DataverseGeoRecordFile> meta = javaObject.getGeoDataMeta();
        LinkedList<DataverseGeoRecordFile> recs = (geo.size()>=meta.size())? geo:meta;
        boolean geoRecs = geo.size()==recs.size();
        int count = drf.getBbCount();
        if(recs.size()>1){
            JSONArray ja = new JSONArray();
            for(DataverseGeoRecordFile dgrf : recs){
                if(geoRecs) {
                    if (!getGeoserverLabel(drf.getGBB()).equals(getGeoserverLabel(dgrf.getGBB()))) {
                        ja.put(getGeoserverLabel(dgrf.getGBB()).toLowerCase());
                    }
                } else{
                    int curBB =  dgrf.getBbCount();
                    if(count!=curBB)
                        ja.put(getGeoserverLabel(dgrf.getGBB()).toLowerCase() + padZeros(curBB,total));
                }
            }
            jo.put("dc_source_sm",ja);
            jo.put("dct_isPartOf_sm",javaObject.getSimpleFieldVal(TITLE));
        }
    }

    private void getFileType(DataverseRecordFile drf) {
        if(!drf.getGBB().getField(FILE_URL).isEmpty()) {
            String format = getFileTypeName(drf.getTranslatedTitle());
            if(format.isEmpty())
                format = "File";
            jo.put("dc_format_sm", format);
        }
    }

    private String getFileTypeName(String translatedTitle) {
        try {
            String extension = translatedTitle.substring(translatedTitle.lastIndexOf(".") + 1).toLowerCase();
            switch (extension){
                case ("zip"):
                    return "Zip File";
                case ("shp"):
                    return "Shapefile";
                case ("geojson"):
                    return "GeoJSON";
                case ("tif"):
                case ("geotif"):
                case ("tiff"):
                case ("geotiff"):
                    return "GeoTIFF";
                case ("png"):
                    return "PNG";
                default:
                    return "Geospatial File";



            }
        }catch (IndexOutOfBoundsException e){
            logger.warn("There was no extension on the original file name for record " + doi);
            return "";
        }
    }

    private void getType() {
        jo.put("dc_type_s","Dataset");
    }

    private void getSubjects() {
        JSONArray ja = new JSONArray();
        List<String> subjects = javaObject.getCitationFields().getListField(SUBJECT);
        for(String s : subjects){
            ja.put(s);
        }
        jo.put("dc_subject_sm",ja);
    }

    /*This version is only be activated when the GeoBlacklight schema changes to allow multiple values
    private void getLanguages() {
        JSONArray ja = new JSONArray();
        List<String> languages = javaObject.getCitationFields().getListField(LANGUAGE);
        for(String s : languages){
            ja.put(s);
        }
        jo.put("dc_language_s",ja);
    }
     */
    // This version is only to be used while the GeoBlacklight schema doesn't allow multiple value, when that happens use the method directly above
    private void getLanguages() {
        String languageStrings = "";
        List<String> languages = javaObject.getCitationFields().getListField(LANGUAGE);
        if(languages.size()==0)
            return;
        for (String s : languages) {
            if (languageStrings.isEmpty())
                languageStrings = s;
            else
                languageStrings = languageStrings + ", " + s;
        }
        jo.put("dc_language_s", languageStrings);
    }

    private void getIssueDate() {
        String dateString = javaObject.getSimpleFields().getField(PUB_DATE);
        if(dateString.equals(""))
            return;
        Date d = new Date(dateString);
        jo.put("dct_issued_s",d.getDateAsString());
    }


    private void getAuthors() {
        JSONArray ja = new JSONArray();
        List<Author> authors = javaObject.getCitationFields().getListField(AUTHOR);
        if(authors.size()==0)
            return;
        for(Author a:authors){
            ja.put(a.getField(AUTHOR_NAME));
        }
        jo.put("dc_creator_sm",ja);
    }
    private void getPlaceNames(){
        JSONArray ja =  new JSONArray();
        HashSet<String> placeNames = new HashSet<>();
        List<GeographicCoverage> places = javaObject.getGeoFields().getGeoCovers();
        for(GeographicCoverage gc:places){
            for(String place:gc.getPlaceNames()){
                placeNames.add(place);
            }
        }
        if(placeNames.size()>0) {
            for (String s : placeNames) {
                ja.put(s);
            }
            jo.put("dct_spatial_sm",ja);
        }

    }
    //Description as array, but that seems to be wrong
    private void getDSDescription() {
        JSONArray ja = new JSONArray();
        List<Description> descriptions = javaObject.getCitationFields().getListField(DS_DESCRIPT);
        if(descriptions.size()==0)
            return;
        for(Description d:descriptions){
            ja.put(d.getDsDescriptionValue());
        }
        jo.put("dc_description_s",ja);
    }
    //Description as String, but that could be wrong
    private void getDSDescriptionSingle(){
        String answer = "";
        List<Description> descriptions = javaObject.getCitationFields().getListField(DS_DESCRIPT);
        if(descriptions.size()==0)
            return;
        for(Description d:descriptions){
            answer += d.getDsDescriptionValue()+" ";
        }
        if(!answer.isEmpty())
            jo.put("dc_description_s",answer);
    }

    public void saveJSONToFile(String json, String doi, String recordLabel){
        genDirs(recordLabel, BASE_LOCATION_TO_STORE_METADATA);
        BaseFiles.FileWriter file = new BaseFiles.FileWriter();
        recordLabel= GeodisyStrings.replaceSlashes(GEODISY_PATH_ROOT + BASE_LOCATION_TO_STORE_METADATA + recordLabel + "/" +"geoblacklight.json");
        try {
            file.writeStringToFile(json,GeodisyStrings.replaceSlashes(recordLabel));
        } catch (IOException e) {
            logger.error("Something went wrong trying to create a JSON file with doi:" + doi);
        }

    }

    private String getNumber(String folderName) {
        int start = folderName.indexOf("File ") + 5;
        int end = folderName.indexOf(" of");
        return folderName.substring(start,end);
    }

    public void saveJSONToTestFile(String json, String recordLabel, String uuid){
        genDirs(recordLabel, TEST_OPEN_METADATA_LOCAL_REPO);
        BaseFiles.FileWriter file = new BaseFiles.FileWriter();
        try {
            file.writeStringToFile(json,"./"+TEST_OPEN_METADATA_LOCAL_REPO + recordLabel + "/" + uuid + "/" +"geoblacklight.json");
        } catch (IOException e) {
            logger.error("Something went wrong trying to create a JSON file with recordLabel:" + recordLabel);
        }

    }

}
