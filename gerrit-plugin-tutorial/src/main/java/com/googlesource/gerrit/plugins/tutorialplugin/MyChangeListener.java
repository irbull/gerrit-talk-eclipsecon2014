package com.googlesource.gerrit.plugins.tutorialplugin;

import com.google.gerrit.common.ChangeListener;
import com.google.gerrit.server.data.ChangeAttribute;
import com.google.gerrit.server.events.ChangeEvent;
import com.google.gerrit.server.events.ChangeMergedEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

public class MyChangeListener implements ChangeListener {

  private static final String HTTPS_BUGS_ECLIPSE_ORG_0_9 = "(.*)https?://bugs.eclipse.org/[0-9]+/?(.*)";

  @Override
  public void onChangeEvent(ChangeEvent changeEvent) {
    if ( changeEvent instanceof ChangeMergedEvent )  {
      ChangeMergedEvent mergeEvent = ((ChangeMergedEvent) changeEvent);
      ChangeAttribute change = mergeEvent.change;
      String commitMessage = change.commitMessage;
      if ( containsBugzillaReference(commitMessage)) {
        notifyBugzilla(change);
      }
    }
  }

  private static final Logger log = LoggerFactory.getLogger(MyChangeListener.class);

  private boolean containsBugzillaReference(String commitMessage) {
    Pattern p = Pattern.compile(HTTPS_BUGS_ECLIPSE_ORG_0_9,Pattern.MULTILINE | Pattern.DOTALL);
    return p.matcher(commitMessage).matches();
  }

  private void notifyBugzilla(ChangeAttribute change) {
    log.info("Change processed: " + change.id);
  }


}
