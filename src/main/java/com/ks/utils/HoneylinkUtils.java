package com.ks.utils;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Random;


public final class HoneylinkUtils {
	private static final boolean MASK_AMPERSANDS = false;
	private static final String[] HEX = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F", "G", "H"};
	private static final String[] FREQUENT_LETTERS = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "R", "S", "T", "U", "W"};


	private static final int WORD_COUNT_MIN = 3;


	private static final int WORD_COUNT_MAX = 4;


	private static final List WORDS = Arrays.asList(new String[]{"Gate", "Access", "Row", "Deny", "Num", "Special", "Key", "List", "Show", "Admin", "Lock", "User", "View", "Edit", "Wizard", "Copy", "Search", "Lookup", "Config", "Sql", "Query", "Display", "Clone", "Mail", "Form", "Post", "Unify", "Send", "Shell", "Passwd", "Cmd", "Exec", "Run", "Error", "Web", "Open", "Close", "Alllow", "Group", "Dbms", "Renderer", "Flush", "Cache", "Populate", "Schedule", "Cron", "Validator", "Job", "Verify", "Library", "Soap", "Interface", "Service", "Contract", "Management", "Action", "Trigger", "Table", "Column", "Object", "Reject", "Sequence", "Flip", "Reverse", "Stream", "Media", "Network", "Ping", "Trace", "Pack", "Compress", "Package", "Static", "Dynamic", "Micro", "Kernel", "Sys", "Unix", "Linux", "Apache", "Upload", "Download", "Save", "Safe", "Current", "Script", "Business", "Bean", "Input", "Output", "Transfer", "Load", "Test", "Backup", "Previous", "Next", "Preview", "Backend", "Calculate", "Address", "Change", "Switch", "Route", "Directory", "File", "Path", "Classpath", "Boot", "Traverse", "Parse", "Content", "News", "Partner", "Direct", "Dump", "Debug", "Session", "Unit", "Source", "Ticket", "Issue", "Guide", "Guard", "Runtime", "Share", "Server", "Rotate", "Meta", "Handle", "Print", "Protocol", "Crypt", "Dictionary", "Register", "Migration", "Import", "Export", "Box", "Bundle", "Resource", "Properties", "Dependency", "Analyze", "Process", "Report", "Summary", "Webservice", "Controller", "Dispatcher", "Account"});


	private static final String[] TITLE_PARTS = {"view", "more", "debug", "test", "step", "back", "direct", "details", "open", "show", "dump", "jump", "go", "fill", "populate", "access", "load", "force"};


	private static final String[] CSS_CLASSES = {"view", "nav", "hidden", "special", "test", "mode2", "navSmall", "footer", "stressed", "sidebar"};


	private static final String[] SUFFIX_PARTS = {"", "", "", "", "", "", "", "", ".jsp", ".jsp", ".jsp", ".jsp", ".jsp", ".jsp", ".jsp", ".jspx", ".jspx", ".jspx", ".jspx", ".jsf", ".faces", ".do", ".do", ".do", ".action", ".action"};


	private static final String[] PARAMETER_PARTS_DECIMAL = {"id=", "key=", "row=", "sys=", "call=", "upt=", "obj=", "pk=", "ref=", "cur="};


	private static final String[] PARAMETER_PARTS_HEX = {"chk=", "io=", "chkp=", "tm=", "tx=", "ta=", "input="};


	private static final Random HONEY_RANDOM = new Random();


	public static final short nextTagPartCounterTarget(Random random) {
		if (random == null) random = HONEY_RANDOM;
		return (short) (61 + random.nextInt(121));
	}


	public static String generateHoneylink(Random random, String prefix, String suffix, boolean isWithinTable) {
		if (random == null) random = HONEY_RANDOM;
		StringBuilder honeylink = new StringBuilder(200);
		boolean link = random.nextInt(100) > 25;
		boolean developer = random.nextInt(100) < 7;
		honeylink.append("<!-- ");
		if (developer) {
			honeylink.append(generateDeveloper(random));
			honeylink.append(random.nextInt(10) < 8 ? " " : "\t");
			honeylink.append(generateDate(random));
			honeylink.append("\n");
		} else if (random.nextInt(100) < 10) {
			honeylink.append(random.nextInt(10) < 5 ? "hotfix \n" : "bugfix \n");
		}
		if (random.nextInt(10) > 8) honeylink.append("\n");
		if (isWithinTable) honeylink.append("<td>");
		if (random.nextInt(10) > 8) honeylink.append("\t");
		if (random.nextInt(10) > 6) honeylink.append("\n");
		honeylink.append(link ? "<a " : "<span ");
		if (random.nextInt(10) > 9) honeylink.append("\t");
		honeylink.append(link ? "href=\"" : "onclick=\"location.href='");
		if (prefix != null) honeylink.append(prefix);
		honeylink.append(generateFilename(random));
		if (suffix != null) honeylink.append(suffix);
		honeylink.append(generateParameters(random, 0, 3));
		honeylink.append(link ? "\"" : "';\"");
		if (random.nextInt(10) > 7) {
			honeylink.append(" class=\"").append(generateCssClass(random)).append("\"");
		}
		honeylink.append(">");
		honeylink.append(generateTitle(random));
		honeylink.append(link ? "</a>" : "</span>");
		if (random.nextInt(10) > 8) honeylink.append("\n");
		if (random.nextInt(10) > 8) honeylink.append("\t");
		if (isWithinTable) honeylink.append("</td>");
		if (random.nextInt(10) > 8) honeylink.append("\n");
		if (random.nextInt(10) > 8) honeylink.append("\n");
		honeylink.append(" -->");
		return honeylink.toString();
	}


	public static boolean isHoneylinkFilename(String filename) {
		if (filename == null) return false;
		filename = filename.trim();

		int pos = filename.lastIndexOf('/');
		if ((pos != -1) && (pos + 1 < filename.length())) filename = filename.substring(pos + 1);
		int length = filename.length();
		if ((length < 3) || (length > 300)) return false;
		if (!Character.isUpperCase(filename.charAt(0))) return false;
		StringBuilder word = new StringBuilder();

		for (int i = 0; i < length; i++) {
			char c = filename.charAt(i);
			if (!Character.isLetter(c)) break;
			boolean upper = Character.isUpperCase(c);
			if ((upper) && (i > 0)) {
				if (!WORDS.contains(word.toString())) return false;
				word.setLength(0);
			}
			word.append(c);
		}
		return true;
	}


	private static final String generateTitle(Random random) {
		return TITLE_PARTS[random.nextInt(TITLE_PARTS.length)];
	}

	static final String generateFilename(Random random) {
		StringBuilder word = new StringBuilder();
		int wordCount = 3 + random.nextInt(2);
		for (int i = 0; i < wordCount; i++) {
			int tries = 0;
			String part;
			do {
				part = (String) WORDS.get(random.nextInt(WORDS.size()));
			} while ((word.indexOf(part) != -1) && (tries++ < 50));
			word.append(part);
		}
		return word.toString();
	}

	private static final String generateSuffix(Random random) {
		return SUFFIX_PARTS[random.nextInt(SUFFIX_PARTS.length)];
	}

	private static final String generateCssClass(Random random) {
		return CSS_CLASSES[random.nextInt(CSS_CLASSES.length)];
	}

	private static String generateDeveloper(Random random) {
		StringBuilder developer = new StringBuilder(2);
		for (int i = 0; i < 2; i++) developer.append(FREQUENT_LETTERS[random.nextInt(FREQUENT_LETTERS.length)]);
		return random.nextInt(8) <= 5 ? developer.toString() : developer.toString().toLowerCase();
	}

	private static String generateDate(Random random) {
		SimpleDateFormat format = new SimpleDateFormat(random.nextInt(8) <= 5 ? "yyyy-MM-dd" : "yyyyMMdd");
		Calendar date = Calendar.getInstance();
		date.add(Calendar.DATE, -1 * (10 + random.nextInt(200)));

		if ((date.get(2) == 11) && (date.get(5) > 20)) {
			date.add(2, 1);
		}
		if (date.get(7) == 1) date.add(5, 1);
		return format.format(date.getTime());
	}

	static final String generateParameters(Random random, int minParameterCount, int maxParameterCount) {
		int parameterCount = random.nextInt(maxParameterCount - minParameterCount + 1) + minParameterCount;
		if (parameterCount <= 0) return "";
		StringBuilder parameters = new StringBuilder("?");

		boolean hex = random.nextInt(8) <= 5;
		parameters.append(generateParameterName(random, hex));
		parameters.append(generateParameterValue(random, hex));
		for (int i = 1; i < parameterCount; i++) {
			parameters.append("&");
			hex = random.nextInt(8) <= 5;
			parameters.append(generateParameterName(random, hex));
			parameters.append(generateParameterValue(random, hex));
		}
		return parameters.toString();
	}

	private static String generateParameterName(Random random, boolean hex) {
		if (hex) return PARAMETER_PARTS_HEX[random.nextInt(PARAMETER_PARTS_HEX.length)];
		return PARAMETER_PARTS_DECIMAL[random.nextInt(PARAMETER_PARTS_DECIMAL.length)];
	}

	private static String generateParameterValue(Random random, boolean hex) {
		if (!hex) return "" + random.nextInt(99999);
		int length = 5 + random.nextInt(10);
		StringBuilder value = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			value.append(HEX[random.nextInt(HEX.length)]);
		}
		return value.toString();
	}
}


