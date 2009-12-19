package subs;

import java.io.File;

import utils.FileStruct;

public interface Provider
{
    public String getName();
    
    public void doWork(File fi);
    
    /**
     * Search based on the normalized name, and then if its a series
     * look in the series page for sub_id
     */
    public Results searchByActualName(FileStruct currentFile);
    
    /**
     * Parse the site to get the subid for the episode in question
     * 
     * @return
     */
    public String searchForCorrectSubidOfSeries(String seriesInfo, FileStruct currentFile);
}
