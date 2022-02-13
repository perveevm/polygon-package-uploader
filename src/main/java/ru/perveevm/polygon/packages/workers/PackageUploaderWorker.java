package ru.perveevm.polygon.packages.workers;

import ru.perveevm.polygon.packages.exceptions.PolygonPackageUploaderException;

/**
 * @author Perveev Mike (perveev_m@mail.ru)
 */
public interface PackageUploaderWorker {
    void uploadProblem() throws PolygonPackageUploaderException;
}
