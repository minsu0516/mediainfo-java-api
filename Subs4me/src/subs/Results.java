package subs;

import java.util.LinkedList;

public class Results
{

    boolean            _isCorrectResult = false;
    LinkedList<String> _results          = null;
    LinkedList<String> _names          = null;
    
    public Results(LinkedList<String> results, boolean isCorrectResult)
    {
        _results = results;
        _isCorrectResult = isCorrectResult;
    }
    
    public LinkedList<String> getResults()
    {
        return _results;
    }
    
    public void setNames(LinkedList<String> names)
    {
        _names = names;
    }
    
    public LinkedList<String> getNames()
    {
        return _names;
    }
    
    public boolean isCorrectResults()
    {
        return _isCorrectResult;
    }
}