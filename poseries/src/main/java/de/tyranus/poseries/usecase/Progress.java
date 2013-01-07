package de.tyranus.poseries.usecase;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.joda.time.DateTimeUtils;

public class Progress {

	/**
	 * Typs of the {@link ProgressObservable}.
	 */
	public enum Type {
		DeltaSize, TotalSize
	}

	private Type type;
	private long startTimeMillis;
	private long totalSizeByte;
	private long deltaSizeByte;
	private long leftSizeByte;
	private long bytePerSecond;
	private long secondsLeft;
	private long currentSizeByte;
	private long lastTimeMillis;
	private Calendar predictedEndTime;
	private Object lock;
	private int updateCount;

	public Progress(long startTimeMillis, long totalSize) {
		lock = new Object();
		predictedEndTime = new GregorianCalendar();
		this.startTimeMillis = startTimeMillis;
		this.lastTimeMillis = startTimeMillis;
		this.totalSizeByte = totalSize;
		updateCount = 0;
		currentSizeByte = 0;
	}

	public Type getType() {
		synchronized (lock) {
			return type;
		}
	}

	public long getTotalSize() {
		synchronized (lock) {
			return totalSizeByte;
		}
	}

	public int getUpdateCount() {
		synchronized (lock) {
			return updateCount;
		}
	}

	public long getBytesPerSecond() {
		synchronized (lock) {
			return bytePerSecond;
		}
	}

	/**
	 * @return the current delta size in bytes.
	 */
	public long getDeltaSize() {
		synchronized (lock) {
			return deltaSizeByte;
		}
	}

	public long getCurrentSize() {
		synchronized (lock) {
			return currentSizeByte;
		}
	}

	public Calendar getPredictedEndTime() {
		synchronized (lock) {
			return predictedEndTime;
		}
	}

	public void updateTotalSize(long sizeByte) {
		synchronized (lock) {
			type = Type.TotalSize;
			totalSizeByte = sizeByte;
			totalSizeByte = sizeByte;
		}
	}

	public void updateProgress(long sizeByte) {
		synchronized (lock) {
			++updateCount;
			type = Type.DeltaSize;
			deltaSizeByte = sizeByte;
			currentSizeByte += sizeByte;

			// Calculate current time in seconds
			final double deltaSeconds = (DateTimeUtils.currentTimeMillis() - lastTimeMillis) / 1000.;
			lastTimeMillis = DateTimeUtils.currentTimeMillis();
			if (deltaSeconds > 0) {
				bytePerSecond = (long)(deltaSizeByte / deltaSeconds);
			}

			// Calculate end time
			leftSizeByte = totalSizeByte - currentSizeByte;
			secondsLeft = leftSizeByte / bytePerSecond;
			predictedEndTime.setTimeInMillis(DateTimeUtils.currentTimeMillis());
			predictedEndTime.add(Calendar.SECOND, (int) secondsLeft);
		}
	}
}
