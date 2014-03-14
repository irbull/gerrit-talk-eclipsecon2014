package com.googlesource.gerrit.plugins.tutorialplugin;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.reviewdb.client.Branch;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.reviewdb.client.Change.Id;
import com.google.gerrit.reviewdb.client.Change.Status;
import com.google.gerrit.reviewdb.client.Project.NameKey;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.change.ChangeInserter;
import com.google.gerrit.server.change.ChangeInserter.Factory;
import com.google.gerrit.server.config.CanonicalWebUrl;
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
import org.eclipse.jgit.errors.RepositoryNotFoundException;
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
  private static final String INITIAL_COMMIT_MESSAGE = "Work for Bug #";
  private static final String REF_FOR_COMMIT = "refs/heads/master";
  private static final String BRANCH = "master";

  private ReviewDb db;
  private GitRepositoryManager repoManager;
  private ProjectCache projectCache;
  private Factory changeInserterFactory;
  private IdentifiedUser currentUser;

  @Argument(usage = "projectame bugNumber", index = 0)
  private String projectName = null;
  @Argument(usage = "name of user", index = 1)
  private String bugNumber = null;
  private String changeID;
  private Id id;
  private String canonicalWebUrl;
  private String pluginName;

  @Inject
  CreateChange(ReviewDb db, final GitRepositoryManager repoManager,
      ProjectCache projectCache,
      final ChangeInserter.Factory changeInserterFactory,
      IdentifiedUser currentUser,
      @CanonicalWebUrl final String canonicalWebUrl,
      @PluginName final String pluginName
  ) {
    this.db = db;
    this.repoManager = repoManager;
    this.projectCache = projectCache;
    this.changeInserterFactory = changeInserterFactory;
    this.currentUser = currentUser;
    this.canonicalWebUrl = canonicalWebUrl;
    this.pluginName = pluginName;
  }

  @Override
  protected void run() {
    try {
      if ( projectName == null || bugNumber == null ) {
        stdout.println(getUsageMessage());
        return;
      }
      NameKey projectNameKey = getProjectNameKey();
      if ( projectNameKey == null ) {
        stdout.println("Cannot find project: " + projectName );
        return;
      }
      id = new Change.Id(db.nextChangeId());
      Change change = createWorkItemChangeSet(projectNameKey);
      stdout.println(" Create workitem at " + canonicalWebUrl + change.getChangeId() );
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private String getUsageMessage() {
    return "usage ssh <server> -p <port> " + pluginName + " createChange <projectName> <bugNumber>";
  }

  private Change createWorkItemChangeSet(NameKey projectNameKey)
      throws RepositoryNotFoundException, IOException, MissingObjectException,
      IncorrectObjectTypeException, OrmException, UnsupportedEncodingException {
    Repository repository = repoManager.openRepository(projectNameKey);
    RevCommit commit = buildEmptyCommit(repository, BRANCH);
    return creaetAndInsertChange(projectNameKey, repository, commit);
  }

  private Change creaetAndInsertChange(NameKey projectNameKey, Repository repository,
      RevCommit commit) throws OrmException, IOException {
    RefControl ctrl = createRefControl(projectNameKey);
    com.google.gerrit.reviewdb.client.Branch.NameKey dest = new Branch.NameKey(projectNameKey, REF_FOR_COMMIT);
    Change.Key changeKey = new Change.Key(changeID);
    Change change = createChange(id, changeKey, dest);
    final ChangeInserter ins =
        changeInserterFactory.create(ctrl, change, commit)
            .setReviewers(Collections
                .<com.google.gerrit.reviewdb.client.Account.Id> emptySet()).setDraft(false);
    String refName = ins.getPatchSet().getRefName();
    createRef(repository, commit, refName);
    ins.insert();
    return change;
  }

  private RevCommit buildEmptyCommit(Repository repository, String referenceName) throws IOException {
    Ref ref = repository.getRef(referenceName);
    RevCommit commitAtRef = getCommitAtRef(repository, ref);
    RevCommit commit = buildEmptyCommit(repository, ref, commitAtRef, id);
    return commit;
  }

  private RevCommit buildEmptyCommit(Repository repository, Ref ref, RevCommit commit, Id id)
      throws UnsupportedEncodingException, IOException, MissingObjectException,
      IncorrectObjectTypeException {
    RevWalk revWalk = new RevWalk(repository);
    ObjectInserter newObjectInserter = repository.newObjectInserter();
    CommitBuilder builder = buildCommit(id, ref, commit);
    changeID = createChangeIDFromCommit(newObjectInserter, builder);
    return insertCommit(id, repository, revWalk, builder, changeID);
  }

  private RevCommit getCommitAtRef(Repository repository, Ref ref)
      throws MissingObjectException, IncorrectObjectTypeException, IOException {
    RevWalk revWalk = new RevWalk(repository);
    return revWalk.parseCommit(ref.getObjectId());
  }

  private NameKey getProjectNameKey() {
    NameKey projectNameKey = null;
    if ( projectName != null ) {
      Iterable<NameKey> byName = projectCache.byName(projectName);
      projectNameKey = byName.iterator().next();
    }
    return projectNameKey;
  }

  private RevCommit insertCommit(Id id, Repository repository, RevWalk revWalk,
      CommitBuilder builder, String changeID) throws IOException,
      MissingObjectException, IncorrectObjectTypeException {
    builder.setMessage(INITIAL_COMMIT_MESSAGE + bugNumber + "\n\n" + "Change-Id: " + changeID);
    ObjectId insert = repository.newObjectInserter().insert(builder);
    return revWalk.parseCommit(insert);
  }

  private String createChangeIDFromCommit(ObjectInserter newObjectInserter,
      CommitBuilder builder) throws UnsupportedEncodingException {
    byte[] data = builder.build();
    return createChangeID(newObjectInserter, data);
  }

  private String createChangeID(ObjectInserter newObjectInserter, byte[] data) {
    ObjectId commitID = newObjectInserter.idFor(Constants.OBJ_COMMIT, data, 0, data.length);
    return "I" + commitID.getName();
  }

  private CommitBuilder buildCommit(Id id, Ref ref, RevCommit commit) {
    CommitBuilder builder = new CommitBuilder();
    builder.addParentId(ref.getObjectId());
    builder.setTreeId(commit.getTree().getId());
    PersonIdent personIdent = extracted();
    builder.setCommitter(personIdent);
    builder.setAuthor(personIdent);
    builder.setMessage(INITIAL_COMMIT_MESSAGE + bugNumber);
    return builder;
  }

  private PersonIdent extracted() {
    String name = currentUser.getName();
    String email = currentUser.getNameEmail();
    return new PersonIdent(name, email);
  }

  private RefControl createRefControl(NameKey projectNameKey) {
    ProjectControl projectControl  = projectCache.get(projectNameKey).controlFor(currentUser);
    return projectControl.controlForRef(REF_FOR_COMMIT);
  }

  private Change createChange(Id id, Change.Key changeKey,
      com.google.gerrit.reviewdb.client.Branch.NameKey dest) {
    Change change = new Change(changeKey, id, currentUser.getAccountId(), dest, TimeUtil.nowTs());
    change.setStatus(Status.NEW);
    return change;
  }

  private void createRef(Repository repository, RevCommit newCommit, String refName ) throws OrmException, IOException {
    RefUpdate refUpdate = repository.getRefDatabase().newUpdate(refName, false);
    refUpdate.setNewObjectId(newCommit);
    refUpdate.update();
  }

}
