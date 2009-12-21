package utils;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.RegexFilter;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

public class IFeelLucky
{
    static String searchString = "/search;_ylt=A0geu_K5sCNLshcAKflXNyoA?p=inf-lol.mkv+imdb.com%2Ftitle&fr2=sb-top&fr=sfp&sao=0";
    public IFeelLucky()
    {
        String baseUrl = "http://www.yahoo.com";
        Parser parser;
        try
        {
            parser = new Parser("http://search.yahoo.com/search;_ylt=A0geu_K5sCNLshcAKflXNyoA?p=inf-lol.mkv+imdb.com%2Ftitle&fr2=sb-top&fr=sfp&sao=0");
            parser.setEncoding("UTF-8");
            NodeFilter filter = null;

            filter = new RegexFilter("imdb.com");
            NodeList list = new NodeList();
            for (NodeIterator e = parser.elements(); e.hasMoreNodes();)
            {
                Node node = e.nextNode();
//                System.out.println(node.toHtml());
                node.collectInto(list, filter);
                System.out.println(node.toHtml());
            }
            System.out.println(list.toHtml());
            
        } catch (ParserException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args)
    {
        IFeelLucky fl = new IFeelLucky();
    }
}
