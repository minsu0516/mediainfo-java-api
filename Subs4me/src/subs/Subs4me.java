package subs;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import subs.providers.OpenSubs;
import subs.providers.Torec;

public class Subs4me
{
    public static final String SRT_EXISTS = "/c";
    public static final String VERSION = "0.8.3";
    public static final String RECURSIVE_SEARCH = "/r";
    public static final String FULL_DOWNLOAD = "/all";
    public static final String PROVIDERS = "/p";
    
    String srcDir = new String();
    // private String _group = "";
    public static boolean checkSrtExists = false;
    private static boolean recursive = false;
//    private static boolean intense = false;
    private static boolean fullDownload = false;
    
    private static LinkedList<Provider> availableProviders = new LinkedList<Provider>();
    
    private static LinkedList<Provider> providers = null;
    
    private static Subs4me instance = new Subs4me();
    
    public static Subs4me getInstance()
    {
        return instance;
    }
    
    public Subs4me()
    {
    }

    /**
     * need to be recursive to process sub directories
     * 
     * @param src
     */
    public void startProcessingFiles(String src)
    {
        for (Iterator iterator = providers.iterator(); iterator.hasNext();)
        {
            Provider p = (Provider) iterator.next();
            String[] sources = findFilesInDir(src);
            if (sources == null)
            {
                // this is a file and not a directory
                File fi = new File(src);
                // String f = fi.getName();
                p.doWork(fi);
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
                        p.doWork(f);
                    }
                }
            }
        }
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
        new Torec();
        new OpenSubs();
        
        providers = new LinkedList<Provider>();
        if (provNames == null)
        {
            providers.add(getProvider("opensubs"));
            providers.add(getProvider("torec"));
        }
        else
        {
            for (Iterator iterator = provNames.iterator(); iterator.hasNext();)
            {
                String p = (String) iterator.next();
                Provider prov = getProvider(p);
                if (prov != null)
                {
                    providers.add(prov);
                }
            }
        }
    }
    
    private Provider getProvider(String name)
    {
        for (Iterator iterator2 = availableProviders.iterator(); iterator2.hasNext();)
        {
            Provider availProv = (Provider) iterator2.next();
            if (availProv.getName().equals(name))
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
        
        LinkedList<String> providers = null;
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
                providers = new LinkedList<String>();
                String[] pros = arg.substring(PROVIDERS.length()+1).split(",");
                for (int j = 0; j < pros.length; j++)
                {
                    String p = pros[j];
                    providers.add(p);
                }
            }
        }
        Subs4me as = Subs4me.getInstance();
        as.initProviders(providers);
        as.startProcessingFiles(args[0]);
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
        sb.append("\tautosubs \"C:\\movies\" /r /all \n\n");
        sb.append("Params:\n");
        sb.append("  c: If an srt file exists do not try to get the subtitels for this file\n");
        sb.append("  r: Recurse over all the files in all the directories\n");
        sb.append("  p: select providers, /p=torec,opensubs will select these two providers, default is opensubs,torec \n");
        sb.append("     Currently supporting: torec, opensubs, subscene");
//        sb.append("  intense: Download all the subs that correspond to the same group, uzip, and rename to be: original_fileName.zip entry.srt\n");
        sb.append("  all: Download all the subtitles for this title and unzip with the above schema\n");
        sb.append("\nCreated by ilank\nEnjoy...");
        System.out.println(sb.toString());
        System.exit(-1);
    }
    
    public static void registerProvider(Provider provider)
    {
        if (availableProviders.contains(provider))
        {
            return;
        }
        availableProviders.add(provider);
    }
}

