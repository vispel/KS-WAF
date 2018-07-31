package com.ks.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public final class ZipScannerUtils {

	private ZipScannerUtils() {}

	private static final String[] EMPTY = {};

	public static final String[] extractNameAndCommentStrings(final InputStream input) throws IOException {
		File temp = null;
		try {
			temp = TempFileUtils.writeToTempFile(input);
			return extractNameAndCommentStrings(temp);
		} finally {
			if (temp != null) TempFileUtils.deleteTempFile(temp);
		}
	}
	public static final String[] extractNameAndCommentStrings(final File file) throws IOException {
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(file, ZipFile.OPEN_READ);
			final Set/*<String>*/ nameAndComments = new HashSet();
			ZipEntry entry;
			String name, comment;
			for (final Enumeration/*<ZipEntry>*/ entries = zipFile.entries(); entries.hasMoreElements();) {
				entry = (ZipEntry) entries.nextElement();
				name = entry.getName();
				comment = entry.getComment();
				if (name != null) nameAndComments.add(name);
				if (comment != null) nameAndComments.add(comment);
			}
			return nameAndComments.isEmpty() ? null : (String[]) nameAndComments.toArray( new String[0] );
		} catch (ZipException e) {
			// not a ZIP file - so no ZIP comments
			return EMPTY;
		} finally {
			if (zipFile != null) try { zipFile.close(); } catch(IOException ignored) {}
		}
	}


	public static final boolean isZipBomb(final InputStream input, final long thresholdTotalSize, final long thresholdFileCount) throws IOException {
		File temp = null;
		try {
			temp = TempFileUtils.writeToTempFile(input);
			return isZipBomb(temp, thresholdTotalSize, thresholdFileCount);
		} finally {
			if (temp != null) TempFileUtils.deleteTempFile(temp);
		}
	}
	public static final boolean isZipBomb(final File file, final long thresholdTotalSize, final long thresholdFileCount) throws IOException {
		if (thresholdTotalSize < 0) throw new IllegalArgumentException("thresholdTotalSize must not be negative");
		if (thresholdFileCount < 0) throw new IllegalArgumentException("thresholdFileCount must not be negative");
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(file, ZipFile.OPEN_READ);
			ZipEntry entry;
			long countedFiles = 0, countedTotalBytes = 0;
			for (final Enumeration/*<ZipEntry>*/ entries = zipFile.entries(); entries.hasMoreElements();) {
				if (++countedFiles > thresholdFileCount && thresholdFileCount > 0) return true;
				entry = (ZipEntry) entries.nextElement();
				final long size = entry.getSize();
				if (size > 0 && thresholdTotalSize > 0) {
					if (size > thresholdTotalSize) return true;
					countedTotalBytes += size;
					if (countedTotalBytes > thresholdTotalSize) return true;
				}
			}
			return false;
		} catch (ZipException e) {
			// not a ZIP file - so no ZIP bomb
			return false;
		} finally {
			if (zipFile != null) try { zipFile.close(); } catch(IOException ignored) {}
		}
	}


}


