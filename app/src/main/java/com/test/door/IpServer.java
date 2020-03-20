package com.test.door;

/**
 * Create by xwr on 2020/3/16
 * Describe:
 */
public class IpServer {
  static {
    System.loadLibrary("mip");
  }

  public static native String stringFromJNI();

  public static native String roomIdGetIP(int floor, int roomId);
}
