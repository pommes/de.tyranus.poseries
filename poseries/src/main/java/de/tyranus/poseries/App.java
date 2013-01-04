package de.tyranus.poseries;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import de.tyranus.poseries.config.PoseriesConfig;
import de.tyranus.poseries.gui.MainWindow;

/**
 * Startet die App.
 */
public class App {
	private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

	@Autowired
	MainWindow window;

	public static void main(String[] args) {
		ApplicationContext context = null;
		try {
			context = new AnnotationConfigApplicationContext(PoseriesConfig.class);
		}
		catch (Exception e) {

			LOGGER.error(e.getMessage());
			System.exit(1);
		}

		context.getBean(App.class).start();
	}

	public void start() {
		window.open();
	}
}
