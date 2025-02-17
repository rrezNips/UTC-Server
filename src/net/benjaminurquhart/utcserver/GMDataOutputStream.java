package net.benjaminurquhart.utcserver;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.google.common.io.LittleEndianDataOutputStream;

public class GMDataOutputStream extends FilterOutputStream implements DataOutput {
	
	private LittleEndianDataOutputStream stream;
	private ByteArrayOutputStream byteStream;
	
	
	public GMDataOutputStream(BufferedOutputStream stream) {
		this(new LittleEndianDataOutputStream(stream));
	}
	
	public GMDataOutputStream(ByteArrayOutputStream stream) {
		this(new LittleEndianDataOutputStream(stream));
		this.byteStream = stream;
	}
	
	public GMDataOutputStream(OutputStream stream) {
		this(new BufferedOutputStream(stream, 1<<16));
	}
	
	public GMDataOutputStream(LittleEndianDataOutputStream stream) {
		super(stream);
		this.stream = stream;
	}
	
	public LittleEndianDataOutputStream getStream() {
		return stream;
	}
	
	protected ByteArrayOutputStream getByteStream() {
		return byteStream;
	}

	@Override
	public void write(int b) throws IOException {
		stream.write(b);
	}

	@Override
	public void write(byte[] b) throws IOException {
		stream.write(b);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		stream.write(b, off, len);
	}

	@Override
	public void writeBoolean(boolean v) throws IOException {
		stream.writeBoolean(v);
	}

	@Override
	public void writeByte(int v) throws IOException {
		stream.writeByte(v);
	}

	@Override
	public void writeShort(int v) throws IOException {
		stream.writeShort(v);
	}

	@Override
	public void writeChar(int v) throws IOException {
		stream.writeChar(v);
	}

	@Override
	public void writeInt(int v) throws IOException {
		stream.writeInt(v);
	}

	@Override
	public void writeLong(long v) throws IOException {
		stream.writeLong(v);
	}

	@Override
	public void writeFloat(float v) throws IOException {
		stream.writeFloat(v);
	}

	@Override
	public void writeDouble(double v) throws IOException {
		stream.writeDouble(v);
	}

	@Override
	@Deprecated
	public void writeBytes(String s) throws IOException {
		stream.writeBytes(s);
	}

	@Override
	public void writeChars(String s) throws IOException {
		stream.writeChars(s);
	}

	/**
	 * Writes a UTF8-encoded <code>buffer_string</code> as written by 
	 * <code><a href="https://manual.gamemaker.io/lts/en/GameMaker_Language/GML_Reference/Buffers/buffer_write.htm">buffer_write</a></code>.
	 */
	@Override
	public void writeUTF(String s) throws IOException {
		byte[] bytes = new byte[s.length() + 1];
		System.arraycopy(s.getBytes("utf-8"), 0, bytes, 0, s.length());
		this.write(bytes);
	}

}
