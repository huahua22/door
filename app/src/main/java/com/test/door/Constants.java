package com.test.door;

/**
 * Create by xwr on 2019/11/29
 * Describe:
 */
public class Constants {
  public final static byte[] requestVideoByte = {(byte) 0xee, 0x01, 0x00, 0x00};//请求视频
  public final static byte[] acceptVideoByte = {(byte) 0xee, 0x03, 0x00, 0x00}; //接受视频
  public final static byte[] refuseVideoByte = {(byte) 0xee, 0x07, 0x00, 0x00};//拒接视频
  public final static byte[] handUpVideoByte = {(byte) 0xee, 0x02, 0x00, 0x00};//挂断视频
  public final static byte[] endingByte = {(byte) 0xee, 0x00, (byte) 0xee, 0x00};//ending
}
