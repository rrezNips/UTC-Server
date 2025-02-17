package net.benjaminurquhart.utcserver;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.google.common.io.LittleEndianDataInputStream;

public class GMDataInputStream extends FilterInputStream implements DataInput {
	
	private LittleEndianDataInputStream stream;
	
	public GMDataInputStream(InputStream stream) {
		this(new LittleEndianDataInputStream(stream));
	}
	
	public GMDataInputStream(LittleEndianDataInputStream stream) {
		super(stream);
		this.stream = stream;
	}
	
	public LittleEndianDataInputStream getStream() {
		return stream;
	}

	@Override
	public void readFully(byte[] b) throws IOException {
		stream.readFully(b);
	}

	@Override
	public void readFully(byte[] b, int off, int len) throws IOException {
		stream.readFully(b, off, len);
	}

	@Override
	public int skipBytes(int n) throws IOException {
		return stream.skipBytes(n);
	}

	@Override
	public boolean readBoolean() throws IOException {
		return stream.readBoolean();
	}

	@Override
	public byte readByte() throws IOException {
		return stream.readByte();
	}

	@Override
	public int readUnsignedByte() throws IOException {
		return stream.readUnsignedByte();
	}

	@Override
	public short readShort() throws IOException {
		return stream.readShort();
	}

	@Override
	public int readUnsignedShort() throws IOException {
		return stream.readUnsignedShort();
	}

	@Override
	public char readChar() throws IOException {
		return stream.readChar();
	}

	@Override
	public int readInt() throws IOException {
		return stream.readInt();
	}

	@Override
	public long readLong() throws IOException {
		return stream.readLong();
	}

	@Override
	public float readFloat() throws IOException {
		return stream.readFloat();
	}

	@Override
	public double readDouble() throws IOException {
		return stream.readDouble();
	}

	@Override
	public String readLine() throws IOException {
		if(stream.available() < 1) {
			return null;
		}
		
		StringBuffer sb = new StringBuffer();
		
		byte b;
		while(stream.available() > 0) {
			b = this.readByte();
			if(b == 0 || b == '\n') break;
			sb.append((char)b);
		}
		
		return sb.toString();
	}
	
	/**
	 * Reads a UTF8-encoded <code>buffer_string</code> as read by 
	 * <code><a href="https://manual.gamemaker.io/lts/en/GameMaker_Language/GML_Reference/Buffers/buffer_read.htm">buffer_read</a></code>.
	 */
	@Override
	public String readUTF() throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		
		byte b;
		while((b = this.readByte()) != 0) {
			bytes.write(b);
		}
		
		return new String(bytes.toByteArray(), "utf-8");
	}
	
	public String readUTF(int len) throws IOException {
		byte[] bytes = new byte[len];
		this.read(bytes);
		
		return new String(bytes, "utf-8");
	}
}
