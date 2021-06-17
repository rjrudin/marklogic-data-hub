package org.example;

import com.beust.jcommander.Parameter;

public class Options {

    @Parameter(names = {"--host"})
    private String host = "localhost";

    @Parameter(names = {"--personCount"})
    private int personCount = 100;

    @Parameter(names = {"--steps"})
    private String steps = "2,3,4,5";

    @Parameter(names = {"--clearData"}, arity = 1)
    private boolean clearData = true;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPersonCount() {
        return personCount;
    }

    public void setPersonCount(int personCount) {
        this.personCount = personCount;
    }

    public String getSteps() {
        return steps;
    }

    public void setSteps(String steps) {
        this.steps = steps;
    }

    public boolean isClearData() {
        return clearData;
    }

    public void setClearData(boolean clearData) {
        this.clearData = clearData;
    }
}
