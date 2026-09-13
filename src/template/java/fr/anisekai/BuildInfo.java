package fr.anisekai;

import java.time.format.DateTimeFormatter;

/**
 * This class is automatically updated and compile upon project compilation, and allow to retrieve various information
 * about the build, like the version and the datetime at which the build occurred.
 */
@SuppressWarnings("SameReturnValue")
public final class BuildInfo {

    private BuildInfo() {}

    /**
     * Retrieve the project version. The version is composed of 3 numbers separated by a dot and prefixed by the letter
     * {@code v}.
     * <p>
     * For {@code vX.Y.Z}, {@code X} is the major version, {@code Y} is the minor version amd {@code Z} is the patch
     * version
     * <p>
     * On some special builds, the version can be suffixed with the release stream (like beta or release candidate
     * versions), and the build time (if no tag has been created). Some examples:
     * <ul>
     *     <li>v2.0.0-beta.1</li>
     *     <li>v2.0.0-rc.3</li>
     *     <li>v2.0.2-beta.0.2+20251023T213936Z</li>
     * </ul>
     *
     * @return The version (vX.X.X)
     */
    public static String getVersion() {

        return "v${version}";
    }

    /**
     * Retrieve the project build date, represented as string using the {@link DateTimeFormatter#ISO_DATE_TIME}
     * formatter.
     *
     * @return The datetime at which the build occurred.
     */
    public static String getDate() {

        return "${date}";
    }

}
