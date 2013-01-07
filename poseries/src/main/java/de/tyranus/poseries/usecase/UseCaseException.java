package de.tyranus.poseries.usecase;

import java.io.IOException;

public class UseCaseException extends Exception {
	private static final long serialVersionUID = -8285389664862057939L;

	private UseCaseException(String msg) {
		super(msg);
	}

	public final static UseCaseException createReadError(IOException cause) {
		return new UseCaseException(String.format("Error on reading file: %s", cause.getMessage()));
	}

	public static UseCaseException createCopyMoveError(IOException e, PostProcessMode mode) {
		return new UseCaseException(String.format("Error during operation '%s': %s", mode, e.getMessage()));
	}

	public static UseCaseException createWriteErrorProperties(IOException e) {
		return new UseCaseException(String.format("Can not write local properties: %s", e.getMessage()));
	}

	public static UseCaseException createFileSizeError(IOException e) {
		return new UseCaseException(String.format("Can not get size of file: %s", e.getMessage()));		
	}
}
