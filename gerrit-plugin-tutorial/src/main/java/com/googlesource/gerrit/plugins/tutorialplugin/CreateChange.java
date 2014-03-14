package com.googlesource.gerrit.plugins.tutorialplugin;

import com.google.gerrit.reviewdb.client.Branch;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.reviewdb.client.Change.Id;
import com.google.gerrit.reviewdb.client.Change.Status;
import com.google.gerrit.reviewdb.client.Project.NameKey;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.change.ChangeInserter;
import com.google.gerrit.server.change.ChangeInserter.Factory;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.project.ProjectControl;
import com.google.gerrit.server.project.RefControl;
import com.google.gerrit.server.util.TimeUtil;
import com.google.gerrit.sshd.CommandMetaData;
import com.google.gerrit.sshd.SshCommand;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;

import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.kohsuke.args4j.Argument;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;

@CommandMetaData(name = "createChange", description = "Creates a new change")
public class CreateChange extends SshCommand {
  private static final String BRANCH = "refs/heads/master";
  private static final String PARENT_REF = "master";

  private ReviewDb db;
  private GitRepositoryManager repoManager;
  private ProjectCache projectCache;
  private Factory changeInserterFactory;
  private IdentifiedUser currentUser;

  @Argument(usage = "project name")
  private String projectName = "example-app";

  @Inject
  CreateChange(ReviewDb db, final GitRepositoryManager repoManager,
      ProjectCache projectCache,
      final ChangeInserter.Factory changeInserterFactory,
      IdentifiedUser currentUser
  ) {
    this.db = db;
    this.repoManager = repoManager;
    this.projectCache = projectCache;
    this.changeInserterFactory = changeInserterFactory;
    this.currentUser = currentUser;
  }

  @Override
  protected void run() {
    try {
      stdout.println("Creating workitem on project: " + projectName);
      Id id = new Change.Id(db.nextChangeId());
      Iterable<NameKey> byName = projectCache.byName(projectName);
      NameKey projectNameKey = byName.iterator().next();
      Repository repository = repoManager.openRepository(projectNameKey);
      RevWalk revWalk = new RevWalk(repository);
      Ref ref = repository.getRef(PARENT_REF);
      RevCommit commit = revWalk.parseCommit(ref.getObjectId());

      ObjectInserter newObjectInserter = repository.newObjectInserter();
      CommitBuilder builder = buildCommit(id, ref, commit);
      String changeID = createChangeID(newObjectInserter, builder);
      RevCommit newCommit = insertCommit(id, repository, revWalk, builder, changeID);

      RefControl ctrl = createRefControl(projectNameKey);
      com.google.gerrit.reviewdb.client.Branch.NameKey dest = new Branch.NameKey(projectNameKey, BRANCH);
      Change.Key changeKey = new Change.Key(changeID);
      Change change = createChange(id, changeKey, dest);
      createRef(repository, newCommit, ctrl, change);
      stdout.println("Change: " + change);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private RevCommit insertCommit(Id id, Repository repository, RevWalk revWalk,
      CommitBuilder builder, String changeID) throws IOException,
      MissingObjectException, IncorrectObjectTypeException {
    builder.setMessage("Dummy commit\n\n" + "Change-Id: " + changeID);
    ObjectId insert = repository.newObjectInserter().insert(builder);
    RevCommit newCommit = revWalk.parseCommit(insert);
    return newCommit;
  }

  private String createChangeID(ObjectInserter newObjectInserter,
      CommitBuilder builder) throws UnsupportedEncodingException {
    byte[] data = builder.build();
    String changeID = createChangeID(newObjectInserter, data);
    return changeID;
  }

  private String createChangeID(ObjectInserter newObjectInserter, byte[] data) {
    ObjectId commitID = newObjectInserter.idFor(Constants.OBJ_COMMIT, data, 0, data.length);
    String changeID = "I" + commitID.getName();
    return changeID;
  }

  private CommitBuilder buildCommit(Id id, Ref ref, RevCommit commit) {
    CommitBuilder builder = new CommitBuilder();
    builder.addParentId(ref.getObjectId());
    builder.setTreeId(commit.getTree().getId());
    builder.setCommitter(new PersonIdent("ian", "irbull@gmail.com"));
    builder.setAuthor(new PersonIdent("ian", "irbull@gmail.com"));
    builder.setMessage("A test commit message " + id );
    return builder;
  }

  private RefControl createRefControl(NameKey projectNameKey) {
    ProjectControl projectControl  = projectCache.get(projectNameKey).controlFor(currentUser);
    RefControl ctrl = projectControl.controlForRef(BRANCH);
    return ctrl;
  }

  private Change createChange(Id id, Change.Key changeKey,
      com.google.gerrit.reviewdb.client.Branch.NameKey dest) {
    Change change = new Change(changeKey, id, currentUser.getAccountId(), dest, TimeUtil.nowTs());
    change.setStatus(Status.NEW);
    return change;
  }

  private void createRef(Repository repository, RevCommit newCommit,
      RefControl ctrl, Change change) throws OrmException, IOException {
    final ChangeInserter ins =
        changeInserterFactory.create(ctrl, change, newCommit)
            .setReviewers(Collections
                .<com.google.gerrit.reviewdb.client.Account.Id> emptySet()).setDraft(false);
    String refName = ins.getPatchSet().getRefName();
    ins.insert();
    RefUpdate refUpdate = repository.getRefDatabase().newUpdate(refName, false);
    refUpdate.setNewObjectId(newCommit);
    refUpdate.update();
  }

}
