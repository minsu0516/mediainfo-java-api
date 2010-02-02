package utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.RegexFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

public class ScrapeGoogle
{
    public ScrapeGoogle()
    {
        //                checkURLOk("http://www.google.co.il/search?hl=en&source=hp&q=cbgb-aas.mkv%2Bwww.imdb.com%2Ftitle&btnG=Google+Search&aq=f&aqi=&oq=");
        getGoogle("http://www.google.co.il/search?hl=en&source=hp&q=cbgb-aas.mkv%2Bwww.imdb.com%2Ftitle&btnG=Google+Search&aq=f&aqi=&oq=");
    }
    
    public static HttpURLConnection createGoogleSearch(String urlString,
            StringBuffer extraProps)
    {
        URL url;
        HttpURLConnection connection = null;
        PrintWriter out;

        try
        {
            url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);

            // more or less of these may be required
            // see Request Header Definitions:
            // http://www.ietf.org/rfc/rfc2616.txt
            connection.setRequestProperty("Accept-Charset", "*");
            connection.setRequestProperty("Accept_Languaget", "en-us,en;q=0.5");
            connection.setRequestProperty("Accept-Encoding", "gzip,deflate,sdch");
            connection.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
            connection.setRequestProperty("X-Requested-With", "XMLHttpRequest");
            connection.setRequestProperty("Referer", "http://www.google.co.il");
            
            
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US) AppleWebKit/532.5 (KHTML, like Gecko) Chrome/4.0.249.78 Safari/532.5");
           

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
    
    public static String checkURLOk(String url)
    {
        HttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet(url); 
        httpget.setHeader("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US) AppleWebKit/532.5 (KHTML, like Gecko) Chrome/4.0.249.78 Safari/532.5");
//        httpget.setHeader("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.3");
        httpget.setHeader("Accept-Charset", "utf-8;q=0.7,*;q=0.3");
        httpget.setHeader("Accept-Encoding", "gzip,deflate,sdch");
        httpget.setHeader("Avail-Dictionary", "jGXzm-WO");
        httpget.setHeader("Referer", "http://www.google.co.il");
        httpget.setHeader("Accept", "application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
     // Create a response handler
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        try
        {
            String responseBody = httpclient.execute(httpget, responseHandler);
            HttpResponse response = httpclient.execute(httpget);
            if (response.getStatusLine().getStatusCode() == 200
                    && responseBody.indexOf("http://www.imdb.com/title") >-1)
            {
//                int i = responseBody.indexOf("http://www.imdb.com/title");
                Pattern p = Pattern.compile("http://www.imdb.com/title/[a-z,0-9]*");
                Matcher m = p.matcher(responseBody);
                if (m.find())
                {
                    return(m.group());
                }
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
        
        return null;
    }
    
    private void getGoogle(String url)
    {
        Parser parser;
        try
        {
            parser = new Parser(url);
            parser.setEncoding("UTF-8");
            NodeFilter filter = new AndFilter(
                    new TagNameFilter("div"), new HasAttributeFilter("class", "s"));
//            NodeFilter filter = new RegexFilter("www.imdb.com/title</em>/tt[\\d]*");
            NodeList list = new NodeList();
            for (NodeIterator e = parser.elements(); e.hasMoreNodes();)
            {
                Node node =  e.nextNode();
//                System.out.println(node.toHtml());
                node.collectInto(list, filter);
            }
            Node[] nodes = list.toNodeArray();
            for (int i = 0; i < nodes.length; i++)
            {
                Node nd = nodes[i];
                Pattern p = Pattern.compile("http://www.imdb.com/title/[a-z,0-9]*");
                Matcher m = p.matcher(nd.toPlainTextString());
                if (m.find())
                {
                    String me = m.group();
//                    return(m.group());
                }
                System.out.println(nd.toPlainTextString());
            }
            System.out.println("list= " + list);
        } catch (ParserException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
    
    public static void main(String[] args)
    {
        ScrapeGoogle s = new ScrapeGoogle();
    }
}
