package de.tyranus.poseries.usecase;

import java.io.IOException;

public class UseCaseServiceException extends Exception {
	private static final long serialVersionUID = -8285389664862057939L;

	private UseCaseServiceException(String msg) {
		super(msg);
	}

	public final static UseCaseServiceException createReadError(IOException cause) {
		return new UseCaseServiceException(String.format("Error on reading file: %s", cause.getMessage()));
	}

	public static UseCaseServiceException createCopyMoveError(IOException e, PostProcessMode mode) {
		return new UseCaseServiceException(String.format("Error during operation '%s': %s", mode, e.getMessage()));
	}

	public static UseCaseServiceException createWriteErrorProperties(IOException e) {
		return new UseCaseServiceException(String.format("Can not write local properties: %s", e.getMessage()));
	}
}
