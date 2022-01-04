package ru.perveevm.polygon.packages.cli;

import picocli.CommandLine;

/**
 * @author Perveev Mike (perveev_m@mail.ru)
 */
@CommandLine.Command
public class TopLevelCommand {
    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "Display this help message")
    boolean usageHelpRequested;
}
