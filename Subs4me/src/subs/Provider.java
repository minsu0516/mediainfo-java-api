package subs;

import utils.FileStruct;

public interface Provider
{
    public String getName();
    public boolean doWork(FileStruct fs);
}
