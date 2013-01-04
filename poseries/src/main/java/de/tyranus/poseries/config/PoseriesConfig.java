package de.tyranus.poseries.config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import de.tyranus.poseries.App;
import de.tyranus.poseries.gui.MainWindow;
import de.tyranus.poseries.usecase.UseCaseService;
import de.tyranus.poseries.usecase.intern.UseCaseServiceImpl;

@Configuration
/*
 * @PropertySource(value = { "classpath:/" +
 * PoseriesConfig.FILE_GLOBAL_PROPERTIES,
 * "classpath:/" + PoseriesConfig.FILE_LOCAL_PROPERTIES })
 */
public class PoseriesConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger(PoseriesConfig.class);

	public final static String FILE_GLOBAL_PROPERTIES = "global.properties";
	public final static String FILE_LOCAL_PROPERTIES = "local.properties";

	@Autowired
	private Environment env;

	@Value("${ext.history.1}")
	private String[] extHistory1;	
	@Value("${ext.history.2}")
	private String[] extHistory2;
	@Value("${ext.history.3}")
	private String[] extHistory3;
	@Value("${ext.history.4}")
	private String[] extHistory4;
	@Value("${ext.history.5}")
	private String[] extHistory5;
	@Value("${process.parallel.count}")
	private int processParallelCount;
	

	/**
	 * Creats properties from the properties files.
	 * 
	 * @return
	 * @throws IOException
	 *             if the global properties file does not exist or the local
	 *             properties file could not be created.
	 */
	@Bean
	public static PropertySourcesPlaceholderConfigurer properties() throws IOException {
		final PropertySourcesPlaceholderConfigurer pspc = new PropertySourcesPlaceholderConfigurer();
		final ClassPathResource resGlobal = new ClassPathResource(FILE_GLOBAL_PROPERTIES);
		final ClassPathResource resLocal = new ClassPathResource(FILE_LOCAL_PROPERTIES);

		// Throw exception if the file for resGlobal does not exist.
		resGlobal.getFile();

		try {
			try {
				// Throw exception if file for resLocal does not exist.
				resLocal.getFile();
			}
			catch (IOException e) {
				LOGGER.warn("File '{}' does not exist. Creating it...", PoseriesConfig.FILE_LOCAL_PROPERTIES);
				final File localFile = new File(resGlobal.getFile().getAbsolutePath()
						.replace(PoseriesConfig.FILE_GLOBAL_PROPERTIES, PoseriesConfig.FILE_LOCAL_PROPERTIES));
				localFile.createNewFile();
				LOGGER.info("File created: '{}'", resLocal.getFile().getAbsoluteFile());
			}
		}
		catch (IOException e) {
			// Catch exceptions during creation of not existing local properties file
			throw e;
		}

		final Resource[] resources = new ClassPathResource[] { resGlobal, resLocal };
		pspc.setLocations(resources);
		pspc.setIgnoreUnresolvablePlaceholders(true);
		return pspc;
	}
	
	@Bean
	public Properties localProperties() throws IOException {
		final Resource resource = new ClassPathResource(FILE_LOCAL_PROPERTIES);
		final Properties props = PropertiesLoaderUtils.loadProperties(resource);
		return props;
	}
	
	@Bean File localPropertiesFile() throws IOException {
		return new ClassPathResource(FILE_LOCAL_PROPERTIES).getFile();
	}

	@Bean
	public App app() {
		return new App();
	}

	@Bean
	public MainWindow mainWindow() {
		final Set<String[]>extHistory = new HashSet<>();
		extHistory.add(extHistory1);
		extHistory.add(extHistory2);
		extHistory.add(extHistory3);
		extHistory.add(extHistory4);
		extHistory.add(extHistory5);
		
		return new MainWindow(extHistory);
	}

	@Bean
	public UseCaseService useCaseService() {
		return new UseCaseServiceImpl(processParallelCount);
	}
	
	@PostConstruct
	public void logProperties() {
		LOGGER.info("### Collected Properties ###");
		LOGGER.info(String.format("# ext.history.1=%s", Arrays.toString(extHistory1)));
		LOGGER.info(String.format("# ext.history.2=%s", Arrays.toString(extHistory2)));
		LOGGER.info(String.format("# ext.history.3=%s", Arrays.toString(extHistory3)));
		LOGGER.info(String.format("# ext.history.4=%s", Arrays.toString(extHistory4)));
		LOGGER.info(String.format("# ext.history.5=%s", Arrays.toString(extHistory5)));
		LOGGER.info(String.format("# process.parallel.count=%s", processParallelCount));
		LOGGER.info("############################");
	}
}
