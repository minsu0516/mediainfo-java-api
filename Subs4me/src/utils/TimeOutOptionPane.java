package utils;

import java.awt.Component;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

/**
 * http://www.jguru.com/faq/view.jsp?EID=266182
 * @author ilan
 *
 */
public class TimeOutOptionPane extends JOptionPane
{
    private int _timeout = 5;
    
    public TimeOutOptionPane()
    {
        super();
    }
    public TimeOutOptionPane(int timeout)
    {
        super();
        _timeout = timeout;
    }

    public int showTimeoutDialog(Component parentComponent, Object message,
            final String title, int optionType, int messageType,
            Object[] options, final Object initialValue)
    {
        JOptionPane pane = new JOptionPane(message, messageType, optionType,
                null, options, initialValue);

        pane.setInitialValue(initialValue);

        final JDialog dialog = pane.createDialog(parentComponent, title);

        pane.selectInitialValue();
        new Thread()
        {
            public void run()
            {
                try
                {
                    for (int i = _timeout; i >= 0; i--)
                    {
                        Thread.sleep(1000);
                        if (dialog.isVisible() && i < 300)
                        {
                            dialog.setTitle(title + "  (" + i
                                    + " seconds before auto \"" + initialValue
                                    + "\")");
                        }
                    }
                    if (dialog.isVisible())
                    {
                        dialog.setVisible(false);
                    }
                } catch (Throwable t)
                {
                    // ok - ugly I know!
                }
            }
        }.start();
        dialog.setVisible(true);

        Object selectedValue = pane.getValue();
        if (selectedValue == null)
        {
            return CLOSED_OPTION;
        }
        if (selectedValue.equals("uninitializedValue"))
        {
            selectedValue = initialValue;
        }
        if (selectedValue == null)
            return CLOSED_OPTION;
        if (options == null)
        {
            if (selectedValue instanceof Integer)
                return ((Integer) selectedValue).intValue();
            return CLOSED_OPTION;
        }
        for (int counter = 0, maxCounter = options.length; counter < maxCounter; counter++)
        {
            if (options[counter].equals(selectedValue))
                return counter;
        }
        return CLOSED_OPTION;
    }

}
