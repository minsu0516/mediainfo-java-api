package subs;

import java.io.File;
import java.io.IOException;

import org.ini4j.InvalidFileFormatException;
import org.ini4j.Wini;

/**
 * This class should handle launching the relevanat applications with their correct params.
 * Getsubs, Getsubs + HandleMultipulSubs, HandleMultipulSubs
 * @author ilan
 *
 */
public class Launcher
{
    public static final String DEFAULT_INI = "./properties/subs4me-default.ini";
    public static final String LAUNCHER_INI = "./launcher.ini";
    public Launcher()
    {
        // TODO Auto-generated constructor stub
    }
    
    public static void main(String[] args)
    {
        Wini ini;
        try
        {
            ini = new Wini(new File(DEFAULT_INI));
            String sequence = ini.get("launcher", "launch");
            String[] launch = sequence.split(",");
            for (int i = 0; i < launch.length; i++)
            {
                String prog = launch[i];
                if (prog.startsWith("getsubs"))
                {
                    System.out.println("getsubs activated");
                }
                else if (prog.startsWith("handlemultiplesubs"))
                {
                    System.out.println("handlemultiplesubs activated");
                }
            }
        }
        catch (InvalidFileFormatException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
