package ru.perveevm.polygon.packages.workers;

import de.vandermeer.asciitable.AsciiTable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import ru.perveevm.polygon.api.PolygonSession;
import ru.perveevm.polygon.api.entities.enums.SolutionTag;
import ru.perveevm.polygon.api.entities.enums.TestGroupFeedbackPolicy;
import ru.perveevm.polygon.api.entities.enums.TestGroupPointsPolicy;
import ru.perveevm.polygon.exceptions.api.PolygonSessionException;
import ru.perveevm.polygon.packages.exceptions.PolygonPackageUploaderException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Perveev Mike (perveev_m@mail.ru)
 */
public class PCMSPackageUploaderWorker extends AbstractPackageUploaderWorker {
    private Element problem;
    private final Scanner scanner = new Scanner(System.in);

    public PCMSPackageUploaderWorker(final PolygonSession session, final Path packagePath, final int problemId,
                                     final Set<UploaderProperties> properties) {
        super(session, packagePath, problemId, properties);
    }

    @Override
    public void uploadProblem() throws PolygonPackageUploaderException {
        logger.logBeginStage("PARSING PROBLEM.XML FILE");

        Document document = getXMLDocument(packagePath, "problem.xml");
        problem = (Element) document.getElementsByTagName("problem").item(0);
        problem = (Element) problem.getElementsByTagName("judging").item(0);

        uploadResources();
        uploadExecutables();
        uploadAssets();
        uploadTests();
        uploadGroups();
        uploadStatements();
    }

    private void uploadResources() throws PolygonPackageUploaderException {
        logger.logBeginStage("UPLOADING RESOURCES");

        List<String> fileNames = getAllFiles();
        fileNames.sort(Comparator.comparing(this::removeExtension));

        String table = generateFilesAsciiTable(fileNames);
        List<Integer> indexes = askToInputIDs(table,
                "Please, select IDs of resource files to upload and press Enter.", 1, fileNames.size(), false);

        for (Integer index : indexes) {
            String fileName = fileNames.get(index - 1);
            Path path = Path.of(packagePath.toString(), "files", fileName);

            logger.logInfo(String.format("Uploading resource %s", path));

            try {
                session.problemSaveFile(problemId, false, "resource", path.getFileName().toString(), path.toFile(),
                        null, null, null, null);
            } catch (PolygonSessionException e) {
                throw new PolygonPackageUploaderException("Error happened while uploading resource file", e);
            }
        }
    }

    private void uploadExecutables() throws PolygonPackageUploaderException {
        logger.logBeginStage("UPLOADING EXECUTABLES");

        List<String> fileNames = getAllFiles();
        fileNames.sort(Comparator.comparing(this::removeExtension));

        String table = generateFilesAsciiTable(fileNames);
        List<Integer> indexes = askToInputIDs(table,
                "Please, select IDs of executable files to upload and press Enter.", 1, fileNames.size(), false);

        for (Integer index : indexes) {
            String fileName = fileNames.get(index - 1);
            Path path = Path.of(packagePath.toString(), "files", fileName);

            logger.logInfo(String.format("Uploading executable %s", path));

            try {
                session.problemSaveFile(problemId, false, "source", path.getFileName().toString(), path.toFile(),
                        null, null, null, null);
            } catch (PolygonSessionException e) {
                throw new PolygonPackageUploaderException("Error happened while uploading executable file", e);
            }
        }
    }

    private void uploadAssets() throws PolygonPackageUploaderException {
        uploadChecker();
        uploadValidator();
        uploadMainCorrectSolution();
    }

    private void uploadChecker() throws PolygonPackageUploaderException {
        logger.logBeginStage("UPLOADING CHECKER");

        List<String> fileNames = getAllFiles();
        fileNames.sort(Comparator.comparing(this::removeExtension));

        String table = generateFilesAsciiTable(fileNames);
        List<Integer> indexes = askToInputIDs(table,
                "Please, select ID of checker file to set and press Enter.", 1, fileNames.size(), true);

        for (Integer index : indexes) {
            String fileName = fileNames.get(index - 1);
            Path path = Path.of(packagePath.toString(), "files", fileName);

            logger.logInfo(String.format("Uploading checker %s", path));

            try {
                session.problemSetChecker(problemId, path.getFileName().toString());
            } catch (PolygonSessionException e) {
                throw new PolygonPackageUploaderException("Error happened while setting checker", e);
            }
        }
    }

    private void uploadValidator() throws PolygonPackageUploaderException {
        logger.logBeginStage("UPLOADING VALIDATOR");

        List<String> fileNames = getAllFiles();
        fileNames.sort(Comparator.comparing(this::removeExtension));

        String table = generateFilesAsciiTable(fileNames);
        List<Integer> indexes = askToInputIDs(table,
                "Please, select ID of validator file to set and press Enter.", 1, fileNames.size(), true);

        for (Integer index : indexes) {
            String fileName = fileNames.get(index - 1);
            Path path = Path.of(packagePath.toString(), "files", fileName);

            logger.logInfo(String.format("Uploading validator %s", path));

            try {
                session.problemSetValidator(problemId, path.getFileName().toString());
            } catch (PolygonSessionException e) {
                throw new PolygonPackageUploaderException("Error happened while setting validator", e);
            }
        }
    }

    private void uploadMainCorrectSolution() throws PolygonPackageUploaderException {
        logger.logBeginStage("UPLOADING MAIN CORRECT SOLUTION");

        List<String> fileNames = getAllSolutions();
        fileNames.sort(Comparator.comparing(this::removeExtension));

        String table = generateFilesAsciiTable(fileNames);
        List<Integer> indexes = askToInputIDs(table,
                "Please, select ID of main correct solution file to set and press Enter.", 1, fileNames.size(), true);

        for (Integer index : indexes) {
            String fileName = fileNames.get(index - 1);
            Path path = Path.of(packagePath.toString(), "solutions", fileName);

            logger.logInfo(String.format("Uploading main correct solution %s", path));

            try {
                session.problemSaveSolution(problemId, false, path.getFileName().toString(),
                        path.toFile(), null, SolutionTag.MA);
            } catch (PolygonSessionException e) {
                throw new PolygonPackageUploaderException("Error happened while setting main correct solution", e);
            }
        }
    }

    private void uploadTests() throws PolygonPackageUploaderException {
        logger.logBeginStage("UPLOADING TESTS");

        Element testset = findMainTestset();
        String timeLimitString = testset.getAttribute("time-limit");
        String memoryLimitString = testset.getAttribute("memory-limit");

        int timeLimit = (int) Math.round(
                Double.parseDouble(timeLimitString.substring(0, timeLimitString.length() - 1)) * 1000);
        int memoryLimit = Integer.parseInt(memoryLimitString) / 1024 / 1024;

        logger.logInfo("Uploading TL and ML");
        try {
            session.problemUpdateInfo(problemId, null, null, false, timeLimit, memoryLimit);
        } catch (PolygonSessionException e) {
            throw new PolygonPackageUploaderException("Error happened while uploading TL and ML", e);
        }

        NodeList tests = testset.getElementsByTagName("test");
        Map<String, List<Integer>> testsByScriptLine = new HashMap<>();
        for (int i = 0; i < tests.getLength(); i++) {
            Element currentTest = (Element) tests.item(i);
            if (!currentTest.hasAttribute("comment")) {
                throw new PolygonPackageUploaderException(String.format("Error happened while uploading tests: there " +
                        "is no comment for test %d", i + 1));
            }

            if (currentTest.getAttribute("comment").equals("manual")) {
                logger.logInfo(String.format("Uploading manual test #%d", i + 1));

                try {
                    session.problemSaveTest(problemId, false, "tests", i + 1,
                            Path.of(packagePath.toString(), "tests", String.format("%02d", i + 1)).toFile(), null,
                            null, null, false, null, null, null);
                } catch (PolygonSessionException e) {
                    throw new PolygonPackageUploaderException("Error happened while uploading manual test", e);
                }
            } else {
                logger.logInfo(String.format("Adding script line for generated test #%d", i + 1));

                String scriptLine = currentTest.getAttribute("comment");
                if (!scriptLine.startsWith("generated cmd: ")) {
                    throw new PolygonPackageUploaderException("Cannot parse generator script line");
                }
                scriptLine = scriptLine.substring(scriptLine.indexOf('\'') + 1, scriptLine.lastIndexOf('\''));

                if (!testsByScriptLine.containsKey(scriptLine)) {
                    testsByScriptLine.put(scriptLine, new ArrayList<>());
                }
                testsByScriptLine.get(scriptLine).add(i + 1);
            }
        }

        String generatorScript = getGeneratorScript(testsByScriptLine);
        if (!generatorScript.isEmpty()) {
            logger.logInfo("Uploading generator script");

            try {
                session.problemSaveScript(problemId, "tests", generatorScript);
            } catch (PolygonSessionException e) {
                throw new PolygonPackageUploaderException("Error happened while uploading generator script");
            }
        }

        NodeList groupsList = testset.getElementsByTagName("test-group");
        if (groupsList.getLength() != 0) {
            try {
                session.problemEnableGroups(problemId, "tests", true);
            } catch (PolygonSessionException e) {
                throw new PolygonPackageUploaderException("Error happened while enabling groups", e);
            }

            boolean pointsEnabled = false;
            int testId = 0;
            for (int i = 0; i < groupsList.getLength(); i++) {
                Element currentGroup = (Element) groupsList.item(i);
                NodeList currentGroupTests = currentGroup.getElementsByTagName("test");
                for (int j = 0; j < currentGroupTests.getLength(); j++) {
                    testId++;
                    Element currentTest = (Element) currentGroupTests.item(j);

                    Double score = null;
                    String group = currentGroup.getAttribute("comment").replaceAll("[^a-zA-Z0-9\\-]", "");
                    if (currentTest.hasAttribute("score")) {
                        score = Double.parseDouble(currentTest.getAttribute("score"));
                    } else if (j == currentGroupTests.getLength() - 1 && currentGroup.hasAttribute("group-bonus")) {
                        score = Double.parseDouble(currentGroup.getAttribute("group-bonus"));
                    }

                    if (score != null && !pointsEnabled) {
                        try {
                            session.problemEnablePoints(problemId, true);
                            pointsEnabled = true;
                        } catch (PolygonSessionException e) {
                            throw new PolygonPackageUploaderException("Error happened while enabling points", e);
                        }
                    }

                    logger.logInfo(String.format("Uploading test #%d info", testId));
                    try {
                        session.problemSaveTest(problemId, false, "tests", testId, (String) null, group, score, null,
                                false, null, null, null);
                    } catch (PolygonSessionException e) {
                        throw new PolygonPackageUploaderException("Error happened while uploading test info", e);
                    }
                }
            }
        } else {
            boolean pointsEnabled = false;
            for (int i = 0; i < tests.getLength(); i++) {
                logger.logInfo(String.format("Uploading test #%d info", i + 1));

                Element currentTest = (Element) tests.item(i);
                if (currentTest.hasAttribute("score")) {
                    if (!pointsEnabled) {
                        try {
                            session.problemEnablePoints(problemId, true);
                        } catch (PolygonSessionException e) {
                            throw new PolygonPackageUploaderException("Error happened while enabling points", e);
                        }
                    }

                    Double score = Double.parseDouble(currentTest.getAttribute("score"));
                    try {
                        session.problemSaveTest(problemId, false, "tests", i + 1, (String) null, null, score, null,
                                false, null, null, null);
                    } catch (PolygonSessionException e) {
                        throw new PolygonPackageUploaderException("Error happened while uploading test info", e);
                    }
                }
            }
        }
    }

    private void uploadGroups() throws PolygonPackageUploaderException {
        logger.logBeginStage("UPLOADING GROUPS");

        Element testset = findMainTestset();
        NodeList groups = testset.getElementsByTagName("test-group");

        for (int i = 0; i < groups.getLength(); i++) {
            Element currentGroup = (Element) groups.item(i);

            TestGroupPointsPolicy pointsPolicy = TestGroupPointsPolicy.COMPLETE_GROUP;
            NodeList tests = currentGroup.getElementsByTagName("test");
            for (int j = 0; j < tests.getLength(); j++) {
                Element currentTest = (Element) tests.item(j);
                if (currentTest.hasAttribute("score")) {
                    pointsPolicy = TestGroupPointsPolicy.EACH_TEST;
                    break;
                }
            }

            TestGroupFeedbackPolicy feedbackPolicy = switch (currentGroup.getAttribute("feedback")) {
                case "statistics" -> TestGroupFeedbackPolicy.COMPLETE;
                case "outcome" -> TestGroupFeedbackPolicy.ICPC;
                case "group-score" -> TestGroupFeedbackPolicy.POINTS;
                default -> TestGroupFeedbackPolicy.NONE;
            };

            String[] dependencies = null;
            if (currentGroup.hasAttribute("require-groups")) {
                String requirementsString = currentGroup.getAttribute("require-groups");
                if (!requirementsString.isEmpty()) {
                    dependencies = requirementsString.split(" ");
                    dependencies = Arrays.stream(dependencies)
                            .filter(name -> !name.equals(currentGroup.getAttribute("comment")))
                            .toArray(String[]::new);
                }
            }

            logger.logInfo(String.format("Updating group #%s parameters", currentGroup.getAttribute("comment")));
            try {
                session.problemSaveTestGroup(problemId, "tests",
                        currentGroup.getAttribute("comment").replaceAll("[^a-zA-Z0-9\\-]", ""), pointsPolicy,
                        feedbackPolicy, dependencies);
            } catch (PolygonSessionException e) {
                throw new PolygonPackageUploaderException("Error happened while updating group info", e);
            }
        }
    }

    private void uploadStatements() throws PolygonPackageUploaderException {
        uploadStatementsFromDirectory(Path.of(packagePath.toString(), "statements"));
    }

    private Element findMainTestset() throws PolygonPackageUploaderException {
        NodeList testsets = problem.getElementsByTagName("testset");
        Element testset = null;
        for (int i = 0; i < testsets.getLength(); i++) {
            Element currentTestset = (Element) testsets.item(i);
            if (currentTestset.hasAttribute("name") && currentTestset.getAttribute("name").equals("main")) {
                testset = currentTestset;
                break;
            }
        }

        if (testset == null) {
            throw new PolygonPackageUploaderException("Error happened while uploading tests: there is no testset " +
                    "with name \"main\"");
        }

        return testset;
    }

    private List<String> getAllFiles() throws PolygonPackageUploaderException {
        return getAllFiles("files");
    }

    private List<String> getAllSolutions() throws PolygonPackageUploaderException {
        return getAllFiles("solutions");
    }

    private List<String> getAllFiles(final String directory) throws PolygonPackageUploaderException {
        Path filesDirectory = Path.of(packagePath.toString(), directory);
        try (Stream<Path> paths = Files.walk(filesDirectory, 1)) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .map(File::getName)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new PolygonPackageUploaderException("Error happened while walking files directory", e);
        }
    }

    private String removeExtension(final String fileName) {
        if (!fileName.contains(".")) {
            return fileName;
        }
        return fileName.substring(fileName.indexOf('.'));
    }

    private List<Integer> parsePattern(final String pattern, final int first, final int last)
            throws PolygonPackageUploaderException {
        if (pattern.isEmpty()) {
            return Collections.emptyList();
        }
        if (!pattern.matches("[\\s\\d\\-,]+")) {
            throw new PolygonPackageUploaderException("Expression contains unsupported characters");
        }

        Set<Integer> result = new HashSet<>();
        String[] blocks = pattern.replaceAll("\\s+", "").split(",");
        for (String block : blocks) {
            if (!block.contains("-")) {
                result.add(Integer.parseInt(block));
            } else {
                if (block.indexOf('-') != block.lastIndexOf('-')) {
                    throw new PolygonPackageUploaderException("Expression is incorrect");
                }
                if (block.charAt(0) == '-' || block.charAt(block.length() - 1) == '-') {
                    throw new PolygonPackageUploaderException("Expression is incorrect");
                }

                int delimiter = block.indexOf('-');
                String prefix = block.substring(0, delimiter);
                String suffix = block.substring(delimiter + 1);

                try {
                    int from = Math.max(Integer.parseInt(prefix), first);
                    int to = Math.min(Integer.parseInt(suffix), last);
                    for (int i = from; i <= to; i++) {
                        result.add(i);
                    }
                } catch (NumberFormatException e) {
                    throw new PolygonPackageUploaderException("Expression is incorrect", e);
                }
            }
        }

        return result.stream().sorted().collect(Collectors.toList());
    }

    private List<Integer> askToInputIDs(final String table, final String message, final int first, final int last,
                                        final boolean needOneID) {
        List<Integer> indexes;
        while (true) {
            System.out.println(table);
            System.out.println(message);
            System.out.println("Input format example: 1-3, 4, 6-9, 10, 13");
            System.out.print("IDs: ");

            String selectedPattern = scanner.nextLine();
            System.out.println(selectedPattern);

            try {
                indexes = parsePattern(selectedPattern, first, last);
                if (needOneID && indexes.size() != 1) {
                    System.out.println("You should select only one file");
                    continue;
                }
                break;
            } catch (PolygonPackageUploaderException e) {
                System.out.println("Expression cannot be parsed, cause: " + e.getMessage());
            }
        }
        return indexes;
    }

    private String generateFilesAsciiTable(final List<String> fileNames) {
        AsciiTable table = new AsciiTable();
        table.addRule();
        table.addRow("#", "File name", "File extension");
        for (int i = 0; i < fileNames.size(); i++) {
            table.addRule();
            table.addRow(String.valueOf(i + 1), fileNames.get(i), removeExtension(fileNames.get(i)));
        }
        table.addRule();
        return table.render();
    }
}
