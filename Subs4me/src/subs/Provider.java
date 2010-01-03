package subs;

import java.io.File;

public interface Provider
{
    public String getName();
    public boolean doWork(File fi);
}
