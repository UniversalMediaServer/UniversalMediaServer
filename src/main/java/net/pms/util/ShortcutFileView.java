package net.pms.util;

import java.io.File;
import javax.swing.filechooser.FileView;

/**
 * @see https://www.oreilly.com/library/view/swing-hacks/0596009070/chapter-64.html
 */
public class ShortcutFileView extends FileView {
  public boolean isDirLink(File f) {
    if (f.getName().toLowerCase().endsWith(".lnk")) {
      return true;
    }
    return false;
  }
  
  public Boolean isTraversable(File f) {
    if (isDirLink(f)) {
      return new Boolean(true);
    }
    return null;
  }
}