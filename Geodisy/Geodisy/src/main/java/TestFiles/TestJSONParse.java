package TestFiles;

import Dataverse.DataverseJavaObject;
import Dataverse.FRDRAPI;
import _Strings.GeodisyStrings;
import org.json.JSONObject;
import org.junit.Before;

import java.io.*;

import static _Strings.GeodisyStrings.ALL_CITATION_METADATA;

public class TestJSONParse implements Test {
    DataverseJavaObject djo;
    JSONObject jo;
    String json;

    public TestJSONParse() {
        InputStream is = null;

        try {
            is = new FileInputStream(GeodisyStrings.replaceSlashes(ALL_CITATION_METADATA));

            BufferedReader buf = new BufferedReader(new InputStreamReader(is));

            String line = buf.readLine();
            StringBuilder sb = new StringBuilder();

            while (line != null) {
                sb.append(line).append("\n");
                line = buf.readLine();
            }
            json = sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        FRDRAPI f = new FRDRAPI();
        f.callFRDRHarvester(json);
    }
}
