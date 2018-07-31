 package com.ks.crypto;

 import javax.crypto.SecretKey;

 public final class CryptoKeyAndSalt implements java.io.Serializable
 {
   private static final long serialVersionUID = 1L;
   private final SecretKey key;
   private byte[] saltBefore;
   private byte[] saltAfter;
   private int repeatedHashingCount;

   public CryptoKeyAndSalt(SecretKey key) {
     if (key == null) throw new IllegalArgumentException("key must not be null");
     this.key = key;
   }

   public CryptoKeyAndSalt(byte[] saltBefore, SecretKey key, byte[] saltAfter, int repeatedHashingCount) {
     this(key);
     if (saltBefore == null) throw new IllegalArgumentException("saltBefore must not be null");
     if (saltAfter == null) throw new IllegalArgumentException("saltAfter must not be null");
     if (repeatedHashingCount < 1) throw new IllegalArgumentException("repeatedHashingCount must be positive");
     this.saltBefore = saltBefore;
     this.saltAfter = saltAfter;
     this.repeatedHashingCount = repeatedHashingCount;
   }


   public boolean isExtraHashingProtection()
   {
     return (this.saltBefore != null) && (this.saltAfter != null) && (this.repeatedHashingCount > 0);
   }

   public SecretKey getKey()
   {
     return this.key;
   }

   public byte[] getSaltBefore() {
     return this.saltBefore;
   }

   public byte[] getSaltAfter() {
     return this.saltAfter;
   }

   public int getRepeatedHashingCount() {
     return this.repeatedHashingCount;
   }


   public String toString()
   {
     return "CKAS";
   }
 }

