package com.googlesource.gerrit.plugins.tutorialplugin;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.annotations.RequiresCapability;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.reviewdb.client.Project.NameKey;
import com.google.gerrit.server.config.CanonicalWebUrl;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.sshd.CommandMetaData;
import com.google.gerrit.sshd.SshCommand;
import com.google.inject.Inject;

import org.kohsuke.args4j.Argument;

@RequiresCapability("createWorkItem")
@CommandMetaData(name = "createChange", description = "Creates a new change")
public class CreateWorkItemSSHCmd extends SshCommand {

  @Argument(usage = "projectame", index = 0)
  private String projectName = null;
  @Argument(usage = "bugnumber", index = 1)
  private String bugNumber = null;
  private String pluginName;
  private CreateWorkItemService createWorkItemService;
  private String summary;
  private String description;
  private String canonicalWebUrl;
  private ProjectCache projectCache;

  @Inject
  CreateWorkItemSSHCmd(CreateWorkItemService createWorkItemService,
      @CanonicalWebUrl final String canonicalWebUrl,
      @PluginName final String pluginName, ProjectCache projectCache) {
    this.createWorkItemService = createWorkItemService;
    this.canonicalWebUrl = canonicalWebUrl;
    this.pluginName = pluginName;
    this.projectCache = projectCache;
  }

  protected void run() {
    try {
      if (projectName == null || bugNumber == null) {
        stdout.println(getUsageMessage());
        return;
      }
      NameKey projectNameKey = getProjectNameKey();
      if (projectNameKey == null) {
        stdout.println("Cannot find project: " + projectName);
        return;
      }
      lookupBuzillaEntry(bugNumber);
      Change workItem =
          createWorkItemService.createGerritWorkItem(projectName, summary,
              description);
      stdout.println("Work item created at: " + canonicalWebUrl
          + workItem.getChangeId());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void lookupBuzillaEntry(String bugNumber) {
    this.summary = "Fix the foo subsystem";
    this.description =
        "Implement the foo subsystem using the bar implementation.\n\n"
            + "http://bugs.eclipse.org/" + bugNumber;
  }

  private String getUsageMessage() {
    return "usage ssh <server> -p <port> " + pluginName
        + " createChange <projectName> <bugNumber>";
  }

  private NameKey getProjectNameKey() {
    NameKey projectNameKey = null;
    if (projectName != null) {
      Iterable<NameKey> byName = projectCache.byName(projectName);
      projectNameKey = byName.iterator().next();
    }
    return projectNameKey;
  }

}
