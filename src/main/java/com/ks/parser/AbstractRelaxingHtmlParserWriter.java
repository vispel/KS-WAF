package com.ks.parser;

import java.io.CharArrayWriter;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;

public abstract class AbstractRelaxingHtmlParserWriter
		extends FilterWriter
		implements RelaxingHtmlParser {
	private final boolean useTunedBlockParser;
	private final int[] LAST_FEW = new int[4];
	private final CharArrayWriter currentTag = new CharArrayWriter();

	private boolean isWithinTag = false;
	private boolean isWithinComment = false;


	protected AbstractRelaxingHtmlParserWriter(Writer delegate, boolean useTunedBlockParser) {
		super(delegate);
		this.useTunedBlockParser = useTunedBlockParser;
	}


	public final void writeToUnderlyingSink(String string)
			throws IOException {
		this.out.write(string);
	}

	public final void writeToUnderlyingSink(char[] chars, int start, int count) throws IOException {
		this.out.write(chars, start, count);
	}

	public final void writeToUnderlyingSink(int character) throws IOException {
		this.out.write(character);
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

	public void handleText(int character) throws IOException {
		writeToUnderlyingSink(character);
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
					handlePseudoTagRestart(this.currentTag.toCharArray());
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
			String tag = this.currentTag.toString().trim();
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


	private final void handleNonTagRelevantContentChunk(char[] charsWithoutAnyTagRelevantChars, int startPosInclusive, int endPosExclusive)
			throws IOException {
		int count = endPosExclusive - startPosInclusive;
		if (this.isWithinTag) {
			if (count >= 4) {
				this.LAST_FEW[0] = charsWithoutAnyTagRelevantChars[(endPosExclusive - 4)];
				this.LAST_FEW[1] = charsWithoutAnyTagRelevantChars[(endPosExclusive - 3)];
				this.LAST_FEW[2] = charsWithoutAnyTagRelevantChars[(endPosExclusive - 2)];
				this.LAST_FEW[3] = charsWithoutAnyTagRelevantChars[(endPosExclusive - 1)];
			} else if (count == 3) {
				this.LAST_FEW[0] = this.LAST_FEW[1];
				this.LAST_FEW[1] = charsWithoutAnyTagRelevantChars[(endPosExclusive - 3)];
				this.LAST_FEW[2] = charsWithoutAnyTagRelevantChars[(endPosExclusive - 2)];
				this.LAST_FEW[3] = charsWithoutAnyTagRelevantChars[(endPosExclusive - 1)];
			} else if (count == 2) {
				this.LAST_FEW[0] = this.LAST_FEW[1];
				this.LAST_FEW[1] = this.LAST_FEW[2];
				this.LAST_FEW[2] = charsWithoutAnyTagRelevantChars[(endPosExclusive - 2)];
				this.LAST_FEW[3] = charsWithoutAnyTagRelevantChars[(endPosExclusive - 1)];
			} else if (count == 1) {
				this.LAST_FEW[0] = this.LAST_FEW[1];
				this.LAST_FEW[1] = this.LAST_FEW[2];
				this.LAST_FEW[2] = this.LAST_FEW[3];
				this.LAST_FEW[3] = charsWithoutAnyTagRelevantChars[(endPosExclusive - 1)];
			}

			this.currentTag.write(charsWithoutAnyTagRelevantChars, startPosInclusive, count);
		} else {
			handleText(new String(charsWithoutAnyTagRelevantChars, startPosInclusive, count));
		}
	}

	private final void handleNonTagRelevantContentChunk(String charsWithoutAnyTagRelevantChars, int startPosInclusive, int endPosExclusive) throws IOException {
		int count = endPosExclusive - startPosInclusive;
		if (this.isWithinTag) {
			if (count >= 4) {
				this.LAST_FEW[0] = charsWithoutAnyTagRelevantChars.charAt(endPosExclusive - 4);
				this.LAST_FEW[1] = charsWithoutAnyTagRelevantChars.charAt(endPosExclusive - 3);
				this.LAST_FEW[2] = charsWithoutAnyTagRelevantChars.charAt(endPosExclusive - 2);
				this.LAST_FEW[3] = charsWithoutAnyTagRelevantChars.charAt(endPosExclusive - 1);
			} else if (count == 3) {
				this.LAST_FEW[0] = this.LAST_FEW[1];
				this.LAST_FEW[1] = charsWithoutAnyTagRelevantChars.charAt(endPosExclusive - 3);
				this.LAST_FEW[2] = charsWithoutAnyTagRelevantChars.charAt(endPosExclusive - 2);
				this.LAST_FEW[3] = charsWithoutAnyTagRelevantChars.charAt(endPosExclusive - 1);
			} else if (count == 2) {
				this.LAST_FEW[0] = this.LAST_FEW[1];
				this.LAST_FEW[1] = this.LAST_FEW[2];
				this.LAST_FEW[2] = charsWithoutAnyTagRelevantChars.charAt(endPosExclusive - 2);
				this.LAST_FEW[3] = charsWithoutAnyTagRelevantChars.charAt(endPosExclusive - 1);
			} else if (count == 1) {
				this.LAST_FEW[0] = this.LAST_FEW[1];
				this.LAST_FEW[1] = this.LAST_FEW[2];
				this.LAST_FEW[2] = this.LAST_FEW[3];
				this.LAST_FEW[3] = charsWithoutAnyTagRelevantChars.charAt(endPosExclusive - 1);
			}

			this.currentTag.write(charsWithoutAnyTagRelevantChars, startPosInclusive, count);
		} else {
			handleText(charsWithoutAnyTagRelevantChars.substring(startPosInclusive, endPosExclusive));
		}
	}


	public final void write(char[] cbuf, int off, int len)
			throws IOException {
		int end = off + len;
		if (this.useTunedBlockParser) {
			int pos = off;
			for (int i = off; i < end; i++) {
				if ((cbuf[i] == '<') || (cbuf[i] == '>') || (cbuf[i] == '-')) {
					if (i > pos) {
						handleNonTagRelevantContentChunk(cbuf, pos, i);
					}
					write(cbuf[i]);

					pos = i + 1;
				}
			}
			if (pos < end) {
				handleNonTagRelevantContentChunk(cbuf, pos, end);
			}
		} else {
			for (int i = off; i < end; i++) {
				write(cbuf[i]);
			}
		}
	}


	public final void write(String str, int off, int len)
			throws IOException {
		int end = off + len;
		if (this.useTunedBlockParser) {
			int pos = off;
			for (int i = off; i < end; i++) {
				if ((str.charAt(i) == '<') || (str.charAt(i) == '>') || (str.charAt(i) == '-')) {
					if (i > pos) {
						handleNonTagRelevantContentChunk(str, pos, i);
					}
					write(str.charAt(i));

					pos = i + 1;
				}
			}
			if (pos < end) {
				handleNonTagRelevantContentChunk(str, pos, end);
			}
		} else {
			for (int i = off; i < end; i++) {
				write(str.charAt(i));
			}
		}
	}
}


