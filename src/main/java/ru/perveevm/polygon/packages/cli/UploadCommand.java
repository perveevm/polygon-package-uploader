package ru.perveevm.polygon.packages.cli;

import picocli.CommandLine;
import ru.perveevm.polygon.packages.PackageUploader;
import ru.perveevm.polygon.packages.exceptions.PolygonPackageUploaderException;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.prefs.Preferences;

/**
 * @author Perveev Mike (perveev_m@mail.ru)
 */
@CommandLine.Command(name = "upload", description = "Uploads problem into Polygon")
public class UploadCommand implements Callable<Integer> {
    @CommandLine.ArgGroup(exclusive = false, multiplicity = "1")
    UploadCommandArguments arguments;

    @Override
    public Integer call() {
        Preferences preferences;
        try {
            preferences = Preferences.userRoot().node("ru.perveevm.polygon.packages.api-credentials");
        } catch (Exception e) {
            System.out.println("There are no saved API credentials for you. Please, call init command first");
            return 1;
        }

        String key = preferences.get("key", null);
        String secret = preferences.get("secret", null);
        if (key == null || secret == null) {
            System.out.println("There are no saved API credentials for you. Please, call init command first");
            return 1;
        }

        PackageUploader uploader = new PackageUploader(key, secret);
        try {
            if (arguments.fromZip) {
                uploader.uploadProblemFromZip(arguments.path, arguments.problemId);
            } else {
                uploader.uploadProblem(arguments.path, arguments.problemId);
            }
        } catch (PolygonPackageUploaderException e) {
            System.out.println(e.getMessage());
            if (arguments.showStacktrace) {
                e.printStackTrace();
            }
            return 2;
        }

        return 0;
    }

    static class UploadCommandArguments {
        @CommandLine.Option(names = {"-i", "--problem-id"}, required = true, description = "Problem ID in Polygon")
        int problemId;

        @CommandLine.Option(names = {"-p", "--path"}, required = true,
                description = "Path to the problem directory or problem archive")
        Path path;

        @CommandLine.Option(names = {"-z", "--zip"}, description = "If problem package is a ZIP archive")
        boolean fromZip;

        @CommandLine.Option(names = {"-d", "--debug"}, description = "Show stacktrace")
        boolean showStacktrace;
    }
}
