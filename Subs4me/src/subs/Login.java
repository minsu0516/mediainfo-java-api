package subs;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.htmlparser.http.ConnectionManager;

import utils.PropertiesUtil;
import utils.Utils;

class Login extends JFrame implements ActionListener
{
    public static final String baseUrl = "http://sratim.co.il";
    static ConnectionManager _manager;
    private static Logger logger = Logger.getLogger("login");

    JButton SUBMIT = null;
    JPanel panel = null;
    JLabel label1, label2 = null;
    JTextField text1 = null;
    JTextField text2 = null;
    JTextField capatchText = null;
    JLabel captchaImage = null;
    JPanel capatcha = null;

    String login = null;
    String pass = null;
    String code = null;

    public Login()
    {
        buildUi();
        login = PropertiesUtil.getProperty("sratim.username", "");
        pass = PropertiesUtil.getProperty("sratim.password", "");
        code = PropertiesUtil.getProperty("sratim.code", "");

        getImage();
    }

    private void buildUi()
    {
        label1 = new JLabel();
        label1.setText("Username:");
        text1 = new JTextField(login, 15);

        label2 = new JLabel();
        label2.setText("Password:");
        text2 = new JTextField(pass, 15);
        capatchText = new JTextField(code, 15);
        captchaImage = new JLabel();
        captchaImage.setSize(100, 60);
        SUBMIT = new JButton("SUBMIT");

        panel = new JPanel(new GridLayout(5, 1));
        panel.add(label1);
        panel.add(text1);
        panel.add(label2);
        panel.add(text2);
        panel.add(new Label("captcha"));
        panel.add(capatchText);
        panel.add(captchaImage);
        panel.add(new Label());
        panel.add(SUBMIT);
        add(panel, BorderLayout.CENTER);
        SUBMIT.addActionListener(this);
        setTitle("Sratim Login - Please enter correct info");
    }

    private void getImage()
    {
        try
        {
            URL url = new URL("http://www.sratim.co.il/users/login.aspx");
            HttpURLConnection connection = (HttpURLConnection) (url
                    .openConnection());

            cookieHeader = "";

            // read new cookies and update our cookies
            for (Map.Entry<String, List<String>> header : connection
                    .getHeaderFields().entrySet())
            {
                if ("Set-Cookie".equals(header.getKey()))
                {
                    for (String rcookieHeader : header.getValue())
                    {
                        String[] cookieElements = rcookieHeader.split(" *; *");
                        if (cookieElements.length >= 1)
                        {
                            String[] firstElem = cookieElements[0]
                                    .split(" *= *");
                            String cookieName = firstElem[0];
                            String cookieValue = firstElem.length > 1 ? firstElem[1]
                                    : null;

                            logger.finest("cookieName:" + cookieName);
                            logger.finest("cookieValue:" + cookieValue);

                            if (!cookieHeader.equals(""))
                                cookieHeader = cookieHeader + "; ";
                            cookieHeader = cookieHeader + cookieName + "="
                                    + cookieValue;
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

            ImageIcon icon = new ImageIcon(ImageIO.read(connection
                    .getInputStream()));
            captchaImage.setIcon(icon);

        } catch (Exception error)
        {
            logger.severe("Error : " + error.getMessage());
            return;
        }
    }

    public void actionPerformed(ActionEvent ae)
    {
        final String value1 = text1.getText();
        final String value2 = text2.getText();
       
        PropertiesUtil.setProperty("sratim.username", login);
        PropertiesUtil.setProperty("sratim.password", pass);
        PropertiesUtil.setProperty("sratim.code", code);
        
        // Check if cookie file exist
        try
        {
            FileReader cookieFile = new FileReader("sratim.cookie");
            BufferedReader in = new BufferedReader(cookieFile);
            cookieHeader = in.readLine();
            in.close();
        } catch (Exception error)
        {
        }

        if (!cookieHeader.equals(""))
        {
            // Verify cookie by loading main page
            try
            {
                URL url = new URL("http://www.sratim.co.il/");
                HttpURLConnection connection = (HttpURLConnection) (url
                        .openConnection());
                connection.setRequestProperty("Cookie", cookieHeader);

                // Get Response
                InputStream is = connection.getInputStream();
                BufferedReader rd = new BufferedReader(
                        new InputStreamReader(is));
                String line;
                StringBuffer response = new StringBuffer();
                while ((line = rd.readLine()) != null)
                {
                    response.append(line);
                }
                rd.close();
                String xml = response.toString();

                if (xml.indexOf("logout=1") != -1)
                {
                    logger.finest("Sratim Subtitles Cookies Valid");
                    return;
                }

            } catch (Exception error)
            {
                logger.severe("Error : " + error.getMessage());
                return;
            }

            logger
                    .severe("Sratim Cookie Use Failed - Creating new session and jpg files");

            cookieHeader = "";
            File dcookieFile = new File("c:/1/sratim.cookie");
            dcookieFile.delete();
        }

        // Check if session file exist
        try
        {
            FileReader sessionFile = new FileReader("sratim.session");
            BufferedReader in = new BufferedReader(sessionFile);
            cookieHeader = in.readLine();
            in.close();
        } catch (Exception error)
        {
        }

        // Check if we don't have the verification code yet
        if (!cookieHeader.equals(""))
        {
            try
            {
                logger.finest("cookieHeader: " + cookieHeader);

                // Build the post request
                String post;
                post = "Username=" + login + "&Password=" + pass
                        + "&VerificationCode=" + capatchText.getText()
                        + "&Referrer=%2Fdefault.aspx%3F";

                logger.finest("post: " + post);

                URL url = new URL("http://www.sratim.co.il/users/login.aspx");
                HttpURLConnection connection = (HttpURLConnection) (url
                        .openConnection());
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type",
                        "application/x-www-form-urlencoded");
                connection.setRequestProperty("Content-Length", ""
                        + Integer.toString(post.getBytes().length));
                connection.setRequestProperty("Accept-Language",
                        "en-us,en;q=0.5");
                connection.setRequestProperty("Accept-Charset",
                        "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
                connection.setRequestProperty("Referer",
                        "http://www.sratim.co.il/users/login.aspx");

                connection.setRequestProperty("Cookie", cookieHeader);
                connection.setUseCaches(false);
                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.setInstanceFollowRedirects(false);

                // Send request
                DataOutputStream wr = new DataOutputStream(connection
                        .getOutputStream());
                wr.writeBytes(post);
                wr.flush();
                wr.close();

                cookieHeader = "";

                // read new cookies and update our cookies
                for (Map.Entry<String, List<String>> header : connection
                        .getHeaderFields().entrySet())
                {
                    if ("Set-Cookie".equals(header.getKey()))
                    {
                        for (String rcookieHeader : header.getValue())
                        {
                            String[] cookieElements = rcookieHeader
                                    .split(" *; *");
                            if (cookieElements.length >= 1)
                            {
                                String[] firstElem = cookieElements[0]
                                        .split(" *= *");
                                String cookieName = firstElem[0];
                                String cookieValue = firstElem.length > 1 ? firstElem[1]
                                        : null;

                                logger.finest("cookieName:" + cookieName);
                                logger.finest("cookieValue:" + cookieValue);

                                if (!cookieHeader.equals(""))
                                    cookieHeader = cookieHeader + "; ";
                                cookieHeader = cookieHeader + cookieName + "="
                                        + cookieValue;
                            }
                        }
                    }
                }

                // Get Response
                InputStream is = connection.getInputStream();
                BufferedReader rd = new BufferedReader(
                        new InputStreamReader(is));
                String line;
                StringBuffer response = new StringBuffer();
                while ((line = rd.readLine()) != null)
                {
                    response.append(line);
                }
                rd.close();
                String xml = response.toString();

                if (xml
                        .indexOf("<h2>Object moved to <a href=\"%2fdefault.aspx%3f\">here</a>.</h2>") != -1)
                {

                    // write the session cookie to a file
                    FileWriter cookieFile = new FileWriter("sratim.cookie");
                    cookieFile.write(cookieHeader);
                    cookieFile.close();

                    // delete the old session and jpg files
                    File dimageFile = new File("sratim.jpg");
                    dimageFile.delete();

                    File dsessionFile = new File("sratim.session");
                    dsessionFile.delete();

                    return;
                }

                logger
                        .severe("Sratim Login Failed - Creating new session and jpg files");

            } catch (Exception error)
            {
                logger.severe("Error : " + error.getMessage());
                return;
            }

        }

        // String appURL=baseUrl + "/users/login.aspx";
        // HttpPost post = new HttpPost(appURL);
        // StringEntity stEntity = null;
        // try
        // {
        // stEntity = new StringEntity(sb.toString());
        // } catch (UnsupportedEncodingException e1)
        // {
        // // TODO Auto-generated catch block
        // e1.printStackTrace();
        // }
        //        
        // HttpPost httppost = new HttpPost(appURL);
        // httppost.addHeader("Cookie", cookie.get(0));
        // httppost.setEntity(stEntity);
        //        
        // DefaultHttpClient httpclient = new DefaultHttpClient();
        // HttpResponse response;
        // try
        // {
        // response = httpclient.execute(httppost);
        // Header[] headers = response.getHeaders(SET_COOKIE);
        // for (int i = 0; i < headers.length; i++)
        // {
        // Header header = headers[i];
        // if (header.getName().equalsIgnoreCase(SET_COOKIE))
        // {
        //
        // }
        // }
        // } catch (ClientProtocolException e)
        // {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // } catch (IOException e)
        // {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }

        // post.addRequestHeader("Cookie",
        // "ASP.NET_SessionId=ahdoxxfc4df5jlbjvpqzpz55");
        // NameValuePair[] data = {
        // new NameValuePair("Username", value1),
        // new NameValuePair("Password", value2),
        // new NameValuePair("VerificationCode", capatchText.getText()),
        // new NameValuePair("Referrer", "www.sratim.co.il")
        // };
        // post.setRequestBody(data);

        // Parser parser;
        // try
        // {
        // parser = new Parser(connection);
        // }
        // catch (ParserException e)
        // {
        // // // TODO Auto-generated catch block
        // // e.printStackTrace();
        // }

        if (value1.equals("roseindia") && value2.equals("roseindia"))
        {
            // NextPage page = new NextPage();
            // page.setVisible(true);
            // JLabel label = new JLabel("Welcome:" + value1);
            // page.getContentPane().add(label);
        } else
        {
            // System.out.println("enter the valid username and password");
            // JOptionPane.showMessageDialog(this,
            // "Incorrect login or password",
            // "Error", JOptionPane.ERROR_MESSAGE);
            // getImage();
        }
    }

    private static String cookieHeader = "";

    protected String postRequest(String url)
    {
        StringBuilder content = new StringBuilder();
        try
        {
            int queryStart = url.indexOf('&');
            if (queryStart == -1)
                queryStart = url.length();
            String baseUrl = url.substring(0, queryStart);
            URL ourl = new URL(baseUrl);
            String data = "";
            if (queryStart > -1)
                data = url.substring(queryStart + 1);

            // Send data
            URLConnection conn = ourl.openConnection();
            conn.setDoOutput(true);

            conn.setRequestProperty("Host", "www.sratim.co.il");
            conn
                    .setRequestProperty(
                            "User-Agent",
                            "Mozilla/5.0 (Windows; U; Windows NT 5.2; en-US; rv:1.9.0.11) Gecko/2009060215 Firefox/3.0.11");
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Accept-Language", "he");
            conn.setRequestProperty("Accept-Encoding", "deflate");
            conn.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded; charset=utf-8");
            conn.setRequestProperty("Accept-Charset", "utf-8");
            conn.setRequestProperty("Referer", baseUrl);

            OutputStreamWriter wr = new OutputStreamWriter(conn
                    .getOutputStream());
            wr.write(data); // post data
            wr.flush();

            conn.getHeaderFields(); // unused
            // Get the response
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn
                    .getInputStream(), "UTF-8"));
            String line;

            while ((line = rd.readLine()) != null)
            {
                content.append(line);
            }
            wr.close();
            rd.close();
        } catch (Exception error)
        {
            logger
                    .severe("Failed retrieving sratim season episodes information.");
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
        }
        return content.toString();
    }

//    public void loadSratimCookie()
//    {
//
//        // Check if we already logged in and got the correct cookie
//        if (!cookieHeader.equals(""))
//            return;
//
//        // Check if cookie file exist
//        try
//        {
//            FileReader cookieFile = new FileReader("sratim.cookie");
//            BufferedReader in = new BufferedReader(cookieFile);
//            cookieHeader = in.readLine();
//            in.close();
//        } catch (Exception error)
//        {
//        }
//
//        if (!cookieHeader.equals(""))
//        {
//            // Verify cookie by loading main page
//            try
//            {
//                URL url = new URL("http://www.sratim.co.il/");
//                HttpURLConnection connection = (HttpURLConnection) (url
//                        .openConnection());
//                connection.setRequestProperty("Cookie", cookieHeader);
//
//                // Get Response
//                InputStream is = connection.getInputStream();
//                BufferedReader rd = new BufferedReader(
//                        new InputStreamReader(is));
//                String line;
//                StringBuffer response = new StringBuffer();
//                while ((line = rd.readLine()) != null)
//                {
//                    response.append(line);
//                }
//                rd.close();
//                String xml = response.toString();
//
//                if (xml.indexOf("logout=1") != -1)
//                {
//                    logger.finest("Sratim Subtitles Cookies Valid");
//                    return;
//                }
//
//            } catch (Exception error)
//            {
//                logger.severe("Error : " + error.getMessage());
//                return;
//            }
//
//            logger
//                    .severe("Sratim Cookie Use Failed - Creating new session and jpg files");
//
//            cookieHeader = "";
//            File dcookieFile = new File("c:/1/sratim.cookie");
//            dcookieFile.delete();
//        }
//
//        // Check if session file exist
//        try
//        {
//            FileReader sessionFile = new FileReader("sratim.session");
//            BufferedReader in = new BufferedReader(sessionFile);
//            cookieHeader = in.readLine();
//            in.close();
//        } catch (Exception error)
//        {
//        }
//
//        // Check if we don't have the verification code yet
//        if (!cookieHeader.equals(""))
//        {
//            try
//            {
//                logger.finest("cookieHeader: " + cookieHeader);
//
//                // Build the post request
//                String post;
//                post = "Username=" + login + "&Password=" + pass
//                        + "&VerificationCode=" + code
//                        + "&Referrer=%2Fdefault.aspx%3F";
//
//                logger.finest("post: " + post);
//
//                URL url = new URL("http://www.sratim.co.il/users/login.aspx");
//                HttpURLConnection connection = (HttpURLConnection) (url
//                        .openConnection());
//                connection.setRequestMethod("POST");
//                connection.setRequestProperty("Content-Type",
//                        "application/x-www-form-urlencoded");
//                connection.setRequestProperty("Content-Length", ""
//                        + Integer.toString(post.getBytes().length));
//                connection.setRequestProperty("Accept-Language",
//                        "en-us,en;q=0.5");
//                connection.setRequestProperty("Accept-Charset",
//                        "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
//                connection.setRequestProperty("Referer",
//                        "http://www.sratim.co.il/users/login.aspx");
//
//                connection.setRequestProperty("Cookie", cookieHeader);
//                connection.setUseCaches(false);
//                connection.setDoInput(true);
//                connection.setDoOutput(true);
//                connection.setInstanceFollowRedirects(false);
//
//                // Send request
//                DataOutputStream wr = new DataOutputStream(connection
//                        .getOutputStream());
//                wr.writeBytes(post);
//                wr.flush();
//                wr.close();
//
//                cookieHeader = "";
//
//                // read new cookies and update our cookies
//                for (Map.Entry<String, List<String>> header : connection
//                        .getHeaderFields().entrySet())
//                {
//                    if ("Set-Cookie".equals(header.getKey()))
//                    {
//                        for (String rcookieHeader : header.getValue())
//                        {
//                            String[] cookieElements = rcookieHeader
//                                    .split(" *; *");
//                            if (cookieElements.length >= 1)
//                            {
//                                String[] firstElem = cookieElements[0]
//                                        .split(" *= *");
//                                String cookieName = firstElem[0];
//                                String cookieValue = firstElem.length > 1 ? firstElem[1]
//                                        : null;
//
//                                logger.finest("cookieName:" + cookieName);
//                                logger.finest("cookieValue:" + cookieValue);
//
//                                if (!cookieHeader.equals(""))
//                                    cookieHeader = cookieHeader + "; ";
//                                cookieHeader = cookieHeader + cookieName + "="
//                                        + cookieValue;
//                            }
//                        }
//                    }
//                }
//
//                // Get Response
//                InputStream is = connection.getInputStream();
//                BufferedReader rd = new BufferedReader(
//                        new InputStreamReader(is));
//                String line;
//                StringBuffer response = new StringBuffer();
//                while ((line = rd.readLine()) != null)
//                {
//                    response.append(line);
//                }
//                rd.close();
//                String xml = response.toString();
//
//                if (xml
//                        .indexOf("<h2>Object moved to <a href=\"%2fdefault.aspx%3f\">here</a>.</h2>") != -1)
//                {
//
//                    // write the session cookie to a file
//                    FileWriter cookieFile = new FileWriter("sratim.cookie");
//                    cookieFile.write(cookieHeader);
//                    cookieFile.close();
//
//                    // delete the old session and jpg files
//                    File dimageFile = new File("sratim.jpg");
//                    dimageFile.delete();
//
//                    File dsessionFile = new File("sratim.session");
//                    dsessionFile.delete();
//
//                    return;
//                }
//
//                logger
//                        .severe("Sratim Login Failed - Creating new session and jpg files");
//
//            } catch (Exception error)
//            {
//                logger.severe("Error : " + error.getMessage());
//                return;
//            }
//
//        }
//
//        try
//        {
//            URL url = new URL("http://www.sratim.co.il/users/login.aspx");
//            HttpURLConnection connection = (HttpURLConnection) (url
//                    .openConnection());
//
//            cookieHeader = "";
//
//            // read new cookies and update our cookies
//            for (Map.Entry<String, List<String>> header : connection
//                    .getHeaderFields().entrySet())
//            {
//                if ("Set-Cookie".equals(header.getKey()))
//                {
//                    for (String rcookieHeader : header.getValue())
//                    {
//                        String[] cookieElements = rcookieHeader.split(" *; *");
//                        if (cookieElements.length >= 1)
//                        {
//                            String[] firstElem = cookieElements[0]
//                                    .split(" *= *");
//                            String cookieName = firstElem[0];
//                            String cookieValue = firstElem.length > 1 ? firstElem[1]
//                                    : null;
//
//                            logger.finest("cookieName:" + cookieName);
//                            logger.finest("cookieValue:" + cookieValue);
//
//                            if (!cookieHeader.equals(""))
//                                cookieHeader = cookieHeader + "; ";
//                            cookieHeader = cookieHeader + cookieName + "="
//                                    + cookieValue;
//                        }
//                    }
//                }
//            }
//
//            // write the session cookie to a file
//            FileWriter sessionFile = new FileWriter("sratim.session");
//            sessionFile.write(cookieHeader);
//            sessionFile.close();
//
//            // Get the jpg code
//            url = new URL("http://www.sratim.co.il/verificationimage.aspx");
//            connection = (HttpURLConnection) (url.openConnection());
//            connection.setRequestProperty("Cookie", cookieHeader);
//
//            // Write the jpg code to the file
//            File imageFile = new File("sratim.jpg");
//            Utils.copy(connection.getInputStream(), new FileOutputStream(
//                    imageFile));
//
//            // Exit and wait for the user to type the jpg code
//            logger
//                    .severe("#############################################################################");
//            logger
//                    .severe("### Open \"sratim.jpg\" file, and write the code in the sratim.code field ###");
//            logger
//                    .severe("#############################################################################");
//            System.exit(0);
//
//        } catch (Exception error)
//        {
//            logger.severe("Error : " + error.getMessage());
//            return;
//        }
//
//    }
//    
    public static void main(String arg[])
    {
        try
        {
            Login frame = new Login();
            frame.setSize(250, 200);
            frame.setVisible(true);
        } catch (Exception e)
        {
            JOptionPane.showMessageDialog(null, e.getMessage());
        }
    }
}