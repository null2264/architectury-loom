package net.fabricmc.loom.util;

@Deprecated
public class FunnyTodoException extends UnsupportedOperationException {
	public FunnyTodoException(String message) {
		super("TODO: " + message);
	}

	public static void yes(String message) {
		throw new FunnyTodoException(message);
	}
}
