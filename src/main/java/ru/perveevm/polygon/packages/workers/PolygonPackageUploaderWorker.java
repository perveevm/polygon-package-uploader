package ru.perveevm.polygon.packages.workers;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import ru.perveevm.polygon.api.PolygonSession;
import ru.perveevm.polygon.api.entities.enums.SolutionTag;
import ru.perveevm.polygon.api.entities.enums.TestGroupFeedbackPolicy;
import ru.perveevm.polygon.api.entities.enums.TestGroupPointsPolicy;
import ru.perveevm.polygon.exceptions.api.PolygonSessionException;
import ru.perveevm.polygon.packages.TestInfo;
import ru.perveevm.polygon.packages.exceptions.PolygonPackageUploaderException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Perveev Mike (perveev_m@mail.ru)
 */
public class PolygonPackageUploaderWorker extends AbstractPackageUploaderWorker {
    private Element problem;

    public PolygonPackageUploaderWorker(final PolygonSession session, final Path packagePath, final int problemId,
                                        final Set<UploaderProperties> properties) {
        super(session, packagePath, problemId, properties);
    }

    @Override
    public void uploadProblem() throws PolygonPackageUploaderException {
        logger.logBeginStage("PARSING PROBLEM.XML FILE");

        Document document = getXMLDocument(packagePath, "problem.xml");
        problem = (Element) document.getElementsByTagName("problem").item(0);
        uploadTags();
        uploadResources();
        uploadExecutables();
        uploadAssets();
        uploadTests();
        uploadGroups();
        uploadStatements();
    }

    private void uploadResources() throws PolygonPackageUploaderException {
        logger.logBeginStage("UPLOADING RESOURCES");

        Element filesNode = (Element) problem.getElementsByTagName("files").item(0);
        Element resourcesNode = (Element) filesNode.getElementsByTagName("resources").item(0);
        NodeList allResources = resourcesNode.getElementsByTagName("file");
        for (int i = 0; i < allResources.getLength(); i++) {
            Path path = Path.of(allResources.item(i).getAttributes().getNamedItem("path").getTextContent());
            logger.logInfo(String.format("Uploading resource %s", path));

            try {
                session.problemSaveFile(problemId, false, "resource", path.getFileName().toString(),
                        Path.of(packagePath.toString(), path.toString()).toFile(), null, null, null, null);
            } catch (PolygonSessionException e) {
                throw new PolygonPackageUploaderException(
                        String.format("Error happened while uploading resource file %s", path), e);
            }
        }
    }

    private void uploadExecutables() throws PolygonPackageUploaderException {
        logger.logBeginStage("UPLOADING EXECUTABLES");

        Element filesNode = (Element) problem.getElementsByTagName("files").item(0);
        if (filesNode.getElementsByTagName("executables").getLength() == 0) {
            return;
        }

        Element executablesNode = (Element) filesNode.getElementsByTagName("executables").item(0);
        NodeList allExecutables = executablesNode.getElementsByTagName("executable");
        for (int i = 0; i < allExecutables.getLength(); i++) {
            Element curExecutable = (Element) allExecutables.item(i);
            Node curExecutableSource = curExecutable.getElementsByTagName("source").item(0);
            Path curExecutablePath = Path.of(curExecutableSource.getAttributes().getNamedItem("path").getTextContent());
            String curExecutableType = curExecutableSource.getAttributes().getNamedItem("type").getTextContent();
            logger.logInfo(String.format("Uploading executable %s of type \"%s\"",
                    curExecutablePath, curExecutableType));

            try {
                session.problemSaveFile(problemId, false, "source", curExecutablePath.getFileName().toString(),
                        Path.of(packagePath.toString(), curExecutablePath.toString()).toFile(), curExecutableType,
                        null, null, null);
            } catch (PolygonSessionException e) {
                throw new PolygonPackageUploaderException(
                        String.format("Error happened while uploading source file %s", curExecutablePath), e);
            }
        }
    }

    private void uploadAssets() throws PolygonPackageUploaderException {
        Element assetNode = (Element) problem.getElementsByTagName("assets").item(0);

        uploadChecker(assetNode);
        uploadValidators(assetNode);
        uploadSolutions(assetNode);
        uploadInteractor(assetNode);
    }

    private void uploadTests() throws PolygonPackageUploaderException {
        logger.logBeginStage("UPLOADING TESTS");

        Element judgingNode = (Element) problem.getElementsByTagName("judging").item(0);
        NodeList allTestsets = judgingNode.getElementsByTagName("testset");

        boolean groupsEnabled = false;
        boolean pointsEnabled = false;

        for (int i = 0; i < allTestsets.getLength(); i++) {
            Node curTestset = allTestsets.item(i);
            String curTestsetName = curTestset.getAttributes().getNamedItem("name").getTextContent();
            logger.logBeginStage(String.format("UPLOADING TESTSET %s", curTestsetName));

            if (!curTestsetName.equals("tests")) {
                continue;
            }

            Node timeLimitNode = ((Element) curTestset).getElementsByTagName("time-limit").item(0);
            Node memoryLimitNode = ((Element) curTestset).getElementsByTagName("memory-limit").item(0);

            int timeLimit = Integer.parseInt(timeLimitNode.getTextContent());
            int memoryLimit = (int) (Long.parseLong(memoryLimitNode.getTextContent()) / 1024 / 1024);

            logger.logInfo("Uploading TL and ML");
            try {
                session.problemUpdateInfo(problemId, null, null, null, timeLimit, memoryLimit);
            } catch (PolygonSessionException e) {
                throw new PolygonPackageUploaderException("Error happened while updating problem info", e);
            }

            Element testsNode = (Element) ((Element) curTestset).getElementsByTagName("tests").item(0);
            NodeList allTests = testsNode.getElementsByTagName("test");

            Map<String, List<Integer>> testsByScriptLine = new HashMap<>();
            Map<Integer, TestInfo> generatedTestsInfo = new HashMap<>();
            for (int j = 0; j < allTests.getLength(); j++) {
                Node curTest = allTests.item(j);
                String method = curTest.getAttributes().getNamedItem("method").getTextContent();
                Node pointsNode = curTest.getAttributes().getNamedItem("points");
                Node groupNode = curTest.getAttributes().getNamedItem("group");
                Node isSampleNode = curTest.getAttributes().getNamedItem("sample");

                Double points = null;
                String group = null;
                boolean isSample = false;

                if (pointsNode != null) {
                    points = Double.parseDouble(pointsNode.getTextContent());
                    if (!pointsEnabled) {
                        pointsEnabled = true;
                        try {
                            session.problemEnablePoints(problemId, true);
                        } catch (PolygonSessionException e) {
                            throw new PolygonPackageUploaderException("Error happened while enabling test points", e);
                        }
                    }
                }
                if (groupNode != null) {
                    group = groupNode.getTextContent();
                    if (!groupsEnabled) {
                        groupsEnabled = true;
                        try {
                            session.problemEnableGroups(problemId, curTestsetName, true);
                        } catch (PolygonSessionException e) {
                            throw new PolygonPackageUploaderException("Error happened while enabling groups", e);
                        }
                    }
                }
                if (isSampleNode != null) {
                    isSample = Boolean.parseBoolean(isSampleNode.getTextContent());
                }

                if (method.equals("manual")) {
                    logger.logInfo(String.format("Uploading manual test #%d", j + 1));

                    try {
                        session.problemSaveTest(problemId, false, curTestsetName, j + 1,
                                Path.of(packagePath.toString(), curTestsetName, String.format("%02d", j + 1)).toFile(),
                                group, points, null, isSample, null, null, null);
                    } catch (PolygonSessionException e) {
                        throw new PolygonPackageUploaderException("Error happened while uploading manual test", e);
                    }
                } else {
                    logger.logInfo(String.format("Adding script line for generated test #%d", j + 1));
                    String cmd = curTest.getAttributes().getNamedItem("cmd").getTextContent();
                    testsByScriptLine.putIfAbsent(cmd, new ArrayList<>());
                    testsByScriptLine.get(cmd).add(j + 1);
                    generatedTestsInfo.put(j + 1, new TestInfo(points, group, isSample));
                }
            }

            String generatorScript = getGeneratorScript(testsByScriptLine);
            if (!generatorScript.isEmpty()) {
                logger.logInfo("Uploading generator script");
                try {
                    session.problemSaveScript(problemId, curTestsetName, generatorScript);
                } catch (PolygonSessionException e) {
                    throw new PolygonPackageUploaderException("Error happened while uploading generator script", e);
                }
            }

            for (Map.Entry<Integer, TestInfo> testInfo : generatedTestsInfo.entrySet()) {
                logger.logInfo(String.format("Updating generated test #%d data", testInfo.getKey()));

                try {
                    session.problemSaveTest(problemId, false, curTestsetName, testInfo.getKey(), (String) null,
                            testInfo.getValue().getGroup(), testInfo.getValue().getPoints(), null,
                            testInfo.getValue().isSample(), null, null, null);
                } catch (PolygonSessionException e) {
                    throw new PolygonPackageUploaderException("Error happened while editing generated test", e);
                }
            }
        }
    }

    private void uploadGroups() throws PolygonPackageUploaderException {
        logger.logBeginStage("UPLOADING GROUPS");

        Element judgingNode = (Element) problem.getElementsByTagName("judging").item(0);
        NodeList allTestsets = judgingNode.getElementsByTagName("testset");

        for (int i = 0; i < allTestsets.getLength(); i++) {
            Node curTestset = allTestsets.item(i);
            String curTestsetName = curTestset.getAttributes().getNamedItem("name").getTextContent();

            if (!curTestsetName.equals("tests")) {
                continue;
            }

            NodeList groupsNode = ((Element) curTestset).getElementsByTagName("groups");
            if (groupsNode.getLength() == 0) {
                continue;
            }

            NodeList allGroups = ((Element) groupsNode.item(0)).getElementsByTagName("group");
            for (int j = 0; j < allGroups.getLength(); j++) {
                Node curGroup = allGroups.item(j);
                String groupName = curGroup.getAttributes().getNamedItem("name").getTextContent();
                String feedbackPolicyString = curGroup.getAttributes().getNamedItem("feedback-policy").getTextContent();
                String pointsPolicyString = curGroup.getAttributes().getNamedItem("points-policy").getTextContent();

                TestGroupPointsPolicy pointsPolicy;
                if (pointsPolicyString.equals("complete-group")) {
                    pointsPolicy = TestGroupPointsPolicy.COMPLETE_GROUP;
                } else {
                    pointsPolicy = TestGroupPointsPolicy.EACH_TEST;
                }

                TestGroupFeedbackPolicy feedbackPolicy = switch (feedbackPolicyString) {
                    case "complete" -> TestGroupFeedbackPolicy.COMPLETE;
                    case "icpc" -> TestGroupFeedbackPolicy.ICPC;
                    case "points" -> TestGroupFeedbackPolicy.POINTS;
                    default -> TestGroupFeedbackPolicy.NONE;
                };

                String[] dependencies = null;
                NodeList dependenciesNode = ((Element) curGroup).getElementsByTagName("dependencies");
                if (dependenciesNode.getLength() != 0) {
                    NodeList dependenciesList = ((Element) dependenciesNode.item(0)).getElementsByTagName("dependency");
                    dependencies = new String[dependenciesList.getLength()];
                    for (int d = 0; d < dependenciesList.getLength(); d++) {
                        String dependencyName = dependenciesList.item(d).getAttributes().getNamedItem("group")
                                .getTextContent();
                        dependencies[d] = dependencyName;
                    }
                }

                logger.logInfo(String.format("Updating group #%s parameters", groupName));
                try {
                    session.problemSaveTestGroup(problemId, curTestsetName, groupName, pointsPolicy, feedbackPolicy,
                            dependencies);
                } catch (PolygonSessionException e) {
                    throw new PolygonPackageUploaderException("Error happened while updating group parameters", e);
                }
            }
        }
    }

    private void uploadChecker(final Element assetNode) throws PolygonPackageUploaderException {
        logger.logBeginStage("UPLOADING CHECKER");

        Element checkerNode = (Element) assetNode.getElementsByTagName("checker").item(0);
        Element checkerSourceNode = (Element) checkerNode.getElementsByTagName("source").item(0);

        Path checkerPath = Path.of(packagePath.toString(), checkerSourceNode.getAttribute("path"));


        String checkerType = checkerSourceNode.getAttribute("type");
        String checkerName = checkerPath.getFileName().toString();
        logger.logInfo(String.format("Uploading checker %s of type \"%s\"",
                checkerName, checkerType));
        try {
            session.problemSaveFile(problemId, false, "source", checkerName, checkerPath.toFile(),
                    checkerType, null, null, null);
            session.problemSetChecker(problemId, checkerName);
        } catch (PolygonSessionException e) {
            throw new PolygonPackageUploaderException("Error happened while uploading checker", e);
        }

    }

    private void uploadValidators(final Element assetNode) throws PolygonPackageUploaderException {
        logger.logBeginStage("UPLOADING VALIDATORS");

        NodeList validators = assetNode.getElementsByTagName("validators");
        if (validators.getLength() == 0) {
            return;
        }

        Element validatorsNode = (Element) validators.item(0);
        Element singleValidator = (Element) validatorsNode.getElementsByTagName("validator").item(0); // wtf

        Path validatorPath = Path.of(packagePath.toString(), singleValidator.getElementsByTagName("source").item(0)
                .getAttributes().getNamedItem("path").getTextContent());

        String validatorName = validatorPath.getFileName().toString();
        String validatorType = singleValidator.getElementsByTagName("source").item(0)
                .getAttributes().getNamedItem("type").getTextContent();
        logger.logInfo(String.format("Uploading validator %s of type \"%s\"",
                validatorName, validatorType));

        try {
            session.problemSaveFile(problemId, false, "source", validatorName, validatorPath.toFile(),
                    validatorType, null, null, null);
            session.problemSetValidator(problemId, validatorName);
        } catch (PolygonSessionException e) {
            throw new PolygonPackageUploaderException("Error happened while uploading validators", e);
        }
    }

    private void uploadSolutions(final Element assetNode) throws PolygonPackageUploaderException {
        logger.logBeginStage("UPLOADING SOLUTIONS");

        Element solutionsNode = (Element) assetNode.getElementsByTagName("solutions").item(0);
        NodeList solutions = solutionsNode.getElementsByTagName("solution");

        for (int i = 0; i < solutions.getLength(); ++i) {
            Element solutionNode = (Element) solutions.item(i);

            Element sourceNode = (Element) solutionNode.getElementsByTagName("source").item(0);

            Path solutionPath = Path.of(packagePath.toString(), sourceNode.getAttribute("path"));

            SolutionTag tag = switch (solutionNode.getAttribute("tag")) {
                case "main" -> SolutionTag.MA;
                case "accepted" -> SolutionTag.OK;
                case "rejected" -> SolutionTag.RJ;
                case "wrong-answer" -> SolutionTag.WA;
                case "memory-limit-exceeded" -> SolutionTag.ML;
                case "presentation-error" -> SolutionTag.PE;
                case "time-limit-exceeded" -> SolutionTag.TL;
                case "time-limit-exceeded-or-memory-limit-exceeded" -> SolutionTag.TM;
                case "time-limit-exceeded-or-accepted" -> SolutionTag.TO;
                default -> SolutionTag.NR;
            };
            String type = sourceNode.getAttribute("type");
            String solutionName = solutionPath.getFileName().toString();

            logger.logInfo(String.format("Uploading solution %s of type \"%s\" with tag \"%s\"",
                    solutionName, type, tag));
            if (!properties.contains(UploaderProperties.ONLY_MAIN_SOLUTION)
                    || properties.contains(UploaderProperties.ONLY_MAIN_SOLUTION) && tag == SolutionTag.MA) {
                try {
                    session.problemSaveSolution(problemId, false, solutionName, solutionPath.toFile(),
                            type, tag);
                } catch (PolygonSessionException e) {
                    throw new PolygonPackageUploaderException("Error happened while uploading solutions", e);
                }
            }
        }
    }

    private void uploadInteractor(final Element assetNode) throws PolygonPackageUploaderException {
        logger.logBeginStage("UPLOADING INTERACTOR");

        NodeList interactorNode = assetNode.getElementsByTagName("interactor");
        if (interactorNode.getLength() != 0) {
            try {
                session.problemUpdateInfo(problemId, null, null, true, null, null);
            } catch (PolygonSessionException e) {
                throw new PolygonPackageUploaderException("Error happened while setting interactive mode", e);
            }

            Node interactorSource = ((Element) interactorNode.item(0)).getElementsByTagName("source").item(0);
            Path interactorPath = Path.of(interactorSource.getAttributes().getNamedItem("path").getTextContent());
            String interactorType = interactorSource.getAttributes().getNamedItem("type").getTextContent();

            logger.logInfo(String.format("Uploading interactor %s of type \"%s\"",
                    interactorPath.getFileName().toString(), interactorType));

            try {
                session.problemSaveFile(problemId, false, "source", interactorPath.getFileName().toString(),
                        Path.of(packagePath.toString(), interactorPath.toString()).toFile(),
                        interactorType, null, null, null);
                session.problemSetInteractor(problemId, interactorPath.getFileName().toString());
            } catch (PolygonSessionException e) {
                throw new PolygonPackageUploaderException("Error happened while uploading interactor", e);
            }
        }
    }

    private void uploadStatements() throws PolygonPackageUploaderException {
        logger.logBeginStage("UPLOADING STATEMENTS");

        Path statementSectionsPath = Path.of(packagePath.toString(), "statement-sections");
        Path statementsPath = Path.of(packagePath.toString(), "statements");
        if (!Files.exists(statementSectionsPath) && !Files.exists(statementsPath)) {
            return;
        }

        if (Files.exists(statementSectionsPath)) {
            try {
                List<Exception> exceptions = new ArrayList<>();
                Files.list(statementSectionsPath).forEach(path -> {
                    String language = path.getFileName().toString();
                    logger.logInfo(String.format("Uploading %s statements", language));

                    try {
                        String name = readFileContent(Path.of(path.toString(), "name.tex"));
                        String legend = readFileContent(Path.of(path.toString(), "legend.tex"));
                        String input = readFileContent(Path.of(path.toString(), "input.tex"));
                        String output = readFileContent(Path.of(path.toString(), "output.tex"));
                        String scoring = readFileContent(Path.of(path.toString(), "scoring.tex"));
                        String notes = readFileContent(Path.of(path.toString(), "notes.tex"));
                        String tutorial = readFileContent(Path.of(path.toString(), "tutorial.tex"));
                        session.problemSaveStatement(problemId, language, "utf-8", name, legend, input, output,
                                scoring, notes, tutorial);
                    } catch (PolygonSessionException | PolygonPackageUploaderException e) {
                        exceptions.add(e);
                    }
                });

                if (!exceptions.isEmpty()) {
                    throw new PolygonPackageUploaderException("Error happened while uploading statements",
                            exceptions.get(0));
                }
            } catch (IOException e) {
                throw new PolygonPackageUploaderException("Error happened while parsing statement sections", e);
            }
        } else {
            uploadStatementsFromDirectory(statementsPath);
        }
    }

    private void uploadTags() throws PolygonPackageUploaderException {
        logger.logBeginStage("UPLOADING TAGS");

        NodeList tagsNode = problem.getElementsByTagName("tags");
        if (tagsNode.getLength() != 0) {
            NodeList tags = ((Element) tagsNode.item(0)).getElementsByTagName("tag");
            String[] tagsArray = new String[tags.getLength()];
            for (int i = 0; i < tags.getLength(); i++) {
                Node property = tags.item(i);
                String value = property.getAttributes().getNamedItem("value").getTextContent();
                tagsArray[i] = value;
            }

            try {
                session.problemSaveTags(problemId, tagsArray);
            } catch (PolygonSessionException e) {
                throw new PolygonPackageUploaderException("Error happened while uploading tags", e);
            }
        }
    }

    private String readFileContent(final Path path) throws PolygonPackageUploaderException {
        if (!Files.exists(path)) {
            return null;
        }
        try {
            return Files.readAllLines(path).stream().collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            throw new PolygonPackageUploaderException(String.format("Error happened while reading file %s", path), e);
        }
    }
}
