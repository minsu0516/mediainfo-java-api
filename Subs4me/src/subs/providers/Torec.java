package subs.providers;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.HasChildFilter;
import org.htmlparser.filters.HasParentFilter;
import org.htmlparser.filters.LinkRegexFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import subs.Provider;
import subs.Results;
import subs.Subs4me;
import utils.FileStruct;
import utils.Utils;

public class Torec implements Provider
{
    public static final String baseUrl ="http://www.torec.net";
    private FileStruct currentFile = null;
    
    private static final Torec instance = new Torec();
    static
    {
        Subs4me.registerProvider(instance);
    }
    
    public static Torec getInstance()
    {
        return instance;
    }

    @Override
    public boolean doWork(FileStruct fs)
    {
        Results subsID = null;
        currentFile = fs;
        try
        {
//            Sratim.searchByActualName(currentFile);
            String f = currentFile.getNameNoExt();
            System.out.println("*** Torec trying direct download: " + f);

            // try brute force, just try to get the filename - ext + .zip
            boolean success = false;
            success = Utils.downloadZippedSubs(baseUrl + "/zip_versions/"
                    + Utils.escape(f) + ".zip", f+ ".zip");
            if (success)
            {
                Utils.unzipSubs(currentFile, Utils.escape(f)+ ".zip", true);
                return true;
            }
            else
            {
                System.out.println("*** Torec could not find direct download for:" + Utils.escape(f) + " on Torec"); 
            }

            subsID = searchByActualName(currentFile);
            if (subsID != null && subsID.getResults().size() > 0)
            {
                for (String subID : subsID.getResults())
                {
                    success = Utils.downloadZippedSubs(baseUrl + "/zip_versions/"
                            + Utils.escape(subID) + ".zip", subID + ".zip");
                    if (success)
                    {
                        Utils.unzipSubs(currentFile, subID + ".zip", subsID.isCorrectResults());
                    }
                }
            }
            else
            {
                System.out.println("*** Torec searchByActualName Could not find:" + Utils.escape(f) + ".zip on Torec"); 
            }

        } catch (Exception e)
        {
            System.out.println("******** Error - Torec cannot get subs for "
                    + currentFile.getFullFileName());
            // e.printStackTrace();
        }
        
        if (subsID != null && subsID.isCorrectResults())
        {
            return true;
        }
        
        return false;
    }

    @Override
    public String getName()
    {
        return "Torec";
    }

    public Results searchByActualName(FileStruct currentFile)
    {
        StringBuffer buffer;
            buffer = new StringBuffer(1024);
            // 'input' fields separated by ampersands (&)
            buffer.append("search=");
            String[] names = currentFile.getNormalizedName().split(" ");
            for (int i = 0; i < names.length; i++)
            {
                String part = names[i];
                if (i != 0)
                {
                    buffer.append("+");
                }
                buffer.append(part);
            }

            HttpURLConnection connection = Utils.createPost(
                    "http://www.torec.net/ssearch.asp", buffer);

            Parser parser;
            try
            {
                parser = new Parser(connection);
                parser.setEncoding("UTF-8");

            NodeList list = new NodeList();
            // check if we need tvseries
            NodeFilter filter = null;
            if (currentFile.isTV())
            {
                filter = new AndFilter(new LinkRegexFilter("series_id"),
                        new HasChildFilter(new TagNameFilter("IMG")));
            } else
            {                
               filter = new AndFilter(new LinkRegexFilter("_id="),
                        new HasParentFilter(new AndFilter(new TagNameFilter("td"),
                                new HasAttributeFilter("class", "newd_table_titleLeft_BG")), true));
            }

            ArrayList<String> subids = new ArrayList<String>();
            // parsing the links on the search page itself
            for (NodeIterator e = parser.elements(); e.hasMoreNodes();)
            {
                e.nextNode().collectInto(list, filter);
            }

            if (!currentFile.isTV())
            {
                if (!list.toHtml().equals(""))
                {
                    Node[] nodes = list.toNodeArray();
                    for (int i = 0; i < nodes.length; i++)
                    {
                        Node node = nodes[i];
                        if (node.toPlainTextString() == null || node.toPlainTextString().equals(""))
                            continue;
                        
                        String[] namess = node.toPlainTextString().split("/");
                        if (namess.length < 2)
                            continue;
                        
                        if (!Utils.isSameMovie2(namess[1], currentFile.getNormalizedName()))
                            continue;
//                        System.out.println(node.toPlainTextString());
                        String ref = ((TagNode) node).getAttribute("href");
                        if (ref.contains("_id="))
                        {
                            subids.add(ref);
                        }
                        
                        // System.out.println("subid = " + subids.get(i));
                    }
                }
    
                for (String id : subids)
                {
                    Results subs = locateFileInFilePageOnTorec(id, currentFile.getNameNoExt());
                    if (subs != null)
                    {
                        return subs;
                    }
                }
            }
            else
            {
                // ////////////////////////////////////////////////////////////////////////////////////////
                /*
                 * No luck finding the correct movie name, it must be a tv
                 * series So we need to search for series
                 */

                if (!list.toHtml().equals(""))
                {
                    Node[] nodes = list.toNodeArray();
                    for (int i = 0; i < nodes.length; i++)
                    {
                        Node node = nodes[i];
                        // System.out.println(((TagNode)
                        // node).getAttribute("href"));
                        String subid = searchForCorrectSubidOfSeries(((TagNode) node)
                                .getAttribute("href"), currentFile);
                        if (subid != null)
                        {
                            Results subFiles = locateFileInFilePageOnTorec(subid
                                    .substring(1), currentFile.getNameNoExt());
                            if (subFiles != null)
                            {
                                return subFiles;
                            }
                        }
                    }
                }
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

    public String searchForCorrectSubidOfSeries(String seriesInfo,
            FileStruct currentFile)
    {
        try
        {
            if (seriesInfo.startsWith("/"))
                seriesInfo = seriesInfo.substring(1);
            Parser parser = new Parser(baseUrl + "/" + seriesInfo);
            parser.setEncoding("UTF-8");
            
            //lets find out how many seasons are showing:
            NodeFilter filter = new AndFilter(new TagNameFilter("div"),
                     new HasAttributeFilter("class", "season_table"));
            NodeList list = new NodeList();
            list = parser.parse(filter);
            Node[] nodes = list.toNodeArray();
            LinkedList<String> seasons = new LinkedList<String>();
            for (int i = 0; i < nodes.length; i++)
            {
                Node node = nodes[i];
                seasons.add(((TagNode)node).getAttribute("id"));
//                System.out.println();
            }
            
            list.removeAll();
            parser.reset();
            String seNum = currentFile.getSeasonSimple();
            
            //there are times where there are no season translations, in that case do not continue
            if (seasons.size() == 0)
            {
                return null;
            }
            //The Simpsons S19E01 He Loves to Fly and He D'ohs.avi
            int firstKnownSeason = Integer.parseInt(seasons.get(0).substring(7));
            if (firstKnownSeason == 0)
            {
                //if the first known season starts at 0, decreas the season number to suit it
                seNum = Integer.toString(Integer.parseInt(seNum)-1);
            }
            String se = null;
            for (Iterator iterator = seasons.iterator(); iterator.hasNext();)
            {
                String seTmp = (String) iterator.next();
                if (seTmp.substring(7).equals(seNum))
                {
                    se = seTmp;
                    break;
                }
            }
            
            if (se == null)
                return null;
            
            filter = new AndFilter(new TagNameFilter("a"),
                    new HasParentFilter(new AndFilter(new TagNameFilter("div"),
                            new HasAttributeFilter("id", se)), true));
            list = parser.parse(filter);
            nodes = list.toNodeArray();

            for (int i = 0; i < nodes.length; i++)
            {
                Node node = nodes[i];
                // just get the ep number
                String epi = "";
                Pattern p = Pattern.compile("([\\d]+ - [\\d]+)|(\\b[\\d]+\\b)");
                Matcher m = p.matcher(node.toPlainTextString());
                if (m.find())
                    epi = m.group();

                if (Utils.isInRange(currentFile.getEpisodeSimple(), epi))
                {
                    // found the ep number, return the subid
                    return ((TagNode) node).getAttribute("href");
                }
                // System.out.println(node.toPlainTextString());
            }
            //            
            list = new NodeList();
            
        } catch (ParserException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // System.out.println("Did not find a file to download");
        return null;
    }

    /**
         * locate files in download page of that movie/series in torec based on
         * sub_id
         * 
         * @param subid
         * @param movieName
         * @param simplified
         * @return
         */
        private Results locateFileInFilePageOnTorec(String subid, String movieName)
        {
            LinkedList<String> intenseFilesList = new LinkedList<String>();
            LinkedList<String> longerFilesList = new LinkedList<String>();
            
            try
            {
                // now we move into the movie page itself
                // bring the table for download files
                Parser parser = new Parser(baseUrl + "/" + subid);
                parser.setEncoding("UTF-8");
                NodeFilter filter = new AndFilter(new TagNameFilter("option"),
                        new HasParentFilter(new TagNameFilter("table"), true));
                NodeList list = new NodeList();
                for (NodeIterator e = parser.elements(); e.hasMoreNodes();)
                {
                    Node node = e.nextNode();
                    node.collectInto(list, filter);
                }
                Node[] nodes = list.toNodeArray();
                ArrayList<String> filesTodl = new ArrayList<String>();
                for (int i = 0; i < nodes.length; i++)
                {
                    Node node = nodes[i];
                    if (node.toPlainTextString().indexOf("כל הגרסאות") > -1)
                    {
                        break;
                    }
    
                    filesTodl.add(((TagNode) node).getAttribute("value"));
                    // System.out.println(node.toPlainTextString().trim());
                }
    
                list = new NodeList();
    
                // bring the table for display names
    //            parser = new Parser("http://www.torec.net/" + subid);
    //            parser.setEncoding("UTF-8");
                parser.reset();
                filter = new AndFilter(new TagNameFilter("span"),
                        new HasParentFilter(new TagNameFilter("p")));
    
                for (NodeIterator e = parser.elements(); e.hasMoreNodes();)
                {
                    e.nextNode().collectInto(list, filter);
                }
    
                ArrayList<String> displayNames = new ArrayList<String>();
                nodes = list.toNodeArray();
                for (int i = 0; i < nodes.length; i++)
                {
                    Node node = nodes[i];
                    // System.out.println(node.toPlainTextString().trim());
    
                    if (Utils.isSameMovie(new FileStruct(node.toPlainTextString().trim()), new FileStruct(movieName)))
                    {
                        displayNames.add(node.toPlainTextString().trim());
                        String dlPlease = postForFileName(subid.substring(15),
                                filesTodl.get(i));
                        System.out.println("Torec - found exact movie name proceeding to dl: "
                                + dlPlease);
                        LinkedList<String> lst = new LinkedList<String>();
                        lst.add(dlPlease);
                        return new Results(lst, true);
                        // } else if (node.toPlainTextString().trim()
                        // .startsWith(movieName))
                    } else
                    {
                        String remoteName = node.toPlainTextString().trim();
                        String dlPlease = postForFileName(subid.substring(15),
                                filesTodl.get(i));
    //                    String dlPlease = node.toPlainTextString().trim();
                        //add the file to longer list
                        if (Subs4me.isFullDownload())
                        {
                            longerFilesList.add(dlPlease);
                        }
                        
                        //check group
                        if (!(currentFile.getReleaseName().equalsIgnoreCase(Utils
                                .parseReleaseName(remoteName))))
                        {
                            continue;
                        }
                        
                        //add the file to intense list
                        if (!Subs4me.isFullDownload())
                        {
                            intenseFilesList.add(dlPlease);
                        }
    //                    
    //                    //check HDLevel
    //                    if (!Utils.compareHDLevel(remoteName, currentFile))
    //                    {
    //                        continue;
    //                    }
    //                    
    //                    if (!Utils.compareReleaseSources(remoteName, currentFile))
    //                    {
    //                        continue;
    //                    }
    //                    
    //                    displayNames.add(node.toPlainTextString().trim());
    ////                    System.out.println("found same same (group + getHDLevel) movie name proceeding to dl: "
    ////                            + dlPlease);
    //                    regularLst.add(dlPlease);
                    }
                    // }
                }
            } catch (ParserException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
    
//            try
//            {
//                Runtime.getRuntime().exec(baseUrl + "/" + subid);
//            } catch (IOException e1)
//            {
//                // TODO Auto-generated catch block
//                e1.printStackTrace();
//            }
            /*
             * Now we get to a dillema:
             * 1. the user did not specify all and there is more than 1 proposal to download
             * 2. the user did specify all
             */
            
            if (Subs4me.isFullDownload())
            {
                if (longerFilesList.size()>0)
                {
                    File f = new File(currentFile.getFile().getParent(), currentFile.getFullNameNoExt()+ Subs4me.DO_WORK_EXT);
                    try
                    {
                        f.createNewFile();
                    } catch (IOException e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                return new Results(longerFilesList, false);
            }
            else if (intenseFilesList.size() > 0)
            {
                File f = new File(currentFile.getFile().getParent(), currentFile.getFullNameNoExt()+ Subs4me.DO_WORK_EXT);
                try
                {
                    f.createNewFile();
                } catch (IOException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return new Results(intenseFilesList, false);
            }
            // System.out.println("Did not find a file to download");
            return null;
        }

    private String postForFileName(String subid, String code)
    {
        StringBuffer buffer = new StringBuffer(1024);
        // 'input' fields separated by ampersands (&)
        buffer.append("sub_id=" + subid);
        HttpURLConnection connection = Utils.createPost(
                baseUrl + "/ajax/sub/guest_time.asp", buffer);
    
        Parser parser;
        try
        {
            parser = new Parser(connection);
            parser.setEncoding("UTF-8");
    
            // HtmlPage page = new HtmlPage(parser);
            // TableTag[] tables = page.getTables();
            // tables[0].get
    
            String guest = "";
            for (NodeIterator e = parser.elements(); e.hasMoreNodes();)
            {
                Node node = e.nextNode();
                guest = node.toPlainTextString();
                // node.collectInto(list, filter);
                // System.out.println(node.toHtml());
            }
    
            buffer = new StringBuffer(1024);
            // 'input' fields separated by ampersands (&)
            buffer.append("sub_id=" + subid);
            buffer.append("&code=");
            buffer.append(code);
            buffer.append("&guest=");
            buffer.append(guest);
            connection = Utils.createPost(
                    baseUrl + "/ajax/sub/download.asp", buffer);
    
            parser = new Parser(connection);
            parser.setEncoding("UTF-8");
            String name = "";
            for (NodeIterator e = parser.elements(); e.hasMoreNodes();)
            {
                Node node = e.nextNode();
                name = node.toPlainTextString();
                // node.collectInto(list, filter);
                // System.out.println(node.toHtml());
            }
    
            // System.out.println("file name returning is:" +
            // guest.substring(14, guest.length() - 4));
            if (name.startsWith("/zip_versions/"))
                return name.substring(14, name.length() - 4);
            else if (!name.isEmpty())
                return name.substring(0, name.length() - 4);
            
        } catch (ParserException e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        // check if we need tvseries
    
        return null;
    }
}
