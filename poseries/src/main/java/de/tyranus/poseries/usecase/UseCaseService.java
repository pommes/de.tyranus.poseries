package de.tyranus.poseries.usecase;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

/**
 * Business logic that is called by the presentation layer.
 * 
 * @author Tim
 * 
 */
public interface UseCaseService {

	/**
	 * Finds a source directory pattern by the selected source dir.
	 * 
	 * @param selectedSrcDir
	 * @return found source directory pattern.
	 */
	String findSrcDirPattern(String selectedSrcDir);

	/**
	 * Creates the final source dir from the selected source dir.
	 * The selected source dir is one of the downloaded series directories.
	 * 
	 * @param selectedSrcDir
	 *            one of the downloaded series directories.
	 * @return the final source directory which simply is one directory higher
	 *         than the selected source directory.
	 */
	Path createFinalSrcDir(String selectedSrcDir);

	/**
	 * Returns a list of subdirectories of finalSrcDir that matches the source
	 * directory pattern.
	 * 
	 * @param finalSrcDir
	 *            the finalized source dir.
	 * @param srcDirPattern
	 *            The source directory pattern.
	 * @return Set of matching files.
	 * @throws UseCaseServiceException
	 *             if an IOException while walking through the directories.
	 */
	Set<Path> findMatchingSrcDirs(Path finalSrcDir, String srcDirPattern) throws UseCaseServiceException;

	/**
	 * Returns a list of subdirectories of finalSrcDir that matches the source
	 * directory pattern.
	 * 
	 * @param finalSrcDir
	 *            the finalized source dir.
	 * @param srcDirPattern
	 *            The source directory pattern.
	 * @param extensions
	 *            pre set the file extensions to find.
	 * @return Set of matching files.
	 * @throws UseCaseServiceException
	 *             if an IOException while walking through the directories.
	 */
	Set<Path> findMatchingSrcDirs(Path finalSrcDir, String srcDirPattern, Set<String> extensions)
			throws UseCaseServiceException;

	/**
	 * Formats the found file list.
	 * 
	 * @param files
	 *            the file list
	 * @return the formatted file list.
	 */
	String formatFileList(Set<Path> files);

	/**
	 * Converts each String array to a comma separated string and adds it to the
	 * returning string array.
	 * 
	 * @param extHistory
	 *            the historical patterns (ordered as passed in the returning
	 *            string array).
	 * @return the converted strings.
	 */
	String[] convertFilePatternsToString(Set<String[]> extHistory);

	/**
	 * Saves the file pattern history
	 * 
	 * @param extHistory
	 *            the file pattern history and returns it as a set.
	 * @throws UseCaseServiceException
	 *             if the history could not be saved.
	 */
	Set<String[]> saveFilePatternHistory(String[] extHistory) throws UseCaseServiceException;

	/**
	 * Gets a string representation of the file extensions
	 * 
	 * @param extensions
	 *            the file extensions
	 * @return string representation.
	 */
	String explodeVideoExtensions(Set<String> extensions);

	/**
	 * Gets a set of file extensions from its string representation.
	 * 
	 * @param extensions
	 *            the string representation.
	 * @return the file extensions.
	 */
	Set<String> implodeVideoExtensions(String extensions);

	/**
	 * Post processes the source files and copy/moves them to dst depending to
	 * the mode.
	 * 
	 * @param sourceFiles
	 *            source files to process
	 * @param dst
	 *            destination directory to insert source files
	 * @param mode
	 *            processing mode
	 * @param observable
	 *            the observable that notifies progress changes.
	 * @throws UseCaseServiceException
	 */
	void postProcessSeries(Set<Path> sourceFiles, Path dst, PostProcessMode mode, ProgressObservable observable)
			throws UseCaseServiceException;

	/**
	 * Returns the size of the filesToProcess in bytes.
	 * 
	 * @param filesToProcess
	 *            the files to process.
	 * @return the sum of the files in bytes.
	 * @throws UseCaseServiceException
	 *             if an {@link IOException} occures during getting the file
	 *             size of one of the files to process.
	 */
	long getFileSize(Set<Path> filesToProcess) throws UseCaseServiceException;

	/**
	 * Formats the file size to a readable format
	 * 
	 * @param fileSize
	 *            in byte.
	 * @return human reabable file size in kB if less then 1000, in MB if less
	 *         then 1000*1000, ...
	 */
	String formatSize(long fileSize);

}
