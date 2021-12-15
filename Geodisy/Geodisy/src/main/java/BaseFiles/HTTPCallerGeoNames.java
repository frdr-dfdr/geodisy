package BaseFiles;



import java.io.IOException;

/**
 * Class for making HTTP calls and getting JSON string responses
 */
public class HTTPCallerGeoNames extends HTTPCaller {

    public HTTPCallerGeoNames() {
        logger = new GeoLogger(this.getClass());
    }

    @Override
    protected void ioError(IOException e){
        logger.error("Something went wrong getting a bounding box from Geonames " + e);
    }
}
