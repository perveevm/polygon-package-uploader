package ru.perveevm.polygon.packages;

import net.lingala.zip4j.ZipFile;
import ru.perveevm.polygon.api.PolygonSession;
import ru.perveevm.polygon.packages.exceptions.PolygonPackageUploaderException;
import ru.perveevm.polygon.packages.uploaders.PolygonPackageUploaderWorker;
import ru.perveevm.polygon.packages.uploaders.UploaderProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * @author Perveev Mike (perveev_m@mail.ru)
 */
public class PackageUploader {
    private final PolygonSession session;
    private final Set<UploaderProperties> properties;

    public PackageUploader(final String key, final String secret, final Set<UploaderProperties> properties) {
        session = new PolygonSession(key, secret);
        this.properties = properties;
    }

    public void uploadProblem(final Path packagePath, final int problemId) throws PolygonPackageUploaderException {
        new PolygonPackageUploaderWorker(session, packagePath, problemId, properties).uploadProblem();
    }

    public void uploadProblemFromZip(final Path packageArchivePath, final int problemId)
            throws PolygonPackageUploaderException {
        Path tempPath;
        try {
            tempPath = Files.createTempDirectory("polygon-uploader");
        } catch (IOException e) {
            throw new PolygonPackageUploaderException("Couldn't create temporary directory to extract an archive", e);
        }
        tempPath.toFile().deleteOnExit();

        try {
            ZipFile zipFile = new ZipFile(packageArchivePath.toFile());
            zipFile.extractAll(tempPath.toString());
        } catch (IOException e) {
            throw new PolygonPackageUploaderException("Couldn't extract an archive", e);
        }

        uploadProblem(tempPath, problemId);
    }
}
