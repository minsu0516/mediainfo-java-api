package utils;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import org.htmlparser.beans.StringBean;

/**
 * WhoIs.java
 * Use POST to get information about an IP address from ws.arin.net.
 * Created on April 29, 2006, 11:06 PM
 */
public class WhoIs
{
    String mText; // text extracted from the response to the POST request

    /**
     * Creates a new instance of WhoIs.
     */
    public WhoIs (String ipaddress)
    {
        URL url;
        HttpURLConnection connection;
        StringBuffer buffer;
        PrintWriter out;
        StringBean bean;

        try
        {
            // from the 'action' (relative to the refering page)
            url = new URL ("http://ws.arin.net/cgi-bin/whois.pl");
            connection = (HttpURLConnection)url.openConnection ();
            connection.setRequestMethod ("POST");

            connection.setDoOutput (true);
            connection.setDoInput (true);
            connection.setUseCaches (false);

            // more or less of these may be required
            // see Request Header Definitions: http://www.ietf.org/rfc/rfc2616.txt
            connection.setRequestProperty ("Accept-Charset", "*");
            connection.setRequestProperty ("Referer", "http://ws.arin.net/cgi-bin/whois.pl");
            connection.setRequestProperty ("User-Agent", "WhoIs.java/1.0");

            buffer = new StringBuffer (1024);
            // 'input' fields separated by ampersands (&)
            buffer.append ("queryinput=");
            buffer.append (ipaddress);
            // etc.

            out = new PrintWriter (connection.getOutputStream ());
            out.print (buffer);
            out.close ();

            bean = new StringBean ();
            bean.setConnection (connection);
            mText = bean.getStrings ();
        }
        catch (Exception e)
        {
            mText = e.getMessage ();
        }

    }

    public String getText ()
    {
        return (mText);
    }

    /**
     * Program mainline.
     * @param args The ip address (dot notation) to look up.
     */
    public static void main (String[] args)
    {
        if (0 >= args.length)
            System.out.println ("Usage:  java WhoIs <ipaddress>");
        else
            System.out.println (new WhoIs (args[0]).getText ());
    }
}
