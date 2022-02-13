package ru.perveevm.polygon.packages;

/**
 * @author Perveev Mike (perveev_m@mail.ru)
 */
public class TestInfo {
    private final Double points;
    private final String group;
    private final boolean isSample;

    public TestInfo(final Double points, final String group, final boolean isSample) {
        this.points = points;
        this.group = group;
        this.isSample = isSample;
    }

    public Double getPoints() {
        return points;
    }

    public String getGroup() {
        return group;
    }

    public boolean isSample() {
        return isSample;
    }
}
