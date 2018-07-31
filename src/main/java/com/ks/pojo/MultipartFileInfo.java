package com.ks.pojo;

import com.ks.utils.TempFileUtils;

import java.io.*;


public final class MultipartFileInfo {
	private final boolean bufferFileUploadsToDisk;
	private String name;
	private String contentType;
	private String filename;
	private long length;
	private byte[] data = new byte[0];

	private File buffer;

	private MultipartFileInfo(String name, String contentType, String filename, boolean bufferFileUploadsToDisk) {
		this.name = name;
		this.contentType = contentType;
		this.filename = filename;
		this.bufferFileUploadsToDisk = bufferFileUploadsToDisk;
	}


	public MultipartFileInfo(String name, String contentType, String filename, InputStream in, boolean bufferFileUploadsToDisk)
			throws IOException {
		this(name, contentType, filename, bufferFileUploadsToDisk);
		if (bufferFileUploadsToDisk) {
			this.buffer = TempFileUtils.writeToTempFile(in);
			this.length = this.buffer.length();
		} else {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			int read = in.read();
			while (read >= 0) {
				out.write((byte) read);
				read = in.read();
			}
			this.data = out.toByteArray();
			this.length = this.data.length;
		}
	}

	public MultipartFileInfo(String name, String contentType, String filename, InputStream in, long length, boolean bufferFileUploadsToDisk) throws IOException {
		this(name, contentType, filename, bufferFileUploadsToDisk);
		this.length = length;
		if (bufferFileUploadsToDisk) {
			this.buffer = TempFileUtils.writeToTempFile(in, length, 0L);
		} else {
			this.data = new byte[(int) length];
			int index = 0;
			int read = in.read();
			while ((read >= 0) && (index < length)) {
				this.data[index] = ((byte) read);
				read = in.read();
				index++;
			}
		}
	}


	public String getName() {
		return this.name;
	}

	public String getContentType() {
		return this.contentType;
	}

	public String getFilename() {
		return this.filename;
	}

	public long getLength() {
		return this.length;
	}

	public InputStream getFile() throws FileNotFoundException {
		if (this.bufferFileUploadsToDisk) {
			return new BufferedInputStream(new FileInputStream(this.buffer));
		}
		return new ByteArrayInputStream(this.data);
	}


	public String toString() {
		String sb = "(" +
				"Name=" + this.name +
				" " +
				"Content-Type=" + this.contentType +
				" " +
				"Filename=" + this.filename +
				" " +
				"Length=" + getLength() +
				")";
		return sb;
	}
}


