package subs;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import subs.providers.OpenSubs;
import subs.providers.Sratim;
import subs.providers.Subscene;
import subs.providers.Torec;
import utils.FileStruct;
import utils.PropertiesUtil;
import utils.Utils;

public class Subs4me
{
    public static final String SRT_EXISTS = "/c";
    public static final String VERSION = "1.1.3";
    public static final String SUBS4ME_IGNORE_FILE_NAME = "ignore_directory.subs4me";
    
    public static final String RECURSIVE_SEARCH = "/r";
    public static final String FULL_DOWNLOAD = "/all";
    public static final String PROVIDERS = "/p";
    public static final String GET_MOVIE_PIC = "/i";
    public static final String GRT_MOVIE_PIC_FORCED = "/if";
    public static final String DO_NOT_USE_OPENSUBS_FOR_FILE_REALIZATION = "/n";
    public static final String USE_PARENT_DIRNAME_AS_NAME = "/useDirName";
    public static final String USE_EXIT_CODES = "/useExitCodes";
    public static final String USE_HD_IN_NAMES = "/hd";
    public static final String SHOW_HELP = "-?";
    public static final String DO_WORK_EXT = ".run_HandleMultiplesubs";
    
    public static final String PROVIDERS_PROPERTY = "get_subs_providers";
    public static final String SUBS_CHECK_ALL_PROPERTY = "get_subs_check_exists";
    public static final String SUBS_RECURSIVE_PROPERTY = "get_subs_recursive";
    public static final String SUBS_GET_ALL_PROPERTY = "get_subs_all";
    public static final String SUBS_DOWNLOAD_PICTURE_PROPERTY = "get_subs_download_movie_picture";
    public static final String SUBS_DOWNLOAD_PICTURE_FORCE_PROPERTY = "get_subs_download_movie_picture_force";
    public static final String SUBS_USE_PARENT_DIRNAME_AS_NAME_PROPERTY = "get_subs_use_parent_dirname_as_moviename";
    public static final String get_subs_default_directories = "get_subs_default_directories";
    public static final String use_opensubs_for_file_name_realization = "use_opensubs_for_file_name_realization";
    public static final String get_subs_always_use_hd_in_names = "get_subs_always_use_hd_in_names";
    
    public static String SESSION_ID = "";
    
    public LinkedList<String> oneSubsFound = new LinkedList<String>();
    public LinkedList<String> moreThanOneSubs = new LinkedList<String>();
    public LinkedList<String> noSubs = new LinkedList<String>();
    
    String srcDir = new String();
    // private String _group = "";
    public static boolean checkSrtExists = false;
    private static boolean recursive = false;
//    private static boolean intense = false;
    private static boolean fullDownload = false;
    private static boolean getMoviePic = false;
    private static boolean getMoviePicForce = false;
    private static boolean _useParentDirAsName = false;
    
    public static String propertiesName = "./subs4me.properties";
    
    private static LinkedList<Provider> _availableProviders = new LinkedList<Provider>();
    
    public static LinkedList<Provider> _providers = null;
    
    private static Subs4me instance = new Subs4me();
    public static boolean dontUseOpenSubsForNameSearch;
    public static boolean _alwaysSearchForHDNmaes;
    private static boolean _useExitCodes = false;
    
    public static Subs4me getInstance()
    {
        return instance;
    }
    
    public Subs4me()
    {
//        System.setProperty ("sun.net.client.defaultReadTimeout", "22000");
//        System.setProperty ("sun.net.client.defaultConnectTimeout", "22000");
//        System.out.println("Locale = " + Locale.getDefault());
//        Locale.setDefault(new Locale("en", "US"));
//        System.out.println("New Locale = " + Locale.getDefault());
        SESSION_ID = Long.toString(System.currentTimeMillis());
    }

    /**
     * need to be recursive to process sub directories
     * 
     * @param srcs directry/file list to work on
     */
    public void startProcessingFiles(List<String> srcs)
    {
        for (String src : srcs)
        {
            String[] sources = findFilesInDir(src);
            if (sources == null)
            {
                // this is a file and not a directory
                File fi = new File(src);
                doWork(fi);
            } else
            {
                for (int j = 0; j < sources.length; j++)
                {
                    File fi = new File(src + File.separator + sources[j]);
                    if (fi.isDirectory())
                    {
                        List<String> lst = new LinkedList<String>();
                        lst.add(fi.getPath());
                        startProcessingFiles(lst);
                    } else
                    {
                        doWork(fi);
                    }
                }
            }
        }
    }
    
    /**
     * iterate over providers to get the subs
     * @param fi file to work on
     * @return Providre values to help print the list of files to which we found subtitles
     */
    private void doWork(File fi)
    {
        int ret = Provider.not_found;
        FileStruct fs = null;
//        fs = new FileStruct(fi, false, true);
        if (_useParentDirAsName)
        {
            fs = new FileStruct(fi, true, true);
        }
        else
        {
            fs = new FileStruct(fi);
        }
        for (Iterator iterator = _providers.iterator(); iterator.hasNext();)
        {
            Provider p = (Provider) iterator.next();
            try
            {
                int retTemp = p.doWork(fs);
                if (retTemp > ret)
                {
                    ret = retTemp;
                }
                if (retTemp == Provider.perfect)
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
        
        if (ret == Provider.perfect)
        {
            oneSubsFound.add(fs.getSrcDir() + File.separator + fs.getFullFileName());
        }
        else if (ret == Provider.not_perfect)
        {
            moreThanOneSubs.add(fs.getSrcDir() + File.separator + fs.getFullFileName());
        }
        else if (ret == Provider.not_found)
        {
            noSubs.add(fs.getSrcDir() + File.separator + fs.getFullFileName());
        }
//        return ret;
    }
    
    private void cleanup(File f)
    {
        FileStruct fs = new FileStruct(f, false);
        String[] files = findFilesTocleanupInDir(fs);
        for (int i = 0; i < files.length; i++)
        {
            String delName = files[i];
            File del = new File(f.getParent(), delName);
            Utils.deleteFile(del);
//            del.delete();
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
        final Pattern p1 = Pattern.compile(".*([.].*$)");
        final Pattern samplePattern = Pattern.compile("(?i)\\bsample\\b");
        if (dir.isDirectory())
        {
            FilenameFilter filter = new FilenameFilter()
            {
                public boolean accept(File dir, String name)
                {
                    File parent = new File(dir, name);
                    if (isRecursive() && parent.isDirectory())
                    {
                        File dont = new File(parent, SUBS4ME_IGNORE_FILE_NAME);
                        if (dont.exists())
                            return false;
                        
                        return true;
                    }
                    //NO SAMPLE FILES!!!!
                    
                    File dont = new File(parent.getParentFile(), SUBS4ME_IGNORE_FILE_NAME);
                    if (dont.exists())
                        return false;
                    
                    Matcher m1 = samplePattern.matcher(name);
                    if (m1.find())
                    {
                        return false;
                    }
                    if (name.endsWith("mkv") || name.endsWith("avi"))
                    {
                        if (checkSrtExists)
                        {
                            m1 = p1.matcher(name);
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
                        System.out.println("Trying to init Sratim, this may take some time");
                        if (!Sratim.getInstance().loadSratimCookie())
                        {
                            Login login = new Login();
                            if (!login.isLoginOk())
                            {
                                System.exit(-1);
                            }
                        }
                        System.out.println("Sratim works OK");
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
//        if (args.length < 1)
//        {
//            exitShowMessage();
//        }
        
        Subs4me as = Subs4me.getInstance();
        LinkedList<String> providers = null;
        providers = initProperties();
        List<String> destinations = new LinkedList<String>();
        for (int i = 0; i < args.length; i++)
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
            else if (arg.startsWith(PROVIDERS + "="))
            {
                providers = parseProviderNames(arg);
            }
            else if (arg.startsWith(DO_NOT_USE_OPENSUBS_FOR_FILE_REALIZATION))
            {
                dontUseOpenSubsForNameSearch = true;
            }
            else if (arg.startsWith(GET_MOVIE_PIC))
            {
                getMoviePic = true;
            }
            else if (arg.startsWith(GRT_MOVIE_PIC_FORCED))
            {
                getMoviePicForce = true;
            }
            else if (arg.equals(USE_EXIT_CODES))
            {
                _useExitCodes  = true;
            }
            else if (arg.equals(USE_PARENT_DIRNAME_AS_NAME))
            {
                _useParentDirAsName = true;
            }
            else if (arg.equals(SHOW_HELP))
            {
                exitShowMessage();
            }
            else
            {
                destinations.add(arg);
            }
        }
        String defDirs = PropertiesUtil.getProperty(get_subs_default_directories, null);
        if (destinations.size() == 0 && defDirs != null)
        {
            String[] dests = defDirs.split(",");
            for (int i = 0; i < dests.length; i++)
            {
                String dst = dests[i];
                destinations.add(dst.trim());
            }
        }
        for (String dst : destinations)
        {
            File f = new File(dst);
            if (!f.exists())
            {
                exitShowMessage();
            }
        }
        
        StringBuilder sb = new StringBuilder();
        for (Iterator iterator = providers.iterator(); iterator.hasNext();)
        {
            String p = (String) iterator.next();
            sb.append(p + ",");
        }
        StringBuilder sbMsg = new StringBuilder("Subs4me version ");
        sbMsg.append(VERSION);
        sbMsg.append("\n");
        sbMsg.append("   looking in: ");

        for (Iterator iterator = destinations.iterator(); iterator.hasNext();)
        {
            String dst = (String) iterator.next();
            sbMsg.append(dst + " ");
        }
        sbMsg.append("\n");
        sbMsg.append("        selected providers are:");
        sbMsg.append(sb.toString());
        sbMsg.append("\n");
        sbMsg.append("        check recursively = ");
        sbMsg.append(isRecursive());
        sbMsg.append("\n");
        sbMsg.append("        do not check if srt exists = ");
        sbMsg.append(checkSrtExists);
        sbMsg.append("\n");
        sbMsg.append("        download everything = ");
        sbMsg.append(isFullDownload());
        sbMsg.append("\n");
        sbMsg.append("        check movie name using opensubs first = ");
        sbMsg.append(!dontUseOpenSubsForNameSearch);
        sbMsg.append("\n");
        sbMsg.append("        get movie picture = ");
        sbMsg.append(getMoviePic);
        sbMsg.append(", (forced = ");
        sbMsg.append(getMoviePicForce);
        sbMsg.append(")");
        sbMsg.append("\n");
        sbMsg.append("        use parent directory as name = ");
        sbMsg.append(_useParentDirAsName);
        sbMsg.append("\n");
        sbMsg.append("        use exit codes = ");
        sbMsg.append(_useExitCodes);
        sbMsg.append("\n");
        System.out.println(sbMsg);
        as.initProviders(providers);
        
        as.startProcessingFiles(destinations);
        
        if (as.oneSubsFound.size() > 0)
        {
            StringBuilder sbExact = new StringBuilder("\n********************** found exact matches **********************************************");
            sbExact.append("\nExact matches were found for:");
            for (Iterator iterator = as.oneSubsFound.iterator(); iterator.hasNext();)
            {
                String src = (String) iterator.next();
                sbExact.append("\n");
                sbExact.append(src);
            }
            System.out.println(sbExact.toString());
        }
        
        if (as.moreThanOneSubs.size() > 0)
        {
            StringBuilder sbExact = new StringBuilder("\n************** found inexact matches, run HandleMultipleSubs ****************************");
            sbExact.append("\nPlease run HandleMultipleSubs on:");
            for (Iterator iterator = as.moreThanOneSubs.iterator(); iterator.hasNext();)
            {
                String src = (String) iterator.next();
                sbExact.append("\n");
                sbExact.append(src);
            }
            System.out.println(sbExact.toString());
        }
        if (as.noSubs.size() > 0)
        {
            StringBuilder sbExact = new StringBuilder("\n********************* no subs were found ************************************************");
            sbExact.append("\nNo subs were found for:");
            for (Iterator iterator = as.noSubs.iterator(); iterator.hasNext();)
            {
                String src = (String) iterator.next();
                sbExact.append("\n");
                sbExact.append(src);
            }
            System.out.println(sbExact.toString());
        }
        if (as.moreThanOneSubs.size() > 0 || as.oneSubsFound.size() > 0)
        {
            System.out.println("*****************************************************************************************");
        }
        System.out.println("******* Thanks for using subs4me, hope you enjoy the results *******");
        
        if (_useExitCodes)
        {
            if (as.oneSubsFound.size() > 0
                       && as.moreThanOneSubs.size() ==0
                       && as.noSubs.size() == 0)
            {
                System.out.println("existing code 0");
                System.exit(0);
            }
    
            if (as.moreThanOneSubs.size() >0 
                    && as.noSubs.size() == 0)
            {
                System.out.println("existing code 1");
                System.exit(1);
            }
            System.out.println("existing code 2");
            System.exit(2); 
        }
    }
    
    public static LinkedList<String> initProperties()
    {
        // Load the sub4me-default.properties file
        if (!PropertiesUtil.setPropertiesStreamName("./properties/subs4me-default.properties")) {
            System.exit(-1);
        }

        // Load the user properties file "moviejukebox.properties"
        // No need to abort if we don't find this file
        // Must be read before the skin, because this may contain an override skin
        PropertiesUtil.setPropertiesStreamName(propertiesName);
        
        LinkedList<String> providers = null;
        
        String value = PropertiesUtil.getProperty(PROVIDERS_PROPERTY, "opensubs,sratim,torec");
        if (value != null)
        {
            providers = parseProviderNames(value);
        }
        
        value = PropertiesUtil.getProperty(SUBS_CHECK_ALL_PROPERTY, "true");
        if (value != null)
        {
            checkSrtExists = value.equalsIgnoreCase("true");
        }
        
        value = PropertiesUtil.getProperty(SUBS_RECURSIVE_PROPERTY, "true");
        if (value != null)
        {
            setRecursive(value.equalsIgnoreCase("true"));
        }
        
        value = PropertiesUtil.getProperty(SUBS_GET_ALL_PROPERTY, "true");
        if (value != null)
        {
            fullDownload = value.equalsIgnoreCase("true");
        }
        
        value = PropertiesUtil.getProperty(SUBS_DOWNLOAD_PICTURE_PROPERTY, "true");
        if (value != null)
        {
            getMoviePic = value.equalsIgnoreCase("true");
        }
        
        value = PropertiesUtil.getProperty(SUBS_DOWNLOAD_PICTURE_FORCE_PROPERTY, "true");
        if (value != null)
        {
            getMoviePicForce = value.equalsIgnoreCase("true");
        }
        
        value = PropertiesUtil.getProperty(SUBS_USE_PARENT_DIRNAME_AS_NAME_PROPERTY, "false");
        if (value != null)
        {
            _useParentDirAsName = value.equalsIgnoreCase("true");
        }
        
        value = PropertiesUtil.getProperty(use_opensubs_for_file_name_realization, "true");
        if (value != null)
        {
            dontUseOpenSubsForNameSearch = value.equalsIgnoreCase("false");
        }
        
        value = PropertiesUtil.getProperty(get_subs_always_use_hd_in_names, "false");
        if (value != null)
        {
            _alwaysSearchForHDNmaes = value.equalsIgnoreCase("false");
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
        sb.append(" Can accept more than one file/directory as destination paramater, all must be VALID\n");
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
        sb.append("  i[f]: get the image file for the movie\n");
        sb.append("         f = force getting the movie image (refresh current)\n");
        sb.append("  " + USE_PARENT_DIRNAME_AS_NAME);
        sb.append(": use parents dir name as name for movie, will not work at all if there are 2 movies at the same directry\n");
        sb.append("  useExitCodes: when getsubs ends, give an exit code\n");
        //sb.append("  hd: always look for hd ref in names (480P, 720P, 1080P) and only give those options\n");
        sb.append("/?: this help file\n");
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
    
    public static boolean shouldGetPic()
    {
        return getMoviePic;
    }
    
    public static boolean shouldForceGetPic()
    {
        return getMoviePicForce;
    }
    
    public static boolean shouldUseParentDirName()
    {
        return _useParentDirAsName;
    }
}
