package utils;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.HasParentFilter;
import org.htmlparser.filters.LinkRegexFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

public class Test
{
    
    public static String query  ="/search?hl=en&client=firefox-a&rls=org.mozilla%3Aen-US%3Aofficial&hs=6BY&q=red-flashforward.s01e05.720p.hdtv.x264.mkv+www.tvrage.com&btnG=Search&aq=f&oq=&aqi=";
    public static void main(String[] args)
    {
        try
        {
            Parser parser = new Parser("http://www.google.com" + query);
            parser.setEncoding("UTF-8");
            NodeFilter filter = new AndFilter(new LinkRegexFilter(""),
                    new HasParentFilter(new TagNameFilter("h3"), false));
            
            NodeList list = new NodeList();
            for (NodeIterator e = parser.elements(); e.hasMoreNodes();)
            {
                Node node = e.nextNode();
                node.collectInto(list, filter);
//                 System.out.println(node.toHtml());
            }
            Node[] nodes = list.toNodeArray();
            for (int i = 0; i < nodes.length; i++)
            {
                Node node = nodes[i];
                System.out.println(((TagNode) node).getAttribute("href"));
            }
        }
        catch (ParserException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
