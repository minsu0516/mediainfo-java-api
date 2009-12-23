package subs.providers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.sourceforge.filebot.ui.panel.subtitle.MemoryFile;
import net.sourceforge.filebot.ui.panel.subtitle.ZipArchive;
import net.sourceforge.filebot.web.SearchResult;
import net.sourceforge.filebot.web.SubsceneSubtitleClient;
import net.sourceforge.filebot.web.SubtitleDescriptor;
import subs.Provider;
import subs.Results;
import subs.Subs4me;
import utils.FileStruct;

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
    public boolean doWork(File fi)
    {
        currentFile = new FileStruct(fi, false);
        File[] files = new File[1];
        files[0] = fi;
        try
        {
            List<SearchResult> searchResults = subsceneClient.search(fi.getName());
            LinkedList<SubtitleDescriptor> subs = new LinkedList<SubtitleDescriptor>();
            for (Iterator<SearchResult> iterator = searchResults.iterator(); iterator
                    .hasNext();)
            {
                SearchResult searchResult = (SearchResult) iterator.next();
                List<SubtitleDescriptor> descs = subsceneClient.getSubtitleList(searchResult, "Hebrew");
                for (Iterator<SubtitleDescriptor> iterator2 = descs.iterator(); iterator2.hasNext();)
                {
                    SubtitleDescriptor sub = (SubtitleDescriptor) iterator2
                            .next();
                    if (sub.getName().toLowerCase().indexOf(currentFile.getFullNameNoExt().toLowerCase()) > -1)
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
                downloadSubs(subFileBuffer, fi.getParent(), subs.get(0), false);
                return true;
            }
            else
            {
                File f = new File(currentFile.getFile().getParent(), currentFile.getFullNameNoExt() + ".dowork");
                f.createNewFile();
                for (Iterator iterator = subs.iterator(); iterator.hasNext();)
                {
                    SubtitleDescriptor subtitleDescriptor = (SubtitleDescriptor) iterator
                            .next();
                    ByteBuffer subFileBuffer = subtitleDescriptor.fetch();
                    downloadSubs(subFileBuffer, fi.getParent(), subtitleDescriptor, true);
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

    @Override
    public Results searchByActualName(FileStruct currentFile)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String searchForCorrectSubidOfSeries(String seriesInfo,
            FileStruct currentFile)
    {
        // TODO Auto-generated method stub
        return null;
    }

}
