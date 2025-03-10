package arc.funpay.ext

import java.io.File

/**
 * Object for handling application paths.
 */
object ArcPath {
    /**
     * The application directory.
     * Lazily initialized to the directory of the application's code source location.
     */
    val applicationDirectory: File by lazy {
        val file = File(
            ArcPath::class.java.protectionDomain.codeSource.location.toURI()
        ).parentFile

        if (!file.exists()) {
            file.mkdirs()
        }
        file
    }

    /**
     * Retrieves a file based on the specified location and path.
     *
     * @param loc The location type (CLASSPATH, ABSOLUTE, LOCAL).
     * @param path The path to the file.
     * @return The file corresponding to the specified location and path.
     */
    fun file(loc: FileLocation, path: String): File = when (loc) {
        FileLocation.CLASSPATH -> classpath(path)
        FileLocation.ABSOLUTE -> absolute(path)
        FileLocation.LOCAL -> local(path)
    }

    /**
     * Retrieves a file from the classpath.
     *
     * @param path The path to the file in the classpath.
     * @return The file corresponding to the specified classpath.
     */
    fun classpath(path: String): File {
        val classLoader = Thread.currentThread().contextClassLoader
        val resourceUrl = classLoader.getResource(path) ?: return File("")
        return File(resourceUrl.toURI())
    }

    /**
     * Retrieves a file from an absolute path.
     *
     * @param path The absolute path to the file.
     * @return The file corresponding to the specified absolute path.
     */
    fun absolute(path: String): File = File(path)

    /**
     * Retrieves a file from the local application directory.
     *
     * @param path The path to the file relative to the application directory.
     * @return The file corresponding to the specified local path.
     */
    fun local(path: String): File = File(applicationDirectory, path)
}

/**
 * Enum class representing different file locations.
 */
enum class FileLocation {
    CLASSPATH,
    ABSOLUTE,
    LOCAL
}