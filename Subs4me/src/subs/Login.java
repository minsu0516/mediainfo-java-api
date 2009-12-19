package subs;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.htmlparser.Parser;
import org.htmlparser.http.ConnectionManager;
import org.htmlparser.util.ParserException;

import utils.Utils;

class Login extends JFrame implements ActionListener
{
    public static final String baseUrl ="http://sratim.co.il";
    
    JButton SUBMIT;
    JPanel  panel;
    JLabel  label1, label2;
    final JTextField text1, text2, capatchText;
    JButton btn;
    JPanel capatcha;

    Login()
    {
        label1 = new JLabel();
        label1.setText("Username:");
        text1 = new JTextField("crouprat", 15);

        label2 = new JLabel();
        label2.setText("Password:");
        text2 = new JTextField("Y5TfTT5W", 15);
        capatchText = new JTextField("", 15); 
        btn = new JButton();
        btn.setSize(100,100);
        SUBMIT = new JButton("SUBMIT");

        panel = new JPanel(new GridLayout(5, 1));
        panel.add(label1);
        panel.add(text1);
        panel.add(label2);
        panel.add(text2);
        panel.add(new Label("capatcha"));
        panel.add(capatchText);
        panel.add(btn);
        panel.add(new Label());
        panel.add(SUBMIT);
        add(panel, BorderLayout.CENTER);
        SUBMIT.addActionListener(this);
        setTitle("LOGIN FORM");
        getImage();
        
    }
    
    private void getImage()
    {
        Parser parser;
        try
        {
            parser = new Parser(baseUrl + "/users/login.aspx");
            parser.setEncoding("UTF-8");
            
//            parser = new Parser(baseUrl + "/" + "verificationimage.aspx");
            URL url = new URL(baseUrl + "/verificationimage.aspx");
            ImageIcon icon = new ImageIcon(url);
            btn.setIcon(icon);
        }
        catch (ParserException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (MalformedURLException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }



    public void actionPerformed(ActionEvent ae)
    {
        String value1 = text1.getText();
        String value2 = text2.getText();
        StringBuffer sb = new StringBuffer();
        sb.append("Username=");
        sb.append(value1);
        sb.append("&Password=");
        sb.append(value2);
        sb.append("&VerificationCode=");
        sb.append(capatchText.getText());
//        sb.append("&Referrer=%2Fdefault.aspx%3F");
        sb.append("&Referrer=www.sratim.co.il");
        HttpURLConnection connection =  Utils.createPost(baseUrl + "/users/login.aspx", sb);
        Parser parser;
        try
        {
            parser = new Parser(connection);
            parser.setEncoding("UTF-8");
            ConnectionManager manager = Parser.getConnectionManager ();
            manager.setCookieProcessingEnabled(true);
            
            
        }
        catch (ParserException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        if (value1.equals("roseindia") && value2.equals("roseindia"))
        {
//            NextPage page = new NextPage();
//            page.setVisible(true);
//            JLabel label = new JLabel("Welcome:" + value1);
//            page.getContentPane().add(label);
        }
        else
        {
            System.out.println("enter the valid username and password");
            JOptionPane.showMessageDialog(this, "Incorrect login or password",
                    "Error", JOptionPane.ERROR_MESSAGE);
            getImage();
        }
    }
}

class LoginDemo
{
    public static void main(String arg[])
    {
        try
        {
            Login frame = new Login();
            frame.setSize(500, 400);
            frame.setVisible(true);
        }
        catch (Exception e)
        {
            JOptionPane.showMessageDialog(null, e.getMessage());
        }
    }
}