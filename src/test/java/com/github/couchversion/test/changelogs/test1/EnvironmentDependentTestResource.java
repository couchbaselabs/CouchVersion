package com.github.couchversion.test.changelogs.test1;

import com.couchbase.client.java.Bucket;
import com.github.couchversion.changeset.ChangeLog;
import com.github.couchversion.changeset.ChangeSet;

@ChangeLog(order = "3")
public class EnvironmentDependentTestResource {
  @ChangeSet(author = "testuser", id = "Envtest1", order = "01")
  public void testChangeSet7WithEnvironment(Bucket bucket) {
    System.out.println("invoked Envtest1 with bucket=" + bucket.name() );
  }
}
