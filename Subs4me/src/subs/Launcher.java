package subs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

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
    public static final String DEFAULT_INI = "./properties/launcher-default.ini";
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
            if (sequence == null)
                return;
            
            String[] launch = sequence.split(",");
            for (int i = 0; i < launch.length; i++)
            {
                String prog = launch[i];
                String[] paramsSplit = prog.split(" ");
                ArrayList<String> params = new ArrayList<String>(paramsSplit.length + 1);
                params.add(args[0]);
                params.addAll(Arrays.asList(paramsSplit));
                params.remove(1);
                if (prog.toLowerCase().startsWith("getsubs"))
                {
                    Subs4me.main(params.toArray(new String[params.size()]));
                }
                else if (prog.toLowerCase().startsWith("handlemultiplesubs"))
                {
                    HandleMultipleSubs.main(params.toArray(new String[params.size()]));
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
