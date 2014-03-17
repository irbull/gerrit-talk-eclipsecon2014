// Copyright (C) 2013 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.tutorialplugin;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.annotations.RequiresCapability;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gerrit.extensions.webui.UiAction;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.config.CanonicalWebUrl;
import com.google.gerrit.server.project.ProjectResource;
import com.google.inject.Inject;
import com.google.inject.Provider;

@RequiresCapability("createWorkItem")
class CreateWorkItemAction implements UiAction<ProjectResource>,
    RestModifyView<ProjectResource, CreateWorkItemAction.Input> {

  private Provider<CurrentUser> user;
  private String canonicalWebUrl;
  private CreateWorkItemService createWorkItemModule;

  static class Input {
    String bugnumber;
    String summary;
    String description;
  }

  @Inject
  CreateWorkItemAction(Provider<CurrentUser> user,
      @CanonicalWebUrl final String canonicalWebUrl,
      @PluginName final String pluginName,
      CreateWorkItemService createWorkItemModule) {
    this.canonicalWebUrl = canonicalWebUrl;
    this.user = user;
    this.createWorkItemModule = createWorkItemModule;
  }

  @Override
  public String apply(ProjectResource rsrc, Input input) {
    if (input.bugnumber != null) {
      lookupBuzillaEntry(input);
    }
    Change change = createWorkItemModule.createGerritWorkItem(rsrc.getName(), input.summary, input.description);
     return canonicalWebUrl + change.getChangeId();
  }

  private void lookupBuzillaEntry(Input input) {
    input.summary = "Fix the foo subsystem";
    input.description =
        "Implement the foo subsystem using the bar implementation.\n\n"
            + "http://bugs.eclipse.org/" + input.bugnumber;
  }

  @Override
  public Description getDescription(ProjectResource resource) {
    return new Description().setLabel("Create work item")
        .setTitle("Create work item on " + resource.getName())
        .setVisible(user.get() instanceof IdentifiedUser);
  }
}
