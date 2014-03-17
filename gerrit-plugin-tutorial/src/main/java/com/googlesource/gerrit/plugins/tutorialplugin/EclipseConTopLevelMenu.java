package com.googlesource.gerrit.plugins.tutorialplugin;

import com.google.common.collect.Lists;
import com.google.gerrit.extensions.webui.TopMenu;

import java.util.List;

public class EclipseConTopLevelMenu implements TopMenu {

  @Override
  public List<MenuEntry> getEntries() {
    return Lists.newArrayList(
        new MenuEntry("EclipseCon", Lists.newArrayList(
               new MenuItem("EclipseCon 2014", "http://www.eclipsecon.org/na2014/"),
               new MenuItem("Create work item", "/#/x/tutorialPlugin/"))));
  }

}
