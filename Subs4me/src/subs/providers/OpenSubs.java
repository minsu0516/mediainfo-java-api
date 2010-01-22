package subs.providers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sourceforge.filebot.web.OpenSubtitlesClient;
import net.sourceforge.filebot.web.OpenSubtitlesSubtitleDescriptor;
import net.sourceforge.filebot.web.SubtitleDescriptor;
import subs.Provider;
import subs.Subs4me;
import utils.FileStruct;

public class OpenSubs implements Provider
{
    static FileStruct currentFile = null;
    static final OpenSubtitlesClient openClient = new OpenSubtitlesClient("subs4me");
    static final OpenSubs instance = new OpenSubs();
    static
    {
        Subs4me.registerProvider(instance);
    }
    
    public static OpenSubs getInstance()
    {
        return instance;
    }
    
    @Override
    public int doWork(FileStruct fs) throws Exception
    {
        currentFile = fs;
        File[] files = new File[]{fs.getFile()};
        System.out.println("*** Opensubs trying:" + currentFile.getFullFileName());
        Map<File, List<SubtitleDescriptor>> list = openClient.getSubtitleList(files, "Hebrew");
        List<SubtitleDescriptor> descList = list.get(files[0]);
        if (descList.size() == 0)
            return Provider.not_found;

        SubtitleDescriptor sub = null;
        for (Iterator<SubtitleDescriptor> iterator = descList.iterator(); iterator.hasNext();)
        {
            SubtitleDescriptor subtitleDescriptor = (SubtitleDescriptor) iterator
            .next();
            if (subtitleDescriptor.getType().equals("srt"))
            {
                sub = subtitleDescriptor;
                System.out.println("*** Opensubs found:" + sub.getName());
                break;
            }
        }
        if (sub == null)
            return Provider.not_found;

        ByteBuffer subFileBuffer = sub.fetch();
        downloadSubs(subFileBuffer, files[0].getParent(), sub);
        return Provider.perfect;
    }
    
    /**
     * 
     */
    public static boolean downloadSubs(ByteBuffer buffer, String location, SubtitleDescriptor subDesc)
    {
        FileOutputStream fileOutputStream;
        FileChannel fileChannel;

        try
        {
            fileOutputStream = new FileOutputStream(currentFile.buildDestSrt("." + subDesc.getType()));
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
    
    public static String[] getMovieNames(File fi)
        throws Exception
    {
        String[] ret = null;
        System.out.println("*** Opensubs trying to get movie name for:" + fi.getName());
        Map<File, List<SubtitleDescriptor>> list;
        try
        {
            list = openClient.getSubtitleList(new File[]{fi}, "all");
            List<SubtitleDescriptor> descList = list.get(fi);
            if (descList.size() == 0)
                return null;
            
            SubtitleDescriptor sub = null;
            for (Iterator<SubtitleDescriptor> iterator = descList.iterator(); iterator.hasNext();)
            {
                OpenSubtitlesSubtitleDescriptor subtitleDescriptor = (OpenSubtitlesSubtitleDescriptor) iterator.next();
                ret = new String[]{subtitleDescriptor.getMovieName(), subtitleDescriptor.getMovieNameEng()};
                System.out.println("*** Opensubs found movie name:" + ret[0] + ", " + ret[1]);
                break;
            }
        } catch (Exception e)
        {
            throw new Exception("Could not retrive names", e);
        }
        return ret;
    }

    @Override
    public String getName()
    {
        return "opensubs";
    }
}
