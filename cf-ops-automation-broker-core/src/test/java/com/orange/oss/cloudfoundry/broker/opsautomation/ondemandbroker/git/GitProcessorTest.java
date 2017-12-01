package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import static org.fest.assertions.Assertions.assertThat;

public class GitProcessorTest {

    private static final Logger logger = LoggerFactory.getLogger(GitProcessorTest.class);
    private static final String GIT_URL = "git://127.0.0.1:9418/volatile-repo.git";

    static GitServer gitServer;

    GitProcessor processor = new GitProcessor("gituser", "gitsecret", GIT_URL, "committerName", "committer@address.org");
    Context ctx = new Context();



    @BeforeClass
    public static void startGitServer() throws IOException, GitAPIException {
        gitServer = new GitServer();
        gitServer.startEphemeralReposServer(GitServer.NO_OP_INITIALIZER);
    }

    @AfterClass
    public static void stopGitServer() throws InterruptedException {
        gitServer.stopAndCleanupReposServer();
    }

    @After
    public void cleanUpClone() throws IOException {
        if (processor != null) {
            processor.deleteWorkingDir(ctx);
        }
        gitServer.cleanUpRepos();
    }

    @Test
    public void noop_when_no_changes_made() throws Exception {
        //given a clone of an empty repo
        processor.cloneRepo(ctx);
        Iterable<RevCommit> initialCommits = processor.getGit(ctx).log().call();

        //when no changes are made
        processor.commitPushRepo(ctx, false);

        //then repo does not contain new commits
        Iterable<RevCommit> resultingCommits = processor.getGit(ctx).log().call();
        assertThat(countIterables(resultingCommits)).isEqualTo(countIterables(initialCommits));
    }

    @Test
    public void configures_user_name_and_email_in_commits() throws GitAPIException, IOException {
        //given explicit user config
        processor = new GitProcessor("gituser", "gitsecret", GIT_URL, "committerName", "committer@address.org");
        processor.cloneRepo(ctx);

        //when adding files
        //and asking to commit and push
        addAFile(ctx);
        processor.commitPushRepo(ctx, false);

        //then commit have proper identity
        Iterable<RevCommit> resultingCommits = processor.getGit(ctx).log().call();
        RevCommit commit = resultingCommits.iterator().next();
        PersonIdent committerIdent = commit.getCommitterIdent();
        assertThat(committerIdent.getName()).isEqualTo("committerName");
        assertThat(committerIdent.getEmailAddress()).isEqualTo("committer@address.org");
        PersonIdent authorIdent = commit.getAuthorIdent();
        assertThat(authorIdent.getName()).isEqualTo("committerName");
        assertThat(commit.getAuthorIdent().getEmailAddress()).isEqualTo("committer@address.org");
    }

    @Test
    public void supports_default_user_name_and_emails_config() throws GitAPIException, IOException {
        //given no user config specified (null values)
        processor = new GitProcessor("gituser", "gitsecret", GIT_URL, null, null);
        processor.cloneRepo(ctx);

        //when adding files
        //and asking to commit and push
        addAFile(ctx);
        processor.commitPushRepo(ctx, true);

        //then commit does not fail
        //we don't asser the content since this depends on the local execution environment
    }


    @Test
    public void adds_and_deletes_files() throws GitAPIException, IOException {
        //given a clone of an empty repo
        processor.cloneRepo(ctx);

        //when adding files
        //and asking to commit and push
        addAFile(ctx);
        processor.commitPushRepo(ctx, true);

        //then file should be persisted
        Context ctx1 = new Context();
        processor.preCreate(ctx1);
        Path secondClone = getWorkDir(ctx1);
        File secondCloneSameFile = secondClone.resolve("afile.txt").toFile();
        assertThat(secondCloneSameFile).exists();

        //when deleting file
        assertThat(secondCloneSameFile.delete()).isTrue();
        //and committing
        processor.commitPushRepo(ctx1, true);

        //then file should be removed from repo
        Path thirdClone = cloneRepo(processor);
        File thirdCloneFile = thirdClone.resolve("afile.txt").toFile();
        assertThat(thirdCloneFile).doesNotExist();

        //cleanup
        processor.deleteWorkingDir(ctx1);
    }

    @Test
    public void rebases_during_push_conflicts() throws GitAPIException, IOException {
        //given a clone of an empty repo
        Context ctx1 = new Context();
        GitProcessor processor1 = new GitProcessor("gituser", "gitsecret", GIT_URL, "committerName", "committer@address.org");
        processor1.cloneRepo(ctx1);

        //Given concurrent commits
        Context ctx2 = new Context();
        GitProcessor processor2 = new GitProcessor("gituser", "gitsecret", GIT_URL, "committerName", "committer@address.org");
        processor2.cloneRepo(ctx2);
        addAFile(ctx2, "content2", "a_first_file.txt");
        processor2.commitPushRepo(ctx2, true);

        //when trying to commit and push
        addAFile(ctx1, "content1", "a_second_file.txt");
        processor1.commitPushRepo(ctx1, true);

        //then a rebase should be tried, a 3rd clone sees both files commited
        Context ctx3 = new Context();
        GitProcessor processor3 = new GitProcessor("gituser", "gitsecret", GIT_URL, "committerName", "committer@address.org");
        processor3.cloneRepo(ctx3);
        Path thirdClone = getWorkDir(ctx3);
        assertThat(thirdClone.resolve("a_first_file.txt").toFile()).exists();
        assertThat(thirdClone.resolve("a_second_file.txt").toFile()).exists();

        //cleanup
        processor1.deleteWorkingDir(ctx1);
        processor2.deleteWorkingDir(ctx2);
        processor3.deleteWorkingDir(ctx3);
    }


    @Test
    public void fails_if_pull_rebase_fails_during_push() throws GitAPIException, IOException {
        //given a clone of an empty repo
        Context ctx1 = new Context();
        GitProcessor processor1 = new GitProcessor("gituser", "gitsecret", GIT_URL, "committerName", "committer@address.org");
        processor1.cloneRepo(ctx1);

        //Given concurrent commits
        Context ctx2 = new Context();
        GitProcessor processor2 = new GitProcessor("gituser", "gitsecret", GIT_URL, "committerName", "committer@address.org");
        processor2.cloneRepo(ctx2);
        addAFile(ctx2, "content2", "same_file.txt");
        processor2.commitPushRepo(ctx2, true);

        //when trying to commit and push
        addAFile(ctx1, "content1", "same_file.txt");
        try {
            processor1.commitPushRepo(ctx1, true);
            Assert.fail("expected exception");
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("conflict");
        }
    }


    @Test
    public void supports_custom_commit_msg() throws GitAPIException, IOException {
        //given a clone of an empty repo
        Context ctx = new Context();
        processor.cloneRepo(ctx);

        //when adding files with a custom message
        ctx.contextKeys.put(GitProcessorContext.commitMessage.toString(), "a custom message");
        addAFile(ctx);
        processor.commitPushRepo(ctx, false);

        Iterable<RevCommit> resultingCommits = processor.getGit(ctx).log().call();
        RevCommit commit = resultingCommits.iterator().next();
        assertThat(commit.getShortMessage()).isEqualTo("a custom message");
    }

    public void addAFile(Context ctx) throws IOException {
        addAFile(ctx, "hello.txt", "afile.txt");
    }

    public void addAFile(Context ctx, String content, String fileRelativePath) throws IOException {
        Path workDir = getWorkDir(ctx);
        try (FileWriter writer = new FileWriter(workDir.resolve(fileRelativePath).toFile())) {
            writer.append(content);
        }
    }

    public Path getWorkDir(Context ctx) {
        return (Path) ctx.contextKeys.get(GitProcessorContext.workDir.toString());
    }

    public int countIterables(Iterable<RevCommit> resultingCommits) {
        int size = 0;
        for (RevCommit ignored : resultingCommits) {
            size++;
        }
        return size;
    }

    public Path cloneRepo(GitProcessor processor) {
        Context ctx = new Context();
        processor.preCreate(ctx);

        return getWorkDir(ctx);
    }


}