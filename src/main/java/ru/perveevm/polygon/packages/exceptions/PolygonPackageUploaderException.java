package ru.perveevm.polygon.packages.exceptions;

/**
 * @author Perveev Mike (perveev_m@mail.ru)
 */
public class PolygonPackageUploaderException extends Exception {
    public PolygonPackageUploaderException(final String message) {
        super("Error happened while uploading package to Polygon."
                + System.lineSeparator()
                + "Error message: "
                + message);
    }

    public PolygonPackageUploaderException(final String message, final Throwable cause) {
        super("Error happened while uploading package to Polygon."
                + System.lineSeparator()
                + "Error message: "
                + message
                + System.lineSeparator()
                + "Cause message: "
                + cause.getMessage());
    }
}
