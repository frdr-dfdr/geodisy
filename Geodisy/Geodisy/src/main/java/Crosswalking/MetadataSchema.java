/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Crosswalking;


import java.io.File;

/**
 * Interface for the output metadata schema side of a crosswalk
 * @author pdante
 */
public interface MetadataSchema {
    File genDirs(String doi, String localRepoPath);

}
