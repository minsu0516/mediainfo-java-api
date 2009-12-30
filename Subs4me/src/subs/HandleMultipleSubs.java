package subs;

import java.io.File;
import java.io.FilenameFilter;
import java.util.LinkedList;
import java.util.Scanner;

import utils.FileStruct;

public class HandleMultipleSubs
{
    public static final String RECURSIVE_SEARCH = "/r";
    public static final String DO_WORK_EXT = "dowork";
    public static final String VERSION = "0.5";
    
    private static boolean recursive = false;
    private FileStruct currentFile;
    
    public HandleMultipleSubs(String srcDirOrFile)
    {
        processFiles(srcDirOrFile);
    }

    private void processFiles(String src)
    {
        String[] sources = findFilesInDir(src);
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
            for (int j = 0; j < sources.length; j++)
            {
                File f = new File(src + File.separator + sources[j]);
                if (f.isDirectory())
                {
                    processFiles(f.getPath());
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
                    if (name.endsWith(".dowork"))
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
        if (files.length == 0)
        {
            if (currentFile.getExt().equals(DO_WORK_EXT))
            {
                currentFile.getFile().delete();
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
        System.out.println(sb.toString());
     // get their input
        int sel = -1;
        while (sel == -1 || sel > files.length)
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
            if (sel == -1 || sel > files.length)
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
                System.out.println("You chose " + sel + ", renaming file to " + files[sel-1].substring(currentFile.getNameNoExt().length() + 5) + "\n");
                File ff = new File(currentFile.getFile().getParent(), files[sel-1]);
                File dest = new File(currentFile.getFile().getParent(), currentFile.getFullNameNoExt() + ".srt");
                ff.renameTo(dest);
                if (!currentFile.isVideoFile())
                {
                    currentFile.getFile().delete();
                }
                
                for (int i = 0; i < files.length; i++)
                {
                    String delName = files[i];
                    if (i == sel-1)
                        continue;
                    File del = new File(currentFile.getFile().getParent(), delName);
                    del.delete();
                }
                
                //cleanup dowrok file
                if (!currentFile.getExt().equals("dowork"))
                {
                    File del = new File(currentFile.getFile().getParent(), currentFile.getFullNameNoExt() + ".dowork");
                    del.delete();
                }
                break;
        }
    }
    
    private void cleanup(String[] files)
    {
        if (!currentFile.isVideoFile())
        {
            currentFile.getFile().delete();
        }
        
        for (int i = 0; i < files.length; i++)
        {
            String delName = files[i];
            File del = new File(currentFile.getFile().getParent(), delName);
            del.delete();
        }
        
        //cleanup dowrok file
        if (!currentFile.getExt().equals("dowork"))
        {
            File del = new File(currentFile.getFile().getParent(), currentFile.getFullNameNoExt() + ".dowork");
            del.delete();
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
        if (args.length < 1)
        {
            exitShowMessage();
        }
        
        for (int i = 1; i < args.length; i++)
        {
            String arg = args[i];
            if (arg.equals(RECURSIVE_SEARCH))
            {
                setRecursive(true);
            }
        }
        
        File f = new File(args[0]);
        if (!f.exists())
        {
            exitShowMessage();
        }
        System.out.println("       *** HandleMultipleSubs version " + VERSION);
        new HandleMultipleSubs(args[0]);
        System.out.println("******* Thanks for using HandleMultipleSubs *******");
    }
    
    private static void exitShowMessage()
    {
        StringBuffer sb = new StringBuffer("Usage: HandleMultipleSubs \"[file]\" | \"[directory]\" [/params]");
        sb.append("\nVersion ");
        sb.append(VERSION);
        sb.append("\n");
        sb.append("Example:\n");
        sb.append("\tHandleMultipleSubs \"C:\\movies\" \n\n");
        sb.append("Params:\n");
//        sb.append("  chksrtexists: If an srt file exists do not try to get the subtitels for this file\n");
        sb.append("  r: Recurse over all the files in all the directories\n");
////        sb.append("  intense: Download all the subs that correspond to the same group, uzip, and rename to be: original_fileName.zip entry.srt\n");
//        sb.append("  all: Download all the subtitles for this title and unzip with the above schema\n");
        sb.append("\nCreated by ilank\nEnjoy...");
        System.out.println(sb.toString());
        System.exit(-1);
    }
}
