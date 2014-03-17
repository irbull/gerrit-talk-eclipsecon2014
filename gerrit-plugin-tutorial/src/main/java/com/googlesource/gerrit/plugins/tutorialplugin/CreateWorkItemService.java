package com.googlesource.gerrit.plugins.tutorialplugin;

import com.google.gerrit.reviewdb.client.Change;



public interface CreateWorkItemService {

  public abstract Change createGerritWorkItem( String projectName,
                                               String summary,
                                               String description );

}
