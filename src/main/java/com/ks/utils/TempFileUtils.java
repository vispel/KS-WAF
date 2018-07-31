package com.ks.utils;

import com.ks.exceptions.ServerAttackException;

import java.io.*;


public final class TempFileUtils {
	public static void pipeStreams(InputStream in, OutputStream out, ByteArrayOutputStream sink)
			throws IOException {
		pipeStreams(in, out, sink, 0L, 0L);
	}

	public static void pipeStreams(InputStream in, OutputStream out, ByteArrayOutputStream sink, long lengthToUseFromStream, long maxInputStreamLength) throws IOException {
		long totalByteCount = 1L;
		int bite = in.read();
		while (bite >= 0) {
			out.write(bite);
			bite = in.read();

			if ((maxInputStreamLength > 0L) && (sink.size() > maxInputStreamLength))
				throw new ServerAttackException("maximum stream size (DoS protection) threshold exceeded");
			if (lengthToUseFromStream > 0L) {
				totalByteCount += 1L;
				if (totalByteCount > lengthToUseFromStream) {
					break;
				}
			}
		}
	}

	public static final File writeToTempFile(InputStream input)
			throws IOException {
		return writeToTempFile(input, 0L, 0L);
	}


	public static final File writeToTempFile(InputStream input, long lengthToUseFromStream, long maxInputStreamLength)
			throws IOException {
		OutputStream output = null;
		try {
			File temp = File.createTempFile("tmp", null, ParamConsts.TEMP_DIRECTORY);
			output = new BufferedOutputStream(new FileOutputStream(temp));
			byte[] buffer = new byte['䀀'];

			long bytesWritten = 0L;
			int bytesRead;
			int bytesToWriteLastChunk;
			while ((bytesRead = input.read(buffer)) != -1) {
				if (bytesRead > 0) {
					if ((lengthToUseFromStream > 0L) &&
							(bytesWritten + bytesRead > lengthToUseFromStream)) {
						bytesToWriteLastChunk = (int) (lengthToUseFromStream - bytesWritten);
						if (bytesToWriteLastChunk > 0) {
							output.write(buffer, 0, bytesToWriteLastChunk);
						}
					} else {
						output.write(buffer, 0, bytesRead);

						if ((lengthToUseFromStream > 0L) || (maxInputStreamLength > 0L)) {
							bytesWritten += bytesRead;
							if ((maxInputStreamLength > 0L) && (bytesWritten > maxInputStreamLength))
								throw new ServerAttackException("maximum stream size (DoS protection) threshold exceeded");
						}
					}
				}
			}
			return temp;
		} finally {
			if (output != null) try {
				output.close();
			} catch (IOException ignored) {
			}
		}
	}

	public static final boolean deleteTempFile(File temp) {
		if (temp != null)
			try {
				return !temp.delete();
			} catch (RuntimeException e) {
				return false;
			}
		return false;
	}
}


