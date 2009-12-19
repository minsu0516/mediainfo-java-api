package subs;

import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.HasChildFilter;
import org.htmlparser.filters.HasParentFilter;
import org.htmlparser.filters.LinkRegexFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

public class Test
{
    String mText; // text extracted from the response to the POST request
    
    public void testname() throws Exception
    {
        String movieName = "Children.Of.Men.REPACK.PAL.DVDR-SCREAM";
        searchByActualMovieNameInTorec(movieName);
    }

    /**
     * 
     */
    private void searchByActualMovieNameInTorec(String movieName)
    {
        String movieNameSimplified = normalizeMovieName(movieName);
        
        URL url;
        HttpURLConnection connection;
        StringBuffer buffer;
        PrintWriter out;

        try
        {
            // from the 'action' (relative to the referring page)
            url = new URL ("http://www.torec.net/ssearch.asp");
            connection = (HttpURLConnection)url.openConnection ();
            connection.setRequestMethod ("POST");

            connection.setDoOutput (true);
            connection.setDoInput (true);
            connection.setUseCaches (false);

            // more or less of these may be required
            // see Request Header Definitions: http://www.ietf.org/rfc/rfc2616.txt
            connection.setRequestProperty ("Accept-Charset", "*");
            connection.setRequestProperty ("Referer", "http://www.torec.net/ssearch.asp");

            buffer = new StringBuffer (1024);
            // 'input' fields separated by ampersands (&)
            buffer.append ("search=");
            String[] names =  movieNameSimplified.split(" ");
            for (int i = 0; i < names.length; i++)
            {
                String part = names[i];
                if (i != 0)
                {
                    buffer.append ("+");
                }
                buffer.append (part);
            }

            out = new PrintWriter (connection.getOutputStream ());
            out.print (buffer);
            out.close ();

            Parser parser = new Parser(connection); 
            parser.setEncoding("UTF-8");
            NodeList list = new NodeList();
            NodeFilter filter =
                new AndFilter (
                        new LinkRegexFilter ("sub_id"),
                    new HasChildFilter (
                        new TagNameFilter ("IMG")));

            ArrayList<String> subids = new ArrayList<String>();
            //parsing the links on the search page itself
            for (NodeIterator e = parser.elements(); e.hasMoreNodes();)
            {
                e.nextNode().collectInto(list, filter);
            }
            
            if (!list.toHtml().equals(""))
            {
                Node[] nodes = list.toNodeArray();
                for (int i = 0; i < nodes.length; i++)
                {
                    Node node = nodes[i];
                    subids.add(((TagNode)node).getAttribute("href"));
                    System.out.println("subid = " + subids.get(i));
                }
            }
            
            for (String id : subids)
            {
                String sub = locateFileInFilePageOnTorec(id, movieName, movieNameSimplified);
                if (sub != null)
                {
                    downloadTheSub();
                    return;
                }
            }
        }
        catch (Exception e)
        {
            mText = e.getMessage ();
        }
    }
 
    private void downloadTheSub()
    {
        // TODO Auto-generated method stub
        
    }

    private String locateFileInFilePageOnTorec(String subid, String movieName, String simplified)
    {
        try
        {
            //now we move into the movie page itself
            //bring the table for download files
            Parser parser = new Parser("http://www.torec.net/" + subid); 
            parser.setEncoding("UTF-8");
            NodeFilter filter =
                new AndFilter (
                        new TagNameFilter ("option"),
                    new HasParentFilter (
                        new TagNameFilter("table"), true));
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
                filesTodl.add(node.toPlainTextString().trim());
                System.out.println(node.toPlainTextString().trim());
            }
            
            list = new NodeList();
            
            //bring the table for display names
            parser = new Parser("http://www.torec.net/" + subid); 
            parser.setEncoding("UTF-8");
            
            filter =
                new AndFilter (
                        new TagNameFilter ("span"),
                    new HasParentFilter (
                                  new TagNameFilter("p")));
            
            for (NodeIterator e = parser.elements(); e.hasMoreNodes();)
            {
                e.nextNode().collectInto(list, filter);
            }
            
            ArrayList<String> displayNames = new ArrayList<String>();
            nodes = list.toNodeArray();
            for (int i = 0; i < nodes.length; i++)
            {
                Node node = nodes[i];
                System.out.println(node.toPlainTextString().trim());
                if (node.toPlainTextString().trim().equals(movieName))
                {
                    String dlPlease = filesTodl.get(filesTodl.size()-i -1);
                    displayNames.add(node.toPlainTextString().trim());
                    System.out.println("found movie name proceeding to dl: " + dlPlease);
                    return dlPlease;
                }
            }
        }
        catch (ParserException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        System.out.println("Did not find a file to download");
        return null;
    }
    
    public String normalizeMovieName(String in)
    {
        String pattern = "(PAL)|(DVDR)|(REPACK)|(720p)|(x264)|(BD5)|(bluray)|(-\\w*$)|(\\A\\w*-)";
        //need to take into account that if the is a word at the beginning and then a -
        //its the group name or a cdxxx
        String ret = in.replaceAll("[.]", " ");
        ret = ret.replaceAll(pattern, "");
        
        ret = ret.replaceAll("[ ]+", " ");
        return ret;
    }
    
    
    public static void main(String[] args)
    {
        Test t = new Test();
        try
        {
            t.testname();
        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
