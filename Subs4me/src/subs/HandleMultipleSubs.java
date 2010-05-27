package subs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import subs.providers.Sratim;
import subs.providers.Torec;
import utils.FileStruct;
import utils.PropertiesUtil;
import utils.Utils;

public class HandleMultipleSubs
{
    public static final String RECURSIVE_SEARCH = "/r";
    public static final String VERSION = "0.6";
    public static final String RECURSIVE_SEARCH_PROPEERTY = "handle_multipule_subtitles_recursive";
    public static final String handle_multipule_subtitles_default_directories = "handle_multipule_subtitles_default_directories";
    
    private static boolean recursive = false;
    private FileStruct currentFile;
    
    public HandleMultipleSubs(List<String> srcs)
    {
        PropertiesUtil.setPropertiesStreamName("./properties/subs4me-default.properties");
        PropertiesUtil.setPropertiesStreamName(Subs4me.propertiesName);
        processFiles(srcs);
    }

    private void processFiles(List<String> srcs)
    {
        for (String src : srcs)
        {
            String[] sources = findFilesInDir(src);
            List<String> sourcesList = null;
            if (sources != null)
            {
                sourcesList = Arrays.asList(sources);
            }
            
            if (sources == null)
            {
                // this is a file and not a directory
                File fi = new File(src);
                // String f = fi.getName();
                try
                {
                    doWork(fi);
                } catch (Exception e)
                {
                    System.err.println(" ****** HandleMultipleSubs error handling:" + fi.getName());
                    e.printStackTrace();
                }
            } else
            {
                for (String source : sourcesList)
                {
                    File f = new File(src + File.separator + source);
                    if (f.isDirectory())
                    {
                        List<String> lst = new LinkedList<String>();
                        lst.add(f.getPath());
                        processFiles(lst);
                    } else
                    {
                        try
                        {
                            doWork(f);
                        } catch (Exception e)
                        {
                            System.err.println(" ****** HandleMultipleSubs error handling:" + f.getName());
                            e.printStackTrace();
                        }
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
                    if (name.endsWith(Subs4me.DO_WORK_EXT))
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

    private void doWork(File file)
    {
        currentFile = new FileStruct(file, false);
        String f = currentFile.getNameNoExt();
        String[] files = findSrtFilesInDir(currentFile.getFile().getParent());
        LinkedList<ProviderResult> pResults = loadDoWorkFile();
        if (files.length == 0 && pResults == null)
        {
            if (currentFile.getExt().equals(Subs4me.DO_WORK_EXT.substring(1)))
            {
                Utils.deleteFile(currentFile.getFile());
            }
            return;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Found ");
        sb.append(currentFile.getFile().getParent() + "\\" + currentFile.getFullNameNoExt());
        sb.append("\nPlease select one of the following:\n");
        sb.append("   Enter 0 if you see no correct option and want to cleanup all the files\n");
        for (int i = 0; i < files.length; i++)
        {
            String fNmae = files[i];
            sb.append("   Enter ");
            sb.append(i+1);
            sb.append(" for ");
            sb.append(fNmae.substring(currentFile.getNameNoExt().length() + 5));
            sb.append("\n");
        }
        if (pResults != null)
        {
            for (int j = 0; j < pResults.size(); j++)
            {
                sb.append("   Enter ");
                sb.append( j + files.length +1);
                sb.append(" for p:");
                sb.append(pResults.get(j).getProviderName());
                sb.append(" - ");
                sb.append(pResults.get(j).getDestFileName());
                sb.append("\n");
            }
        }
        
        System.out.println(sb.toString());
     // get their input
        int sel = -1;
        while (sel == -1 || sel > (files.length + (pResults == null ? 0 : pResults.size())))
        {
            Scanner scanner = new Scanner(System.in);
            // there are several ways to get the input, this is 
            // just one approach
            String selection = scanner.nextLine();
            try
            {
                sel = Integer.parseInt(selection);
            } 
            catch (NumberFormatException e)
            {
            }
            if (sel == -1 || sel > files.length + (pResults == null ? 0 : pResults.size()))
            {
                System.out.println("Your selection \"" + selection + "\" is not a valid option, plesae select again...\n");
                System.out.println(sb.toString());
            }
        }

        switch(sel)
        {
            case 0:
                System.out.println("You chose " + sel + ", cleaning up\n");
                cleanup(files);
                break;
            case -1:
                break;
            default:
                boolean ok = false;
                System.out.println("You chose " + sel + ", renaming \"" + pResults.get(sel-1).getDestFileName() + "\" to \"" + currentFile.getNameNoExt() + ".srt\"\n");
                
                if (sel > files.length)
                {
                    ProviderResult p = pResults.get(sel - files.length -1);
                    if (p.getProviderName().equalsIgnoreCase(Sratim.getInstance().getName()))
                    {
                        Sratim.getInstance().downloadFile(p.getFileURL(), p.getDestFileName(), currentFile);
                        Utils.deleteFile(currentFile.getFile());
                    }
                    else if (p.getProviderName().equalsIgnoreCase(Torec.getInstance().getName()))
                    {
                        Torec.getInstance().downloadFile(p.getFileURL(), p.getDestFileName(), currentFile);
                        Utils.deleteFile(currentFile.getFile());
                    }
                    
                    for (int i = 0; i < files.length; i++)
                    {
                        String delName = files[i];
                        File del = new File(currentFile.getFile().getParent(), delName);
                        Utils.deleteFile(del);
                    }
                }
                else
                {
                    File ff = new File(currentFile.getFile().getParent(), files[sel-1]);
                    File dest = new File(currentFile.getFile().getParent(), currentFile.getFullNameNoExt() + ".srt");
                    ff.renameTo(dest);
                    Utils.deleteFile(currentFile.getFile());
//                    if (!currentFile.isVideoFile())
//                    {
//                        currentFile.getFile().delete();
//                    }
                    
                    for (int i = 0; i < files.length; i++)
                    {
                        String delName = files[i];
                        if (i == sel-1)
                            continue;
                        File del = new File(currentFile.getFile().getParent(), delName);
                        Utils.deleteFile(del);
//                        del.delete();
                    }
                }
                
                //cleanup dowrok file
                if (!currentFile.getExt().equals(Subs4me.DO_WORK_EXT.substring(1)))
                {
                    File del = new File(currentFile.getFile().getParent(), currentFile.getFullNameNoExt() + Subs4me.DO_WORK_EXT);
                    Utils.deleteFile(del);
//                    del.delete();
                }
                break;
        }
    }
    
    private void cleanup(String[] files)
    {
        Utils.deleteFile(currentFile.getFile());
//        if (!currentFile.isVideoFile())
//        {
//            currentFile.getFile().delete();
//        }
        
        for (int i = 0; i < files.length; i++)
        {
            String delName = files[i];
            File del = new File(currentFile.getFile().getParent(), delName);
            Utils.deleteFile(del);
//            del.delete();
        }
        
        //cleanup dowrok file
        if (!currentFile.getExt().equals(Subs4me.DO_WORK_EXT.substring(1)))
        {
            File del = new File(currentFile.getFile().getParent(), currentFile.getFullNameNoExt() + Subs4me.DO_WORK_EXT);
            Utils.deleteFile(del);
//            del.delete();
        }
    }
    
    private String[] findSrtFilesInDir(String src)
    {
        File dir = new File(src);
        FilenameFilter filter = new FilenameFilter()
        {
            public boolean accept(File dir, String name)
            {
                if (name.startsWith(currentFile.getNameNoExt()+".srt") 
                        && name.endsWith(".srt")
                        && (name.length() > (currentFile.getNameNoExt()+".srt").length()))
                {
                    return true;
                }
                return false;
            }
        };
        return dir.list(filter);
    }
    
    public LinkedList<String> findOptions()
    {
        
        return null;
    }
    
    public static boolean isRecursive()
    {
        return recursive;
    }

    public static void setRecursive(boolean recursive)
    {
        HandleMultipleSubs.recursive = recursive;
    }
    
    public static void main(String[] args)
    {
//        if (args.length < 1)
//        {
//            exitShowMessage();
//        }
        
        // Load the sub4me-default.properties file
        if (!PropertiesUtil.setPropertiesStreamName("./properties/subs4me-default.properties")) {
            return;
        }

        // Load the user properties file "moviejukebox.properties"
        // No need to abort if we don't find this file
        // Must be read before the skin, because this may contain an override skin
        PropertiesUtil.setPropertiesStreamName(Subs4me.propertiesName);
        initProperties();
        List<String> destinations = new LinkedList<String>();
        for (int i = 0; i < args.length; i++)
        {
            String arg = args[i];
            if (arg.equals(RECURSIVE_SEARCH))
            {
                setRecursive(true);
            }
            else
            {
                destinations.add(arg);
            }
        }
        
        String defDirs = PropertiesUtil.getProperty(handle_multipule_subtitles_default_directories, null);
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
        
        StringBuilder sbMsg = new StringBuilder("       *** HandleMultipleSubs version ");
        sbMsg.append(VERSION);
        sbMsg.append("\n");
        sbMsg.append("   looking in: ");

        for (Iterator iterator = destinations.iterator(); iterator.hasNext();)
        {
            String dst = (String) iterator.next();
            sbMsg.append(dst + " ");
        }
        sbMsg.append("\n");
//        System.out.println("       *** HandleMultipleSubs version " + VERSION);
        System.out.println("       *** HandleMultipleSubs recursive = " + recursive);
        new HandleMultipleSubs(destinations);
        System.out.println("******* Thanks for using HandleMultipleSubs *******");
    }
    
    private static void initProperties()
    {
        String handleRecursive = PropertiesUtil.getProperty(RECURSIVE_SEARCH_PROPEERTY, "true");
        if (handleRecursive != null)
        {
            setRecursive(handleRecursive.equalsIgnoreCase("true"));
        }
    }
    
    private static void exitShowMessage()
    {
        StringBuffer sb = new StringBuffer("Usage: HandleMultipleSubs \"[file]\" | \"[directory]\" [/params]\n");
        sb.append(" Can accept more than one file/directory as destination paramater, all must be VALID\n");
        sb.append("Version ");
        sb.append(VERSION);
        sb.append("\n");
        sb.append("Example:\n");
        sb.append("\tHandleMultipleSubs \"c:\\movies\" \"c:\\movies2\" \n\n");
        sb.append("Params:\n");
//        sb.append("  chksrtexists: If an srt file exists do not try to get the subtitels for this file\n");
        sb.append("  r: Recurse over all the files in all the directories\n");
////        sb.append("  intense: Download all the subs that correspond to the same group, uzip, and rename to be: original_fileName.zip entry.srt\n");
//        sb.append("  all: Download all the subtitles for this title and unzip with the above schema\n");
        sb.append("\nCreated by ilank\nEnjoy...");
        System.out.println(sb.toString());
        System.exit(-1);
    }
    
    private LinkedList<ProviderResult> loadDoWorkFile()
    {
        LinkedList<ProviderResult> ret = new LinkedList<ProviderResult>();
        File f = new File(currentFile.getFile().getParent(), currentFile.getFullNameNoExt() + Subs4me.DO_WORK_EXT);
        if (!f.exists())
            return null;
        
        try
        {
            BufferedReader in = new BufferedReader(new FileReader(f));
            String str;
            while ((str = in.readLine()) != null) 
            {
                if (str.startsWith("session"))
                    continue;
                String[] split = str.split(", ");
                ProviderResult p = new ProviderResult(split[0], split[1], split[2]);
                ret.add(p);
            }
            in.close();
            return ret;
        }
        catch (FileNotFoundException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return null;
    }
}
