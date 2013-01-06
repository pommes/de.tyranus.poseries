package de.tyranus.poseries.usecase;

import java.util.Observable;

public class ProgressObservable extends Observable {

	/**
	 * Typs of the {@link ProgressObservable}.
	 */
	public enum Type {
		DeltaSize, TotalSize
	}

	private Type type;
	private long totalSize;
	private long deltaSize;
	
	public void updateProgress(long size) {
		type = Type.DeltaSize;
		this.deltaSize = size;  
		setChanged();
		notifyObservers();
	}

	public void updateTotalSize(long size) {
		type = Type.TotalSize;
		totalSize = size;
		setChanged();
		notifyObservers();
	}
	
	public Type getType() {
		return type;
	}
	
	public long getTotalSize() {
		return totalSize;
	}
	
	public long getDeltaSize() {
		return deltaSize;
	}
}
