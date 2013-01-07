package de.tyranus.poseries.gui;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wb.swt.SWTResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tyranus.poseries.usecase.PostProcessMode;
import de.tyranus.poseries.usecase.Progress;
import de.tyranus.poseries.usecase.Progress.Type;
import de.tyranus.poseries.usecase.ProgressObservable;
import de.tyranus.poseries.usecase.UseCase;
import de.tyranus.poseries.usecase.UseCaseException;

public class MainWindow implements Observer {
	private static final Logger LOGGER = LoggerFactory.getLogger(MainWindow.class);

	/** Size of extension list. Must match the properties file. */
	private static final int EXTENSION_LIST_SIZE = 5;

	@Autowired
	private UseCase useCase;

	private Display display;
	private Shell shell;
	private Text txtSrcDir;
	private Text txtDstDir;
	private Button btnSrcSelect;
	private Label lblSourceDirectory;
	private Label lblDestinationDirecory;
	private Button btnDstSelect;
	private Combo cmbFileExtensions;
	private Label lblFileExtensions;
	private Button btnRefresh;
	private Text txtSrcPattern;
	private Label lblSourceDirectoryPattern;
	private StyledText txtFiles;
	private Button btnCopy;
	private Button btnMove;
	private Button btnPostProcess;
	private ProgressBar progressBar;
	private Label lblTotalSize;
	private Label lblSpeed;
	private Label lblTotalSizeVal;
	private Label lblSpeedVal;

	private Cursor cursorWait;
	private Cursor cursorArrow;

	private Set<Path> filesToProcess;
	private Set<String[]> extHistory;
	private Label lblDirPattern;
	private Label lblFileExtension;
	private Label lblRefresh;
	private Label lblDestinationDir;
	private Label lblProcess;

	private Label lblEnd;
	private Label lblEndVal;

	public MainWindow(Set<String[]> extHistory) {
		this.extHistory = extHistory;
	}

	/**
	 * Launch the application.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			final Set<String[]> extHistory = new HashSet<>();
			extHistory.add(new String[] { "avi", "mkv" });
			final MainWindow window = new MainWindow(extHistory);
			window.open();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Open the window.
	 */
	public void open() {
		display = Display.getDefault();
		createContents();
		shell.open();
		shell.layout();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	/**
	 * Create contents of the window.
	 */
	protected void createContents() {
		cursorWait = new Cursor(display, SWT.CURSOR_WAIT);
		cursorArrow = new Cursor(display, SWT.CURSOR_ARROW);
		shell = new Shell(SWT.SHELL_TRIM & (~SWT.RESIZE) & (~SWT.MAX));
		shell.setSize(458, 625);
		shell.setText(Messages.MainWindow_shell_text);
		//shell.setImage(new Image(display, "icon.png"));
		final String[] filenamePatterns = useCase.convertFileExtensionsToString(extHistory);

		lblSourceDirectory = new Label(shell, SWT.NONE);
		lblSourceDirectory.setToolTipText(Messages.MainWindow_lblSourceDirectory_toolTipText);
		lblSourceDirectory.setAlignment(SWT.RIGHT);
		lblSourceDirectory.setBounds(20, 32, 129, 15);
		lblSourceDirectory.setText(Messages.MainWindow_lblSourceDirectory_text);

		txtSrcDir = new Text(shell, SWT.BORDER);
		txtSrcDir.addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseHover(MouseEvent e) {
				txtSrcDir.setToolTipText(txtSrcDir.getText());
			}
		});
		txtSrcDir.setBounds(155, 29, 199, 21);

		btnSrcSelect = new Button(shell, SWT.NONE);
		btnSrcSelect.setBounds(360, 27, 82, 25);
		btnSrcSelect.setText(Messages.MainWindow_btnSrcSelect_text);

		lblSourceDirectoryPattern = new Label(shell, SWT.NONE);
		lblSourceDirectoryPattern.setAlignment(SWT.RIGHT);
		lblSourceDirectoryPattern.setBounds(20, 105, 129, 15);
		lblSourceDirectoryPattern.setText(Messages.MainWindow_lblSourceDirectoryPattern_text);

		txtSrcPattern = new Text(shell, SWT.BORDER);
		txtSrcPattern.setEnabled(false);
		txtSrcPattern.setBounds(155, 102, 199, 23);

		lblFileExtensions = new Label(shell, SWT.NONE);
		lblFileExtensions.setToolTipText(Messages.MainWindow_lblFileExtensions_toolTipText);
		lblFileExtensions.setAlignment(SWT.RIGHT);
		lblFileExtensions.setBounds(20, 169, 129, 15);
		lblFileExtensions.setText(Messages.MainWindow_lblFileExtensions_text);

		cmbFileExtensions = new Combo(shell, SWT.NONE);
		cmbFileExtensions.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (cmbFileExtensions.getSelectionIndex() >= 0) {
					cmbFileExtensions.setText(cmbFileExtensions.getItem(cmbFileExtensions.getSelectionIndex())
							.toString());
				}
				else {
					LOGGER.debug("widgetSelected: No file pattern selected.");
				}
			}
		});
		cmbFileExtensions.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				LOGGER.debug("focusLost");
				updateExtensionList();
			}
		});
		cmbFileExtensions.setEnabled(false);
		cmbFileExtensions.setItems(filenamePatterns);
		cmbFileExtensions.setText(filenamePatterns[0]);
		cmbFileExtensions.setBounds(155, 166, 199, 23);

		btnRefresh = new Button(shell, SWT.NONE);
		btnRefresh.setEnabled(false);
		btnRefresh.setBounds(20, 219, 422, 25);
		btnRefresh.setText(Messages.MainWindow_btnRefresh_text);

		txtFiles = new StyledText(shell, SWT.BORDER | SWT.V_SCROLL);
		txtFiles.setFont(SWTResourceManager.getFont("Segoe UI", 10, SWT.NORMAL));
		txtFiles.setDoubleClickEnabled(false);
		txtFiles.setEditable(false);
		txtFiles.setBounds(20, 250, 422, 166);

		lblDestinationDirecory = new Label(shell, SWT.NONE);
		lblDestinationDirecory.setAlignment(SWT.RIGHT);
		lblDestinationDirecory.setBounds(20, 448, 129, 15);
		lblDestinationDirecory.setText(Messages.MainWindow_lblDestinationDirecory_text);

		txtDstDir = new Text(shell, SWT.BORDER);
		txtDstDir.setEnabled(false);
		txtDstDir.addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseHover(MouseEvent e) {
				txtDstDir.setToolTipText(txtDstDir.getText());
			}
		});
		txtDstDir.setBounds(155, 445, 159, 21);

		btnDstSelect = new Button(shell, SWT.NONE);
		btnDstSelect.setEnabled(false);
		btnDstSelect.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectDestination();
			}
		});
		btnDstSelect.setBounds(320, 443, 82, 25);
		btnDstSelect.setText(Messages.MainWindow_btnDstSelect_text);

		btnPostProcess = new Button(shell, SWT.NONE);
		btnPostProcess.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				processFiles();
			}
		});
		btnPostProcess.setEnabled(false);
		btnPostProcess.setBounds(20, 515, 422, 25);
		btnPostProcess.setText(Messages.MainWindow_btnPostProcess_text);

		progressBar = new ProgressBar(shell, SWT.NONE);
		progressBar.setBounds(20, 546, 422, 17);

		btnCopy = new Button(shell, SWT.RADIO);
		btnCopy.setSelection(true);
		btnCopy.setBounds(155, 472, 67, 16);
		btnCopy.setText(Messages.MainWindow_btnCopy_text);

		btnMove = new Button(shell, SWT.RADIO);
		btnMove.setBounds(228, 472, 86, 16);
		btnMove.setText(Messages.MainWindow_btnRadioButton_text);

		Label lblSelectSourceDir = new Label(shell, SWT.NONE);
		lblSelectSourceDir.setForeground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_DARK_SHADOW));
		lblSelectSourceDir.setBounds(10, 10, 443, 15);
		lblSelectSourceDir.setText(Messages.MainWindow_lblStep1_text);

		lblDirPattern = new Label(shell, SWT.NONE);
		lblDirPattern.setForeground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_DARK_SHADOW));
		lblDirPattern.setBounds(10, 60, 443, 36);
		lblDirPattern.setText(Messages.MainWindow_lblStep2_text);

		lblFileExtension = new Label(shell, SWT.NONE);
		lblFileExtension.setForeground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_DARK_SHADOW));
		lblFileExtension.setBounds(10, 131, 432, 32);
		lblFileExtension.setText(Messages.MainWindow_lblStep3_text);

		lblRefresh = new Label(shell, SWT.NONE);
		lblRefresh.setForeground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_DARK_SHADOW));
		lblRefresh.setBounds(10, 198, 432, 15);
		lblRefresh.setText(Messages.MainWindow_lblStep4_text);

		lblDestinationDir = new Label(shell, SWT.NONE);
		lblDestinationDir.setForeground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_DARK_SHADOW));
		lblDestinationDir.setBounds(10, 422, 432, 15);
		lblDestinationDir.setText(Messages.MainWindow_lblStep5_text);

		lblProcess = new Label(shell, SWT.NONE);
		lblProcess.setForeground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_DARK_SHADOW));
		lblProcess.setBounds(10, 494, 432, 15);
		lblProcess.setText(Messages.MainWindow_lblStep6_text);

		lblTotalSize = new Label(shell, SWT.NONE);
		lblTotalSize.setBounds(20, 569, 67, 15);
		lblTotalSize.setText(Messages.MainWindow_lblTotalSize_text);

		lblSpeed = new Label(shell, SWT.NONE);
		lblSpeed.setBounds(177, 569, 59, 15);
		lblSpeed.setText(Messages.MainWindow_lblSpeed_text);

		lblTotalSizeVal = new Label(shell, SWT.NONE);
		lblTotalSizeVal.setBounds(93, 569, 78, 15);

		lblSpeedVal = new Label(shell, SWT.NONE);
		lblSpeedVal.setBounds(243, 569, 82, 15);

		lblEnd = new Label(shell, SWT.NONE);
		lblEnd.setBounds(331, 569, 36, 15);
		lblEnd.setText(Messages.MainWindow_lblEnd_text);

		lblEndVal = new Label(shell, SWT.NONE);
		lblEndVal.setBounds(373, 569, 69, 15);

		btnSrcSelect.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectSource();
			}
		});

		btnRefresh.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				modifySource();
			}
		});

		// State: Init
		state(DlgState.Init);
	}

	private void selectSource() {
		LOGGER.debug("btnSrcSelect klicked.");
		state(DlgState.Loading);
		DirectoryDialog dlg = new DirectoryDialog(shell);

		// Set the initial filter path according
		// to anything they've selected or typed in
		dlg.setFilterPath(txtSrcDir.getText());

		// Change the title bar text
		dlg.setText("SWT's DirectoryDialog");

		// Customizable message displayed in the dialog
		dlg.setMessage("Select a directory");

		// Calling open() will open and run the dialog.
		// It will return the selected directory, or
		// null if user cancels
		String dir = dlg.open();
		if (dir == null) {
			// Cancel was clicked
			state(DlgState.Init);
		}
		else {
			// find the source dir pattern
			final String srcDirPattern = useCase.findSrcDirPattern(dir);

			// find the final source dir
			final Path finalSrcDir = useCase.createFinalSrcDir(dir);

			try {
				// finds the matching files
				filesToProcess = useCase.findMatchingSrcDirs(finalSrcDir, srcDirPattern);

				// preview matching files
				txtFiles.setText(useCase.formatFileList(filesToProcess));

				// get the filename pattern
//				final Set<String> extensions = useCaseService.getFoundVideoExtensions(filesToProcess);
//				final String filenamePattern = useCaseService.explodeVideoExtensions(extensions);
//				cmbFilePattern.setText(filenamePattern);

				// Set the text box to the new selection
				txtSrcDir.setText(finalSrcDir.toString());

				// Set the source dir pattern
				txtSrcPattern.setText(srcDirPattern);

				// total file size
				lblTotalSizeVal.setText(useCase.formatSize(useCase.getFileSize(filesToProcess)));

				// State: SourceSelected
				state(DlgState.SourceSelected);
			}
			catch (UseCaseException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				state(DlgState.Init);
			}
		}
	}

	private void modifySource() {
		state(DlgState.Loading);
		// Reset text field
		txtFiles.setText("");
		shell.update();

		final Set<String> extensions = useCase.implodeVideoExtensions(cmbFileExtensions.getText());
		final Path finalSrcDir = Paths.get(txtSrcDir.getText());
		try {
			filesToProcess = useCase.findMatchingSrcDirs(finalSrcDir, txtSrcPattern.getText(), extensions);

			// preview matching files
			txtFiles.setText(useCase.formatFileList(filesToProcess));

			// total file size
			lblTotalSizeVal.setText(useCase.formatSize(useCase.getFileSize(filesToProcess)));
		}
		catch (UseCaseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			state(DlgState.Init);
		}

		// State: SourceSelected
		state(DlgState.SourceSelected);
	}

	private void selectDestination() {
		state(DlgState.Loading);
		LOGGER.debug("btnDstSelect klicked.");
		DirectoryDialog dlg = new DirectoryDialog(shell);

		// Set the initial filter path according
		// to anything they've selected or typed in
		dlg.setFilterPath(txtDstDir.getText());

		// Change the title bar text
		dlg.setText("SWT's DirectoryDialog");

		// Customizable message displayed in the dialog
		dlg.setMessage("Select a directory");

		// Calling open() will open and run the dialog.
		// It will return the selected directory, or
		// null if user cancels
		String dir = dlg.open();
		if (dir == null) {
			// Cancel case
			state(DlgState.SourceSelected);
		}
		else {
			// Set the text box to the new selection
			txtDstDir.setText(dir);

			// State: DestinationSelected
			state(DlgState.DestinationSelected);
		}
	}

	private void processFiles() {
		state(DlgState.Loading);
		LOGGER.debug("btnPostProcess klicked.");

		// Set progress bar
		progressBar.setEnabled(true);
		progressBar.setMinimum(0);
		progressBar.setMaximum(filesToProcess.size());
		progressBar.setSelection(0);

		// Init observer
		final ProgressObservable observable = new ProgressObservable();
		observable.addObserver(this);

		// Set proecess mode
		final PostProcessMode mode = btnMove.isEnabled() ? PostProcessMode.Move : PostProcessMode.Copy;

		// Do post processing
		final Path dstPath = Paths.get(txtDstDir.getText());
		try {
			useCase.processFiles(filesToProcess, dstPath, mode, observable);
			state(DlgState.Done);
		}
		catch (UseCaseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			state(DlgState.DestinationSelected);
		}
	}

	/**
	 * 
	 * @param sourceselected
	 */
	private void state(DlgState state) {
		switch (state) {
		case Init:
			shell.setCursor(cursorArrow);
			txtSrcDir.setEnabled(false);
			txtSrcDir.setText("");
			btnSrcSelect.setEnabled(true);
			txtSrcPattern.setEnabled(false);
			txtSrcPattern.setText("");
			cmbFileExtensions.setEnabled(false);
			btnRefresh.setEnabled(false);
			btnDstSelect.setEnabled(false);
			txtFiles.setEnabled(false);
			txtFiles.setText("");
			txtDstDir.setEnabled(false);
			txtDstDir.setText("");
			btnCopy.setEnabled(false);
			btnCopy.setEnabled(true);
			btnMove.setEnabled(false);
			btnPostProcess.setEnabled(false);
			progressBar.setEnabled(false);
			break;
		case Loading:
			shell.setCursor(cursorWait);
			txtSrcDir.setEnabled(false);
			btnSrcSelect.setEnabled(false);
			txtSrcPattern.setEnabled(false);
			cmbFileExtensions.setEnabled(false);
			btnRefresh.setEnabled(false);
			btnDstSelect.setEnabled(false);
			txtFiles.setEnabled(false);
			txtDstDir.setEnabled(false);
			btnCopy.setEnabled(false);
			btnMove.setEnabled(false);
			btnPostProcess.setEnabled(false);
			progressBar.setEnabled(false);
			break;
		case SourceSelected:
			shell.setCursor(cursorArrow);
			txtSrcDir.setEnabled(true);
			btnSrcSelect.setEnabled(true);
			txtSrcPattern.setEnabled(true);
			cmbFileExtensions.setEnabled(true);
			btnRefresh.setEnabled(true);
			btnDstSelect.setEnabled(true);
			txtFiles.setEnabled(true);
			txtDstDir.setEnabled(false);
			txtDstDir.setText("");
			btnCopy.setEnabled(false);
			btnMove.setEnabled(false);
			btnPostProcess.setEnabled(false);
			progressBar.setEnabled(false);
			break;
		case DestinationSelected:
			shell.setCursor(cursorArrow);
			txtSrcDir.setEnabled(true);
			btnSrcSelect.setEnabled(true);
			txtSrcPattern.setEnabled(true);
			cmbFileExtensions.setEnabled(true);
			btnRefresh.setEnabled(true);
			btnDstSelect.setEnabled(true);
			txtFiles.setEnabled(true);
			txtDstDir.setEnabled(true);
			btnCopy.setEnabled(true);
			btnMove.setEnabled(true);
			btnPostProcess.setEnabled(true);
			progressBar.setEnabled(false);
			break;
		case Done:
			shell.setCursor(cursorArrow);
			txtSrcDir.setEnabled(false);
			txtSrcDir.setText("");
			btnSrcSelect.setEnabled(true);
			txtSrcPattern.setEnabled(false);
			txtSrcPattern.setText("");
			cmbFileExtensions.setEnabled(false);
			btnRefresh.setEnabled(false);
			btnDstSelect.setEnabled(false);
			txtFiles.setEnabled(false);
			txtFiles.setText("");
			txtDstDir.setEnabled(false);
			txtDstDir.setText("");
			btnCopy.setEnabled(false);
			btnCopy.setEnabled(true);
			btnMove.setEnabled(false);
			btnPostProcess.setEnabled(false);
			progressBar.setEnabled(false);
			break;
		default:
			throw new UnsupportedOperationException(String.format("The state '%s' is not supported!", state));
		}
		shell.update();
	}

	/**
	 * Called if the
	 * {@link UseCase#processFiles(Set, Path, PostProcessMode, ProgressObservable)}
	 * method fires a ProgessObservable to this observer.
	 * 
	 * @param o
	 *            a {@link ProgressObservable}.
	 * @param arg
	 *            depends on the {@link Type}.
	 */
	@Override
	public void update(Observable o, Object arg) {
		if (arg == null && !(arg instanceof Progress)) {
			throw new IllegalArgumentException("The Observable must be a ProgressObservable.");
		}
		final Progress progress = (Progress) arg;
		final int totalSizeMb = (int) (progress.getTotalSize() / 1024 / 1024);
		asyncUpdateProgressBarMax(totalSizeMb);

		// Prediction
		if (totalSizeMb > 0 && progress.getDeltaSize() > 0) {
			asyncUpdateLblSpeedVal(progress.getBytesPerSecond());
			asyncUpdateLblEndVal(progress.getPredictedEndTime());
			asyncUpdateProgressBarProgress((int) progress.getDeltaSize() / 1024 / 1024);
		}
	}

	/**
	 * Invoke display thread and update progress bar max value.
	 * 
	 * @param totalSize
	 *            the new max value.
	 */
	private void asyncUpdateProgressBarMax(final int totalSize) {
		if (display.isDisposed()) {
			return;
		}
		display.asyncExec(new Runnable() {
			@Override
			public void run() {
				progressBar.setMaximum(totalSize);
				shell.update();
			}
		});
	}

	/**
	 * Invoke display thread and update progress bar.
	 * 
	 * @param deltaSize
	 *            value to add to the progress.
	 */
	private void asyncUpdateProgressBarProgress(final int deltaSize) {
		if (display.isDisposed()) {
			return;
		}
		display.asyncExec(new Runnable() {
			@Override
			public void run() {
				progressBar.setSelection(progressBar.getSelection() + deltaSize);
				shell.update();
			}
		});
	}

	/**
	 * Invoke display thread and update the speed value (byte/s).
	 * 
	 * @param bytePerSecond
	 *            the speed value.
	 */
	private void asyncUpdateLblSpeedVal(final double bytePerSecond) {
		if (display.isDisposed()) {
			return;
		}
		display.asyncExec(new Runnable() {
			@Override
			public void run() {
				lblSpeedVal.setText(useCase.formatSize((long) bytePerSecond) + "/s");
				shell.update();

			}
		});
	}

	private void asyncUpdateLblEndVal(final Calendar predictedEndTime) {
		// Invoke display thread and update progress bar.
		if (display.isDisposed()) {
			return;
		}
		display.asyncExec(new Runnable() {
			@Override
			public void run() {
				final DateFormat df = new SimpleDateFormat("HH:mm");
				lblEndVal.setText(df.format(predictedEndTime.getTime()));
				shell.update();
			}
		});
	}

	/**
	 * Updates the file extension list after a extension was entered / edited.
	 */
	private void updateExtensionList() {
		// Remove trailing comma
		if (cmbFileExtensions.getText().endsWith(",")) {
			cmbFileExtensions.setText(cmbFileExtensions.getText()
					.substring(0, cmbFileExtensions.getText().length() - 1));
		}
		if (!cmbFileExtensions.getText().isEmpty()) {
			final String[] oldExtensions = cmbFileExtensions.getItems();
			final List<String> oldExtensionsList = new ArrayList<>(Arrays.asList(oldExtensions));
			int lenItemsNew = (oldExtensions.length == EXTENSION_LIST_SIZE) ? EXTENSION_LIST_SIZE
					: oldExtensions.length + 1;
			final String[] newExtensions = new String[lenItemsNew];
			if (!oldExtensionsList.contains(cmbFileExtensions.getText())) {
				newExtensions[0] = cmbFileExtensions.getText();

				// Rotate the extensions in list. Add the first one. Forget the last one.
				for (int i = 0; i < oldExtensions.length && i < EXTENSION_LIST_SIZE - 1; ++i) {
					newExtensions[i + 1] = oldExtensions[i];
				}

				// Save the file extensions
				try {
					final Set<String[]> extHistory = useCase.saveFileExtensionHistory(newExtensions);
					cmbFileExtensions.setItems(useCase.convertFileExtensionsToString(extHistory));
					cmbFileExtensions.setText(newExtensions[0]);
				}
				catch (UseCaseException e) {
					LOGGER.error("Could not save file extionsion history: {}", e.getMessage());
				}
			}
			// Do nothing if the new file extensions already exist in the list.
		}
		else {
			LOGGER.warn("The file extensions are set empty. This will not be stored in history.");
		}
	}

}
