/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Crosswalking;

import BaseFiles.*;
import _Strings.GeodisyStrings;
import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;

import static _Strings.GeodisyStrings.*;


/**
 * Abstract class for general ISO schema crosswalking. Probably will only be for ISO 19139, but I'm including this so
 * Geodisy can more easily be extended to other ISO formats in the future, if necessary.
 * @author pdante
 */
public abstract class ISO_Schema implements XMLSchema {
    GeoLogger logger = new GeoLogger(this.getClass());

    public void saveXMLToFile(Document doc, String recordLabel) {
        saveXMLToFile(doc, recordLabel, false);
    }

    public void saveXMLToFile(Document doc, String recordLabel, boolean testing) {
        if (!doc.toString().contains("Junk_Dont_Use_This_XML"))
            try {
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
                DOMSource source = new DOMSource(doc);

                String loc = BASE_LOCATION_TO_STORE_METADATA;
                File fileDir = genBaseDirs(recordLabel, loc);
                File file = new File(fileDir + "/" + ISO_19139_XML);
                FileWriter writer = new FileWriter(file);
                StreamResult result = new StreamResult(writer.getFw());

                transformer.transform(source, result);
                Zip zip = new Zip();
                zip.create(file);
            } catch (IOException e) {
                logger.error("Something went wrong creating the FileWriter for " + recordLabel);
            } catch (TransformerException e) {
                logger.error("Something went wrong when trying to write " + recordLabel + "xml to the local repo.");
            }
    }

    public File genDirs(String recordLabel, String localRepoPath) {
        {
            File fileDir = new File(GeodisyStrings.replaceSlashes(GEODISY_PATH_ROOT + localRepoPath + recordLabel));
            if (!fileDir.exists())
                fileDir.mkdirs();

            return fileDir;
        }
    }

    public File genBaseDirs(String recordLabel, String localRepoPath) {
        File fileDir = genDirs(recordLabel, localRepoPath);
        try {
            FileUtils.cleanDirectory(fileDir);
        } catch (IOException |IllegalArgumentException e) {
            logger.error("Something went wrong cleaning the base directory for the XML/JSON");
        }
        return fileDir;
    }
}

