package Crosswalking;

import Crosswalking.XML.XMLTools.XMLDocObject;
import Dataverse.SourceJavaObject;

public interface XMLSchema extends MetadataSchema{
    XMLDocObject generateXML(SourceJavaObject sJO);
}
