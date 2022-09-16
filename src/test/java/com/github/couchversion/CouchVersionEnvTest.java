package com.github.couchversion;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.github.couchversion.changeset.ChangeEntry;
import com.github.couchversion.dao.CouchVersionDAO;
import com.github.couchversion.resources.EnvironmentMock;
import com.github.couchversion.test.changelogs.test1.EnvironmentDependentTestResource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class CouchVersionEnvTest {

  @Mock
  private Bucket bucket;

  @Mock
  private Cluster cluster;

  @Mock
  private Collection collection;

  @Mock
  private CouchVersionDAO dao;

  private CouchVersion runner;

  @Before
  public void init() throws Exception {
    runner = new CouchVersion(cluster, bucket);
    runner.setDAO(dao);
    runner.setEnabled(true);



    when(dao.hasNewChanges(anyList())).thenReturn(true);
    when(dao.getLock(anyLong())).thenReturn(true);
    when(dao.isLocked()).thenReturn(false);
    when(dao.isNewChange(anyObject())).thenReturn(true);
    when(bucket.defaultCollection()).thenReturn(collection);
  }

  @Test
  public void shouldRunChangesetWithEnvironment() throws Exception {
    // given
    runner.setSpringEnvironment(new EnvironmentMock());
    runner.setChangeLogsScanPackage(EnvironmentDependentTestResource.class.getPackage().getName());
    when(bucket.defaultCollection().get(any(String.class))).thenReturn(null);
    // when
    runner.execute();
    // then
    ChangeEntry entry = new ChangeEntry("Envtest1");
    verify(dao, times(1)).save(entry);

  }

  @Test
  public void shouldRunChangesetWithNullEnvironment() throws Exception {
    // given
    runner.setSpringEnvironment(null);
    runner.setChangeLogsScanPackage(EnvironmentDependentTestResource.class.getPackage().getName());
    when(bucket.defaultCollection().get(any(String.class))).thenReturn(null);

    // when
    runner.execute();

    // then
    verify(dao, times(1)).save(new ChangeEntry("Envtest1"));
  }
}
