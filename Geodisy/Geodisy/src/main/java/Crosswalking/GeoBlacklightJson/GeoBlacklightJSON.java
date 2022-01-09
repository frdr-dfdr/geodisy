package Crosswalking.GeoBlacklightJson;

import BaseFiles.FileWriter;
import BaseFiles.GeoLogger;
import Crosswalking.MetadataSchema;
import Dataverse.DataverseGeoRecordFile;
import Dataverse.DataverseJSONFieldClasses.Fields.DataverseJSONGeoFieldClasses.GeographicBoundingBox;
import Dataverse.DataverseJavaObject;
import Dataverse.DataverseRecordFile;
import Dataverse.SourceRecordFiles;
import _Strings.GeodisyStrings;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.InputStream;

import java.util.LinkedList;
import java.util.List;

import static _Strings.DVFieldNameStrings.RECORD_LABEL;
import static _Strings.DVFieldNameStrings.TITLE;
import static _Strings.GeodisyStrings.GEODISY_PATH_ROOT;

/**
 * Takes a DataverseJavaObject and creates a GeoBlacklight JSON from it
 */
public abstract class GeoBlacklightJSON extends JSONCreator implements MetadataSchema {
    protected DataverseJavaObject javaObject;
    protected String geoBlacklightJson;
    protected JSONObject jo;
    protected String doi;
    protected String recordLabel;
    boolean download = false;
    GeoLogger logger;
    LinkedList<DataverseGeoRecordFile> geoFiles;
    LinkedList<DataverseGeoRecordFile> geoMeta;
    SourceRecordFiles files;

    public GeoBlacklightJSON() {
        this.jo = new JSONObject();
        files = SourceRecordFiles.getSourceRecords();
        logger = new GeoLogger(this.getClass());
    }

    public void createJson() {
        int countFile = geoFiles.size();
        int countMeta = geoMeta.size();
        System.out.println("DOI = " + doi + " . Number geoFile: " + countFile + " . Number geoMeta: " + countMeta);
        List<DataverseRecordFile> list = new LinkedList<>();
        list.addAll(geoFiles);
        list.addAll(geoMeta);
        if(list.size()>1){
            int count = 1;
            for(DataverseRecordFile d:list){
                d.setBbCount(count);
                count++;
            }

        }
        int total = list.size();
        for(DataverseRecordFile drf:list){
            createJSONFromFiles(drf, total);
        }
    }

    public void updateJSONs(){
        File file = new File(recordLabel);
        File[] files = file.listFiles();
        for(File f: files){
            if(f.getName().equals("geoblacklight.json"))
                updateJSON(f);
            else if(f.isDirectory())
                updateJSON(f.listFiles()[0]);
        }
    }
    protected void updateJSON(File file){
        InputStream is = GeoBlacklightJSON.class.getResourceAsStream(file.getAbsolutePath());
        if (is == null) {
            throw new NullPointerException("Cannot find resource file " + is);
        }

        JSONTokener tokener = new JSONTokener(is);
        jo = new JSONObject(tokener);
        updateRequiredFields();
        updateOptionalFields();
        geoBlacklightJson = jo.toString();
        saveJSONToFile(geoBlacklightJson, recordLabel, file.getAbsolutePath());

    }

    private void createJSONFromFiles(DataverseRecordFile drf, int total) {
        boolean single = total == 1;
        getRequiredFields(drf.getGBB(), total, drf.getBbCount());
        getOptionalFields(drf,total);
        geoBlacklightJson = jo.toString();
        String slash = GeodisyStrings.replaceSlashes("/");
        if(!javaObject.getSimpleFields().getField(TITLE).isEmpty())
            if (!single)
                saveJSONToFile(geoBlacklightJson, doi, recordLabel + slash+drf.getBbCount());
            else
                saveJSONToFile(geoBlacklightJson, doi, recordLabel);
    }

    public File genDirs(String doi, String localRepoPath) {
        localRepoPath = FileWriter.fixPath(localRepoPath);
        File fileDir = new File(GeodisyStrings.replaceSlashes(GEODISY_PATH_ROOT +localRepoPath + javaObject.getSimpleFieldVal(RECORD_LABEL)));
        if(!fileDir.exists())
            fileDir.mkdirs();
        return fileDir;
    }

    public String getDoi(){
        return doi;
    }
    protected abstract JSONObject getRequiredFields(GeographicBoundingBox gbb, int total, int bboxNumber);



    protected abstract JSONObject getOptionalFields(DataverseRecordFile drf, int totalRecordsInStudy);
    protected abstract JSONObject addDataDownloadOptions(GeographicBoundingBox bb, JSONObject ja, boolean isOnGeoserver); //for records with datasetfiles
    protected abstract JSONObject addBaseRecordInfo(); //adds the base metadata external services that all records need regardless of existence of datafiles
    protected abstract void saveJSONToFile(String json, String doi, String folderName);
    protected abstract JSONObject updateOptionalFields();
    protected abstract JSONObject updateRequiredFields();

}
