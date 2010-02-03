package utils;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import subs.Subs4me;
import subs.providers.OpenSubs;

public class FileStruct
{
    private String _srcDir = "";
    private String _fullFileName = "";
    private String fullFileNameNoGroup = "";
    private String _normalizedName = "";
    private String source = null;
    private String release = "";
    private int releaseStartIndex = -1;
    private String hd = null;
    private boolean hasPic = false;
    
    private boolean picAlreadyDownloaded = false;
    
    public final static String releaseSourcePattern = "(?i)(cam)|(ts)|(tc)|(r5)";
    public final static String hdLevel = "(?i)(720p)|(720i)|(720)|(1080p)|(1080i)|(1080)|(480p)|(480i)|(576p)|(576i)";
    
    Pattern EXT_PATTERN = Pattern.compile(".*([.].*$)");
    public int getReleaseStartIndex()
    {
        return releaseStartIndex;
    }

    public void setReleaseStartIndex(int releaseStartIndex)
    {
        this.releaseStartIndex = releaseStartIndex;
    }

    private String _ext = "";
    private String _season = null;
    
    public String getExt()
    {
        return _ext;
    }

    public void setExt(String ext)
    {
        this._ext = ext;
    }

    private String _episode = null;
    private File _orig;
    
    public FileStruct(File f)
    {
        this(f, true);
    }
    
    public FileStruct(String fileName, boolean isRealFile)
    {
        //because there is usually no ext on the file created like this
        // and we need an extension for the normalize procedure, I am adding it manually
        //time will tell if this is the right move
        _normalizedName = normalizeMovieName(fileName + ".srt");
    }
    
    public FileStruct(File f, boolean extraWebSearchForSearch)
    {
        this(f, extraWebSearchForSearch, false);
    }
    
    public FileStruct(String fileName)
    {
        this(fileName, true);
    }
    
    public FileStruct(File f, boolean extraWebSearchForSearch, boolean useParentDirNameAsFilename)
    {
        _orig = f;
        _srcDir = f.getParent();
        _fullFileName = f.getName();
        if (useParentDirNameAsFilename)
        {
            Matcher m = EXT_PATTERN.matcher(f.getName());
            m.find();
            String ext = m.group(1);
            _fullFileName = f.getParentFile().getName() + ext;
        }
        
        _normalizedName = normalizeMovieName(_fullFileName);
        if (extraWebSearchForSearch)
        {
            String[] names = null;
            if (!Subs4me.dontUseOpenSubsForNameSearch)
            {
                try
                {
                    names = OpenSubs.getInstance().getMovieNames(f);
                }
                catch (Exception e)
                {
                    System.out.println("****** error trying to find real name using open subs: " + e.getMessage());
                }
            }
            
            if (names != null && names[0] != null)
            {
                _normalizedName = names[0];
            }
            else
            {
                if (!Subs4me.dontUseOpenSubsForNameSearch)
                {
                    System.out.println("*** Opensubs was unable to find the movie name" );
                }
                if (!isTV())
                {
                    System.out.println("*** Search using Google for Movie's real name");
                    String n = getNameNoExt();
                    if (useParentDirNameAsFilename)
                    {
                        n = _fullFileName;
                    }
                    String realName = Utils.locateRealNameUsingGoogle(n, "www.imdb.com");
                    if (realName == null)
                    {
                        return;
                    }
                    _normalizedName = realName;
                }
            }
        }
        
        File pic = new File(f, getFullNameNoExt() + ".jpg");
        if(pic.exists())
        {
            setHasPic(true);
        }
    }
    
    public String normalizeMovieName(String in)
    {
        String groupPattern = "(-\\w*(-)|($))|(-\\w*$)|(\\A\\w*-)";
        String seasonEp1 = "(?i)(s([\\d]+)e([\\d]+))";
        String seasonEp2 = "(?i)([.][\\d]{3,}+[.])";
        String pattern = "(?i)(PAL)|(DVDRip)|(DVDR)|(REPACK)|(720p)|(720)|(1080p)|(1080)|(480p)|(x264)|(BD5)|(bluray)|(-.*$)|(\\A\\w*-)|(\\d\\d\\d\\d)|(XviD)|(HDTV)|(s[\\d]+e[\\d]+)|([.][\\d]{3,}+[.])|(\\[.*\\])|(ac3)|(nl)|(limited)";
        
        //need to take into account that if the is a word at the beginning and then a -
        //its the group name or a cdxxx
        
        in = in.replaceAll("(?i)(Blue-ray)|(blu-ray)|(brrip)", "");
        Pattern p;
        //need to remove the ext 
        Matcher m1 = EXT_PATTERN.matcher(in);
        if (m1.find())
        {
            setExt(m1.group(1).substring(1));
            in = in.substring(0, in.length() - getExt().length()-1);
        }
        p = Pattern.compile(groupPattern);
        m1 = p.matcher(in);
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
        p = Pattern.compile(seasonEp1);
        m1 = p.matcher(in);
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
        p = Pattern.compile(hdLevel);
        m1 = p.matcher(in);
        if (m1.find())
        {
            hd = m1.group();
        }
        p = Pattern.compile(releaseSourcePattern);
        m1 = p.matcher(in);
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
        return _srcDir;
    }

    public void setSrcDir(String srcDir)
    {
        this._srcDir = srcDir;
    }

    public String getFullFileName()
    {
        return _fullFileName;
    }
    
    public String getFullFileNameNoGroup()
    {
        return fullFileNameNoGroup;
    }

    public void setFullFileName(String fullFileName)
    {
        this._fullFileName = fullFileName;
    }

    public String getNormalizedName()
    {
        return _normalizedName.trim();
    }

    public void setNormalizedNmae(String normalizedName)
    {
        this._normalizedName = normalizedName;
    }


    public void setRelease(String release)
    {
        this.release = release;
    }

    public String getSeason()
    {
        return _season;
    }

    public String getSeasonSimple()
    {
        return _season.replaceAll("^[0]*", "");
    }
    
    public String getEpisode()
    {
        return _episode;
    }
    
    public String getEpisodeSimple()
    {
        String ret = _episode.replaceAll("^[0]*", "");
        if (ret.isEmpty())
            return "0";
        else
            return ret;
    }

    public void setEpisode(String episode)
    {
        this._episode = episode;
    }

    public void setSeason(String season)
    {
        this._season = season;
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
    
    public String getREALFullNameNoExt()
    {
        if (getFile() == null)
            return getFullFileName();
        
        return getFile().getName().substring(0, getFile().getName().length() - getExt().length() -1);
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
        return buildDestSrt(".srt");
    }
    public String buildDestSrt(String ext)
    {
        String ret = getSrcDir() + File.separator + getREALFullNameNoExt() + ext;
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
        return _orig;
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
    public boolean isHasPic()
    {
        return hasPic;
    }

    public void setHasPic(boolean hasPic)
    {
        this.hasPic = hasPic;
    }

    public void setPicAlreadyDownloaded(boolean picAlreadyDownloaded)
    {
        this.picAlreadyDownloaded = picAlreadyDownloaded;
    }
    
    public boolean hasPicBeenDownloadedAlready()
    {
        return this.picAlreadyDownloaded;
    }
}
