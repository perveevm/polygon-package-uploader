package ru.perveevm.polygon.packages.workers;

import com.google.gson.Gson;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import ru.perveevm.polygon.api.PolygonSession;
import ru.perveevm.polygon.api.entities.Statement;
import ru.perveevm.polygon.packages.ConsoleLogger;
import ru.perveevm.polygon.packages.exceptions.PolygonPackageUploaderException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Perveev Mike (perveev_m@mail.ru)
 */
public abstract class AbstractPackageUploaderWorker implements PackageUploaderWorker {
    protected final ConsoleLogger logger = new ConsoleLogger(this.getClass().getName());

    protected final PolygonSession session;
    protected final Path packagePath;
    protected final int problemId;
    protected final Set<UploaderProperties> properties;

    protected AbstractPackageUploaderWorker(final PolygonSession session, final Path packagePath, final int problemId,
                                            final Set<UploaderProperties> properties) {
        this.session = session;
        this.packagePath = packagePath;
        this.problemId = problemId;
        this.properties = properties;
    }

    protected Document getXMLDocument(final Path packagePath, final String xmlConfigFileName)
            throws PolygonPackageUploaderException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setIgnoringElementContentWhitespace(true);

        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(Path.of(packagePath.toString(), xmlConfigFileName).toFile());
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new PolygonPackageUploaderException(String.format("Error happened while parsing %s config file",
                    xmlConfigFileName), e);
        }
    }

    protected String getGeneratorScript(final Map<String, List<Integer>> testsByScriptLine) {
        StringBuilder generatorScript = new StringBuilder();
        for (Map.Entry<String, List<Integer>> scriptLine : testsByScriptLine.entrySet()) {
            String testsNumber;
            if (scriptLine.getValue().size() == 1) {
                testsNumber = String.valueOf(scriptLine.getValue().get(0));
            } else {
                testsNumber = "{" +
                        scriptLine.getValue().stream().map(String::valueOf).collect(Collectors.joining(","))
                        + "}";
            }
            testsNumber += System.lineSeparator();
            generatorScript.append(scriptLine.getKey()).append(" > ")
                    .append(testsNumber);
        }
        return generatorScript.toString();
    }

    protected void uploadStatementsFromDirectory(final Path statementsPath) throws PolygonPackageUploaderException {
        try {
            List<Exception> exceptions = new ArrayList<>();
            Files.list(statementsPath).forEach(path -> {
                if (!Files.exists(Path.of(path.toString(), "problem-properties.json"))) {
                    return;
                }

                String language = path.getFileName().toString();
                logger.logInfo(String.format("Uploading %s statements", language));

                Gson gson = new Gson();
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = Files.newBufferedReader(Path.of(path.toString(),
                        "problem-properties.json"))) {
                    sb.append(reader.lines().collect(Collectors.joining(System.lineSeparator())));
                } catch (IOException e) {
                    exceptions.add(e);
                    return;
                }

                try {
                    Statement statement = gson.fromJson(sb.toString(), Statement.class);
                    session.problemSaveStatement(problemId, language, "utf-8", statement.getName(),
                            statement.getLegend(), statement.getInput(), statement.getOutput(),
                            statement.getScoring(), statement.getInteraction(), statement.getNotes(),
                            statement.getTutorial());
                } catch (Exception e) {
                    exceptions.add(e);
                }
            });

            if (!exceptions.isEmpty()) {
                throw new PolygonPackageUploaderException("Error happened while uploading statements",
                        exceptions.get(0));
            }
        } catch (IOException e) {
            throw new PolygonPackageUploaderException("Error happened while parsing statements", e);
        }
    }
}
