package BaseFiles;
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import TestFiles.Test;
import TestFiles.Tests;
import _Strings.GeodisyStrings;
import static _Strings.GeodisyStrings.*;


/**
 *
 * @author pdante
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        //Run the below to remove a single records
        //RemoveRecord rr = new RemoveRecord();
        //rr.removeRecord();
        String dev;
        TEST = args.length > 0;
        //TEST = true;
        if(TEST)
            dev = "Using the dev servers, is this correct?";
        else
            dev = "Using the prod servers, is this correct?";
        GeodisyStrings.load();
        GeodisyTask geodisyTask = new GeodisyTask();

        if(!TEST)
            geodisyTask.run();
        else {
        Tests tests = new Tests();
        tests.runTests();
        }
    }
}
