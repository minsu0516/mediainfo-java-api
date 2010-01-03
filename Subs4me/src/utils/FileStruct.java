package utils;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import subs.providers.OpenSubs;

public class FileStruct
{
    private String srcDir = "";
    private String fullFileName = "";
    private String fullFileNameNoGroup = "";
    private String normalizedName = "";
    private String source = null;
    private String release = "";
    private int releaseStartIndex = -1;
    private String hd = null;
    
    public final static String releaseSourcePattern = "(?i)(cam)|(ts)|(tc)|(r5)";
    public final static String hdLevel = "(?i)(720p)|(720i)|(720)|(1080p)|(1080i)|(1080)|(480p)|(480i)|(576p)|(576i)";
    
    
    public int getReleaseStartIndex()
    {
        return releaseStartIndex;
    }

    public void setReleaseStartIndex(int releaseStartIndex)
    {
        this.releaseStartIndex = releaseStartIndex;
    }

    private String ext = "";
    private String season = null;
    
    public String getExt()
    {
        return ext;
    }

    public void setExt(String ext)
    {
        this.ext = ext;
    }

    private String episode = null;
    private File orig;
    
    public FileStruct(File f)
    {
        this(f, true);
    }
    
    public FileStruct(String fileName, boolean isRealFile)
    {
        //because there is usually no ext on the file created like this
        // and we need an extension for the normalize procedure, I am adding it manually
        //time will tell if this is the right move
        normalizedName = normalizeMovieName(fileName + ".srt");
    }
    
    public FileStruct(String fileName)
    {
        this(fileName, true);
    }
    
    public FileStruct(File f, boolean extraWebSearchForSearch)
    {
        orig = f;
        srcDir = f.getParent();
        fullFileName = f.getName();
        normalizedName = normalizeMovieName(fullFileName);
        if (extraWebSearchForSearch)
        {
            String[] names = OpenSubs.getInstance().getMovieNames(f);
            if (names != null && names[0] != null)
            {
                normalizedName = names[0];
            }
            else
            {
                System.out.println("*** Opensubs was unable to find the movie name" );
                if (!isTV())
                {
                    System.out.println("*** Search using Google for Movie's real name");
                    String realName = Utils.locateRealNameUsingGoogle(fullFileName, "www.imdb.com");
                    if (realName == null)
                    {
                        return;
                    }
                    normalizedName = realName;
                }
            }
        }
    }
    
    public String normalizeMovieName(String in)
    {
        String groupPattern = "(-\\w*(-)|($))|(-\\w*$)|(\\A\\w*-)";
        String seasonEp1 = "(?i)(s([\\d]+)e([\\d]+))";
        String seasonEp2 = "(?i)([.][\\d]{3,}+[.])";
        String ext = ".*([.].*$)";
        String pattern = "(?i)(PAL)|(DVDRip)|(DVDR)|(REPACK)|(720p)|(720)|(1080p)|(1080)|(480p)|(x264)|(BD5)|(bluray)|(-.*$)|(\\A\\w*-)|(\\d\\d\\d\\d)|(XviD)|(HDTV)|(s[\\d]+e[\\d]+)|([.][\\d]{3,}+[.])|(\\[.*\\])|(ac3)|(nl)|(limited)";
        
        //need to take into account that if the is a word at the beginning and then a -
        //its the group name or a cdxxx
        
        in = in.replaceAll("(?i)(Blue-ray)|(blu-ray)|(brrip)", "");
        Pattern p1 = Pattern.compile(ext);
        //need to remove the ext 
        Matcher m1 = p1.matcher(in);
        if (m1.find())
        {
            setExt(m1.group(1).substring(1));
            in = in.substring(0, in.length() - getExt().length()-1);
        }
        p1 = Pattern.compile(groupPattern);
        m1 = p1.matcher(in);
        if (m1.find())
        {
            if (!m1.group().isEmpty())
            {
                setRelease(m1.group().replaceAll("-", ""));
                setReleaseStartIndex(m1.start());
                if (m1.start() == 0)
                {
                    fullFileNameNoGroup = in.substring(m1.group().length());
                }
                else
                {
                    fullFileNameNoGroup = in.substring(0, in.length() - m1.group().length());
                }
            }
        }
        //find season and ep
        //in some tv cases, the name could be:
        // house.609.the tyrent.mkv
        //we need to stop the normalize, after the episode.
        int normalizeStopper = -1;
        p1 = Pattern.compile(seasonEp1);
        m1 = p1.matcher(in);
        if (m1.find())
        {
            setSeason(m1.group(2));
            setEpisode(m1.group(3));
            normalizeStopper = m1.start();
        }
        //this creates problems with file names that include the year in them,
        // like Moon.2009.BRRip.XviD.AC3-ViSiON.mkv
//        else
//        {
//            p1 = Pattern.compile(seasonEp2);
//            m1 = p1.matcher(in);
//            if (m1.find())
//            {
//                String tmp = m1.group();
//                setSeason(tmp.substring(tmp.length()-4, tmp.length()-3));
//                setEpisode(tmp.substring(getSeason().length() +1, tmp.length()-1));
//                normalizeStopper = m1.start();
//            }
//        }
        p1 = Pattern.compile(hdLevel);
        m1 = p1.matcher(in);
        if (m1.find())
        {
            hd = m1.group();
        }
        p1 = Pattern.compile(releaseSourcePattern);
        m1 = p1.matcher(in);
        if (m1.find())
        {
            source = m1.group();
        }
        
        String ret = null;
        if (normalizeStopper != -1)
        {
            ret = in.substring(0, normalizeStopper);
        }
        else 
            ret = in;
        ret = ret.replaceAll("[ ]+", " ");
        
        ret = ret.replaceAll(pattern, " ");
        //lets try to set out filedata here:
        ret = ret.replaceAll(releaseSourcePattern, " ");
        ret = ret.replaceAll("[.]", " ");
        ret = ret.replaceAll("[ ]{2,}", " ").trim();
        return ret;
    }
    
    public String getSrcDir()
    {
        return srcDir;
    }

    public void setSrcDir(String srcDir)
    {
        this.srcDir = srcDir;
    }

    public String getFullFileName()
    {
        return fullFileName;
    }
    
    public String getFullFileNameNoGroup()
    {
        return fullFileNameNoGroup;
    }

    public void setFullFileName(String fullFileName)
    {
        this.fullFileName = fullFileName;
    }

    public String getNormalizedName()
    {
        return normalizedName.trim();
    }

    public void setNormalizedNmae(String normalizedName)
    {
        this.normalizedName = normalizedName;
    }


    public void setRelease(String release)
    {
        this.release = release;
    }

    public String getSeason()
    {
        return season;
    }

    public String getSeasonSimple()
    {
        return season.replaceAll("^[0]*", "");
    }
    
    public String getEpisode()
    {
        return episode;
    }
    
    public String getEpisodeSimple()
    {
        return episode.replaceAll("^[0]*", "");
    }

    public void setEpisode(String episode)
    {
        this.episode = episode;
    }

    public void setSeason(String season)
    {
        this.season = season;
    }

    public String getReleaseName()
    {
        return release;
    }
    
    public String getNameNoExt()
    {
        String ret = getFullFileName().substring(0, getFullFileName().length() - getExt().length()-1);
//        if (getReleaseStartIndex() > 0) 
//        {
//            ret = ret.substring(0, getReleaseStartIndex() + getReleaseName().length());
//        }
//        else if (getReleaseStartIndex() == 0) 
//        {
//            ret = ret.substring(getReleaseStartIndex() + getReleaseName().length());
//        }
//        //just incase we left a - out there
//        ret = ret.replaceAll("(^-)|(-$)", "");
        return ret;
    }
    
    public String getFullNameNoExt()
    {
        return getFullFileName().substring(0, getFullFileName().length() - getExt().length() -1);
    }
    
    public static void main(String[] args)
    {
        @SuppressWarnings("unused")
        FileStruct f = new FileStruct(new File("c:/tmp/flashforward.s01e05.720p.hdtv.x264-red.mkv"));
    }

    public boolean isTV()
    {
        if (getEpisode() != null && getSeason() != null)
        {
            return true;
        }
        return false;
    }
    
    @Override
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("File=");
        sb.append(getFullFileName());
        sb.append(", ext=");
        sb.append(getExt());
        if (isTV())
        {
            sb.append(", season=");
            sb.append(getSeason());
            sb.append(", episode=");
            sb.append(getEpisode());
        }
        sb.append(", normalized=");
        sb.append(getNormalizedName());
        sb.append(", group=");
        sb.append(getReleaseName());
        sb.append(", source=");
        sb.append(getSource());
        return sb.toString();
    }
    
    public String buildDestSrt()
    {
        String ret = getSrcDir() + File.separator + getFullNameNoExt() + ".srt";
        return ret;
    }
    
    public String getHDLevel()
    {
        if (hd == null)
            return null;
        
        return hd.replaceAll("[pP]", "");
    }
    
    public String getSource()
    {
        return source;
    }
    
    public File getFile()
    {
        return orig;
    }
    
    public boolean isVideoFile()
    {
        if (getExt().equals("mkv") 
                || getExt().equals("avi"))
        {
            return true;
        }
        
        return false;
    }
    
    
}
