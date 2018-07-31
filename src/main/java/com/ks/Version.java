package com.ks;

public final class Version
{
  public static final byte VERSION_MAJOR = 0;
  public static final byte VERSION_MINOR = 0;
  public static final byte VERSION_PATCH = 1;

  public static final String versionNumber()
  {
    return VERSION_MAJOR + "." + VERSION_MINOR + "." + VERSION_PATCH;
  }


  public static final String tagLine()
  {
    return "KsWaf version " + versionNumber() + " - web application firewall";
  }

  public static final String helpLine()
  {
    return "help: kateSkacheck@gmail.com";
  }

  public static final void main(String[] args) {
    System.out.println(tagLine());
    System.out.println(helpLine());
  }
}
