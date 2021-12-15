package TestFiles;

import Dataverse.GDAL;

import java.io.IOException;

public class GDALTest implements Test{
    @Override
    public void run() {
        String filePath = "/opt/share/geoserver/geoserver-2.17.0/data_dir/data/shapefiles/";
        String fileName = "states.shp";
        GDAL gdal = new GDAL();
        try {
            System.out.println(gdal.getGDALInfo(filePath, fileName));
        } catch(IOException e){
            System.out.println(e);
        }
    }
}
