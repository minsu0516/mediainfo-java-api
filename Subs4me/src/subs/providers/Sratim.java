package subs.providers;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
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
import org.htmlparser.nodes.TagNode;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.util.SimpleNodeIterator;

import subs.Login;
import subs.Provider;
import subs.Results;
import subs.Subs4me;
import utils.FileStruct;
import utils.PropertiesUtil;
import utils.Utils;
import utils.WebBrowser;

/**
 * Some code here, for getting the correct cookie was adapted from YAMJ: http://code.google.com/p/moviejukebox/
 * @author iklein
 */
public class Sratim implements Provider
{
    public static final String baseUrl ="http://sratim.co.il";
    FileStruct currentFile = null;
    static ConnectionManager _manager;
    static String seassionId = ""; 
    private static String cookieHeader="";
    private static Logger logger = Logger.getLogger("SratimProvider");
    
    private static final String ARGUMENT_VIEWSTATE = "__VIEWSTATE";
    private static final String SEASON_FORM_NAME = "__EVENTTARGET";
    private static final String SEASON_FORM_VALUE = "ctl00$ctl00$Body$Body$Box$Menu_";
    private static final String ARGUMENT_FORM_NAME = "__EVENTARGUMENT";
    private static final String ARGUMENT_FORM_VALUE = "lbl";
    
    private static Pattern subIdPattern = Pattern.compile("ID=([\\d]*)");
    
    protected WebBrowser webBrowser;
    
    String login = null;
    String pass = null;
    String code = null;
    
    private static final Sratim instance = new Sratim();
    static
    {
        Subs4me.registerProvider(instance);
    }
    
    public static Sratim getInstance()
    {
        return instance;
    }

    public Sratim()
    {
        login = PropertiesUtil.getProperty("sratim.username", "");
        pass = PropertiesUtil.getProperty("sratim.password", "");
        code = PropertiesUtil.getProperty("sratim.code", "");
        webBrowser                  = new WebBrowser();
    }
    
    public Sratim(FileStruct fs)
    {
        currentFile = fs;
        searchByActualName(currentFile);
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
        
        HttpURLConnection connection = createPost(
                baseUrl + "/movies/search.aspx", buffer);
        Parser parser;
        try
        {
            parser = new Parser(connection);
            parser.setEncoding("UTF-8");
            String res = "";
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
                        
                        //sratim allows for the | sign, so we can try and parse it
                        String sName = node.toPlainTextString();
                        String sNames[];
                        if (sName.indexOf("|") > -1)
                        {
                            sNames = sName.split("|");
                        }
                        else
                        {
                            sNames = new String[]{sName};
                        }
                        boolean foundMovie = false;
                        for (int j = 0; j < sNames.length; j++)
                        {
                            String name = sNames[j];
                            if (!name.equalsIgnoreCase(currentFile.getNormalizedName()))
                            {
                                continue;
                            }
                            foundMovie = true;
                            break;
                        }
                        if (!foundMovie)
                        {
                            continue;
                        }
                        
                        String ref = ((TagNode) node).getAttribute("href");
                        findPicture(currentFile, ref);
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
            String sratimUrl = baseUrl + "/" + seriesInfo;
            Parser parser = new Parser(sratimUrl);
            parser.setEncoding("UTF-8");
            NodeFilter filter = new AndFilter(new TagNameFilter("input"),
                    new HasAttributeFilter("id", "__VIEWSTATE"));
            
            String viewState = "";
            NodeList list = parser.parse(filter);
            for (SimpleNodeIterator iterator = list.elements(); iterator.hasMoreNodes();)
            {
                Node node = (Node) iterator.nextNode();
                viewState = ((TagNode)node).getAttribute("value");
            }
            String seasonUrl = buildSeasonUrl(sratimUrl, Integer.parseInt(currentFile.getSeason()), viewState);
            HttpURLConnection conn = postRequest(seasonUrl);
            
            filter = new LinkRegexFilter("");
            parser = new Parser(conn);
            list = parser.parse(filter);
            for (SimpleNodeIterator iterator = list.elements(); iterator.hasMoreNodes();)
            {
                Node node = (Node) iterator.nextNode();
                String epi = "";
//                 System.out.println(node.toPlainTextString());
                Pattern p = Pattern.compile("(פרק ([\\d]*))");
                Matcher m = p.matcher(node.toPlainTextString());
                if (m.find())
                    epi = m.group(2);
    
                if (Utils.isInRange(currentFile.getEpisodeSimple(), epi))
                {
                    // found the ep number, return the subid
                    return ((LinkTag) node).getAttribute("href");
                }
            }
        } catch (ParserException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (NumberFormatException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (UnsupportedEncodingException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    
        return null;
    }

    @Override
    public int doWork(FileStruct fs)
    {
        currentFile = fs;
        try
        {
            String f = currentFile.getNameNoExt();
            boolean success = false;

            System.out.println("*** Sratim trying to find movie for: " + currentFile.getNormalizedName()); 
            Results subsID = searchByActualName(currentFile);
            if (subsID != null && subsID.getResults().size() > 0)
            {
                if (!subsID.isCorrectResults())
                {
                    handleMoreThanOneSub(subsID);
                    System.out.println("*** Sratim found some possibilities:" + currentFile.getNormalizedName()); 
                    return Provider.not_perfect;
                }
                else
                {
                    for (String subID : subsID.getResults())
                    {
                        Matcher m = subIdPattern.matcher(subID);
                        String name = subID;
                        if (m.find())
                        {
                            name = m.group(1);
                        }
                        success = Utils.downloadZippedSubs(baseUrl + subID, name + ".zip", cookieHeader);
                        if (success)
                        {
                            Utils.unzipSubs(currentFile, name + ".zip", subsID.isCorrectResults());
                        }
                    }
                    return Provider.perfect;
                }
            }
            else
            {
                System.out.println("searchByActualNameInTorec Could not find:" + Utils.escape(f) + ".zip on Torec"); 
                return Provider.not_found;
            }

        } catch (Exception e)
        {
            System.out.println("******** " + getName() + ": Error - cannot get subs for "
                    + currentFile.getFullFileName());
            // e.printStackTrace();
        }
        
        return Provider.not_found;
    }

    private void handleMoreThanOneSub(Results subsID)
    {
        StringBuilder sb = new StringBuilder();
        int i = -1;
        for (String subID : subsID.getResults())
        {
            i++;
            Matcher m = subIdPattern.matcher(subID);
            String name = subID;
            if (m.find())
            {
                name = m.group(1);
            }
            String st = getName() + ", " + baseUrl + subID + ", " + subsID.getNames().get(i);
            sb.append(st);
            sb.append("\n");
//            success = Utils.downloadZippedSubs(baseUrl + subID, name + ".zip", cookieHeader);
//            if (success)
//            {
//                Utils.unzipSubs(currentFile, name + ".zip", subsID.isCorrectResults());
//            }
        }
        File f = new File(currentFile.getFile().getParent(), currentFile.getFullNameNoExt() + Subs4me.DO_WORK_EXT);
        try
        {
            FileWriter dowrkFile = new FileWriter(f);
            dowrkFile.write(sb.toString());
            dowrkFile.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
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
            if (!subid.startsWith("/"))
            {
                subid = "/" + subid;
            }
            LinkedList<String> intenseFilesList = new LinkedList<String>();
            LinkedList<String> intenseFilesListNames = new LinkedList<String>();
            LinkedList<String> longerFilesList = new LinkedList<String>();
            LinkedList<String> longerFilesListNames = new LinkedList<String>();
            Parser parser;
            try
            {
                parser = new Parser(baseUrl + subid);
                parser.setEncoding("UTF-8");
                
                NodeFilter filter = null;

                //get the number of subs for the hebrew lang
                filter = new AndFilter(new TagNameFilter("label"), new HasAttributeFilter("for", "S_1"));  
                NodeList list = parser.parse(filter);
                if (list.size() == 0)
                {
                    return null;
                }
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
                filter = new AndFilter(new TagNameFilter("tr"), new HasAttributeFilter("title"));
                list = parser.parse(filter);
                if (list.size() == 0)
                {
                    return null;
                }
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
                    {
                        displayNames.add(name);
//                        String dlPlease = Utils.postForFileName(subid.substring(15),
//                                filesTodl.get(i));
                        System.out.println("*** Sratim found exact movie name, proceeding to dl: "
                                + filesTodl.get(i));
                        LinkedList<String> lst = new LinkedList<String>();
                        lst.add(filesTodl.get(i));
                        return new Results(lst, true);
                        // } else if (node.toPlainTextString().trim()
                        // .startsWith(movieName))
                    } else
                    {
                        String remoteName = name;
                        //add the file to longer list
                        if (Subs4me.isFullDownload())
                        {
                            longerFilesList.add(filesTodl.get(i));
                            longerFilesListNames.add(displayNames.get(i));
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
                            intenseFilesListNames.add(displayNames.get(i));
                        }
                    }
                }
                /*
               * Now we get to a dilema:
               * 1. the user did not specify all and there is more than 1 proposal to download
               * 2. the user did specify all
               */
              
              if (Subs4me.isFullDownload())
              {
                  if (longerFilesList.size()>0)
                  {
                      File f = new File(currentFile.getFile().getParent(), currentFile.getFullNameNoExt() + Subs4me.DO_WORK_EXT);
                      try
                      {
                          f.createNewFile();
                      } catch (IOException e)
                      {
                          // TODO Auto-generated catch block
                          e.printStackTrace();
                      }
                  }
                  return new Results(longerFilesList, longerFilesListNames, false);
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
                  return new Results(intenseFilesList, intenseFilesListNames, false);
              }

                
            } catch (ParserException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return null;
        }
        
        public boolean loadSratimCookie()
        {
            return loadSratimCookie(false);
        }
        
        public boolean loadSratimCookie(boolean checkalways)
        {
            // Check if we already logged in and got the correct cookie        
            if (!checkalways && !cookieHeader.equals(""))
                return false;


            // Check if cookie file exist
            try {            
                FileReader cookieFile = new FileReader("sratim.cookie");
                BufferedReader in = new BufferedReader(cookieFile);
                cookieHeader = in.readLine();
                in.close();
            } catch (Exception error) {
            }

            if (!cookieHeader.equals(""))
            {
                // Verify cookie by loading main page
                try {            
                    URL url = new URL("http://www.sratim.co.il/");
                    HttpURLConnection connection = (HttpURLConnection) (url.openConnection());
                    connection.setRequestProperty("Cookie", cookieHeader);

                    //Get Response    
                    InputStream is = connection.getInputStream();
                    BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                    String line;
                    StringBuffer response = new StringBuffer(); 
                    while((line = rd.readLine()) != null) {
                        response.append(line);
                    }
                    rd.close();
                    String xml=response.toString();

                    if (xml.indexOf("logout=1")!=-1) {
                        logger.finest("Sratim Subtitles Cookies Valid");
                        return true;
                    }

                } catch (Exception error) {
                    logger.severe("Error : " + error.getMessage());
                    return false;
                }

                logger.severe("Sratim Cookie Use Failed - Creating new session and jpg files");

                cookieHeader = "";
                File dcookieFile = new File("sratim.cookie");
                dcookieFile.delete();
            }


            // Check if session file exist
            try {            
                FileReader sessionFile = new FileReader("sratim.session");
                BufferedReader in = new BufferedReader(sessionFile);
                cookieHeader = in.readLine();
                in.close();
            } catch (Exception error) {
            }


            // Check if we don't have the verification code yet
            if (!cookieHeader.equals("")) {
                try {            
                    logger.finest("cookieHeader: " + cookieHeader);

                    // Build the post request
                    String post;
                    post = "Username=" + login + "&Password=" + pass + "&VerificationCode=" + code + "&Referrer=%2Fdefault.aspx%3F";

                    logger.finest("post: " + post);


                    URL url = new URL("http://www.sratim.co.il/users/login.aspx");
                    HttpURLConnection connection = (HttpURLConnection) (url.openConnection());
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    connection.setRequestProperty("Content-Length", "" + Integer.toString(post.getBytes().length));
                    connection.setRequestProperty("Accept-Language", "en-us,en;q=0.5");
                    connection.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
                    connection.setRequestProperty("Referer", "http://www.sratim.co.il/users/login.aspx");

                    connection.setRequestProperty("Cookie", cookieHeader);
                    connection.setUseCaches (false);
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    connection.setInstanceFollowRedirects(false);

                    //Send request
                    DataOutputStream wr = new DataOutputStream (connection.getOutputStream ());
                    wr.writeBytes (post);
                    wr.flush ();
                    wr.close ();


                    cookieHeader = "";

                    // read new cookies and update our cookies
                    for (Map.Entry<String, List<String>> header : connection.getHeaderFields().entrySet()) {
                        if ("Set-Cookie".equals(header.getKey())) {
                            for (String rcookieHeader : header.getValue()) {
                                String[] cookieElements = rcookieHeader.split(" *; *");
                                if (cookieElements.length >= 1) {
                                    String[] firstElem = cookieElements[0].split(" *= *");
                                    String cookieName = firstElem[0];
                                    String cookieValue = firstElem.length > 1 ? firstElem[1] : null;

                                    logger.finest("cookieName:" + cookieName);
                                    logger.finest("cookieValue:" + cookieValue);

                                    if (!cookieHeader.equals(""))
                                        cookieHeader = cookieHeader + "; ";
                                    cookieHeader = cookieHeader + cookieName + "=" + cookieValue;
                                }
                            }
                        }
                    }

                    //Get Response    
                    InputStream is = connection.getInputStream();
                    BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                    String line;
                    StringBuffer response = new StringBuffer(); 
                    while((line = rd.readLine()) != null) {
                        response.append(line);
                    }
                    rd.close();
                    String xml=response.toString();

                    if (xml.indexOf("<h2>Object moved to <a href=\"%2fdefault.aspx%3f\">here</a>.</h2>")!=-1) {

                        // write the session cookie to a file
                        FileWriter cookieFile = new FileWriter("sratim.cookie");
                        cookieFile.write(cookieHeader);
                        cookieFile.close();

                        // delete the old session and jpg files
                        File dimageFile = new File("sratim.jpg");
                        dimageFile.delete();

                        File dsessionFile = new File("sratim.session");
                        dsessionFile.delete();

                        return true;
                    }

                    logger.severe("Sratim Login Failed - Creating new session and jpg files");

                } catch (Exception error) {
                    logger.severe("Error : " + error.getMessage());
                    return false;
                }

            }

            try {            
                URL url = new URL("http://www.sratim.co.il/users/login.aspx");
                HttpURLConnection connection = (HttpURLConnection) (url.openConnection());

                cookieHeader = "";

                // read new cookies and update our cookies
                for (Map.Entry<String, List<String>> header : connection.getHeaderFields().entrySet()) {
                    if ("Set-Cookie".equals(header.getKey())) {
                        for (String rcookieHeader : header.getValue()) {
                            String[] cookieElements = rcookieHeader.split(" *; *");
                            if (cookieElements.length >= 1) 
                            {
                                String[] firstElem = cookieElements[0].split(" *= *");
                                String cookieName = firstElem[0];
                                String cookieValue = firstElem.length > 1 ? firstElem[1] : null;

                                logger.finest("cookieName:" + cookieName);
                                logger.finest("cookieValue:" + cookieValue);

                                if (!cookieHeader.equals(""))
                                    cookieHeader = cookieHeader + "; ";
                                cookieHeader = cookieHeader + cookieName + "=" + cookieValue;
                            }
                        }
                    }
                }


                // write the session cookie to a file
                FileWriter sessionFile = new FileWriter("sratim.session");
                sessionFile.write(cookieHeader);
                sessionFile.close();

                // Get the jpg code
                url = new URL("http://www.sratim.co.il/verificationimage.aspx");
                connection = (HttpURLConnection) (url.openConnection());
                connection.setRequestProperty("Cookie", cookieHeader);

                // Write the jpg code to the file
                File imageFile = new File("sratim.jpg");
                Utils.copy(connection.getInputStream(), new FileOutputStream(imageFile));

//                // Exit and wait for the user to type the jpg code
//                logger.severe("#############################################################################");
//                logger.severe("### Open \"sratim.jpg\" file, and write the code in the sratim.code field ###");
//                logger.severe("#############################################################################");
                return false;

            } catch (Exception error) {
                logger.severe("Error : " + error.getMessage());
                return false;
            }

        }

        public static HttpURLConnection createPost(String urlString,
                StringBuffer extraProps)
        {
            URL url;
            HttpURLConnection connection = null;
            PrintWriter out;

            try
            {
                url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
//                _manager.addCookies(connection);

                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setUseCaches(false);

                // more or less of these may be required
                // see Request Header Definitions:
                // http://www.ietf.org/rfc/rfc2616.txt
                connection.setRequestProperty("Accept-Charset", "*");
                connection.setRequestProperty("Accept_Languaget", "en-us,en;q=0.5");
                connection.setRequestProperty("Accept-Encoding", "gzip,deflate");
                connection.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
                connection.setRequestProperty("X-Requested-With", "XMLHttpRequest");
                connection.setRequestProperty("Referer", "www.torec.net");
                connection.setRequestProperty("Cookie", cookieHeader);

                out = new PrintWriter(connection.getOutputStream());
                out.print(extraProps);
                out.close();
            }
            catch (MalformedURLException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch (IOException e1)
            {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            
            return connection;
        }
        
        protected HttpURLConnection postRequest(String url){
            try {
                int queryStart = url.indexOf('&');
                if(queryStart==-1)
                    queryStart = url.length();
                String baseUrl = url.substring(0, queryStart);
                URL ourl = new URL(baseUrl);
                String data="";
                if(queryStart>-1)
                    data = url.substring(queryStart+1);

                // Send data
                HttpURLConnection conn = (HttpURLConnection) ourl.openConnection();
                conn.setDoOutput(true);

                conn.setRequestProperty("Host", "www.sratim.co.il");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.2; en-US; rv:1.9.0.11) Gecko/2009060215 Firefox/3.0.11");
                conn.setRequestProperty("Accept","*/*");
                conn.setRequestProperty("Accept-Language","he");
                conn.setRequestProperty("Accept-Encoding","deflate");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
                conn.setRequestProperty("Accept-Charset","utf-8");
                conn.setRequestProperty("Referer",baseUrl);

                OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                wr.write(data); //post data
                wr.flush();
                wr.close();
                return conn;
            } catch (Exception error) {
                logger.severe("Failed retrieving sratim season episodes information.");
                final Writer eResult = new StringWriter();
                final PrintWriter printWriter = new PrintWriter(eResult);
                error.printStackTrace(printWriter);
                logger.severe(eResult.toString());
            }
            return null;
        }
        
        protected String buildSeasonUrl(String baseUrl, int i, String viewState) throws UnsupportedEncodingException {
            StringBuilder surl = new StringBuilder();

            surl.append(baseUrl);
            if(baseUrl.indexOf('?')>-1)
                surl.append('&');
            else
                surl.append('?');

            surl.append("ctl00$ctl00$Body$ScriptManager=ctl00$ctl00$Body$Body$Box$UpdatePanel|");
            surl.append(SEASON_FORM_VALUE);
            surl.append(i);

            surl.append('&').append(SEASON_FORM_NAME).append('=').append(SEASON_FORM_VALUE).append(i);
            surl.append('&').append(ARGUMENT_FORM_NAME).append('=').append(ARGUMENT_FORM_VALUE);
            surl.append("&ctl00$ctl00$Body$Body$Box$Comments$Header=");
            surl.append("&ctl00$ctl00$Body$Body$Box$Comments$Body=");
            surl.append("&__LASTFOCUS=");
            surl.append('&').append(ARGUMENT_VIEWSTATE).append('=').append(URLEncoder.encode(viewState,"UTF-8"));
            surl.append("&ctl00$ctl00$Body$Body$Box$SubtitlesLanguage=1"); //hebrew subtitles
            surl.append("&__ASYNCPOST=true");

            return surl.toString();
        }
        
        /**
         * 
         * @param url where to download from
         * @param dstZipFilename downloaded zip name
         * @param curr the current file working on so we know where to download and what to rename to
         */
        public void downloadFile(String url, String dstZipFilename, FileStruct curr)
        {
            try {            
                FileReader cookieFile = new FileReader("sratim.cookie");
                BufferedReader in = new BufferedReader(cookieFile);
                cookieHeader = in.readLine();
                in.close();
            } catch (Exception error) 
            {
            }
            boolean cookieOk = loadSratimCookie(true);
            if (!cookieOk)
            {
                Login login = new Login();
                if (!login.isLoginOk())
                {
                    return;
                }
                else
                {
                    cookieOk = true;
                 // Check if cookie file exist
                    try {            
                        FileReader cookieFile = new FileReader("sratim.cookie");
                        BufferedReader in = new BufferedReader(cookieFile);
                        cookieHeader = in.readLine();
                        in.close();
                    } catch (Exception error) 
                    {
                    }
                }
            }
            boolean success = Utils.downloadZippedSubs(url, dstZipFilename + ".zip", cookieHeader);
            if (success)
            {
                Utils.unzipSubs(curr, dstZipFilename + ".zip", true);
            }
        }
        
    public boolean findPicture(FileStruct fs, String id)
    {
        if ((Subs4me.shouldGetPic() && !fs.isHasPic())
                || Subs4me.shouldForceGetPic())
        {
            if (fs.hasPicBeenDownloadedAlready())
                return false;
            
            try
            {
                Parser parser = new Parser(baseUrl + id);
                parser.setEncoding("UTF-8");
                NodeFilter filter = new AndFilter(new TagNameFilter("img"),
                        new HasAttributeFilter("id",
                                "ctl00_ctl00_Body_Body_Box_MainPicture"));

                String imgSRC = null;
                NodeList list = parser.parse(filter);
                for (SimpleNodeIterator iterator = list.elements(); iterator
                        .hasMoreNodes();)
                {
                    TagNode node = (TagNode) iterator.nextNode();
                    imgSRC = node.getAttribute("src");
                }

                if (imgSRC == null)
                    return false;

                URL url = new URL(baseUrl + imgSRC);
                HttpURLConnection connection = (HttpURLConnection) (url
                        .openConnection());
                // connection.setRequestProperty("Cookie", cookieHeader);
                // Write the jpg code to the file
                File imageFile = new File(fs.getSrcDir() + File.separator
                        + fs.getFullNameNoExt() + ".jpg");
                Utils.copy(connection.getInputStream(), new FileOutputStream(
                        imageFile));
                
                fs.setPicAlreadyDownloaded(true);
                
                File folder = new File(fs.getFile().getParent(), "folder.jpg");
                if(!folder.exists())
                {
                    InputStream in = new FileInputStream(imageFile);
                    OutputStream out = new FileOutputStream(folder);

                    // Transfer bytes from in to out
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    in.close();
                    out.close();
                }
                return true;

            } catch (ParserException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (MalformedURLException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return false;
    }
}
