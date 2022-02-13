package ru.perveevm.polygon.packages.cli;

import picocli.CommandLine;
import ru.perveevm.polygon.packages.PackageType;
import ru.perveevm.polygon.packages.uploaders.AdvancedPackageUploader;
import ru.perveevm.polygon.packages.uploaders.PackageUploader;
import ru.perveevm.polygon.packages.exceptions.PolygonPackageUploaderException;
import ru.perveevm.polygon.packages.workers.UploaderProperties;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
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

        if (arguments.isInvalid()) {
            System.out.println("Either none of problemId and problem name are not given or both are in args");
            return 3;
        }

        String key = preferences.get("key", null);
        String secret = preferences.get("secret", null);
        if (key == null || secret == null) {
            System.out.println("There are no saved API credentials for you. Please, call init command first");
            return 1;
        }

        Set<UploaderProperties> propertiesSet = new HashSet<>();
        if (arguments.onlyMainCorrect) {
            propertiesSet.add(UploaderProperties.ONLY_MAIN_SOLUTION);
        }
        if (arguments.fromZip) {
            propertiesSet.add(UploaderProperties.ZIP_ARCHIVE);
        }

        PackageType packageType;
        if (arguments.archiveType == null) {
            packageType = PackageType.POLYGON;
        } else {
            if (arguments.archiveType.equals("polygon")) {
                packageType = PackageType.POLYGON;
            } else if (arguments.archiveType.equals("pcms")) {
                packageType = PackageType.PCMS;
            } else {
                System.out.printf("Unsupported package type: %s%n", arguments.archiveType);
                return 3;
            }
        }

        PackageUploader basicUploader = new PackageUploader(key, secret, propertiesSet, packageType);
        try {
            if (arguments.newProblemName != null) {
                String password = preferences.get("password", null);
                String login = preferences.get("login", null);

                if (login == null || password == null) {
                    System.out.println("There are no saved Polygon credentials for you. Please, call init command first");
                    return 1;
                }

                AdvancedPackageUploader uploader = new AdvancedPackageUploader(login, password, basicUploader);

                uploader.uploadProblem(arguments.path, arguments.newProblemName);
            }
            else {
                basicUploader.uploadProblem(arguments.path, arguments.problemId);
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
        @CommandLine.Option(names = {"-i", "--problem-id"}, description = "Problem ID in Polygon")
        Integer problemId;

        @CommandLine.Option(names = {"-p", "--path"}, required = true,
                description = "Path to the problem directory or problem archive")
        Path path;

        @CommandLine.Option(names = {"-z", "--zip"}, description = "If problem package is a ZIP archive")
        boolean fromZip;

        @CommandLine.Option(names = {"-d", "--debug"}, description = "Show stacktrace")
        boolean showStacktrace;

        @CommandLine.Option(names = {"-m", "--main"}, description = "Upload only main correct solution")
        boolean onlyMainCorrect;

        @CommandLine.Option(names = {"-n", "--new"}, description = "Create new problem with given name")
        String newProblemName;

        @CommandLine.Option(names = {"-t", "--type"}, description = "Denotes a type of an archive. Supported values: polygon, pcms")
        String archiveType;

        boolean isInvalid() {
            if (problemId == null && newProblemName == null) {
                return true;
            }
            if (problemId != null && newProblemName != null) {
                return true;
            }
            return false;
        }
    }
}
