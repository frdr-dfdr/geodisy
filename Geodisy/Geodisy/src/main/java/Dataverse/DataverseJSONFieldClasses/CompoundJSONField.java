package Dataverse.DataverseJSONFieldClasses;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Set;

abstract public class CompoundJSONField extends JSONField {
    /**
     * Iterates through the JSONArray calling setField on each object in a compound JSON field.
     * e.g. AuthorName and AuthorAffiliation need to be linked together in a single Author object
     *
     * @param compoundField
     * @return
     */

    public void parseCompoundData(JSONObject compoundField){
        Set<String> keys = compoundField.keySet();
        for(String k:keys) {
            setField((JSONObject) compoundField.get(k));
        }
    }

    /**
     *
     * Each class overrides this with a version using a switch statement to
     * try to fill all the fields of its class
     *
     * @param field
     */
    public abstract void setField(JSONObject field);


    public abstract String getField(String fieldName);

    public void setFields(JSONArray ja){
        for(Object o: ja){
            setField((JSONObject) o);
        }
    }
}
