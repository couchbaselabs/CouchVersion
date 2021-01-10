package com.github.couchversion.test.changelogs;

import com.couchbase.client.java.Bucket;
import com.github.couchversion.changeset.ChangeLog;
import com.github.couchversion.changeset.ChangeSet;

/**
 * @author deniswsrosa
 */
@ChangeLog(order = "2")
public class CouchVersionChange2TestResource {

  @ChangeSet(author = "testuser", id = "Btest1", order = "1")
  public void testChangeSet(){
    System.out.println("invoked B1");
  }
  @ChangeSet(author = "testuser", id = "Btest2", order = "2")
  public void testChangeSet2(){
    System.out.println("invoked B2");
  }

  @ChangeSet(author = "testuser", id = "Btest3", order = "3")
  public void testChangeSet6(Bucket bucket) {
    System.out.println("invoked B3 with bucket=" +bucket.name());
  }

  @ChangeSet(author = "testuser", id = "Btest4", order = "4", runAlways = true)
  public void testChangeSetWithAlways(Bucket bucket) {
    System.out.println("invoked B4 with bucket=" + bucket.name());
  }

  @ChangeSet(author = "testuser", id = "Btest5", order = "5", restartInterrupted = false)
  public void testChangeSetWithRestartInterrupted(Bucket bucket) {
    System.out.println("invoked B5 with bucket=" + bucket.name());
  }

}
