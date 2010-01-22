package subs;

import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JOptionPane;

import utils.PropertiesUtil;
import utils.TimeOutOptionPane;

/**
 * This class should handle launching the relevanat applications with their
 * correct params. Getsubs, Getsubs + HandleMultipulSubs, HandleMultipulSubs
 * 
 * @author ilan
 * 
 */
public class Launcher
{
    public static final String GET_SUBTITLE_PROPERTY = "getsubs";
    public static final String HANDLE_SUBTITLE_PROPERTY = "handlemultiplesubs";
    public static final String WAITFORIT_PROPERTY = "waitforit";
    public static final String WAITFORIT_DO_NOTHING_PROPERTY = "waitforitdonothing";
    
    public Launcher()
    {
        // TODO Auto-generated constructor stub
    }

    public static void main(String[] args)
    {
        System.out.println("***** Starting Subs4Me launcher, edit the launch property in the subs4me.properties to decide what to do as default\n");
        Subs4me.initProperties();
        String sequence = PropertiesUtil.getProperty("launch");
        if (sequence == null)
            return;
//        sequence = WAITFORIT_DO_NOTHING_PROPERTY;
        String[] launch = sequence.split(",");
        for (int i = 0; i < launch.length; i++)
        {
            String prog = launch[i];
            String[] paramsSplit = prog.split(" ");
            ArrayList<String> params = new ArrayList<String>(
                    paramsSplit.length + 1);
            params.add(args[0]);
            params.addAll(Arrays.asList(paramsSplit));
            params.remove(1);
            if (prog.toLowerCase().startsWith(GET_SUBTITLE_PROPERTY))
            {
                System.out.println("Launcher running getsubs");
                Subs4me.main(params.toArray(new String[params.size()]));
            }
            else if (prog.toLowerCase().equals(WAITFORIT_PROPERTY))
            {
                System.out.println("Launcher waiting");
                boolean touched = monitorKeyboard10Secs(true);
                if (touched)
                {
                    System.out.println("Launcher DONE - The END - FINITE - OK, you can go now - BYE BYE - NOOOO??????");
                    System.exit(0);
                }
            }
            else if (prog.toLowerCase().equals(WAITFORIT_DO_NOTHING_PROPERTY))
            {
                System.out.println("Launcher waiting");
                boolean touched = monitorKeyboard10Secs(false);
                if (!touched)
                {
                    System.out.println("Launcher DONE - The END - FINITE - OK, you can go now - BYE BYE - NOOOO??????");
                    System.exit(0);
                }
            } else if (prog.toLowerCase().startsWith(HANDLE_SUBTITLE_PROPERTY))
            {
                System.out.println("Launcher running HandleMultipleSubs");
                HandleMultipleSubs.main(params
                        .toArray(new String[params.size()]));
            }
        }
        System.out.println("Launcher DONE - THE END - FINITE - OK, you can go now - BYE BYE - NOOOO??????");
        System.exit(0);
    }

    /**
     * If okToStop is <b>true</b> the dialog means, we show a message instructing the user to click ok, 
     *  to exit and not do the next operation. 
     * If okToStop is <b>false</b> the dialog means, we show a message instructing the user to click ok, 
     *  to continue to the next operation.
     * @param okToStop true, false
     */
    private static boolean monitorKeyboard10Secs(boolean okToStop)
    {
        TimeOutOptionPane pane = new TimeOutOptionPane();
        String title = "";
        String message = "";
        String[] option = new String[1];  
        if (okToStop)
        {
            title = "Abort script????";
            message = "The script will continue in 10 seconds, press the cancel button to Abort\nClosing the dialog will NOT Abort";
            option[0] = "cancel";
        }
        else
        {
            title = "Continue script????";
            message = "The script will exit in 10 seconds, press the continue button to continue\nClosing the dialog will NOT Continue";   
            option[0] = "continue";
        }
        
        int ret = pane.showTimeoutDialog(null, message, title, JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, option, "closing");
        switch (ret)
        {
            case 0:
                break;
            case 1:
                break;
        }
        
        return ret == 0;
    }
}
