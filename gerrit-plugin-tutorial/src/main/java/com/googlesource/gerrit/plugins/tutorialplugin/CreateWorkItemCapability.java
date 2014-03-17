package com.googlesource.gerrit.plugins.tutorialplugin;

import com.google.gerrit.extensions.config.CapabilityDefinition;

public class CreateWorkItemCapability extends CapabilityDefinition {
  @Override
  public String getDescription() {
    return "Can create a work item in gerrit";
  }
}