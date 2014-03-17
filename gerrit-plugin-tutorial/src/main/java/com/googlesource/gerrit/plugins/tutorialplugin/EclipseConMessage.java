package com.googlesource.gerrit.plugins.tutorialplugin;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.systemstatus.MessageOfTheDay;
import com.google.inject.Inject;

import org.ocpsoft.prettytime.PrettyTime;

import java.lang.management.ManagementFactory;
import java.util.Date;

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
    Date startTime = new Date(ManagementFactory.getRuntimeMXBean().getStartTime());
    String prettyPrintedTime = new PrettyTime(new Date()).format(startTime);
    result.append(pluginName + " says the server was started " + prettyPrintedTime + "<br>");
    return result.toString();
  }

  @Override
  public String getMessageId() {
    return "id";
  }

}
