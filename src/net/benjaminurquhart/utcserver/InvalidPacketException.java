package net.benjaminurquhart.utcserver;

public class InvalidPacketException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8394501891012615686L;
	
	public static void throwFormatted(String formatStr, Object... args) {
		throw new InvalidPacketException(String.format(formatStr, args));
	}
	
	public static void throwFormatted(Throwable cause, String formatStr, Object... args) {
		throw new InvalidPacketException(String.format(formatStr, args));
	}
	
	public InvalidPacketException(String msg) {
		super(msg);
	}
	
	public InvalidPacketException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
