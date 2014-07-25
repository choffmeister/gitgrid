package com.gitgrid.utils

import org.parboiled.common.Base64

object BinaryStringConverter {
  private lazy val base64 = Base64.rfc2045

  def hex2bytes(hex: String): Array[Byte] = {
    hex.sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte)
  }

  def bytes2hex(bytes: Array[Byte]): String = {
    bytes.map("%02x".format(_)).mkString
  }

  def base64ToBytes(str: String): Array[Byte] = {
    base64.decode(str)
  }

  def bytesToBase64(bytes: Array[Byte]): String = {
    base64.encodeToString(bytes, false)
  }

  def base64ToString(str: String): String = {
    new String(base64ToBytes(str), "UTF-8")
  }

  def stringToBase64(str: String): String = {
    base64.encodeToString(str.getBytes("UTF-8"), false)
  }
}
