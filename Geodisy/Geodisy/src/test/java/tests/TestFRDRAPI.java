package tests;


import Dataverse.FRDRAPI;
import Dataverse.SourceJavaObject;
import org.junit.Test;

import java.util.LinkedList;

import static _Strings.DVFieldNameStrings.RECORD_LABEL;

public class TestFRDRAPI {
    @Test
    public void getJSONRecords(){
        FRDRAPI f = new FRDRAPI();
        LinkedList<SourceJavaObject> djos = f.callFRDRHarvester("");
        for(SourceJavaObject s: djos){
            System.out.println(s.getSimpleFieldVal(RECORD_LABEL));
        }

    }
}
