package ru.perveevm.polygon.packages;

import picocli.CommandLine;
import ru.perveevm.polygon.packages.cli.InitCommand;
import ru.perveevm.polygon.packages.cli.TopLevelCommand;
import ru.perveevm.polygon.packages.cli.UploadCommand;

/**
 * @author Perveev Mike (perveev_m@mail.ru)
 */
public class Main {
    public static void main(String[] args) {
        int exitCode = new CommandLine(new TopLevelCommand())
                .addSubcommand(new InitCommand())
                .addSubcommand(new UploadCommand())
                .execute(args);
        System.exit(exitCode);
    }
}
