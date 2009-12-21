package subs.providers;

import java.io.File;

import subs.Provider;
import subs.Results;
import utils.FileStruct;

public class OpenSubs implements Provider
{
    public static final String baseUrl ="http://OPENSUBTITLES.ORG";
    FileStruct currentFile = null;
    
    @Override
    public void doWork(File fi)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public String getName()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Results searchByActualName(FileStruct currentFile)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String searchForCorrectSubidOfSeries(String seriesInfo,
            FileStruct currentFile)
    {
        // TODO Auto-generated method stub
        return null;
    }

}
