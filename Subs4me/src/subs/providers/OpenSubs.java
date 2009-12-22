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
import net.sourceforge.filebot.web.SubtitleDescriptor;
import subs.Provider;
import subs.Results;
import subs.Subs4me;
import utils.FileStruct;

public class OpenSubs implements Provider
{
    FileStruct currentFile = null;
    static final OpenSubtitlesClient openClient = new OpenSubtitlesClient("subs4me");
    static final OpenSubs instance = new OpenSubs();
    static
    {
        Subs4me.registerProvider(instance);
    }
    
    @Override
    public boolean doWork(File fi)
    {
        File[] files = new File[1];
        files[0] = fi;
        try
        {
            Map<File, List<SubtitleDescriptor>> list = openClient.getSubtitleList(files, "Hebrew");
            List<SubtitleDescriptor> descList = list.get(fi);
            if (descList.size() == 0)
                return false;
            
            SubtitleDescriptor sub = null;
            for (Iterator<SubtitleDescriptor> iterator = descList.iterator(); iterator.hasNext();)
            {
                SubtitleDescriptor subtitleDescriptor = (SubtitleDescriptor) iterator
                        .next();
                if (subtitleDescriptor.getType().equals("srt"))
                {
                    sub = subtitleDescriptor;
                    break;
                }
            }
            if (sub == null)
                return false;
            
            ByteBuffer subFileBuffer = sub.fetch();
            downloadSubs(subFileBuffer, fi.getParent(), sub.getName());
        } catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return true;
    }
    
    /**
     * 
     * @param file
     * @param fileName 
     * @return succeeded true or false
     */
    public static boolean downloadSubs(ByteBuffer buffer, String location, String fileName)
    {
        FileOutputStream fileOutputStream;
        FileChannel fileChannel;

        try
        {
            fileOutputStream = new FileOutputStream(location + File.separator + fileName + ".srt");
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
        return "opensubs";
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
