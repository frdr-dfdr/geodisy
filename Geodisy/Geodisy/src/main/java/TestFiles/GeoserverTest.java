package TestFiles;

import Dataverse.DataverseGeoRecordFile;
import Dataverse.DataverseJSONFieldClasses.Fields.CitationCompoundFields.CitationFields;
import Dataverse.DataverseJavaObject;
import Dataverse.SourceJavaObject;
import GeoServer.GeoServerAPI;

import static _Strings.DVFieldNameStrings.RECORD_LABEL;

public class GeoserverTest {

    public void testAddingARaster(){
        String fileName = "age.tif";
        String doi = "rasterTest";
        SourceJavaObject sjo = new DataverseJavaObject("server");
        CitationFields cf = sjo.getCitationFields();
        cf.setPID(doi);
        sjo.setCitationFields(cf);
        GeoServerAPI geoServerAPI = new GeoServerAPI(sjo);
        DataverseGeoRecordFile dgrf = new DataverseGeoRecordFile(sjo.getSimpleFieldVal(RECORD_LABEL),fileName);
        geoServerAPI.addRaster(dgrf);
    }

    public void testAddingAVector() {
        int count = 0;
        String[] files = {"Stops_09Aug19.shp", "Stops_09Aug19_shp9_9Stops_09Aug19.shp", "Shapes_Trips_Routes_09Aug19_shp9_9Shapes_Trips_Routes_09Aug19.shp", "Shapes_Trips_Routes_09Aug19.shp"};
        for (String s : files) {
            System.out.println(s);
            String fileName = s;
            String doi = "vectorTest";
            SourceJavaObject sjo = new DataverseJavaObject("server");
            CitationFields cf = sjo.getCitationFields();
            cf.setPID(doi);
            sjo.setCitationFields(cf);
            DataverseGeoRecordFile dgrf = new DataverseGeoRecordFile(sjo.getSimpleFieldVal(RECORD_LABEL), fileName);
            GeoServerAPI geoServerAPI = new GeoServerAPI(sjo);
            geoServerAPI.addVector(fileName, sjo.getSimpleFieldVal(RECORD_LABEL)+count);
            count++;
        }
    }
}
