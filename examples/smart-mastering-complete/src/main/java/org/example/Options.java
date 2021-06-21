package org.example;

import com.beust.jcommander.Parameter;

public class Options {

    @Parameter(names = {"--host"})
    private String host = "localhost";

    @Parameter(names = {"--personCount"})
    private int personCount = 100;

    // This controls how many random names are generated based on using the names in the data folder plus adding
    // numbers to them. The lower the number, the more random the names, as more names are generated. So a value of 1
    // means that for a last name of e.g. "Smith", and a personCount of 10k, 10k additional last names will be selected
    // from. A value of 10 would mean that 1k additional last names will be selected from, which will lead to more
    // matches occurring.
    @Parameter(names = {"--nameRandomness"})
    private int nameRandomness = 2;

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

    public int getNameRandomness() {
        return nameRandomness;
    }

    public void setNameRandomness(int nameRandomness) {
        this.nameRandomness = nameRandomness;
    }
}
