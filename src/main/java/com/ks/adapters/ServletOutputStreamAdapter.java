package com.ks.adapters;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import java.io.IOException;
import java.io.OutputStream;

public final class ServletOutputStreamAdapter extends ServletOutputStream {
	private final OutputStream sink;

	public ServletOutputStreamAdapter(OutputStream delegate) {
		if (delegate == null) throw new IllegalArgumentException("delegate must not be null");
		this.sink = delegate;
	}

	public void write(int aByte)
			throws IOException {
		this.sink.write(aByte);
	}

	public void close()
			throws IOException {
		this.sink.close();
	}

	public void flush() throws IOException {
		this.sink.flush();
	}

	@Override
	public boolean isReady() {
		return false;
	}

	@Override
	public void setWriteListener(WriteListener writeListener) {

	}
}


