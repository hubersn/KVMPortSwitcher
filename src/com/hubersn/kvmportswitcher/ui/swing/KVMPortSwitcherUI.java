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

package com.hubersn.kvmportswitcher.ui.swing;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyAdapter;
import org.jnativehook.keyboard.NativeKeyEvent;

import com.hubersn.kvmportswitcher.KVMPortSwitcher;

/**
 * Minimal Swing UI for KVMPortSwitcher, registers global keyboard shortcuts
 * via JNativeHook, installs a tray icon and optionally presents a window with
 * buttons for port selection.
 * 
 * There is only one clever piece of code here: to avoid endless native keycode
 * repeat, selection of the port is done asynchronously and not directly called from
 * the native event listener - see schedulePortSelect JavaDoc for more info.
 */
public class KVMPortSwitcherUI {

  private static final String APP_NAME = "KVM Port Switcher";

  private static final String PORT_BUTTON_PREFIX = "Port ";

  private static final int MAX_PORTS = 16;

  private static final int[] PORT_KEY_CODE_MAPPING = new int[] { NativeKeyEvent.VC_F1,
      NativeKeyEvent.VC_F2,
      NativeKeyEvent.VC_F3,
      NativeKeyEvent.VC_F4,
      NativeKeyEvent.VC_F5,
      NativeKeyEvent.VC_F6,
      NativeKeyEvent.VC_F7,
      NativeKeyEvent.VC_F8,
      NativeKeyEvent.VC_F9,
      NativeKeyEvent.VC_F10,
      NativeKeyEvent.VC_F11,
      NativeKeyEvent.VC_F12,
      NativeKeyEvent.VC_3,
      NativeKeyEvent.VC_4,
      NativeKeyEvent.VC_5,
      NativeKeyEvent.VC_6 };

  private static final int HIGHLIGHT_BORDER_PIXELS = 4;

  private static final Border HIGHLIGHT_BORDER = BorderFactory.createLineBorder(Color.RED, HIGHLIGHT_BORDER_PIXELS);

  private static final Border NON_HIGHLIGHT_BORDER = BorderFactory.createEmptyBorder(HIGHLIGHT_BORDER_PIXELS,
                                                                                     HIGHLIGHT_BORDER_PIXELS,
                                                                                     HIGHLIGHT_BORDER_PIXELS,
                                                                                     HIGHLIGHT_BORDER_PIXELS);

  private KVMPortSwitcher portSwitcher;

  private final JFrame mainFrame;

  private final JPanel cp;

  private final JPanel buttonPanel;

  private final ImageIcon frameIcon;

  private final ImageIcon trayIconIcon;

  private TrayIcon trayIcon;

  private PopupMenu trayIconPopupMenu;

  public KVMPortSwitcherUI() {
    this.frameIcon = new ImageIcon(KVMPortSwitcherUI.class.getResource("/kvmportswitcher_32x32.png"));
    this.trayIconIcon = new ImageIcon(KVMPortSwitcherUI.class.getResource("/kvmportswitcher_16x16.png"));
    this.portSwitcher = new KVMPortSwitcher();
    this.mainFrame = new JFrame(APP_NAME + " (c) 2021 hubersn Software");
    this.mainFrame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
    List<Image> frameImages = Arrays.asList(this.frameIcon.getImage(), this.trayIconIcon.getImage());
    this.mainFrame.setIconImages(frameImages);
    this.cp = new JPanel(new BorderLayout());
    this.mainFrame.setContentPane(this.cp);
    this.buttonPanel = new JPanel(new GridLayout(1, 0, 8, 16));
    this.buttonPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    createButtons();
    this.mainFrame.pack();
    installTrayIcon();
    registerShortcuts();
  }

  private PopupMenu getTrayIconPopupMenu() {
    if (this.trayIconPopupMenu == null) {
      this.trayIconPopupMenu = new PopupMenu(APP_NAME);
      MenuItem openItem = new MenuItem("Open");
      openItem.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          final JFrame f = KVMPortSwitcherUI.this.mainFrame;
          f.setState(Frame.NORMAL);
          show();
          f.toFront();
        }
      });
      MenuItem exitItem = new MenuItem("Exit");
      exitItem.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          exit();
        }
      });
      this.trayIconPopupMenu.add(openItem);
      this.trayIconPopupMenu.add(exitItem);
    }
    return this.trayIconPopupMenu;
  }

  private void installTrayIcon() {
    if (SystemTray.isSupported()) {
      this.trayIcon = new TrayIcon(this.trayIconIcon.getImage(), APP_NAME, getTrayIconPopupMenu());
      try {
        SystemTray.getSystemTray().add(this.trayIcon);
      } catch (final AWTException awtx) {
        // we'll lose TrayIcon functionality, but cannot do any recovery etc.
        awtx.printStackTrace();
      }
    }
  }

  private void registerShortcuts() {
    // disable log output from JNativeHook
    Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
    logger.setLevel(Level.OFF);
    logger.setUseParentHandlers(false);

    try {
      GlobalScreen.registerNativeHook();
      GlobalScreen.addNativeKeyListener(new NativeKeyAdapter() {
        @Override
        public void nativeKeyPressed(final NativeKeyEvent nativeKeyEv) {
          if ((nativeKeyEv.getModifiers() & InputEvent.CTRL_MASK) > 0) {
            for (int port = 0; port < PORT_KEY_CODE_MAPPING.length; port++) {
              if (nativeKeyEv.getKeyCode() == PORT_KEY_CODE_MAPPING[port]) {
                // actual port select must happen asynchronously!
                scheduleSelectPort(port + 1);
                // event consume not supported nativeKeyEv.consume();
              }
            }
          }
        }
      });
    } catch (final NativeHookException nhex) {
      // Not sure in which cases this would happen.
      nhex.printStackTrace();
    }
  }

  /**
   * Shows the main window.
   */
  public void show() {
    this.mainFrame.setVisible(true);
    highlightActivePortButton();
  }

  /**
   * Exit the application cleanly.
   */
  private void exit() {
    // tray icon will be removed via shutdown hook
    try {
      GlobalScreen.unregisterNativeHook();
    } catch (NativeHookException e1) {
      // ignore, we'll exit anyway
      e1.printStackTrace();
    }
    if (SystemTray.isSupported()) {
      SystemTray.getSystemTray().remove(this.trayIcon);
    }
    System.exit(0);
  }

  private void createButtons() {
    for (int i = 0; i < MAX_PORTS; i++) {
      this.buttonPanel.add(createButton(i + 1));
    }
    this.buttonPanel.validate();
    this.cp.removeAll();
    this.cp.add(this.buttonPanel, BorderLayout.NORTH);
    this.cp.validate();
  }

  private void highlightActivePortButton() {
    try {
      int activePort = this.portSwitcher.getSelectedPort();
      highlightPortButton(activePort);
    } catch (final Exception ex) {
      ex.printStackTrace();
      showError("Error detecting active port: "+ex.getMessage());
    }
  }

  private void highlightPortButton(final int portNumber) {
    for (int i = 0; i < MAX_PORTS; i++) {
      highlightButton((AbstractButton) this.buttonPanel.getComponent(i), portNumber - 1 == i);
    }
  }

  private static void highlightButton(final AbstractButton btn, final boolean highlight) {
    if (btn instanceof JToggleButton) {
      ((JToggleButton) btn).setSelected(highlight);
    } else {
      // highlight via border - but standard button border must be kept intact
      Border currentBorder = btn.getBorder();
      Border toWrap = currentBorder;
      if (currentBorder instanceof ButtonCompoundBorder) {
        CompoundBorder b = (CompoundBorder) currentBorder;
        toWrap = b.getInsideBorder();
      }
      btn.setBorder(highlight ? new ButtonCompoundBorder(HIGHLIGHT_BORDER, toWrap)
          : new ButtonCompoundBorder(NON_HIGHLIGHT_BORDER, toWrap));
    }
  }

  private AbstractButton createButton(final int portNumber) {
    //final AbstractButton btn = new JToggleButton(PORT_BUTTON_PREFIX + portNumber);
    final JButton btn = new JButton(PORT_BUTTON_PREFIX + portNumber);
    btn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doSelectPort(portNumber);
      }
    });
    btn.setFocusable(false);
    btn.setDefaultCapable(false);
    highlightButton(btn, false);
    return btn;
  }

  /**
   * Schedule selection of port number - calls doSelectPort in a separate thread
   * to allow calling code (the native key listener) to finish before taking away
   * the keyboard. Used to avoid an effect of ever-repeating key events if port
   * switch is done immediately.
   * 
   * @param portNumber port number to select.
   */
  private void scheduleSelectPort(final int portNumber) {
    // asynchronous operation allows key hook event code to continue
    Thread scheduleSelectPortThread = new Thread() {
      @Override
      public void run() {
        try {
          // experiments show that 100ms is enough
          Thread.sleep(100);
        } catch (InterruptedException e) {
          // ignore
        }
        doSelectPort(portNumber);
      }
    };
    scheduleSelectPortThread.start();
  }

  /**
   * Select the port number, first port is "1", not "0"!
   * 
   * @param portNumber port number to select.
   */
  private void doSelectPort(final int portNumber) {
    try {
      this.portSwitcher.selectPort(portNumber);
      // might be called from non-EDT (native key hook)
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          highlightPortButton(portNumber);
        }
      });
    } catch (final Exception ex) {
      showError("Error switching to port " + portNumber);
    }
  }

  private void showError(final String message) {
    JOptionPane.showMessageDialog(this.mainFrame, message, "Error", JOptionPane.ERROR_MESSAGE);
  }

  public static void main(final String[] args) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        setSystemLookAndFeel();
        final KVMPortSwitcherUI switcherUI = new KVMPortSwitcherUI();
        // only one option supported - show window initially or not
        if (args != null && args.length == 1 && args[0].equals("-show")) {
          switcherUI.show();
        }
      }
    });
  }

  //
  // Slightly intelligent Swing Look&Feel handling to avoid Motif L&F ever
  // getting active - just to protect the innocent user from sudden ugliness.
  // Also respect swing.defaultlaf system property.
  //

  /**
   * Sets the system look and feel, but not if it would be "motif" - then
   * try Nimbus if available, else generic cross platform L&F; respect
   * command line system property "swing.defaultlaf".
   */
  private static void setSystemLookAndFeel() {
    try {
      // first check if command line option is present to force L&F
      if (System.getProperty("swing.defaultlaf") != null) {
        return;
      }

      final String systemLnfName = UIManager.getSystemLookAndFeelClassName();
      if (systemLnfName == null || systemLnfName.indexOf("Motif") >= 0) {
        // either change to Nimbus, or if not available leave it alone (usually Metal)
        setNimbusOrCrossPlatformLookAndFeel();
        return;
      }
      UIManager.setLookAndFeel(systemLnfName);
    } catch (final Exception ex) {
      // silently ignore
    }
  }

  private static void setNimbusOrCrossPlatformLookAndFeel() throws UnsupportedLookAndFeelException {
    try {
      boolean lookAndFeelSet = false;
      for (final LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
        if ("Nimbus".equals(info.getName())) {
          UIManager.setLookAndFeel(info.getClassName());
          lookAndFeelSet = true;
          break;
        }
      }
      if (!lookAndFeelSet) {
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
      }
    } catch (final Exception e) {
      throw new UnsupportedLookAndFeelException("Nimbus Look&Feel not supported - probably running on JRE older than 1.6.0_10");
    }
  }

  /**
   * To uniquely identify our own highlighting compound border.
   */
  private static class ButtonCompoundBorder extends CompoundBorder {
    private static final long serialVersionUID = 1L;

    private ButtonCompoundBorder(final Border outsideBorder, final Border insideBorder) {
      super(outsideBorder, insideBorder);
    }
  }
}
