/*
 * (c) hubersn Software
 * www.hubersn.com
 */

/*
This is free and unencumbered software released into the public domain.

Anyone is free to copy, modify, publish, use, compile, sell, or
distribute this software, either in source code form or as a compiled
binary, for any purpose, commercial or non-commercial, and by any
means.

In jurisdictions that recognize copyright laws, the author or authors
of this software dedicate any and all copyright interest in the
software to the public domain. We make this dedication for the benefit
of the public at large and to the detriment of our heirs and
successors. We intend this dedication to be an overt act of
relinquishment in perpetuity of all present and future rights to this
software under copyright law.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

For more information, please refer to <http://unlicense.org/>
*/

package com.hubersn.kvmportswitcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Allows control of IP-connected KVM switches which follow the TESmart way of doing things.
 */
public class KVMPortSwitcher {

  /** Default IP address of TESmart KVM switches. */
  private String kvmIpAddress = "192.168.1.10";

  /** Default IP port of TESmart KVM switches. */
  private int kvmIpPort = 5000;

  public KVMPortSwitcher(final String kvmIpAddress, final int kvmIpPort) {
    this.kvmIpAddress = kvmIpAddress;
    this.kvmIpPort = kvmIpPort;
  }

  public KVMPortSwitcher() {
    // everything set by default
  }

  public void selectPort(final int port) throws IOException {
    try (final Socket sendToServer = new Socket(InetAddress.getByName(this.kvmIpAddress), this.kvmIpPort)) {
      OutputStream os = sendToServer.getOutputStream();
      os.write(0xAA);
      os.write(0xBB);
      os.write(0x03);
      os.write(0x01);
      // real port number, e.g. 1..16 - NOT starting at 0!
      os.write(port);
      os.write(0xEE);
      sendToServer.close();
    }
  }

  public int getSelectedPort() throws IOException {
    try (final Socket sendToServer = new Socket(InetAddress.getByName(this.kvmIpAddress), this.kvmIpPort)) {
      OutputStream os = sendToServer.getOutputStream();
      InputStream is = sendToServer.getInputStream();
      os.write(0xAA);
      os.write(0xBB);
      os.write(0x03);
      os.write(0x10);
      os.write(0);
      os.write(0xEE);
      byte[] returnData = new byte[6];
      is.read(returnData);
      sendToServer.close();
      // confusingly, the active ports are numbered from 0 when getting instead of setting
      return returnData[4] + 1;
    } catch (final Exception ex) {
      ex.printStackTrace();
    }
    return -1;
  }

  public static void main(String[] args) {
    if (args == null || args.length > 1) {
      printUsage();
      System.exit(0);
    }
    try {
      if (args.length == 0) {
        printOutputHeader();
        out("Currently selected port: " + new KVMPortSwitcher().getSelectedPort());
        System.exit(0);
      }
      // parse CLI arguments
      final String arg = args[0];
      if ("-help".equalsIgnoreCase(arg) || "-?".equalsIgnoreCase(arg)) {
        printUsage();
        System.exit(0);
      } else {
        int portnumber = Integer.parseInt(arg);
        new KVMPortSwitcher().selectPort(portnumber);
      }
    } catch (final Exception ex) {
      ex.printStackTrace();
      error("Fatal error: "+ex.getMessage());
    }
  }

  private static void error(final String errorMessage) {
    System.err.println(errorMessage);
    System.exit(1);
  }

  private static void out(final String s) {
    System.out.println(s);
  }

  private static void printOutputHeader() {
    out("KVMPortSwitch 0.1.0 (C) 2021 hubersn Software");
  }

  private static void printUsage() {
    out("Usage: KVMPortSwitcher [portnumber]");
    out("Examples:");
    out("  Show currently selected port: KVMPortSwitcher");
    out("  Select port 4:                KVMPortSwitcher 4");
    out("  Produce this output:          KVMPortSwitcher -help");
    out("  Produce this output:          KVMPortSwitcher -?");
  }

}
