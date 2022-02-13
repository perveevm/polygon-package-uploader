package ru.perveevm.polygon.packages.workers;

import ru.perveevm.polygon.api.PolygonSession;
import ru.perveevm.polygon.packages.PackageType;

import java.nio.file.Path;
import java.util.Set;

/**
 * @author Perveev Mike (perveev_m@mail.ru)
 */
public class PackageUploaderWorkerFactory {
    public static PackageUploaderWorker newPackageUploaderWorker(final PackageType packageType,
                                                                 final PolygonSession session, final Path packagePath,
                                                                 final int problemId,
                                                                 final Set<UploaderProperties> properties) {
        if (packageType == PackageType.POLYGON) {
            return new PolygonPackageUploaderWorker(session, packagePath, problemId, properties);
        }
        if (packageType == PackageType.PCMS) {
            return new PCMSPackageUploaderWorker(session, packagePath, problemId, properties);
        }

        throw new IllegalArgumentException(String.format("Unsupported package type: %s", packageType.toString()));
    }
}
