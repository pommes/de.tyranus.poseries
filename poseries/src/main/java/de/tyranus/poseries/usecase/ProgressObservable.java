package de.tyranus.poseries.usecase;

import java.util.Observable;

public class ProgressObservable extends Observable {

	public void updateProgress(Progress progress, long sizeByte) {
		progress.updateProgress(sizeByte);
		setChanged();
		notifyObservers(progress);
	}

	public void updateTotalSize(Progress progress) {
		setChanged();
		notifyObservers(progress);
	}
}
