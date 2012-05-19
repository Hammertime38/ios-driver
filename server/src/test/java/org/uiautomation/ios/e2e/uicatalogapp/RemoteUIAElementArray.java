package org.uiautomation.ios.e2e.uicatalogapp;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.uiautomation.ios.UIAModels.UIAElement;
import org.uiautomation.ios.UIAModels.UIAElementArray;
import org.uiautomation.ios.UIAModels.UIATableCell;
import org.uiautomation.ios.UIAModels.UIATableView;
import org.uiautomation.ios.UIAModels.predicate.ClassCriteria;
import org.uiautomation.ios.UIAModels.predicate.NameCriteria;
import org.uiautomation.ios.client.uiamodels.impl.RemoteUIADriver;
import org.uiautomation.ios.client.uiamodels.impl.RemoteUIAWindow;

public class RemoteUIAElementArray extends RemoteTestsBase {


  @Test
  public void findElementOnMyArray() {
    RemoteUIADriver driver = null;
    try {
      String name = "Buttons, Various uses of UIButton";
      driver = getDriver();
      RemoteUIAWindow win = getMainWindow(driver);
      UIAElementArray<UIAElement> cells = win.findElements(new ClassCriteria(UIATableCell.class));
      UIATableCell cell = (UIATableCell) cells.getFirst(new NameCriteria(name));
      Assert.assertEquals(cell.getName(), name);

    } finally {
      if (driver != null) {
        driver.quit();
      }
    }
  }


  @Test
  public void findElementOnNativeArray() {
    RemoteUIADriver driver = null;
    try {
      String name = "Buttons, Various uses of UIButton";
      driver = getDriver();
      RemoteUIAWindow win = getMainWindow(driver);
      UIAElementArray<UIAElement> elements =
          win.findElements(new ClassCriteria(UIATableView.class));

      UIATableView tableView = (UIATableView) elements.get(0);
      UIAElementArray<UIATableCell> cells = tableView.getCells();

      UIATableCell cell = cells.getFirst(new NameCriteria(name));
      Assert.assertEquals(cell.getName(), name);

    } finally {
      if (driver != null) {
        driver.quit();
      }
    }
  }
}