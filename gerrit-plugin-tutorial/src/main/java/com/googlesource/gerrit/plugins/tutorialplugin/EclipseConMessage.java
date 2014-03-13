package com.googlesource.gerrit.plugins.tutorialplugin;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.systemstatus.MessageOfTheDay;
import com.google.inject.Inject;

public class EclipseConMessage extends MessageOfTheDay {

  private String pluginName;

  @Inject
  public EclipseConMessage(@PluginName String pluginName) {
    this.pluginName = pluginName;
  }

  @Override
  public String getHtmlMessage() {
    StringBuilder result = new StringBuilder();
    result.append("<H1>Welcome to EclipseCon 2014</H1>");
    result.append("Contributed by <i>" + pluginName + "</i>.");
    return result.toString();
  }

  @Override
  public String getMessageId() {
    return "id";
  }

}
