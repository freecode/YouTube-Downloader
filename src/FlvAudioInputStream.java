import java.io.IOException;
import java.io.InputStream;

public class FlvAudioInputStream extends InputStream {
	private InputStream input;
	private boolean headRead = false;
	private boolean endOfStream = false;
	private long offset = 0L;
	private int available = 0;//audio bytes

	public FlvAudioInputStream(InputStream input) {
		this.input = input;
	}

	public long getOffset() {
		return offset;
	}

	public void close() throws IOException {
		offset = 0L;
		available = 0;
		headRead = false;
		endOfStream = false;
		input.close();
	}

	public long skip(long paramLong) throws IOException {
		if (endOfStream)
			return 0L;
		if (!headRead)
			readHead();
		long l1 = 0L;
		while (l1 < paramLong && !endOfStream) {
			if (available <= 0) available = readTag();
			long l2 = paramLong - l1;
			long l3 = input.skip(l2 > available ? available : l2);
			l1 += l3;
			available = (int) (available - l3);
		}
		return l1;
	}

	public int read() throws IOException {
		byte[] bytes = new byte[1];
		readAudio(bytes, 0, 1);
		return bytes[0];
	}

	public int read(byte[] buffer) throws IOException {
		return read(buffer, 0, buffer.length);
	}

	public int read(byte[] buffer, int offset, int len) throws IOException {
		return readAudio(buffer, offset, len);
	}

	private void readHead() throws IOException {
		long l1 = readUInt32();
		if (l1 != 1179407873L)
			throw new IOException("Not an FLV file.");
		long l2 = readUInt8();
		long l3 = readUInt32();
		headRead = true;
		_skip(l3 - offset);
	}

	private int readAudio(byte[] buffer, int offset, int len) throws IOException {
		if (endOfStream)
			return -1;
		if (!headRead)
			readHead();
		int i = 0;
		while (i < len && !endOfStream) {
			if (available <= 0) {
				available = readTag();
			} else {
				int l = len - i;
				int read = _read(buffer, offset + i, l > available ? available : l);
				if (read < 0)
					break;
				i += read;
				available -= read;
			}
		}
		return i;
	}

	private int readTag() throws IOException {
		if (endOfStream)
			return 0;
		long l1 = readUInt32();
		long l2 = readUInt8();
		int i = (int) readUInt24();
		long l3 = readUInt24();
		l3 |= readUInt8() << 24;
		long l4 = readUInt24();
		if (i == 0)
			return 0;
		long l5 = readUInt8();
		i--;
		if (l2 == 8L) return i;
		if (l2 == 9L) {
			_skip(i);
			return 0;
		}

		_skip(i);
		return 0;
	}

	private long _skip(long len) throws IOException {
		long l1 = 0L;
		while (l1 < len) {
			long l2 = input.skip(len - l1);
			l1 += l2;
			if (l2 <= 0L) break;
		}
		offset += l1;
		return l1;
	}

	private int _read() throws IOException {
		int i = input.read();
		if (i < 0) {
			endOfStream = true;
			return -1;
		}
		offset += 1L;
		return i;
	}

	private int _read(byte[] buffer) throws IOException {
		return _read(buffer, 0, buffer.length);
	}

	private int _read(byte[] buffer, int offset, int len) throws IOException {
		int i = 0;
		while (i < len) {
			int l = len - i;
			int k = input.read(buffer, offset + i, l);
			if (k < 0) {
				endOfStream = true;
				if (i != 0)
					break;
				return -1;
			}
			i += k;
		}

		this.offset += i;
		return i;
	}

	private long readUInt8() throws IOException {
		return _read();
	}

	private long readUInt24() throws IOException {
		byte[] bytes = new byte[4];
		int i = _read(bytes, 1, 3);
		if (i < 3)
			return 0L;
		return toUInt32(bytes);
	}

	private long readUInt32() throws IOException {
		byte[] arrayOfByte = new byte[4];
		int i = _read(arrayOfByte, 0, 4);
		if (i < 4) return 0L;
		return toUInt32(arrayOfByte);
	}

	private long toUInt32(byte[] bytes) {
		long l = (0xFF & bytes[0]) << 24;
		l |= (0xFF & bytes[1]) << 16;
		l |= (0xFF & bytes[2]) << 8;
		l |= 0xFF & bytes[3];
		return l;
	}
}