package Crosswalking.XML.XMLGroups;

import Crosswalking.XML.XMLTools.SubElement;
import Crosswalking.XML.XMLTools.XMLDocObject;
import Crosswalking.XML.XMLTools.XMLStack;
import Dataverse.DataverseJSONFieldClasses.Fields.CitationCompoundFields.CitationFields;
import Dataverse.DataverseJSONFieldClasses.Fields.CitationSimpleJSONFields.SimpleCitationFields;
import Dataverse.DataverseJavaObject;
import org.w3c.dom.Element;

import java.util.LinkedList;

import static _Strings.GeodisyStrings.CHARACTER;
import static _Strings.XMLStrings.*;
import static _Strings.DVFieldNameStrings.*;

public class DataQualityInfo extends SubElement {
    public DataQualityInfo(DataverseJavaObject djo, XMLDocObject doc, Element root) {
        super(djo, doc, root);
    }

    public Element getFields(){
        SimpleCitationFields simple = djo.getSimpleFields();
        CitationFields cf = djo.getCitationFields();
        Element levelI = doc.createGMDElement("dataQualityInfo");
        Element levelJ = doc.createGMDElement("DQ_DataQuality");
        Element levelK = doc.createGMDElement("scope");
        Element levelL = doc.createGMDElement("resourceLineage");
        Element levelM = doc.createGMDElement("LI_Lineage");
        LinkedList<String> dataSources = (LinkedList) cf.getListField(DATA_SOURCES);
        if(dataSources.size()>0) {
            for(String s: dataSources) {
                Element levelN =  doc.createGMDElement("statement");
                levelN.appendChild(doc.addGCOVal(s, CHARACTER));
                levelM.appendChild(levelN);
            }
            levelL.appendChild(levelM);
        }
        if(!simple.getField(ORIG_OF_SOURCES).isEmpty()) {
            stack = new XMLStack();
            stack.push(levelM);
            stack.push(doc.createGMDElement("processStep")); //N
            stack.push(doc.createGMDElement("LI_ProcessStep")); //O
            stack.push(doc.createGMDElement(DESCRIP)); //P
            levelM = stack.zip(doc.addGCOVal(simple.getField(ORIG_OF_SOURCES), CHARACTER));
            levelL.appendChild(levelM);
        }
        if(!simple.getField(CHAR_OF_SOURCES).isEmpty()) {
            stack = new XMLStack();
            stack.push(levelM);
            stack.push(doc.createGMDElement("source")); //N
            stack.push(doc.createGMDElement("LI_Source")); //O
            stack.push(doc.createGMDElement(DESCRIP)); //P
            levelM = stack.zip(doc.addGCOVal(simple.getField(CHAR_OF_SOURCES), CHARACTER));
            levelL.appendChild(levelM);
        }
        if(!simple.getField(ACCESS_TO_SOURCES).isEmpty()){
            stack = new XMLStack();
            stack.push(levelM);
            stack.push(doc.createGMDElement(ADDITIONAL_DOCS));
            stack.push(doc.createGMDElement(CI_CITE));
            stack.push(doc.createGMDElement(OTHER_CITE_DEETS));
            levelM = stack.zip(doc.addGCOVal(simple.getField(ACCESS_TO_SOURCES), CHARACTER));
            levelL.appendChild(levelM);
        }
        levelK.appendChild(levelL);
        levelJ.appendChild(levelK);
        levelI.appendChild(levelJ);
        return levelI;
    }

}
