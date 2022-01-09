package Dataverse;

import BaseFiles.GeoLogger;
import BaseFiles.HTTPGetCall;
import Dataverse.FindingBoundingBoxes.LocationTypes.BoundingBox;
import _Strings.GeodisyStrings;
import Dataverse.DataverseJSONFieldClasses.Fields.DataverseJSONGeoFieldClasses.*;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;


import static _Strings.GeodisyStrings.*;
import static _Strings.DVFieldNameStrings.*;

/**
 * Java object structure to parse Dataverse Json into
 * May need to change field types for dates, URLs, and/or email addresses.
 *
 * Dataverse Alias: Java Class: JavaObject Variable
 *
 *
 */

public class DataverseJavaObject extends SourceJavaObject {
    private GeoLogger logger = new GeoLogger(this.getClass());

    public DataverseJavaObject(String server) {
        super(server);
    }

    /**
     * Gets the Citation metadata for the record
     * @param citationFieldsArray
     */
    @Override
    public void parseCitationFields(JSONObject citationFieldsArray) {
        citationFields.setBaseFields(citationFieldsArray);
        JSONObject metadata = getVersionSection(citationFieldsArray).getJSONObject("metadataBlocks");
        JSONArray currentArray = metadata.getJSONObject(CITATION).getJSONArray(FIELDS);
        for (Object o : currentArray) {
            JSONObject jo = (JSONObject) o;
            citationFields = citationFields.setFields(jo);
        }
        hasContent=true;
        geoFields = new GeographicFields(this);
    }

    /**
     * Gets the Geospatial metadata for the record
     * @param geoFieldsArray
     */
    @Override
    public void parseGeospatialFields(JSONArray geoFieldsArray) {
        for(Object o: geoFieldsArray){
            JSONObject jo = (JSONObject) o;
             geoFields = geoFields.setFields(jo);
        }
        List<BoundingBox> bb = new LinkedList<>();
        //GeonamesBBs geonames = new GeonamesBBs(this);
        if(!geoFields.hasBB()){
            List<GeographicCoverage> coverages = geoFields.getListField(GEOGRAPHIC_COVERAGE);
            if(!coverages.isEmpty())

                for(GeographicCoverage gc: coverages){
                    String country = gc.getField(GIVEN_COUNTRY);
                    String province = gc.getField(GIVEN_PROVINCE);
                    String city = gc.getField(GIVEN_CITY);
                    if(country.isEmpty())
                        continue;
                    else{
                        if(province.isEmpty()) {
                            bb.add(gc.getCountryObject().getBoundingBox());
                        }
                        else{
                            if(city.isEmpty())
                                bb.add(gc.getProvinceObject().getBoundingBox());
                            else
                                bb.add(gc.getCityObject().getBoundingBox());
                        }
                    }
                }
        }
    }

    /**
     * Gets the File metadata for the record.
     * @param fileFieldsArray
     */
    @Override
    public void parseFiles(JSONArray fileFieldsArray) {
        String recordLabel = getSimpleFieldVal(RECORD_LABEL);
        for(Object o: fileFieldsArray){
            try {
                JSONObject jo = (JSONObject) o;
                if (jo.getBoolean("restricted"))
                    continue;
                JSONObject dataFile = jo.getJSONObject("dataFile");
                String title = dataFile.getString("filename");
                DataverseRecordFile dRF;
                //Some Dataverses don't have individual DOIs for files, so for those I will use the database's file id instead
                int dbID;
                String url ="";
                if (jo.has("frdr_harvester")) {
                    url = dataFile.getString("fileURI");
                    //OpenDataSoft repositories do streaming downloads in geojson format and give
                    if(GeodisyStrings.checkIfOpenDataSoftRepo(url)){
                        int fileName = url.indexOf("download?dataset=") + 17;
                        title = url.substring(fileName) + ".geojson";
                    }

                    dRF = new DataverseRecordFile(title, recordLabel, url);
                    dRF.setOriginalTitle(title);
                } else if (dataFile.has(PERSISTENT_ID) && !dataFile.getString(PERSISTENT_ID).equals("")) {
                    dbID = (int) dataFile.get("id");
                    String doi = dataFile.getString(PERSISTENT_ID);
                    dRF = new DataverseRecordFile(title, doi, dataFile.getInt("id"), server, recordLabel);
                    dRF.setFileURL(server + DV_FILE_ACCESS_PATH + dbID);
                    dRF.setOriginalTitle(title);
                } else {
                    dbID = (int) dataFile.get("id");
                    dRF = new DataverseRecordFile(title, recordLabel, dbID);
                    dRF.setOriginalTitle(title);
                    dRF.setFileURL(server + DV_FILE_ACCESS_PATH + dbID);
                }
                SourceRecordFiles srf = SourceRecordFiles.getSourceRecords();
                srf.putRecord(dRF.getDatasetIdent(), dRF.translatedTitle, dRF);
                dataFiles.add(dRF);
            }catch (ClassCastException e){
                logger.error("Something went wrong trying to parse the file metadata section for " + getPID() + " the json was " + fileFieldsArray.toString());
            }
        }
    }
    //if changed, need to change copy in CitationFields Class
    @Override
    public JSONObject getVersionSection(JSONObject current) {
        if(current.has("latestVersion"))
            return current.getJSONObject("latestVersion");
        else if(current.has("datasetVersion"))
            return current.getJSONObject("datasetVersion");
        else{
            logger.error("missing a _____Version field in the dataverseJson in " + current.toString());
            return new JSONObject();
        }
    }

    /**
     *  Deletes the directory of the record's files if it exists and then downloads the updated
     *  files, excluding non-geospatial file types other than .zip
     *  If there aren't any geospatial files uploaded, the entire directory get's deleted.
     */

    @Override
    public LinkedList<DataverseGeoRecordFile> downloadFiles() {
        String recordLabel = getSimpleFieldVal(RECORD_LABEL);
        long startTime = Calendar.getInstance().getTimeInMillis();
        String path = GeodisyStrings.replaceSlashes(DATA_DIR_LOC + recordLabel);
        File f = new File(path);
        try {
            deleteDir(f);
        } catch (IOException e) {
            logger.error("Something went wrong trying to delete folder: " + f.getAbsolutePath());
        }
        f.mkdirs();
        LinkedList<DataverseRecordFile> drfs = new LinkedList<>();


        HTTPGetCall httpGetCall = new HTTPGetCall();
        dataFiles = httpGetCall.checkDataset(dataFiles,getPID());
        for (DataverseRecordFile dRF : dataFiles) {
            if (GeodisyStrings.fileTypesToIgnore(dRF.translatedTitle)) {
                //System.out.println("Ignored file: " + dRF.translatedTitle);
                continue;
            }
            if(dRF.translatedTitle.startsWith("LAS_")) {
                continue;
            }
            System.out.println("Getting file " + dRF.getFileName());
            LinkedList<DataverseRecordFile> temp = dRF.retrieveFile(this);
            for(DataverseRecordFile d: temp){
                boolean add = true;
                for(DataverseRecordFile d2: drfs){
                    if(d.getTranslatedTitle().equals(d2.getTranslatedTitle())) {
                        add = false;
                        break;
                    }
                }
                if(add)
                    drfs.add(d);
            }
        }
        Long total = Calendar.getInstance().getTimeInMillis() - startTime;
        System.out.println("Finished downloading and unzipping files from " + getPID() + " after " + total + " milliseconds");
        if(drfs.size()==0){
            System.out.print("No drfs generated so deleting folder");
            try {
                FileUtils.deleteDirectory(f);
            } catch (IOException e) {
                logger.error("Something went wrong deleting empty folder: " + path);
            }
            return new LinkedList<DataverseGeoRecordFile>();
        }


        dataFiles = drfs;
        System.out.println("Deleting .zip and .tab files that still exist");
        LinkedList<DataverseRecordFile> left = new LinkedList<>();
        for (int i = 0; i < dataFiles.size(); i++) {
            String name = dataFiles.get(i).getTranslatedTitle();
            if (!name.endsWith("zip") && !name.endsWith("tab"))
                left.add(dataFiles.get(i));
            if((i+1)%50 == 0)
                System.out.print(".");
            if(i%250==0)
                System.out.print(i);
        }
        System.out.println();
        System.out.println("Finished deleting .zip and .tab files that still exist");
        dataFiles = left;

        LinkedList<DataverseGeoRecordFile> newRecs = new LinkedList<>();
        DataverseGeoRecordFile dgrf;
        System.out.print("Creating a new list of DGRFs that have valid bounding boxes");
        for (DataverseRecordFile dRF : dataFiles) {
            if (!GeodisyStrings.isProcessable(dRF.translatedTitle))
                continue;
            if (dRF.getDbID() == -1)
                dRF.setFileURL("");
            GDAL gdal = new GDAL();
            String dirPath = path + GeodisyStrings.replaceSlashes("/");
            dgrf = new DataverseGeoRecordFile(dRF);
            GeographicBoundingBox gbb = gdal.generateBB(new File(dirPath+dRF.getTranslatedTitle()), recordLabel,dRF.getGBBFileNumber());
            dgrf.setGbb(gbb, gbb.getField(FILE_NAME));
            if(dgrf.hasValidBB()) {
                dgrf.setTranslatedTitle(dgrf.gbb.getField(FILE_NAME));
                dgrf.setFileURL(dRF.getFileURL());
                newRecs.add(dgrf);
            }
            else{
                String name = dgrf.getTranslatedTitle();
                File f1;
                if(name.endsWith("shp")){
                    String stub = name.substring(0,name.lastIndexOf(".shp"));
                    for(String ex: NON_SHP_SHAPEFILE_EXTENSIONS){
                        f1 = new File(dirPath+stub+ex);
                        if(f1.exists()) {
                            try {
                                Files.deleteIfExists(Paths.get(f1.getAbsolutePath()));
                            } catch (IOException e) {
                                logger.error("Something went wrong trying to delete : " + f1.getAbsolutePath());
                            }
                        }
                    }
                }
                f1 = new File(dirPath+name);
                if(f1.exists()) {
                    try {
                        Files.deleteIfExists(Paths.get(f1.getAbsolutePath()));
                    } catch (IOException e) {
                        logger.error("Something went wrong trying to delete : " + f1.getAbsolutePath());
                    }
                }
            }
            ExistingGeoLabels existingGeoLabels = ExistingGeoLabels.getExistingLabels();
            ExistingGeoLabelsVals existingGeoLabelsVals = ExistingGeoLabelsVals.getExistingGeoLabelsVals();
            existingGeoLabels.saveExistingFile(existingGeoLabels.getGeoLabels(),EXISTING_GEO_LABELS,"ExistingGeoLabels");
            existingGeoLabelsVals.saveExistingFile(existingGeoLabelsVals.getValues(),EXISTING_GEO_LABELS_VALS,"ExistingGeoLabelsVals");
        }
        return newRecs;
    }

    @Override
    public void updateGeoserver() {
        System.out.println("Updating geoserver");
        Collections.sort(getGeoDataFiles(), new SortByFileName());
        LinkedList<DataverseGeoRecordFile> onGeoserver = new LinkedList<>();
        for(DataverseGeoRecordFile dgrf:getGeoDataFiles()){
            if(dgrf.getTranslatedTitle().endsWith(".shp")) {
                dgrf.onGeoserver = createRecords(dgrf, Integer.parseInt(dgrf.getGBBFileNumber()), VECTOR);
            }else if(dgrf.getTranslatedTitle().endsWith(".tif")) {
                dgrf.onGeoserver = createRecords(dgrf, Integer.parseInt(dgrf.getGBBFileNumber()), RASTER);
            }
            if(dgrf.onGeoserver)
                onGeoserver.add(dgrf);
            System.out.println("Finished Adding file to geoserver");
        }
        setGeoDataFiles(onGeoserver);
    }
    public void updateHarvesterWithBBoxes() {
        //TODO Check to make sure we are sending everything the harvester needs
        try {
            JSONObject jo = new JSONObject();
            JSONArray boxes = new JSONArray();
            for (DataverseGeoRecordFile rf : geoDataFiles) {
                GeographicBoundingBox gbb = rf.gbb;
                BoundingBox bb = gbb.getBB();
                JSONObject box = new JSONObject();
                box.put("northLat", bb.getLatNorth());
                box.put("southLat", bb.getLatSouth());
                box.put("eastLon", bb.getLongEast());
                box.put("westLon", bb.getLongWest());
                box.put("geoserverID",gbb.getBaseGeoserverLocation());
                box.put("file_name",gbb.getField(FILE_NAME));
                boxes.put(box);
            }
            jo.put("bboxes", boxes);

            URL url = new URL(ADD_BOUNDING_BOX + getPID());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            OutputStream os = conn.getOutputStream();
            os.write(jo.toString().getBytes(StandardCharsets.UTF_8));
            os.close();
            InputStream in = new BufferedInputStream(conn.getInputStream());
            String result = org.apache.commons.io.IOUtils.toString(in, StandardCharsets.UTF_8);
            in.close();
            conn.disconnect();

        }catch(IOException e){
            logger.error("Something went wrong trying to call FRDR to add file-based bounding boxes to record #" + getPID() + " as processed by Geodisy");
        }
    }
    class SortByFileName implements Comparator<DataverseGeoRecordFile>{
        public int compare(DataverseGeoRecordFile a, DataverseGeoRecordFile b){
            return a.getTranslatedTitle().compareToIgnoreCase(b.getTranslatedTitle());
        }
    }
    //TODO
    private DataverseGeoRecordFile getRecord(String name) {
        DataverseGeoRecordFile dvgr = new DataverseGeoRecordFile();
        for(DataverseRecordFile drf : dataFiles){
            if(drf.getTranslatedTitle().equals(name)) {
                dvgr = new DataverseGeoRecordFile(drf);
                dvgr.setFileURL(drf.getFileURL());
                return dvgr;
            }
        }
        dvgr.setOriginalTitle(name);
        dvgr.setTranslatedTitle(name);
        String fileURL = dataFiles.get(0).getFileURL();
        fileURL = fileURL.substring(0,fileURL.lastIndexOf(GeodisyStrings.replaceSlashes("/"))+1);
        dvgr.setFileURL(fileURL + name);
        dvgr.setServer(dataFiles.get(0).server);
        dvgr.setDbID(-1);
        dvgr.setDatasetIdent(dataFiles.get(0).datasetIdent);
        return dvgr;
    }

    //May need to merge tilesets later, but ignore this for now
    /*private void mergeTiles(DataverseRecordFile drf) {
        String merge = "gdalbuildvrt mpsaic.vrt " + DATASET_FILES_PATH + drf.getDatasetIdent().replace("_","/") + "/";
        String delete = "rm "+ DATASET_FILES_PATH + drf.getDatasetIdent().replace("_","/") + "/*.tif";
        String transform ="gdal_translate -of GTiff -co \"COMPRESS=JPEG\" -co \"PHOTOMETRIC=YCBCR\" -co \"TILED=YES\" mosaic.vrt" + drf.getDatasetIdent() + ".tif";
        ProcessCall processCall;
        try {
            processCall = new ProcessCall();
            processCall.runProcess(merge,5, TimeUnit.SECONDS,logger);

        } catch (IOException | InterruptedException | ExecutionException e) {
            logger.error("Something went wrong trying to merge tifs in a mosaic file in record: " + drf.getDatasetIdent());
        } catch (TimeoutException e) {
            logger.error("Timeout trying to merge tifs in a mosaic file in record: " + drf.getDatasetIdent());
        }
        try{
            processCall = new ProcessCall();
            processCall.runProcess(delete,5, TimeUnit.SECONDS,logger);

        } catch (IOException | InterruptedException | ExecutionException e) {
            logger.error("Something went wrong trying to delete merged tifs in record: " + drf.getDatasetIdent());
        } catch (TimeoutException e) {
            logger.error("Timeout trying to merge tifs in a mosaic file in record: " + drf.getDatasetIdent());
        }
        try{
            processCall = new ProcessCall();
            processCall.runProcess(transform,5, TimeUnit.SECONDS,logger);

        } catch (IOException | InterruptedException | ExecutionException e) {
            logger.error("Something went wrong trying to convert merged tifs (mosaic) to to a new tif in record: " + drf.getDatasetIdent());
        } catch (TimeoutException e) {
            logger.error("Timeout trying to merge tifs in a mosaic file in record: " + drf.getDatasetIdent());
        }
        DataverseRecordFile temp;
        if(drf.getFileIdent().equals(""))
            temp = new DataverseRecordFile(drf.getFileName(),drf.getDatasetIdent(),drf.getDbID());
        else
            temp = new DataverseRecordFile(drf.getDatasetIdent().replace("_","/") + "/*.tif",drf.getFileIdent(),drf.getDbID(),drf.getServer(),drf.getDatasetIdent());

        for(Iterator<DataverseRecordFile> iter = dataFiles.iterator(); iter.hasNext();){
            DataverseRecordFile check = iter.next();
            if(check.getTranslatedTitle().toLowerCase().endsWith(".tif"))
                iter.remove();
        }
        dataFiles.add(temp);
    }*/

    @Override
    protected boolean createRecords(DataverseGeoRecordFile dgrf, int number, String type) {
        String dirPath = GeodisyStrings.replaceSlashes(DATA_DIR_LOC + dgrf.getDatasetIdent() + "/");
        String filePath = dirPath + dgrf.getTranslatedTitle();
        File fUpdate = new File(filePath);
        if(type.equals(VECTOR)) {
            return addVectorToGeoserver(fUpdate.getName(),dgrf.getGeoserverLabel());
        }else if(type.equals(RASTER)){
            return addRasterToGeoserver(dgrf);
        } else{
            logger.error("Something went wrong parsing DataverseRecordFile: " + dgrf.getTranslatedTitle());
            return false;
        }
    }

    @Override
    public String getDOIProtocal(){
        String answer = super.getDOIProtocal();
        if(answer.equals("Error")){
            logger.warn("Unknown persistant URL protocal used with record: " + getPID());
            return "";
        } else
            return answer;
    }

    @Override
    public void updateRecordFileNumbers() {
        updateRecordFileNumbers(logger);
    }

    public boolean isNewOrHasNewFiles(){
        return citationFields.isNewOrHasNewFiles();
    }
}
