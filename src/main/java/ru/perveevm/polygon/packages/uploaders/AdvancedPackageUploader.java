package ru.perveevm.polygon.packages.uploaders;

import ru.perveevm.polygon.api.entities.Problem;
import ru.perveevm.polygon.exceptions.api.PolygonSessionException;
import ru.perveevm.polygon.exceptions.user.PolygonUserSessionException;
import ru.perveevm.polygon.packages.ConsoleLogger;
import ru.perveevm.polygon.packages.exceptions.PolygonPackageUploaderException;
import ru.perveevm.polygon.user.PolygonUserSession;

import java.nio.file.Path;

public class AdvancedPackageUploader {
    private final PackageUploader uploader;
    private final PolygonUserSession session;
    private final String login;

    private final ConsoleLogger logger = new ConsoleLogger(this.getClass().getName());

    public AdvancedPackageUploader(final String login, final String password, final PackageUploader uploader) {
        this.uploader = uploader;
        this.session = new PolygonUserSession(login, password);
        this.login = login;
    }

    public AdvancedPackageUploader(final PackageUploader uploader) {
        this.uploader = uploader;
        this.session = null;
        this.login = null;
    }

    private int createProblem(final String problemName) throws PolygonPackageUploaderException {
        logger.logBeginStage("CREATING PROBLEM");

        if (session == null) {
            throw new IllegalStateException("Trying to create problem when PolygonUserSession is null");
        }

        try {
            session.problemCreate(problemName);
        } catch (PolygonUserSessionException e) {
            throw new PolygonPackageUploaderException(
                    "Error happened while creating new problem %s".formatted(problemName), e);
        }

        try {
            Problem[] problems = uploader.getSession().problemsList(false, null, problemName, login);
            if (problems.length == 0) {
                throw new PolygonPackageUploaderException(
                        "Cannot find created problem with name %s".formatted(problemName));
            }
            if (problems.length > 1) {
                throw new PolygonPackageUploaderException(
                        "Multiple problems found with name %s".formatted(problemName));
            }
            return problems[0].getId();
        } catch (PolygonSessionException e) {
            throw new PolygonPackageUploaderException(
                    "Error happened while creating new problem %s".formatted(problemName), e);
        }
    }

    private void commitAndPackage(final int problemId) throws PolygonPackageUploaderException {
        if (session == null) {
            throw new IllegalStateException("Trying to create problem when PolygonUserSession is null");
        }

        logger.logBeginStage("COMMITTING PROBLEM");
        try {
            session.problemCommit(problemId);
            logger.logBeginStage("BUILDING PACKAGE");
            session.problemBuildPackage(problemId, true, true);
        } catch (PolygonUserSessionException e) {
            throw new PolygonPackageUploaderException(
                    "Error happened while committing and packaging new problem %d".formatted(problemId), e);
        }
    }

    public void uploadProblem(final Path packagePath, final String problemName) throws PolygonPackageUploaderException {
        if (session == null) {
            throw new IllegalStateException("Trying to create problem when PolygonUserSession is null");
        }

        int problemId = -1;
        try {
            problemId = createProblem(problemName);
            uploader.uploadProblem(packagePath, problemId);
            commitAndPackage(problemId);
        } catch (PolygonPackageUploaderException e) {
            try {
                session.problemDelete(problemId);
            } catch (PolygonUserSessionException ex) {
                throw new PolygonPackageUploaderException("Couldn't delete failed problem", ex);
            }

            throw e;
        }
    }

    public void uploadProblem(final Path packagePath, final int problemId) throws PolygonPackageUploaderException {
        uploader.uploadProblem(packagePath, problemId);
    }
}
