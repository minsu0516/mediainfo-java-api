package subs.providers;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.HasParentFilter;
import org.htmlparser.filters.LinkRegexFilter;
import org.htmlparser.filters.LinkStringFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.http.ConnectionManager;
import org.htmlparser.http.ConnectionMonitor;
import org.htmlparser.http.Cookie;
import org.htmlparser.http.HttpHeader;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import subs.Provider;
import subs.Results;
import subs.Subs4me;
import utils.FileStruct;
import utils.Utils;

public class Sratim implements Provider
{
    public static final String baseUrl ="http://sratim.co.il";
    FileStruct currentFile = null;
    static ConnectionManager _manager;
    
    static final Sratim instance = new Sratim();
    static
    {
        Subs4me.registerProvider(instance);
    }
    
    public Sratim()
    {
       
        login();
    }
    
    public Sratim(FileStruct fs)
    {
        currentFile = fs;
        searchByActualName(currentFile);
        
    }
    
    private void login()
    {
        _manager = Parser.getConnectionManager();
        ConnectionMonitor monitor = new ConnectionMonitor()
        {
            public void preConnect(HttpURLConnection connection)
            {
                System.out.println(HttpHeader.getRequestHeader(connection));
            }

            public void postConnect(HttpURLConnection connection)
            {
                System.out
                        .println(HttpHeader.getResponseHeader(connection));
            }
        };
        _manager.setMonitor(monitor);
        _manager.setCookieProcessingEnabled(true);
//        __utma=232448605.1886441988.1259765317.1260782837.1262007008.7; __utmz=232448605.1259765317.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); LI=49672A9B9602BD9D; LP=356B2A1DE4CE8A87567E81856DEA59E8;
        Cookie cookie = new Cookie ("ASP.NET_SessionId", "y3ezok45xclwcrqbuyl4ri55");
        _manager.setCookie (cookie, "www.sratim.co.il");
        
    }

    public Results searchByActualName(FileStruct currentFile)
    {
        StringBuffer buffer = new StringBuffer(1024);
        // 'input' fields separated by ampersands (&)
        buffer.append("keyword=");
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
                baseUrl + "/movies/search.aspx", buffer);

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
                filter = new AndFilter(new LinkRegexFilter("series"),
                        new HasParentFilter(new AndFilter(new TagNameFilter("table"),
                                new HasAttributeFilter("class", "MovieViews")), true));   
            } else
            {
                filter = new AndFilter(new LinkRegexFilter("movies/view"),
                        new HasParentFilter(new AndFilter(new TagNameFilter("table"),
                                new HasAttributeFilter("class", "MovieViews")), true));   
            }
            
            ArrayList<String> subids = new ArrayList<String>();
            // parsing the links on the search page itself
            for (NodeIterator e = parser.elements(); e.hasMoreNodes();)
            {
                Node node = e.nextNode();
//                System.out.println(node.toHtml());
                node.collectInto(list, filter);
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
                        
                        if (!node.toPlainTextString().equalsIgnoreCase(currentFile.getNormalizedName()))
                        {
                            continue;
                        }
                        
                        String ref = ((TagNode) node).getAttribute("href");
                        if (ref.contains("id="))
                        {
                            if (subids.contains(ref))
                            {
                                continue;
                            }
                            subids.add(ref);
                        }
                        // System.out.println("subid = " + subids.get(i));
                    }
                }
    
                for (String id : subids)
                {
                    Results subs = locateFileInFilePage(id, currentFile.getNameNoExt());
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
                        String subid = searchForCorrectSubidOfSeries(((TagNode) node)
                                .getAttribute("href"), currentFile);
                        if (subid != null)
                        {
                            Results subFiles = locateFileInFilePage(subid
                                    .substring(1), currentFile.getNameNoExt());
                            if (subFiles != null)
                            {
                                return subFiles;
                            }
                        }
                    }
                }
            }
        } catch (ParserException e)
        {
        }
        
        return null;
    }

    /**
     * Parse the site to get the subid for the episode in question
     * 
     * @return
     */
    public String searchForCorrectSubidOfSeries(String seriesInfo, FileStruct currentFile)
    {
        try
        {
            if (seriesInfo.startsWith("/"))
                seriesInfo = seriesInfo.substring(1);
            Parser parser = new Parser(baseUrl + "/" + seriesInfo);
            parser.setEncoding("UTF-8");
            NodeFilter filter = new AndFilter(new TagNameFilter("a"),
                    new HasParentFilter(new AndFilter(new TagNameFilter("div"),
                            new HasAttributeFilter("id", "ctl00_ctl00_Body_Body_Box_EpisodesList"
                                    + currentFile.getSeasonSimple())), true));
    
            NodeList list = new NodeList();
            for (NodeIterator e = parser.elements(); e.hasMoreNodes();)
            {
                Node node = e.nextNode();
                node.collectInto(list, filter);
                // System.out.println(node.toHtml());
            }
    
            Node[] nodes = list.toNodeArray();
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
            //            
            // //bring the table for display names
            // parser = new Parser("http://www.torec.net/" + subid);
            // parser.setEncoding("UTF-8");
            //            
            // filter =
            // new AndFilter (
            // new TagNameFilter ("span"),
            // new HasParentFilter (
            // new TagNameFilter("p")));
            //            
            // for (NodeIterator e = parser.elements(); e.hasMoreNodes();)
            // {
            // e.nextNode().collectInto(list, filter);
            // }
            //            
            // ArrayList<String> displayNames = new ArrayList<String>();
            // nodes = list.toNodeArray();
            // for (int i = 0; i < nodes.length; i++)
            // {
            // Node node = nodes[i];
            // // System.out.println(node.toPlainTextString().trim());
            //                
            // if (node.toPlainTextString().trim().equals(movieName))
            // {
            // String dlPlease = filesTodl.get(filesTodl.size()-i -1);
            // displayNames.add(node.toPlainTextString().trim());
            // System.out.println("found movie name proceeding to dl: " +
            // dlPlease);
            // return dlPlease;
            // }
            // else if (node.toPlainTextString().trim().startsWith(movieName))
            // {
            // String dlPlease = node.toPlainTextString().trim();
            // displayNames.add(node.toPlainTextString().trim());
            // System.out.println("found movie name proceeding to dl: " +
            // dlPlease);
            // return dlPlease;
            // }
            // }
        } catch (ParserException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    
        // System.out.println("Did not find a file to download");
        return null;
    }

    @Override
    public boolean doWork(File fi)
    {
        try
        {
            currentFile = new FileStruct(fi);
            String f = currentFile.getNameNoExt();
            boolean success = false;

            Results subsID = searchByActualName(currentFile);
            if (subsID != null && subsID.getResults().size() > 0)
            {
                for (String subID : subsID.getResults())
                {
                    Pattern p = Pattern.compile("ID=([\\d]*)");
                    Matcher m = p.matcher(subID);
                    String name = subID;
                    if (m.find())
                    {
                        name = m.group(1);
                    }
                    success = Utils.downloadZippedSubs(baseUrl + subID, name + ".zip");
                    if (success)
                    {
                        Utils.unzipSubs(currentFile, name + ".zip", subsID.isCorrectResults());
                        return true;
                    }
                }
            }
            else
            {
                System.out.println("searchByActualNameInTorec Could not find:" + Utils.escape(f) + ".zip on Torec"); 
            }

        } catch (Exception e)
        {
            System.out.println("******** Error - cannot get subs for "
                    + currentFile.getFullFileName());
            // e.printStackTrace();
        }
        
        return false;
    }

    @Override
    public String getName()
    {
        return "Sratim";
    }

    /**
         * locate files in download page of that movie/series based on
         * sub_id
         * 
         * @param subid
         * @param movieName
         * @param simplified
         * @return
         */
        private Results locateFileInFilePage(String subid, String movieName)
        {
            LinkedList<String> intenseFilesList = new LinkedList<String>();
            LinkedList<String> longerFilesList = new LinkedList<String>();
            Parser parser;
            try
            {
                parser = new Parser(baseUrl + subid);
                parser.setEncoding("UTF-8");
                
                NodeFilter filter = null;

                //get the number of subs for the hebrew lang
                filter = new AndFilter(new TagNameFilter("label"), new HasAttributeFilter("for", "S_1"));  
                NodeList list = parser.parse(filter);
                Pattern p1 = Pattern.compile("(\\d+)");
                //need to remove the ext 
                Matcher m1 = p1.matcher(list.elementAt(0).toPlainTextString());
                int subNumber = -1;
                if (m1.find())
                {
                    subNumber = Integer.parseInt(m1.group());
                }
                
                ArrayList<String> filesTodl = new ArrayList<String>();
                ArrayList<String> displayNames = new ArrayList<String>();
                
                parser.reset();
                filter = new AndFilter(new TagNameFilter("tr"), new HasAttributeFilter("title", ""));
                list = parser.parse(filter);
                
                //we are traversing all the subtitle blocks
                Node[] na = list.toNodeArray();
                for (int i = 0; i < subNumber; i++)
                {
                    Node nd = na[i];
                    NodeList refs = new NodeList();
                    nd.collectInto(refs, new LinkStringFilter("subtitles"));
                    String href = ((TagNode) refs.elementAt(0)).getAttribute("href");
//                    System.out.println(href);
                    filesTodl.add(href);
                    
                    //find the title name
                    Node titleNameTR = nd.getChildren().elementAt(1).getChildren().elementAt(1).getFirstChild();
                    displayNames.add(titleNameTR.toPlainTextString());
                    
                }
                
                int i = -1;
                for (String name : displayNames)
                {
                    i++;
                    name = name.trim();
                    if (Utils.isSameMovie(new FileStruct(name.trim()), new FileStruct(movieName)))
//                    if (Utils.isSameMovie(new FileStruct(name), new FileStruct(movieName)))
                    {
                        displayNames.add(name);
//                        String dlPlease = Utils.postForFileName(subid.substring(15),
//                                filesTodl.get(i));
                        System.out.println("found exact movie name proceeding to dl: "
                                + filesTodl.get(i));
                        LinkedList<String> lst = new LinkedList<String>();
                        lst.add(filesTodl.get(i));
                        return new Results(lst, true);
                        // } else if (node.toPlainTextString().trim()
                        // .startsWith(movieName))
                    } else
                    {
                        String remoteName = name;
//                        String dlPlease = Utils.postForFileName(subid.substring(15),
//                                filesTodl.get(i));
                        //                    String dlPlease = node.toPlainTextString().trim();
                        //add the file to longer list
                        if (Subs4me.isFullDownload())
                        {
                            longerFilesList.add(filesTodl.get(i));
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
                            intenseFilesList.add(filesTodl.get(i));
                        }
                    }
                }
                /*
               * Now we get to a dillema:
               * 1. the user did not specify all and there is more than 1 proposal to download
               * 2. the user did specify all
               */
              
              if (Subs4me.isFullDownload())
              {
                  if (longerFilesList.size()>0)
                  {
                      File f = new File(currentFile.getFile().getParent(), currentFile.getFullNameNoExt()+".dowork");
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
                  File f = new File(currentFile.getFile().getParent(), currentFile.getFullNameNoExt()+".dowork");
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

                
            } catch (ParserException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return null;
        }

}
