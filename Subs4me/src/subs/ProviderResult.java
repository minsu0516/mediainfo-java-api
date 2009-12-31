package subs;

public class ProviderResult
{
    String _providerName = null;
    String _fileURL = null;
    String _destFileName = null;
    
    public ProviderResult(String providerName, String fileURL,
            String destFileName)
    {
        super();
        this._providerName = providerName;
        this._fileURL = fileURL;
        this._destFileName = destFileName;
    }

    public String getProviderName()
    {
        return _providerName;
    }

    public String getFileURL()
    {
        return _fileURL;
    }

    public String getDestFileName()
    {
        return _destFileName;
    }
    
    
}
