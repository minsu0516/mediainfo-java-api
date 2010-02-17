package utils;

import java.util.Iterator;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.RegexFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.util.SimpleNodeIterator;

public class Wnet
{
    public Wnet()
    {
        startLooking();
    }
    
    private void startLooking()
    {
        String url = "http://www.fbisearchengine.com/index.php?siteName=wnet.co.il&q=" + "ארץ נהדרת S07 E05" + "&submit=  חפש  ";
        String url2 = "http://www.fbisearchengine.com/show.directdownload.php?lid=122386";
        Parser parser;
        try
        {
            parser = new Parser(url);
            parser.setEncoding("UTF-8");
            NodeFilter filter =
//                new TagNameFilter("a");
            new AndFilter(
                    new TagNameFilter("a"), new HasAttributeFilter("class", "p12b"));
            NodeList nodes = parser.parse(filter);
            for (SimpleNodeIterator iterator = nodes.elements(); iterator.hasMoreNodes();)
            {
                Node node = (Node) iterator.nextNode();
                System.out.println(node.toHtml());
            }
        } catch (ParserException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void main(String[] args)
    {
        Wnet net = new Wnet();
    }
}
