package de.tyranus.poseries.gui;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.eclipse.swt.graphics.Image;
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
import de.tyranus.poseries.usecase.ProgressObservable;
import de.tyranus.poseries.usecase.UseCaseService;
import de.tyranus.poseries.usecase.UseCaseServiceException;

public class MainWindow implements Observer {
	private static final Logger LOGGER = LoggerFactory.getLogger(MainWindow.class);

	@Autowired
	private UseCaseService useCaseService;

	private Display display;
	private Shell shell;
	private Text txtSrcDir;
	private Text txtDstDir;
	private Button btnSrcSelect;
	private Label lblSourceDirectory;
	private Label lblDestinationDirecory;
	private Button btnDstSelect;
	private Combo cmbFilePattern;
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

	private Timestamp startTime;
	private int currentSizeMb = 0;

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
		final String[] filenamePatterns = useCaseService.convertFilePatternsToString(extHistory);

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

		cmbFilePattern = new Combo(shell, SWT.NONE);
		cmbFilePattern.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (cmbFilePattern.getSelectionIndex() >= 0) {
					cmbFilePattern.setText(cmbFilePattern.getItem(cmbFilePattern.getSelectionIndex()).toString());
				}
				else {
					LOGGER.debug("widgetSelected: No file pattern selected.");
				}
			}
		});
		cmbFilePattern.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				LOGGER.debug("focusLost");
				final int maxLen = 5;
				if (verifyFilePattern(cmbFilePattern.getText())) {
					final String[] oldPatterns = cmbFilePattern.getItems();
					final List<String> patterns = new ArrayList<>(Arrays.asList(oldPatterns));
					int lenItemsNew = (oldPatterns.length == maxLen) ? maxLen : oldPatterns.length + 1;
					final String[] newPatterns = new String[lenItemsNew];
					if (!patterns.contains(cmbFilePattern.getText())) {
						newPatterns[0] = cmbFilePattern.getText();
						for (int i = 0; i < oldPatterns.length && i < maxLen - 1; ++i) {
							newPatterns[i + 1] = oldPatterns[i];
						}

						try {
							final Set<String[]> extHistory = useCaseService.saveFilePatternHistory(newPatterns);
							cmbFilePattern.setItems(useCaseService.convertFilePatternsToString(extHistory));
							cmbFilePattern.setText(newPatterns[0]);
						}
						catch (UseCaseServiceException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
				}
				else {
					// TODO: TODO
				}
			}
		});
		cmbFilePattern.setEnabled(false);
		cmbFilePattern.setItems(filenamePatterns);
		cmbFilePattern.setText(filenamePatterns[0]);
		cmbFilePattern.setBounds(155, 166, 199, 23);

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
				postProcessFiles();
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
		lblSpeed.setBounds(181, 569, 72, 15);
		lblSpeed.setText(Messages.MainWindow_lblSpeed_text);
		
		lblTotalSizeVal = new Label(shell, SWT.NONE);
		lblTotalSizeVal.setBounds(93, 569, 82, 15);
		
		lblSpeedVal = new Label(shell, SWT.NONE);
		lblSpeedVal.setBounds(259, 569, 82, 15);

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
			final String srcDirPattern = useCaseService.findSrcDirPattern(dir);

			// find the final source dir
			final Path finalSrcDir = useCaseService.createFinalSrcDir(dir);

			try {
				// finds the matching files
				filesToProcess = useCaseService.findMatchingSrcDirs(finalSrcDir, srcDirPattern);

				// preview matching files
				txtFiles.setText(useCaseService.formatFileList(filesToProcess));

				// get the filename pattern
//				final Set<String> extensions = useCaseService.getFoundVideoExtensions(filesToProcess);
//				final String filenamePattern = useCaseService.explodeVideoExtensions(extensions);
//				cmbFilePattern.setText(filenamePattern);

				// Set the text box to the new selection
				txtSrcDir.setText(finalSrcDir.toString());

				// Set the source dir pattern
				txtSrcPattern.setText(srcDirPattern);
				
				// total file size
				lblTotalSizeVal.setText(useCaseService.formatSize(useCaseService.getFileSize(filesToProcess)));

				// State: SourceSelected
				state(DlgState.SourceSelected);
			}
			catch (UseCaseServiceException e1) {
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

		final Set<String> extensions = useCaseService.implodeVideoExtensions(cmbFilePattern.getText());
		final Path finalSrcDir = Paths.get(txtSrcDir.getText());
		try {
			filesToProcess = useCaseService.findMatchingSrcDirs(finalSrcDir, txtSrcPattern.getText(), extensions);

			// preview matching files
			txtFiles.setText(useCaseService.formatFileList(filesToProcess));
			
			// total file size
			lblTotalSizeVal.setText(useCaseService.formatSize(useCaseService.getFileSize(filesToProcess)));
		}
		catch (UseCaseServiceException e1) {
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

	private void postProcessFiles() {
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

		// Remember start time for predictions
		startTime = new Timestamp(System.currentTimeMillis());

		// Do post processing
		final Path dstPath = Paths.get(txtDstDir.getText());
		try {
			useCaseService.postProcessSeries(filesToProcess, dstPath, mode, observable);
			state(DlgState.Done);
		}
		catch (UseCaseServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			state(DlgState.DestinationSelected);
		}
	}

	private boolean filePatternCharAllowed(char c) {
		final String allowed = "[A-Za-z0-9_-]|,|" + SWT.BS + "|" + SWT.DEL;
		return String.valueOf(c).matches(allowed);
	}

	private boolean verifyFilePattern(String s) {
		return !(s.isEmpty() || s.endsWith(","));
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
			cmbFilePattern.setEnabled(false);
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
			cmbFilePattern.setEnabled(false);
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
			cmbFilePattern.setEnabled(true);
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
			cmbFilePattern.setEnabled(true);
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
			cmbFilePattern.setEnabled(false);
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
	
	@Override
	public void update(Observable o, Object arg) {
		if (!(o instanceof ProgressObservable)) {
			throw new IllegalArgumentException("The Observable must be a ProgressObservable.");
		}
		final ProgressObservable observable = (ProgressObservable) o;

		switch (observable.getType()) {
		case TotalSize:
			final int totalSizeMb = (int) (observable.getTotalSize() / 1024 / 1024);
			updateProgressBarMax(totalSizeMb);
			LOGGER.debug("Total size: {} MB.", totalSizeMb);
			break;
		case DeltaSize:
			final int deltaSizeMb = (int) (observable.getDeltaSize() / 1024 / 1024);
			currentSizeMb += deltaSizeMb;
			// Prediction
			final long deltaSeconds = (System.currentTimeMillis() - startTime.getTime()) / 1000;
			if (deltaSeconds > 0) {
				final double mbPerSecond = currentSizeMb / (double) deltaSeconds;
				if ( mbPerSecond != currentSizeMb ) {
					updateLblSpeedVal(mbPerSecond * 1024 * 1024);
					LOGGER.debug("Current processing speed: {} MB/s", mbPerSecond);
				}
			}

			updateProgressBarProgress(deltaSizeMb);
			LOGGER.debug("Delta size: {} MB.", deltaSizeMb);
			break;
		default:
			throw new UnsupportedOperationException(String.format("The type '%s' is not supported!",
					observable.getType()));
		}

	}

	private void updateProgressBarMax(final int totalSize) {
		// Invoke display thread and update progress bar.
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
	
	private void updateProgressBarProgress(final int deltaSize) {
		// Invoke display thread and update progress bar.
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
	
	private void updateLblSpeedVal(final double mbPerSecond) {
		// Invoke display thread and update progress bar.
		if (display.isDisposed()) {
			return;
		}
		
		display.asyncExec(new Runnable() {
			@Override
			public void run() {
				lblSpeedVal.setText(useCaseService.formatSize((long)mbPerSecond) + "/s");
				shell.update();
				
			}
		});
	}
}
