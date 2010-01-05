package subs;

import utils.FileStruct;

public interface Provider
{
    public final int not_found = -1;
    public final int not_perfect = 0;
    public final int perfect = 1;
    
    public String getName();
    
    /**
     * -1 == not found, 0 = one result, 1 = more than 1 results
     * @param fs
     * @return
     * @throws Exception
     */
    public int doWork(FileStruct fs) throws Exception;
}
