package com.ks.parser;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public abstract class AbstractRelaxingHtmlParserStream
		extends FilterOutputStream
		implements RelaxingHtmlParser {
	private final boolean useTunedBlockParser;
	private final int[] LAST_FEW = new int[4];
	private final ByteArrayOutputStream currentTag = new ByteArrayOutputStream();

	protected final String encoding;
	private boolean isWithinTag = false;
	private boolean isWithinComment = false;


	protected AbstractRelaxingHtmlParserStream(OutputStream delegate, String encoding, boolean useTunedBlockParser) {
		super(delegate);
		if (encoding == null) throw new IllegalArgumentException("encoding must not be null");
		this.encoding = encoding;
		this.useTunedBlockParser = useTunedBlockParser;
	}


	public final void writeToUnderlyingSink(String string)
			throws IOException {
		this.out.write(string.getBytes(this.encoding));
	}

	public final void writeToUnderlyingSink(char[] chars, int start, int count) throws IOException {
		String value = new String(chars, start, count);
		writeToUnderlyingSink(value);
	}

	public final void writeToUnderlyingSink(int b) throws IOException {
		this.out.write(b);
	}


	public void handleTag(String tag)
			throws IOException {
		writeToUnderlyingSink(tag);
	}

	public void handleTagClose(String tag) throws IOException {
		writeToUnderlyingSink(tag);
	}

	public void handlePseudoTagRestart(char[] stuff) throws IOException {
		writeToUnderlyingSink(stuff, 0, stuff.length);
	}

	public void handleText(int b) throws IOException {
		writeToUnderlyingSink(b);
	}

	public void handleText(String text) throws IOException {
		writeToUnderlyingSink(text);
	}


	public final void write(int original)
			throws IOException {
		boolean tagEnd = false;
		boolean writeChar = false;
		if (!this.isWithinComment) {
			if (original == 60) {
				if (this.currentTag.size() > 0) {
					handlePseudoTagRestart(this.currentTag.toString(this.encoding).toCharArray());
				}

				this.currentTag.reset();
				this.LAST_FEW[0] = 0;
				this.LAST_FEW[1] = 0;
				this.LAST_FEW[2] = 0;
				this.LAST_FEW[3] = 0;

				this.isWithinTag = true;
			} else if (original == 62) {
				tagEnd = true;
			}
		}
		if (this.isWithinTag) {
			this.LAST_FEW[0] = this.LAST_FEW[1];
			this.LAST_FEW[1] = this.LAST_FEW[2];
			this.LAST_FEW[2] = this.LAST_FEW[3];
			this.LAST_FEW[3] = original;

			this.currentTag.write(original);
			if (this.isWithinComment) {
				if (original == 62) {

					if ((this.LAST_FEW[1] == 45) && (this.LAST_FEW[2] == 45) && (this.LAST_FEW[3] == 62)) {
						this.isWithinComment = false;
						this.isWithinTag = false;

						tagEnd = true;
					}
				}
			} else if ((this.LAST_FEW[1] == 33) && (this.LAST_FEW[2] == 45) && (this.LAST_FEW[3] == 45) && (this.LAST_FEW[0] == 60))
				this.isWithinComment = true;
		} else {
			writeChar = true;
		}
		if (tagEnd) {
			String tag = this.currentTag.toString(this.encoding).trim();
			boolean closingTag = (tag.length() > 1) && (tag.charAt(1) == '/');
			if (closingTag) {
				handleTagClose(tag);
			} else if (tag.length() > 0) handleTag(tag);
			this.isWithinTag = false;

			this.currentTag.reset();
			this.LAST_FEW[0] = 0;
			this.LAST_FEW[1] = 0;
			this.LAST_FEW[2] = 0;
			this.LAST_FEW[3] = 0;
		}
		if (writeChar) {
			handleText(original);
		}
	}

	private void handleNonTagRelevantContentChunk(byte[] bytesWithoutAnyTagRelevantChars, int startPosInclusive, int endPosExclusive)
			throws IOException {
		int count = endPosExclusive - startPosInclusive;
		if (this.isWithinTag) {
			if (count >= 4) {
				this.LAST_FEW[0] = bytesWithoutAnyTagRelevantChars[(endPosExclusive - 4)];
				this.LAST_FEW[1] = bytesWithoutAnyTagRelevantChars[(endPosExclusive - 3)];
				this.LAST_FEW[2] = bytesWithoutAnyTagRelevantChars[(endPosExclusive - 2)];
				this.LAST_FEW[3] = bytesWithoutAnyTagRelevantChars[(endPosExclusive - 1)];
			} else if (count == 3) {
				this.LAST_FEW[0] = this.LAST_FEW[1];
				this.LAST_FEW[1] = bytesWithoutAnyTagRelevantChars[(endPosExclusive - 3)];
				this.LAST_FEW[2] = bytesWithoutAnyTagRelevantChars[(endPosExclusive - 2)];
				this.LAST_FEW[3] = bytesWithoutAnyTagRelevantChars[(endPosExclusive - 1)];
			} else if (count == 2) {
				this.LAST_FEW[0] = this.LAST_FEW[1];
				this.LAST_FEW[1] = this.LAST_FEW[2];
				this.LAST_FEW[2] = bytesWithoutAnyTagRelevantChars[(endPosExclusive - 2)];
				this.LAST_FEW[3] = bytesWithoutAnyTagRelevantChars[(endPosExclusive - 1)];
			} else if (count == 1) {
				this.LAST_FEW[0] = this.LAST_FEW[1];
				this.LAST_FEW[1] = this.LAST_FEW[2];
				this.LAST_FEW[2] = this.LAST_FEW[3];
				this.LAST_FEW[3] = bytesWithoutAnyTagRelevantChars[(endPosExclusive - 1)];
			}

			this.currentTag.write(bytesWithoutAnyTagRelevantChars, startPosInclusive, count);
		} else {
			handleText(new String(bytesWithoutAnyTagRelevantChars, startPosInclusive, count, this.encoding));
		}
	}

	public final void write(byte[] bbuf, int off, int len)
			throws IOException {
		int end = off + len;
		if (this.useTunedBlockParser) {
			int pos = off;
			for (int i = off; i < end; i++) {
				if ((bbuf[i] == 60) || (bbuf[i] == 62) || (bbuf[i] == 45)) {
					if (i > pos) {
						handleNonTagRelevantContentChunk(bbuf, pos, i);
					}
					write(bbuf[i]);

					pos = i + 1;
				}
			}
			if (pos < end) {
				handleNonTagRelevantContentChunk(bbuf, pos, end);
			}
		} else {
			for (int i = off; i < end; i++) {
				write(bbuf[i]);
			}
		}
	}

	public final void write(byte[] bbuf)
			throws IOException {
		write(bbuf, 0, bbuf.length);
	}
}


