package de.tyranus.poseries.usecase.intern;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
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
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.joda.time.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tyranus.poseries.usecase.PostProcessMode;
import de.tyranus.poseries.usecase.Progress;
import de.tyranus.poseries.usecase.ProgressObservable;
import de.tyranus.poseries.usecase.UseCase;
import de.tyranus.poseries.usecase.UseCaseException;

/**
 * Default implementation of the {@link UseCase}.
 * 
 * @author Tim
 * 
 */
public final class UseCaseImpl implements UseCase {
	private static final Logger LOGGER = LoggerFactory.getLogger(UseCaseImpl.class);

	@Autowired
	private File localPropertiesFile;

	@Autowired
	private Properties localProperties;

	private int processParallelCount;

	/**
	 * Creates the {@link UseCaseImpl}.
	 * 
	 * @param processParallelCount
	 *            Number of parallel threads used during file processing. Each
	 *            thread processes one file.
	 */
	public UseCaseImpl(int processParallelCount) {
		this.processParallelCount = processParallelCount;
	}

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
	public Set<Path> findMatchingSrcDirs(Path finalSrcDir, String srcDirPattern) throws UseCaseException {
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
			throws UseCaseException {

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
			throw UseCaseException.createReadError(e);
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
	 * de.tyranus.poseries.usecase.UseCaseService#convertFilePatternsToString
	 * (java.util.Set)
	 */
	@Override
	public String[] convertFileExtensionsToString(Set<String[]> extHistory) {
		final List<String> extensions = new ArrayList<>();
		for (String[] ext : extHistory) {
			final String str = arrayToStr(ext);
			if (!str.isEmpty()) {
				extensions.add(str);
			}
		}
		return extensions.toArray(new String[0]);
	}

	private String arrayToStr(String[] array) {
		return Arrays.toString(array).replace(" ", "").replace("[", "").replace("]", "");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.tyranus.poseries.usecase.UseCaseService#saveFilePatternHistory(java
	 * .lang.String[])
	 */
	@Override
	public Set<String[]> saveFileExtensionHistory(String[] extHistory) throws UseCaseException {
		final Set<String[]> result = new HashSet<>();
		int i = 1;
		for (String patterns : extHistory) {
			saveProperty("ext.history." + i++, patterns, "Update file patterns");
			result.add(patterns.split(","));
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
	public void processFiles(final Set<Path> sourceFiles,
			final Path dstPath,
			final PostProcessMode mode,
			final ProgressObservable observable) throws UseCaseException {
		// sort files
		final List<Path> orderedFiles = new ArrayList<>(sourceFiles);
		Collections.sort(orderedFiles);

		// process files parallel
		// Runtime.getRuntime().availableProcessors()
		final ExecutorService exec = Executors.newFixedThreadPool(processParallelCount);
		try {
			// Calculate the file size
			final long totalSize = getFileSize(sourceFiles);
			final long startTimeMillis = DateTimeUtils.currentTimeMillis();
			// Init progress object
			final Progress progress = new Progress(startTimeMillis, totalSize);
			LOGGER.debug("File size: {} Byte", totalSize);
			observable.updateTotalSize(progress);

			// Process the files
			for (final Path src : orderedFiles) {
				exec.submit(new Runnable() {
					@Override
					public void run() {
						// target filename
						final Path dst = Paths.get(dstPath + "/" + src.getName(src.getNameCount() - 1));
						Thread fileSizeMon = null;
						try {
							LOGGER.debug("processing file to: {}", dst.toString());

//							// Monitor the target file size
//							fileSizeMon = new Thread(new Runnable() {
//								long lastSize = 0;
//
//								@Override
//								public void run() {
//									boolean isInterrupted = false;
//									while (!isInterrupted) {
//										try {
//											Thread.sleep(1000);
//											final long newSize = Files.size(dst);
//											if (newSize != lastSize) {
//												observable.updateProgress(progress, newSize - lastSize);
//												lastSize = newSize;
//											}
//										}
//										catch (IOException e) {
//											LOGGER.debug("IOException during file size monitoring. This is because the target file was not found yet: {}",
//													e.getMessage());
//										}
//										catch (InterruptedException e) {
//											LOGGER.debug("file size monitoring thread interrupted.");
//											isInterrupted = true;
//										}
//									}
//								}
//							});
//							fileSizeMon.start();

							// Process the file
							switch (mode) {
							// TODO: Target directory must be empty!
							case Copy:
								//Files.copy(src, dst, COPY_ATTRIBUTES);
								copy(observable, progress, src, dst);
								break;
							case Move:
								try {
									// First try a atomic move. Its faster because an inode switch only.
									Files.move(src, dst, ATOMIC_MOVE, COPY_ATTRIBUTES);
								}
								catch (AtomicMoveNotSupportedException e) {
									LOGGER.debug("Atomic move not supported. Using normal move (copy/delete): {}", e);
									copy(observable, progress, src, dst);
									Files.delete(src);
								}
								break;
							default:
								throw new UnsupportedOperationException(String
										.format("The mode '%s' is not supported!", mode));
							}
						}
						catch (IOException e) {
							throw new IOError(e);
						}
						finally {
							if (fileSizeMon != null) {
								fileSizeMon.interrupt();
							}
						}
					}

					private void copy(ProgressObservable observable, Progress progress, Path src, Path dst) {
						try {
							final InputStream in = new FileInputStream(src.toFile());

							//For Append the file.
							//  OutputStream out = new FileOutputStream(f2,true);

							//For Overwrite the file.
							final OutputStream out = new FileOutputStream(dst.toFile());

							final byte[] buf = new byte[1024 * 1024 * 100]; // 100 MB Buffer
							int len;
							while ((len = in.read(buf)) > 0) {
								out.write(buf, 0, len);

								// Update progress listeners
								observable.updateProgress(progress, buf.length);
							}
							in.close();
							out.close();
						}
						catch (FileNotFoundException ex) {
							LOGGER.error(ex.getMessage() + " in the specified directory.");
							// TODOC
						}
						catch (IOException e) {
							LOGGER.error(e.getMessage());
							// TODOC
						}
					}
				});

			}
		}
		catch (IOError e) {
			throw UseCaseException.createCopyMoveError((IOException) e.getCause(), mode);
		}
		finally {
			exec.shutdown();
		}
	}

	private void saveProperty(String key, String value, String comment) throws UseCaseException {
		localProperties.setProperty(key, value);
		try (final OutputStream propOut = new FileOutputStream(localPropertiesFile)) {
			localProperties.store(propOut, comment);
		}
		catch (IOException e) {
			throw UseCaseException.createWriteErrorProperties(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.tyranus.poseries.usecase.UseCaseService#getFileSize(java.util.Set)
	 */
	@Override
	public long getFileSize(Set<Path> filesToProcess) throws UseCaseException {
		// Calculate the file size
		long size = 0;
		for (final Path file : filesToProcess) {
			try {
				size += Files.size(file);
			}
			catch (IOException e) {
				throw UseCaseException.createFileSizeError(e);
			}
		}
		return size;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.tyranus.poseries.usecase.UseCaseService#formatSize(long)
	 */
	@Override
	public String formatSize(long fileSize) {
		return formatSize(fileSize, Dimension.Byte);
	}

	private enum Dimension {
		Byte, kB, MB, GB
	}

	private String formatSize(double fileSize, Dimension dimension) {
		switch (dimension) {
		case Byte:
			if (fileSize >= 1000) {
				return formatSize(fileSize / 1000., Dimension.kB);
			}
			return Math.round(fileSize) + " Byte";
		case kB:
			if (fileSize >= 1000) {
				return formatSize(fileSize / 1000., Dimension.MB);
			}
			return Math.round(fileSize) + " kB";
		case MB:
			if (fileSize >= 1000) {
				return formatSize(fileSize / 1000., Dimension.GB);
			}
			return Math.round(fileSize) + " MB";
		default:
			return Math.round(fileSize) + " GB";
		}

	}

}
