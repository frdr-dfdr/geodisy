package Dataverse.DataverseJSONFieldClasses;

import Dataverse.DataverseJavaObject;
import org.json.JSONObject;

import java.util.List;

public abstract class MetadataType{
    protected String doi;
    protected DataverseJavaObject djo;

    public abstract MetadataType setFields(JSONObject jo);
    public abstract List getListField(String fieldName);
    public abstract String getPID();
    public abstract void setPID(String doi);

}
