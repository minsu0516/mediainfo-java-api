package utils;

import java.awt.Component;
import java.awt.Image;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.LinkRegexFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.filters.RegexFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.json.JSONArray;
import org.json.JSONObject;


public class Utils
{
    private static final String TEMP_SUBS_ZIPPED_FILE = "temp";
    private static final String HTTP_REFERER = "http://www.tvrage.com/";

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
           

            out = new PrintWriter(connection.getOutputStream());
            out.print(extraProps);
            out.close();
        }
        catch (MalformedURLException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return connection;
    }

    /**
     * unzip the files
     * @param zipName 
     * 
     * @param retainTheSameName
     * 
     * @param src
     * @param fileNoExt
     * @param retainTheSameName
     *            - true will keep the original filename as there is only one
     *            download, false will change to filename + entry.srt
     */
    public static void unzipSubs(FileStruct currentFile,
            String zipName, boolean retainTheSameName)
    {
        Enumeration entries;
        ZipFile zipFile;
        String property = "java.io.tmpdir";
        String tempDir = System.getProperty(property);
        try
        {
            zipFile = new ZipFile(tempDir + zipName);
            entries = zipFile.entries();
            while (entries.hasMoreElements())
            {
                ZipEntry entry = (ZipEntry) entries.nextElement();

                if (entry.isDirectory())
                {
                    // Assume directories are stored parents first then
                    // children.
                    // System.err.println("Extracting directory: "
                    // + entry.getName());
                    // This is not robust, just for demonstration purposes.
                    (new File(entry.getName())).mkdir();
                    continue;
                }

                // System.err.println("Extracting file: " + entry.getName());
                String destFileName = "";
                if (entry.getName().endsWith(".srt"))
                {
                    if (retainTheSameName)
                    {
                        destFileName = currentFile.buildDestSrt();
                    }
                    else
                    {
                        destFileName = currentFile.buildDestSrt() + "."
                                + entry.getName();
                    }
                    // destFileName = currentFile.getSrcDir() + File.separator
                    // + currentFile.getNameNoExt() + ".srt";
                }
                else
                {
                    destFileName = currentFile.getSrcDir() + File.separator
                            + entry.getName();
                }
                copyInputStream(zipFile.getInputStream(entry),
                        new BufferedOutputStream(new FileOutputStream(
                                destFileName)));
            }
            zipFile.close();
            new File(tempDir + zipName).delete();
        }
        catch (IOException ioe)
        {
            // System.err.println("Unhandled exception:");
            ioe.printStackTrace();
            return;
        }
    }

    public static final void copyInputStream(InputStream in, OutputStream out)
            throws IOException
    {
        byte[] buffer = new byte[1024];
        int len;

        while ((len = in.read(buffer)) >= 0)
            out.write(buffer, 0, len);

        in.close();
        out.close();
    }

    /**
     * 
     * @param file
     * @param fileName 
     * @return succeeded true or false
     */
    public static boolean downloadZippedSubs(String file, String fileName)
    {
        return downloadZippedSubs(file, fileName, null);
    }
    /**
     * 
     * @param file
     * @param fileName 
     * @param cookie
     * @return succeeded true or false
     */
    public static boolean downloadZippedSubs(String file, String fileName, String cookie)
    {
        StringBuffer sb = new StringBuffer(file);
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        String property = "java.io.tmpdir";
        String tempDir = System.getProperty(property);
        
        URL url;
        try
        {
            File tmpDir = new File(tempDir);
            if (!tmpDir.exists());
            {
                tmpDir.mkdirs();
            }
            File destination = null;
            destination = new File(tempDir + fileName);
            url = new URL(sb.toString());
            URLConnection urlc = url.openConnection();
            if (cookie != null)
            {
                urlc.setRequestProperty("Cookie", cookie);
            }
            
            bis = new BufferedInputStream(urlc.getInputStream());
            bos = new BufferedOutputStream(new FileOutputStream(destination));

            System.out.println("downloading " + sb.toString());
            int i;
            while ((i = bis.read()) != -1)
            {
                bos.write(i);
            }
            if (bis != null)
                try
                {
                    bis.close();
                }
                catch (IOException ioe)
                {
                    ioe.printStackTrace();
                }
            if (bos != null)
                try
                {
                    bos.close();
                }
                catch (IOException ioe)
                {
                    ioe.printStackTrace();
                }
            return true;
        }
        catch (MalformedURLException e1)
        {
            return false;
            // System.out.println("could not download subs: " +
            // e1.getMessage());
        }
        catch (IOException e)
        {
            return false;
            // System.out.println("could not download subs: " + e.getMessage());
        }
        finally
        {
        }
        // return false;
    }

    public static String escape(String string)
    {
        return string.replaceAll(" ", "%20");
    }

    public static boolean isInRange(String num, String range)
    {
        if (range == null)
            return false;
        
        if (num.equals(range))
        {
            return true;
        }

        if (range.indexOf("-") > -1)
        {
            int start = Integer.parseInt(range.substring(0, range.indexOf("-"))
                    .trim());
            int end = Integer.parseInt(range.substring(range.indexOf("-") + 1)
                    .trim());
            for (int i = start; i < end; i++)
            {
                if (num.equals(Integer.toString(i)))
                    return true;
            }
            return false;
        }

        return false;
    }

    public static boolean compareHDLevel(String n1, FileStruct f1)
    {
        Pattern p1 = Pattern.compile(FileStruct.hdLevel);
        Matcher m1 = p1.matcher(n1);
        String fileHd = null;
        if (m1.find())
        {
            fileHd = m1.group();
        }
        if (fileHd != null)
            fileHd = fileHd.replaceAll("[pP]", "");

        if (fileHd == f1.getHDLevel())
        {
            return true;
        }

        if ((fileHd == null && f1.getHDLevel() != null)
                || (fileHd != null && f1.getHDLevel() == null))
        {
            return false;
        }

        if (fileHd.equalsIgnoreCase(f1.getHDLevel()))
            return true;

        return false;
    }

    public static boolean compareReleaseSources(String n1, FileStruct f1)
    {
        Pattern p1 = Pattern.compile(FileStruct.releaseSourcePattern);
        Matcher m1 = p1.matcher(n1);
        String fileSource = null;
        if (m1.find())
        {
            fileSource = m1.group();
        }

        if (fileSource == f1.getSource())
        {
            return true;
        }

        if ((fileSource == null && f1.getSource() != null)
                || (fileSource != null && f1.getSource() == null))
        {
            return false;
        }

        if (fileSource.equalsIgnoreCase(f1.getSource()))
            return true;

        return false;
    }

    public static String parseReleaseName(String name)
    {
        String group = "(-\\w*(-)|($))|(-\\w*$)|(\\A\\w*-)";
        Pattern p1 = Pattern.compile(group);
        Matcher m1 = p1.matcher(name);
        if (m1.find())
        {
            return m1.group().replaceAll("-", "");
        }

        return "";
    }

    public static boolean isSameMovie(FileStruct ff1, FileStruct ff2)
    {
//        if (!ff1.getReleaseName().equalsIgnoreCase(ff2.getReleaseName()))
//        {
//            return false;
//        }
        String file1 = ff1.getFullFileNameNoGroup();
        String file2 = ff2.getFullFileNameNoGroup();
        
        if (file1.equals(file2))
            return true;

        String f1 = file1.replaceAll("[.]", "").replaceAll(" ", "").replaceAll("-", "");
        String f2 = file2.replaceAll("[.]", "").replaceAll(" ", "").replaceAll("-", "");
        if (f1.equalsIgnoreCase(f2))
            return true;

        return false;
    }
    
    public static boolean isSameMovie2(String ff1, String ff2)
    {
        if (ff1.equals(ff2))
            return true;

        String f1 = ff1.replaceAll("[.]", "").replaceAll(" ", "").replaceAll("-", "");
        String f2 = ff2.replaceAll("[.]", "").replaceAll(" ", "").replaceAll("-", "");
        if (f1.equalsIgnoreCase(f2))
            return true;

        return false;
    }

    /**
     * chekc if a movie starts with the same name
     * @param ff1
     * @param ff2
     * @return
     */
    public static boolean isSameMovie3(String ff1, String original)
    {
        if (ff1.equals(original))
            return true;

        String f1 = ff1.replaceAll("[.]", "").replaceAll(" ", "").replaceAll("-", "").toLowerCase();
        String f2 = original.replaceAll("[.]", "").replaceAll(" ", "").replaceAll("-", "").toLowerCase();
        if (f1.startsWith(f2))
            return true;

        return false;
    }
    
 // Get the contents of a URL and return it as an image
    public static Image fetchimage(String address, Component c)
            throws MalformedURLException, IOException
    {
        URL url = new URL(address);
        return c.createImage((java.awt.image.ImageProducer) url.getContent());
    }
    
    public static String searchRealTVShowNameUsingGoogle(String fileName)
    {
        return searchRealNameUsingGoogle(fileName, "www.tvrage.com");
    }
    
    public static String searchRealMovieNameUsingGoogle(String fileName)
    {
        return searchRealNameUsingGoogle(fileName, "www.imdb.com");
    }
    
    /**
     * try to locate a page where the file name is mentioned and an imdb site url is also present
     * @param q 
     * @return
     */
    public static String searchRealNameUsingGoogle(String fileName, String searchForCritiria)
    {
        String query = fileName + " " + searchForCritiria; 
        System.out.println("Querying Google for " + query);
        try
        {
            // Convert spaces to +, etc. to make a valid URL
            query = URLEncoder.encode(query, "UTF-8");
            URL url = new URL(
                    "http://ajax.googleapis.com/ajax/services/search/web?v=1.0&q="
                            + query);
            URLConnection connection = url.openConnection();
            connection.addRequestProperty("Referer", HTTP_REFERER);

            // Get the JSON response
            String line;
            StringBuilder builder = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
            while ((line = reader.readLine()) != null)
            {
                builder.append(line);
            }

            String response = builder.toString();
            JSONObject json = new JSONObject(response);

            JSONArray ja = json.getJSONObject("responseData").getJSONArray("results");

//            System.out.println("Total results = "
//                    + json.getJSONObject("responseData")
//                            .getJSONObject("cursor").getString(
//                                    "estimatedResultCount"));

//            JSONArray ja = json.getJSONObject("responseData").getJSONArray(
//                    "results");

//            System.out.println(" Results:");
            if (ja != null && ja.length() > 0)
            {
                for (int i = 0; i < ja.length(); i++)
                {
                    JSONObject j = ja.getJSONObject(i);
                    String urlTemp = j.getString("url");
                    urlTemp = URLDecoder.decode(urlTemp, "UTF-8");
                    if (checkURLOk(urlTemp))
                    {
                        return urlTemp;
                    }
                }
               
            }
//            for (int i = 0; i < ja.length(); i++)
//            {
//                System.out.print((i + 1) + ". ");
//                JSONObject j = ja.getJSONObject(i);
//                System.out.println(j.getString("titleNoFormatting"));
//                System.out.println(j.getString("url"));
//            }
        } catch (Exception e)
        {
            System.err.println("Something went wrong...");
            e.printStackTrace();
        }
        return null;
    }
    
    public static boolean checkURLOk(String url)
    {
        boolean ret = false;
        HttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet(url); 
     // Create a response handler
//        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        try
        {
            HttpResponse response = httpclient.execute(httpget);
            if (response.getStatusLine().getStatusCode() == 200)
            {
                ret = true;
            }
            // When HttpClient instance is no longer needed, 
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            httpclient.getConnectionManager().shutdown();        
        } 
        catch (ClientProtocolException e)
        {
        } 
        catch (IOException e)
        {
        }
        
        return ret;
    }
    
    public static String locateRealNameUsingGoogle(String fullName)
    {
        String name = locateRealNameUsingGoogle(fullName, "www.imdb.com");
        if (name == null)
        {
            name = locateRealNameUsingGoogle(fullName, "www.tv.com");
        }
        return name;
    }

    /**
     * find out the real name of the tvshow/movie
     * @param fullName
     * @return
     */
    public static String locateRealNameUsingGoogle(String fullName, String critiria)
    {
      //try to find the real name of the movie using google
        String imdbPointingUrl = Utils.searchRealNameUsingGoogle(fullName, critiria);
        if (imdbPointingUrl != null)
        {
            Parser parser;
            try
            {
                parser = new Parser(imdbPointingUrl);
                parser.setEncoding("UTF-8");
                NodeFilter filter = new OrFilter( 
                                        new RegexFilter(critiria), 
                                        new LinkRegexFilter(critiria));
                
                NodeList list = new NodeList();
                for (NodeIterator e = parser.elements(); e.hasMoreNodes();)
                {
                    Node node = e.nextNode();
                    node.collectInto(list, filter);
                    // System.out.println(node.toHtml());
                }
                Node[] nodes = list.toNodeArray();
                if (nodes.length == 0)
                    return null;
                
                String imdbTitleUrl = null;
                if (nodes[0] instanceof LinkRegexFilter)
                {
                    imdbTitleUrl = ((LinkTag)nodes[0]).getLink();
                }
                else
                {
                    Pattern p = Pattern.compile("http://.*title/.*/");
                    String url = "";
                    if (nodes[0] instanceof TextNode)
                    {
                        url = ((TextNode)nodes[0]).getText();
                    }
                    else if (nodes[0] instanceof LinkTag)
                    {
                        url = ((LinkTag)nodes[0]).getText();
                    }
                    Matcher m = p.matcher(url);
                    if (m.find())
                    {
                        imdbTitleUrl = m.group();
                    }
                }
                if (imdbTitleUrl == null)
                {
                    return null;
                }
                
                parser = new Parser(imdbTitleUrl);
                parser.setEncoding("UTF-8");
                filter = new TagNameFilter("title");
                list = parser.parse(filter);
                String tmpName = list.toNodeArray()[0].toPlainTextString().replaceAll(",", "");
                tmpName = tmpName.replaceAll("\\([\\d]*\\)$", "");
                System.out.println("*** Google says - Movie real name is:" + tmpName);
                return tmpName;
                
            } catch (ParserException e1)
            {
                System.out.println("*********** Error trying to get file name using google for: " + fullName);
            }
        }
        else
        {
            
        }
        
        return null;
    }
    
    public static boolean isMovieFile(FileStruct file)
    {
        if (file.getExt().equalsIgnoreCase("mkv")
                || file.getExt().equalsIgnoreCase("avi"))
        {
            return true;
        }
        
        return false;
    }
    
    final static int BUFF_SIZE = 100000;
    final static byte[] buffer = new byte[BUFF_SIZE];
    public static int copy(InputStream is, OutputStream os) throws IOException {
        int bytesCopied = 0;
        try {
            while (true) {
                synchronized (buffer) {
                    int amountRead = is.read(buffer);
                    if (amountRead == -1) {
                        break;
                    }
                    else {
                        bytesCopied += amountRead;
                    }
                    os.write(buffer, 0, amountRead);
                }
            }
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException error) {
                // ignore
            }
            try {
                if (os != null) {
                    os.close();
                }
            } catch (IOException error) {
                // ignore
            }
        }
        return bytesCopied;
    }

    
}
