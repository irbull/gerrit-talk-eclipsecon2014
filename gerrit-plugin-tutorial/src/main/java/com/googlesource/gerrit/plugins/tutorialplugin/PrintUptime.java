package com.googlesource.gerrit.plugins.tutorialplugin;

import com.google.gerrit.sshd.CommandMetaData;
import com.google.gerrit.sshd.SshCommand;
import com.google.inject.Inject;

import org.ocpsoft.prettytime.PrettyTime;

import java.lang.management.ManagementFactory;
import java.util.Date;


@CommandMetaData(name = "uptime", description = "Print server uptime")
public class PrintUptime extends SshCommand {
  @Inject
  PrintUptime() {
  }

  @Override
  protected void run() {
    Date startTime =
        new Date(ManagementFactory.getRuntimeMXBean().getStartTime());
    String prettyPrintedTime = new PrettyTime(new Date()).format(startTime);
    stdout.println("The Gerrit server started " + prettyPrintedTime);
  }
}
