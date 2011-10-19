package com.polopoly.jboss;

public class Environment {
    private String name;
    private String value;

    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        this.name = name;
    }
    public String getValue()
    {
        return value;
    }
    public void setValue(String value)
    {
        this.value = value;
    }
    @Override
    public String toString()
    {
        return name + "=" + value;
    }
}
