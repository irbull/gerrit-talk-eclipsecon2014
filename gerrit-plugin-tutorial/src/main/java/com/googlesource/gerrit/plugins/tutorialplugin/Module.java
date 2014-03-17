// Copyright (C) 2014 The Android Open Source Project
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

import static com.google.gerrit.server.project.ProjectResource.PROJECT_KIND;

import com.google.gerrit.common.ChangeListener;
import com.google.gerrit.extensions.annotations.Exports;
import com.google.gerrit.extensions.config.CapabilityDefinition;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.extensions.restapi.RestApiModule;
import com.google.gerrit.extensions.systemstatus.MessageOfTheDay;
import com.google.gerrit.extensions.webui.TopMenu;
import com.google.inject.AbstractModule;

class Module extends AbstractModule {

  @Override
  protected void configure() {
    DynamicSet.bind(binder(), MessageOfTheDay.class).to(EclipseConMessage.class);
    DynamicSet.bind(binder(), TopMenu.class).to(EclipseConTopLevelMenu.class);
    DynamicSet.bind(binder(), ChangeListener.class).to(MyChangeListener.class);
    bind(CreateWorkItemService.class).to(CreateWorkItemImpl.class);
    install(new RestApiModule() {
      @Override
      protected void configure() {
        post(PROJECT_KIND, "create-item").to(CreateWorkItemAction.class);
      }
    });
    bind(CapabilityDefinition.class)
    .annotatedWith(Exports.named("createWorkItem"))
    .to(CreateWorkItemCapability.class);
  }
}
