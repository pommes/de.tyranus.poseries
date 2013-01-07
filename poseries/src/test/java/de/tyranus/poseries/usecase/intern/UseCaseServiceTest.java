package de.tyranus.poseries.usecase.intern;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import de.tyranus.poseries.config.UseCaseServiceTestConfig;
import de.tyranus.poseries.usecase.PostProcessMode;
import de.tyranus.poseries.usecase.ProgressObservable;
import de.tyranus.poseries.usecase.UseCase;
import de.tyranus.poseries.usecase.UseCaseException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { UseCaseServiceTestConfig.class }, loader = AnnotationConfigContextLoader.class)
public class UseCaseServiceTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(UseCaseServiceTest.class);
	public final static String DIR_REL_SRC_1 = "src/test/resources/data/1/src";
	public final static String DIR_REL_SRC_1_S05E01 = DIR_REL_SRC_1 + "/abc-testvid-s05e01";
	public final static String DIR_REL_SRC_1_S05E01_AVI = DIR_REL_SRC_1_S05E01 + "/abc-testvid-s05e01.avi";
	public final static String DIR_REL_SRC_1_S05E02 = DIR_REL_SRC_1 + "/abc-testvid-s05e02";
	public final static String DIR_REL_SRC_1_S05E02_AVI = DIR_REL_SRC_1_S05E02 + "/other.avi";
	public final static String DIR_REL_DST_1 = "src/test/resources/data/1/dst";

	@Autowired
	UseCase useCaseService;

	@Test
	public void test_findSrcDirPattern_Minus_ok() {
		final File file = new File(DIR_REL_SRC_1_S05E01);
		final Path path = Paths.get(file.getAbsolutePath());

		final String pattern = useCaseService.findSrcDirPattern(path.toString());
		assertEquals("abc-testvid*", pattern);
	}

	@Test
	public void test_findSrcDirPattern_Misc_ok() {
		final File file = new File(DIR_REL_SRC_1 + "/Text mit Leerzeichen_Unterstrichen-Minus 030201");
		final Path path = Paths.get(file.getAbsolutePath());

		final String pattern = useCaseService.findSrcDirPattern(path.toString());
		assertEquals("Text mit Leerzeichen_Unterstrichen*", pattern);
	}

	@Test
	public void test_createFinalSrcDir_ok() {
		final Path finalPath = useCaseService.createFinalSrcDir(DIR_REL_SRC_1_S05E01);
		assertEquals("src", finalPath.getName(finalPath.getNameCount() - 1).toString());
	}

	@Test
	public void test_findMatchingSrcDirs_ok() throws UseCaseException {
		final File file = new File(DIR_REL_SRC_1);
		final Path path = Paths.get(file.getAbsolutePath());

		final Set<Path> paths = useCaseService.findMatchingSrcDirs(path, "abc-testvid*");
		assertEquals(3, paths.size());
	}

	@Test
	public void test_findMatchingSrcDirsExtensions_ok() throws UseCaseException {
		final File file = new File(DIR_REL_SRC_1);
		final Path path = Paths.get(file.getAbsolutePath());
		final Set<String> extensions = new HashSet<>();
		extensions.add("avi");
		extensions.add("flv");

		final Set<Path> paths = useCaseService.findMatchingSrcDirs(path, "abc-testvid*", extensions);
		assertEquals(2, paths.size());
	}

	@Test
	public void test_formatFileList_ok() {
		final Set<Path> files = new HashSet<Path>();
		files.add(Paths.get("abc/def/ghi-001/tim.avi"));
		files.add(Paths.get("abc/def/ghi-002/nochein.avi"));
		files.add(Paths.get("abc/def/ghi-003/tim.wmv"));

		final String result = useCaseService.formatFileList(files);
		assertEquals("nochein.avi\ntim.avi\ntim.wmv", result);
	}

	@Test
	public void test_explodeExtensions_ok() {
		final Set<String> extensions = new HashSet<String>();
		extensions.add("avi");
		extensions.add("wmv");

		final String result = useCaseService.explodeVideoExtensions(extensions);
		assertEquals("wmv,avi", result);
	}

	@Test
	public void test_implodeExtensions_ok() {
		final Set<String> extensions = new HashSet<String>();
		extensions.add("avi");
		extensions.add("wmv");

		final Set<String> result = useCaseService.implodeVideoExtensions("avi,wmv");
		assertEquals(extensions, result);
	}

	@Test
	public void test_postProcessSeries_copy_ok() throws UseCaseException {
		final Set<Path> files = new HashSet<Path>();
		files.add(Paths.get(DIR_REL_SRC_1_S05E01_AVI));
		files.add(Paths.get(DIR_REL_SRC_1_S05E02_AVI));
		final ProgressObservable observable = new ProgressObservable();

		useCaseService.processFiles(files, Paths.get(DIR_REL_DST_1), PostProcessMode.Copy, observable);

	}
}
