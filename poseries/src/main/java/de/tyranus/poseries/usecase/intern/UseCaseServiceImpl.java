package de.tyranus.poseries.usecase.intern;

import java.io.IOError;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tyranus.poseries.usecase.PostProcessMode;
import de.tyranus.poseries.usecase.ProgressObservable;
import de.tyranus.poseries.usecase.UseCaseService;
import de.tyranus.poseries.usecase.UseCaseServiceException;

/**
 * Default implementation of the {@link UseCaseService}.
 * 
 * @author Tim
 * 
 */
public final class UseCaseServiceImpl implements UseCaseService {
	private static final Logger LOGGER = LoggerFactory.getLogger(UseCaseServiceImpl.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.tyranus.poseries.usecase.UseCase#findSrcDirPattern(java.lang.String)
	 */
	public String findSrcDirPattern(String selectedSrcDir) {

		final Path path = Paths.get(selectedSrcDir);
		final Path lastPart = path.getName(path.getNameCount() - 1);

		// try intelligent search for last appearance of _ or - or \s.
		final String pattern = "^(.*[_-]|\\s*[_-]|\\s)(.*)$";
		final String patternResult = lastPart.toString().replaceAll(pattern, "$1");
		if (patternResult.length() > 0 && patternResult.length() < lastPart.toString().length()) {
			return patternResult.substring(0, patternResult.length() - 1) + "*";
		}

		// try the first half of the selectedSrcDir
		return lastPart.toString().substring(0, (lastPart.toString().length() / 2)) + "*";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.tyranus.poseries.usecase.UseCase#createFinalSrcDir(java.lang.String)
	 */
	public Path createFinalSrcDir(String selectedSrcDir) {
		final Path finalSrcDir = FileSystems.getDefault().getPath(selectedSrcDir);
		return finalSrcDir.getParent();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.tyranus.poseries.usecase.UseCase#findMatchingSrcDirs(java.nio.file.Path
	 * ,java.lang.String)
	 */
	public Set<Path> findMatchingSrcDirs(Path finalSrcDir, String srcDirPattern) throws UseCaseServiceException {
		return findMatchingSrcDirs(finalSrcDir, srcDirPattern, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.tyranus.poseries.usecase.UseCase#findMatchingSrcDirs(java.nio.file.Path
	 * ,java.lang.String,java.util.Set)
	 */
	@Override
	public Set<Path> findMatchingSrcDirs(Path finalSrcDir, String srcDirPattern, Set<String> extensions)
			throws UseCaseServiceException {

		// If no extensions are set use '*' as all extension
		final StringBuilder sbExtensions = new StringBuilder();
		if (extensions == null || extensions.size() == 0) {
			sbExtensions.append("*");
		}
		else {
			// Otherwise use the set extensions, for example: *.{ext1,ext2}
			final String strBegin = "*.{";
			sbExtensions.append(strBegin);
			for (String extension : extensions) {
				if (sbExtensions.length() > strBegin.length()) {
					sbExtensions.append(",");
				}
				sbExtensions.append(extension);
			}
			sbExtensions.append("}");
		}

		final String finalSrsDirStr = finalSrcDir.toString().replace("\\", "\\\\");
		final PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher(String.format("glob:%s/%s/%s",
				finalSrsDirStr,
				srcDirPattern,
				sbExtensions.toString()));
		final Set<Path> matchingDirs = new HashSet<Path>();

		try {
			Files.walkFileTree(finalSrcDir, new FileVisitor<Path>() {
				public FileVisitResult visitFileFailed(Path file, IOException ioException) {
					LOGGER.warn("Failed to visit file: {}", ioException.getMessage());
					return FileVisitResult.CONTINUE;
				}

				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
					if ((file != null) && (attrs != null)) {
						if (pathMatcher.matches(file)) {
							matchingDirs.add(file);
						}
						else {
							LOGGER.info("Ignoring file: {}", file.toString());
						}
					}
					return FileVisitResult.CONTINUE;
				}

				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					return FileVisitResult.CONTINUE;
				}
			});
		}
		catch (IOException e) {
			throw UseCaseServiceException.createReadError(e);
		}
		return matchingDirs;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.tyranus.poseries.usecase.UseCaseService#formatFileList(java.util.Set)
	 */
	public String formatFileList(Set<Path> files) {
		final List<String> orderedList = new ArrayList<String>();
		for (Path file : files) {
			final String filename = file.getName(file.getNameCount() - 1).toString();
			orderedList.add(filename);
		}
		Collections.sort(orderedList);

		final StringBuilder sb = new StringBuilder();
		for (String file : orderedList) {
			if (sb.length() > 0) {
				sb.append("\n");
			}
			sb.append(file);
		}
		return sb.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.tyranus.poseries.usecase.UseCaseService#getFoundVideoExtensions(java
	 * .util.List)
	 */
	public Set<String> getFoundVideoExtensions(Set<Path> files) {
		final Set<String> result = new HashSet<String>();
		final List<String> extensions = new ArrayList<String>();
		extensions.add("avi");
		for (Path file : files) {
			final String filename = file.getName(file.getNameCount() - 1).toString();
			final String[] parts = filename.split("\\.");
			if (parts.length > 1) {
				final String extension = parts[parts.length - 1];
				if (extensions.contains(extension)) {
					result.add(extension);
				}
			}
			else {
				LOGGER.warn("Ignoring file without extension: {}.", filename);
			}
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.tyranus.poseries.usecase.UseCaseService#explodeVideoExtensions(java
	 * .util.List)
	 */
	public String explodeVideoExtensions(Set<String> extensions) {
		final StringBuilder sb = new StringBuilder();
		for (String ext : extensions) {
			if (sb.length() > 0) {
				sb.append(",");
			}
			sb.append(ext);
		}
		return sb.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.tyranus.poseries.usecase.UseCaseService#implodeVideoExtensions(java
	 * .lang.String)
	 */
	public Set<String> implodeVideoExtensions(String extensions) {
		final String[] array = extensions.split(",");
		final Set<String> result = new HashSet<String>(Arrays.asList(array));
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.tyranus.poseries.usecase.UseCase#postProcessSeries(java.util.Set,
	 * java.util.Path,
	 * de.tyranus.poseries.usecase.PostProcessMode)
	 */
	public void postProcessSeries(final Set<Path> sourceFiles,
			final Path dstPath,
			final PostProcessMode mode,
			final ProgressObservable observable) throws UseCaseServiceException {
		// sort files
		final List<Path> orderedFiles = new ArrayList<>(sourceFiles);
		Collections.sort(orderedFiles);

		// process files parallel
		// Runtime.getRuntime().availableProcessors()
		final ExecutorService exec = Executors.newFixedThreadPool(1);
		try {
			for (final Path src : orderedFiles) {
				exec.submit(new Runnable() {
					@Override
					public void run() {
						// target filename
						final Path dst = Paths.get(dstPath + "/" + src.getName(src.getNameCount() - 1));
						LOGGER.debug("processing file to: {}", dst.toString());
						try {
							switch (mode) {
							case Copy:
								Files.copy(src, dst);
								break;
							case Move:
								Files.move(src, dst);
								break;
							default:
								throw new IllegalAccessError(String.format("The mode '%s' is not supported!", mode));
							}
						}
						catch (IOException e) {
							throw new IOError(e);
						}

						observable.updateProgress();
					}
				});

			}
		}
		catch (IOError e) {
			throw UseCaseServiceException.createCopyMoveError((IOException) e.getCause(), mode);
		}
		finally {
			exec.shutdown();
		}
	}
}
