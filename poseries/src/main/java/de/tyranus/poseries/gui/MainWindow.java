package de.tyranus.poseries.gui;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
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
	private Button btnPostProcess;
	private ProgressBar progressBar;

	private Set<Path> filesToProcess;
	private Set<String[]> extHistory;

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
		shell = new Shell(SWT.SHELL_TRIM & (~SWT.RESIZE) & (~SWT.MAX));
		shell.setSize(417, 464);
		shell.setText(Messages.MainWindow_shell_text);
		final String[] filenamePatterns = useCaseService.convertFilePatternsToString(extHistory);
		
				lblSourceDirectory = new Label(shell, SWT.NONE);
				lblSourceDirectory.setToolTipText(Messages.MainWindow_lblSourceDirectory_toolTipText);
				lblSourceDirectory.setAlignment(SWT.RIGHT);
				lblSourceDirectory.setBounds(10, 15, 129, 15);
				lblSourceDirectory.setText(Messages.MainWindow_lblSourceDirectory_text);

		txtSrcDir = new Text(shell, SWT.BORDER);
		txtSrcDir.addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseHover(MouseEvent e) {
				txtSrcDir.setToolTipText(txtSrcDir.getText());
			}
		});
		txtSrcDir.setBounds(145, 12, 159, 21);

		btnSrcSelect = new Button(shell, SWT.NONE);
		btnSrcSelect.setBounds(310, 10, 82, 25);
		btnSrcSelect.setText(Messages.MainWindow_btnSrcSelect_text);
				
						lblSourceDirectoryPattern = new Label(shell, SWT.NONE);
						lblSourceDirectoryPattern.setAlignment(SWT.RIGHT);
						lblSourceDirectoryPattern.setBounds(10, 42, 129, 15);
						lblSourceDirectoryPattern.setText(Messages.MainWindow_lblSourceDirectoryPattern_text);
		
				txtSrcPattern = new Text(shell, SWT.BORDER);
				txtSrcPattern.setEnabled(false);
				txtSrcPattern.setBounds(145, 39, 159, 23);
		
				lblFileExtensions = new Label(shell, SWT.NONE);
				lblFileExtensions.setToolTipText(Messages.MainWindow_lblFileExtensions_toolTipText);
				lblFileExtensions.setAlignment(SWT.RIGHT);
				lblFileExtensions.setBounds(10, 71, 129, 15);
				lblFileExtensions.setText(Messages.MainWindow_lblFileExtensions_text);
		
				cmbFilePattern = new Combo(shell, SWT.NONE);
				cmbFilePattern.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						if ( cmbFilePattern.getSelectionIndex() >= 0 ) {
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
								for (int i = 0; i < oldPatterns.length && i<maxLen-1; ++i) {
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
				cmbFilePattern.setBounds(145, 68, 159, 23);

		btnRefresh = new Button(shell, SWT.NONE);
		btnRefresh.setEnabled(false);
		btnRefresh.setBounds(10, 100, 382, 25);
		btnRefresh.setText(Messages.MainWindow_btnRefresh_text);

		txtFiles = new StyledText(shell, SWT.BORDER | SWT.V_SCROLL);
		txtFiles.setFont(SWTResourceManager.getFont("Segoe UI", 10, SWT.NORMAL));
		txtFiles.setDoubleClickEnabled(false);
		txtFiles.setEditable(false);
		txtFiles.setBounds(10, 131, 382, 205);
		
				lblDestinationDirecory = new Label(shell, SWT.NONE);
				lblDestinationDirecory.setAlignment(SWT.RIGHT);
				lblDestinationDirecory.setBounds(10, 347, 129, 15);
				lblDestinationDirecory.setText(Messages.MainWindow_lblDestinationDirecory_text);
		
				txtDstDir = new Text(shell, SWT.BORDER);
				txtDstDir.setEnabled(false);
				txtDstDir.addMouseTrackListener(new MouseTrackAdapter() {
					@Override
					public void mouseHover(MouseEvent e) {
						txtDstDir.setToolTipText(txtDstDir.getText());
					}
				});
				txtDstDir.setBounds(145, 344, 159, 21);
		
				btnDstSelect = new Button(shell, SWT.NONE);
				btnDstSelect.setEnabled(false);
				btnDstSelect.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						selectDestination();
					}
				});
				btnDstSelect.setBounds(310, 342, 82, 25);
				btnDstSelect.setText(Messages.MainWindow_btnDstSelect_text);

		btnPostProcess = new Button(shell, SWT.NONE);
		btnPostProcess.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				postProcessFiles();
			}
		});
		btnPostProcess.setEnabled(false);
		btnPostProcess.setBounds(10, 371, 382, 25);
		btnPostProcess.setText(Messages.MainWindow_btnPostProcess_text);

		progressBar = new ProgressBar(shell, SWT.NONE);
		progressBar.setBounds(10, 402, 382, 17);

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
		// Reset text field
		txtFiles.setText("");
		shell.update();

		final Set<String> extensions = useCaseService.implodeVideoExtensions(cmbFilePattern.getText());
		final Path finalSrcDir = Paths.get(txtSrcDir.getText());
		try {
			filesToProcess = useCaseService.findMatchingSrcDirs(finalSrcDir, txtSrcPattern.getText(), extensions);

			// preview matching files
			txtFiles.setText(useCaseService.formatFileList(filesToProcess));
		}
		catch (UseCaseServiceException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
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

		final Path dstPath = Paths.get(txtDstDir.getText());
		try {
			useCaseService.postProcessSeries(filesToProcess, dstPath, PostProcessMode.Copy, observable);
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
			btnPostProcess.setEnabled(false);
			progressBar.setEnabled(false);
			break;
		case Loading:
			txtSrcDir.setEnabled(false);
			btnSrcSelect.setEnabled(false);
			txtSrcPattern.setEnabled(false);
			cmbFilePattern.setEnabled(false);
			btnRefresh.setEnabled(false);
			btnDstSelect.setEnabled(false);
			txtFiles.setEnabled(false);
			txtDstDir.setEnabled(false);
			btnPostProcess.setEnabled(false);
			progressBar.setEnabled(false);
			break;
		case SourceSelected:
			txtSrcDir.setEnabled(true);
			btnSrcSelect.setEnabled(true);
			txtSrcPattern.setEnabled(true);
			cmbFilePattern.setEnabled(true);
			btnRefresh.setEnabled(true);
			btnDstSelect.setEnabled(true);
			txtFiles.setEnabled(true);
			txtDstDir.setEnabled(false);
			txtDstDir.setText("");
			btnPostProcess.setEnabled(false);
			progressBar.setEnabled(false);
			break;
		case DestinationSelected:
			txtSrcDir.setEnabled(true);
			btnSrcSelect.setEnabled(true);
			txtSrcPattern.setEnabled(true);
			cmbFilePattern.setEnabled(true);
			btnRefresh.setEnabled(true);
			btnDstSelect.setEnabled(true);
			txtFiles.setEnabled(true);
			txtDstDir.setEnabled(true);
			btnPostProcess.setEnabled(true);
			progressBar.setEnabled(false);
			break;
		case Done:
			txtSrcDir.setEnabled(true);
			btnSrcSelect.setEnabled(true);
			txtSrcPattern.setEnabled(true);
			cmbFilePattern.setEnabled(true);
			btnRefresh.setEnabled(true);
			btnDstSelect.setEnabled(true);
			txtFiles.setEnabled(true);
			txtDstDir.setEnabled(true);
			btnPostProcess.setEnabled(true);
			progressBar.setEnabled(false);
			break;
		default:
			throw new IllegalAccessError(String.format("The state '%s' is not supported!", state));
		}
		shell.update();
	}

	@Override
	public void update(Observable o, Object arg) {
		// Invoke display thread and update progress bar.
		if (display.isDisposed()) {
			return;
		}
		display.asyncExec(new Runnable() {
			@Override
			public void run() {
				progressBar.setSelection(progressBar.getSelection() + 1);
				shell.update();
			}
		});

	}
}
