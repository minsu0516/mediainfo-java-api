package subs.providers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.filebot.ui.panel.subtitle.MemoryFile;
import net.sourceforge.filebot.ui.panel.subtitle.RarArchive;
import net.sourceforge.filebot.ui.panel.subtitle.ZipArchive;
import net.sourceforge.filebot.web.SearchResult;
import net.sourceforge.filebot.web.SeasonOutOfBoundsException;
import net.sourceforge.filebot.web.SubsceneSubtitleClient;
import net.sourceforge.filebot.web.SubtitleDescriptor;
import subs.Provider;
import subs.Results;
import subs.Subs4me;
import utils.FileStruct;
import utils.Utils;

public class Subscene implements Provider
{
    static FileStruct currentFile = null;
    static final SubsceneSubtitleClient subsceneClient = new SubsceneSubtitleClient();
    static final Subscene instance = new Subscene();
    static
    {
        Subs4me.registerProvider(instance);
    }
    
    @Override
    public boolean doWork(FileStruct fs)
    {
        currentFile = fs;
        File[] files = new File[]{fs.getFile()};
        try
        {
            System.out.println("   Subscene provider trying:" + currentFile.getFullFileName());
            List<SearchResult> searchResults = subsceneClient.search(files[0].getName());
            LinkedList<SubtitleDescriptor> subs = new LinkedList<SubtitleDescriptor>();
            for (Iterator<SearchResult> iterator = searchResults.iterator(); iterator
                    .hasNext();)
            {
                SearchResult searchResult = (SearchResult) iterator.next();
                if (!Utils.isSameMovie3(searchResult.getName(), currentFile.getNormalizedName()))
                {
                    continue;
                }
                List<SubtitleDescriptor> descs = subsceneClient.getSubtitleList(searchResult, "Hebrew");
                for (Iterator<SubtitleDescriptor> iterator2 = descs.iterator(); iterator2.hasNext();)
                {
                    SubtitleDescriptor sub = (SubtitleDescriptor) iterator2
                            .next();
                    
                    String episodes = null;
                    Pattern p = Pattern.compile("(?i)s([\\d]{2}).e([\\d]{2}-[\\d]{2})"); 
                    Matcher m = p.matcher(sub.getName());
                    if (m.find() && currentFile.isTV())
                    {
                        if (Integer.parseInt(m.group(1)) != Integer.parseInt((currentFile.getSeasonSimple())))
                            continue;
                        
                        episodes = m.group(2);
                    }
                    if (currentFile.isTV() && 
                            (   sub.getName().indexOf("Entire Season") > -1 
                                || sub.getName().indexOf("Subpack") > -1 )
                                || Utils.isInRange(currentFile.getEpisode(), episodes))
                    {
                        ByteBuffer subFileBuffer = sub.fetch();
                        List<MemoryFile> list = null;
                        if (sub.getType().equals("zip"))
                        {
                            ZipArchive zip = new ZipArchive(subFileBuffer);
                            list = zip.extract();
                        }
                        else if (sub.getType().equals("rar"))
                        {
                            RarArchive zip = new RarArchive(subFileBuffer);
                            list = zip.extract();
                        }
                            
                        boolean seasonPrinted = false;
                        for (Iterator iterator3 = list.iterator(); iterator.hasNext();)
                        {
                            MemoryFile memFile = (MemoryFile) iterator3.next();
                            //handle Lost S03 Subpack
//                            if (memFile.getName().s)
//                            {
//                                
//                            }
                            FileStruct fsMem = new FileStruct(memFile.getName());
                            if (!fsMem.getSeason().equals(currentFile.getSeason()))
                            {
                                break;
                            }
                            else if (!seasonPrinted)
                            {
                                System.out.println("Subscene found correct season:" + sub.getName());
                                seasonPrinted = true;
                            }
                            if (memFile.getName().endsWith("srt"))
                            {
                                System.out.println("Enitre season:" + memFile.getName());
                            }
                        }
//                        subs.add(sub);
                    }
                    else if (sub.getName().toLowerCase().indexOf(currentFile.getNormalizedName()) > -1)
                    {
                        System.out.println("Subscene found:" + sub.getName());
                        subs.add(sub);
                    }
                }
            }
            if (subs.size() == 0)
                return false;
            
            if (subs.size() == 1)
            {
                ByteBuffer subFileBuffer = subs.get(0).fetch();
                downloadSubs(subFileBuffer, fs.getFile().getParent(), subs.get(0), false);
                return true;
            }
            else
            {
                File f = new File(currentFile.getFile().getParent(), currentFile.getFullNameNoExt() + Subs4me.DO_WORK_EXT);
                f.createNewFile();
                for (Iterator iterator = subs.iterator(); iterator.hasNext();)
                {
                    SubtitleDescriptor subtitleDescriptor = (SubtitleDescriptor) iterator
                            .next();
                    ByteBuffer subFileBuffer = subtitleDescriptor.fetch();
                    downloadSubs(subFileBuffer, fs.getFile().getParent(), subtitleDescriptor, true);
                } 
                return false;
            }

        } catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return true;
    }
    
    /**
     * 
     */
    public static boolean downloadSubs(ByteBuffer buffer, String location, SubtitleDescriptor subDesc, boolean moreThanOne)
    {
        if (subDesc.getType().equals("zip"))
        {
            ZipArchive zip = new ZipArchive(buffer);
            try         
            {
                List<MemoryFile> list = zip.extract();
                for (Iterator iterator = list.iterator(); iterator.hasNext();)
                {
                    MemoryFile memFile = (MemoryFile) iterator.next();
                    if (memFile.getName().endsWith("srt"))
                    {
                        if (!moreThanOne)
                        {
                            writeToDisk(memFile.getData(), location + File.separator + memFile.getName());
                        }
                        else
                        {
                            writeToDisk(memFile.getData(), location + File.separator + currentFile.getFullNameNoExt() + ".srt.subscene."+ memFile.getName());
                            return true;
                        }
                    }
                }
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return false;
    }

    private static boolean writeToDisk(ByteBuffer buffer, String destination)
    {
        FileOutputStream fileOutputStream;
        FileChannel fileChannel;

        try
        {
            fileOutputStream = new FileOutputStream(destination);
            fileChannel = fileOutputStream.getChannel();
            fileChannel.write(buffer);
            fileChannel.close();
            fileOutputStream.close();
        } catch (IOException exc)
        {
            return false;
        }
        
        return true;
    }
    
    @Override
    public String getName()
    {
        return "subscene";
    }
}
