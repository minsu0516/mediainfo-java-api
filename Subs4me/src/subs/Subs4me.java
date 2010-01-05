package subs;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import subs.providers.OpenSubs;
import subs.providers.Sratim;
import subs.providers.Subscene;
import subs.providers.Torec;
import utils.FileStruct;
import utils.PropertiesUtil;

public class Subs4me
{
    public static final String SRT_EXISTS = "/c";
    public static final String VERSION = "0.9.2";
    public static final String RECURSIVE_SEARCH = "/r";
    public static final String FULL_DOWNLOAD = "/all";
    public static final String PROVIDERS = "/p";
    public static final String DO_NOT_USE_OPENSUBS_FOR_FILE_REALIZATION = "/n";
    public static final String DO_WORK_EXT = ".run_HandleMultiplesubs";
    
    public static final String PROVIDERS_PROPEERTY = "get_subs_providers";
    public static final String SUBS_CHECK_ALL_PROPEERTY = "get_subs_check_exists";
    public static final String SUBS_RECURSIVE_PROPEERTY = "get_subs_recursive";
    public static final String SUBS_GET_ALL_PROPEERTY = "get_subs_all";
    
    
    String srcDir = new String();
    // private String _group = "";
    public static boolean checkSrtExists = false;
    private static boolean recursive = false;
//    private static boolean intense = false;
    private static boolean fullDownload = false;
    
    public static String propertiesName = "./subs4me.properties";
    
    private static LinkedList<Provider> _availableProviders = new LinkedList<Provider>();
    
    public static LinkedList<Provider> _providers = null;
    
    private static Subs4me instance = new Subs4me();
    
    public static Subs4me getInstance()
    {
        return instance;
    }
    
    public Subs4me()
    {
//        System.out.println("Locale = " + Locale.getDefault());
//        Locale.setDefault(new Locale("en", "US"));
//        System.out.println("New Locale = " + Locale.getDefault());
    }

    /**
     * need to be recursive to process sub directories
     * 
     * @param src
     */
    public void startProcessingFiles(String src)
    {
        String[] sources = findFilesInDir(src);
        if (sources == null)
        {
            // this is a file and not a directory
            File fi = new File(src);
            FileStruct fs = new FileStruct(fi);
            // String f = fi.getName();
            for (Iterator iterator = _providers.iterator(); iterator.hasNext();)
            {
                Provider p = (Provider) iterator.next();
                boolean success;
                try
                {
                    success = p.doWork(fs);
                    if (success)
                    {
                        cleanup();
                        break;
                    }
                }
                catch (Exception e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        } else
        {
            for (int j = 0; j < sources.length; j++)
            {
                File f = new File(src + File.separator + sources[j]);
                if (f.isDirectory())
                {
                    startProcessingFiles(f.getPath());
                } else
                {
                    FileStruct fs = new FileStruct(f);
                    for (Iterator iterator = _providers.iterator(); iterator.hasNext();)
                    {
                        Provider p = (Provider) iterator.next();
                        boolean success;
                        try
                        {
                            success = p.doWork(fs);
                            if (success)
                            {
                                cleanup(f);
                                break;
                            }
                        }
                        catch (Exception e)
                        {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
    
    private void cleanup(File f)
    {
        FileStruct fs = new FileStruct(f, false);
        String[] files = findFilesTocleanupInDir(fs);
        for (int i = 0; i < files.length; i++)
        {
            String delName = files[i];
            File del = new File(f.getParent(), delName);
            del.delete();
        }
    }
    
    private String[] findFilesTocleanupInDir(final FileStruct fs)
    {
        File dir = new File(fs.getFile().getParent());
        if (dir.isDirectory())
        {
            FilenameFilter filter = new FilenameFilter()
            {
                public boolean accept(File dir, String name)
                {
                    if (name.equals(fs.getNameNoExt() + Subs4me.DO_WORK_EXT))
                    {
                        return true;
                    }
                    String n = fs.getNameNoExt() + ".srt";
                    if (name.startsWith(n) 
                            && name.length() > n.length())
                    {
                        return true;
                    }
                    return false;
                }
            };
            return dir.list(filter);
        } else
        {
            return null;
        }
    }
    
    private void cleanup()
    {
        // TODO Auto-generated method stub
        
    }

    private String[] findFilesInDir(String src)
    {
        File dir = new File(src);
        if (dir.isDirectory())
        {
            FilenameFilter filter = new FilenameFilter()
            {
                public boolean accept(File dir, String name)
                {
                    if (isRecursive() && new File(dir, name).isDirectory())
                    {
                        return true;
                    }
                    //NO SAMPLE FILES!!!!
                    if (name.indexOf("-sample") > -1)
                    {
                        return false;
                        
                    }
                    if (name.endsWith("mkv") || name.endsWith("avi"))
                    {
                        if (checkSrtExists)
                        {
                            Pattern p1 = Pattern.compile(".*([.].*$)");
                            Matcher m1 = p1.matcher(name);
                            File srt = null;
                            if (m1.find())
                            {
                                String ext = m1.group(1);
                                srt = new File(dir, name.substring(0, name
                                        .length()
                                        - ext.length())
                                        + ".srt");
                            }
                            if (srt.exists())
                            {
                                return false;
                            }
                        }
                        return true;
                    }
                    return false;
                }
            };
            return dir.list(filter);
        } else
        {
            return null;
        }

    }

    public static boolean isRecursive()
    {
        return recursive;
    }

    public static void setRecursive(boolean recursive)
    {
        Subs4me.recursive = recursive;
    }
    
    private void initProviders(LinkedList<String> provNames)
    {
        Torec.getInstance();
        Sratim.getInstance();
        new OpenSubs();
        new Subscene();
        
        _providers = new LinkedList<Provider>();
        if (provNames == null)
        {
            _providers.add(getProvider("opensubs"));
            _providers.add(getProvider("sratim"));
            _providers.add(getProvider("torec"));
        }
        else
        {
            for (Iterator iterator = provNames.iterator(); iterator.hasNext();)
            {
                String p = (String) iterator.next();
                Provider prov = getProvider(p);
                if (prov != null)
                {
                    if (prov.getName().equals(Sratim.getInstance().getName()))
                    {
                        if (!Sratim.getInstance().loadSratimCookie())
                        {
                            Login login = new Login();
                            if (!login.isLoginOk())
                            {
                                System.exit(-1);
                            }
                        }
                    }
                    _providers.add(prov);
                }
            }
        }
    }
    
    private Provider getProvider(String name)
    {
        for (Iterator iterator2 = _availableProviders.iterator(); iterator2.hasNext();)
        {
            Provider availProv = (Provider) iterator2.next();
            if (availProv.getName().toLowerCase().equals(name.toLowerCase()))
            {
                return availProv;
            }
        }
        return null;
    }
    
    public static boolean isFullDownload()
    {
        return fullDownload;
    }

    public static void main(String[] args)
    {
//        args = new String[]{};
        if (args.length < 1)
        {
            exitShowMessage();
        }
        
        File f = new File(args[0]);
        if (!f.exists())
        {
            exitShowMessage();
        }
        
        Subs4me as = Subs4me.getInstance();
        
     // Load the sub4me-default.properties file
        if (!PropertiesUtil.setPropertiesStreamName("./properties/subs4me-default.properties")) {
            return;
        }

        // Load the user properties file "moviejukebox.properties"
        // No need to abort if we don't find this file
        // Must be read before the skin, because this may contain an override skin
        PropertiesUtil.setPropertiesStreamName(propertiesName);
        LinkedList<String> providers = null;
        
        providers = initProperties();
        
        for (int i = 1; i < args.length; i++)
        {
            String arg = args[i];
            if (arg.equals(SRT_EXISTS))
            {
                checkSrtExists = true;
            } else if (arg.equals(RECURSIVE_SEARCH))
            {
                setRecursive(true);
            }
            else if (arg.equals(FULL_DOWNLOAD))
            {
                fullDownload = true;
            }
            else if (arg.startsWith(PROVIDERS))
            {
                providers = parseProviderNames(arg);
            }
        }
        as.initProviders(providers);
        StringBuilder sb = new StringBuilder();
        for (Iterator iterator = _providers.iterator(); iterator.hasNext();)
        {
            Provider p = (Provider) iterator.next();
            sb.append(p.getName() + ",");
        }
        System.out.println("Subs4me version " + VERSION);
        System.out.println("        selected providers are:" + sb.toString());
        System.out.println("        check recursively = " + isRecursive());
        System.out.println("        do not check if srt exists = " + checkSrtExists);
        System.out.println("        download everything = " + isFullDownload());
        as.startProcessingFiles(args[0]);
        
        System.out.println("******* Thanks for using subs4me, hope you enjoy the results *******");
    }
    
    private static LinkedList<String> initProperties()
    {
        LinkedList<String> providers = null;
        
        String pp = PropertiesUtil.getProperty(PROVIDERS_PROPEERTY, "opensubs,sratim,torec");
        if (pp != null)
        {
            providers = parseProviderNames(pp);
        }
        
        String subCheck = PropertiesUtil.getProperty(SUBS_CHECK_ALL_PROPEERTY, "true");
        if (subCheck != null)
        {
            checkSrtExists = subCheck.equalsIgnoreCase("true");
        }
        
        String subRecursive = PropertiesUtil.getProperty(SUBS_RECURSIVE_PROPEERTY, "true");
        if (subRecursive != null)
        {
            setRecursive(subRecursive.equalsIgnoreCase("true"));
        }
        
        String subGetAll = PropertiesUtil.getProperty(SUBS_GET_ALL_PROPEERTY, "true");
        if (subGetAll != null)
        {
            fullDownload = subGetAll.equalsIgnoreCase("true");
        }
        
        return providers;
    }

    private static LinkedList<String> parseProviderNames(String providers)
    {
        if (providers == null || providers.isEmpty())
        {
            return null;
        }
        
        LinkedList<String> ret = new LinkedList<String>();
        String[] pros = null;
        if (providers.startsWith(PROVIDERS))
        {
            pros = providers.substring(PROVIDERS.length()+1).split(",");
        }
        else
        {
            pros = providers.split(",");
        }
        for (int j = 0; j < pros.length; j++)
        {
            String p = pros[j];
            ret.add(p);
        }
        
        return ret;
    }

    /**
     * 
     */
    public static void exitShowMessage()
    {
        StringBuffer sb = new StringBuffer("Usage: subs4me \"[file]\" | \"[directory]\" [/params]");
        sb.append("\nVersion ");
        sb.append(VERSION);
        sb.append("\n");
        sb.append("Example:\n");
        sb.append("\tsubs4me \"C:\\movies\" /r /all \n\n");
        sb.append("Params:\n");
        sb.append("  c: If an srt file exists do not try to get the subtitels for this file\n");
        sb.append("  r: Recurse over all the files in all the directories\n");
        sb.append("  p: select providers, /p=torec,opensubs will select these two providers\n     (order is important), default is opensubs,sratim,torec \n");
        sb.append("     Currently supporting: torec, opensubs, sratim, subscene\n");
        sb.append("  all: Download all the subtitles for this title and unzip with the above schema\n");
        sb.append("  n: do not use opensubs to validate actual movie name (use google only)\n");
        sb.append("\nCreated by ilank\nEnjoy...");
        System.out.println(sb.toString());
        System.exit(-1);
    }
    
    public static void registerProvider(Provider provider)
    {
        if (_availableProviders.contains(provider))
        {
            return;
        }
        _availableProviders.add(provider);
    }
}
