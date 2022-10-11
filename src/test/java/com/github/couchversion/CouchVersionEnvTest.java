package com.github.couchversion;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.github.couchversion.changeset.ChangeEntry;
import com.github.couchversion.dao.CouchVersionDAO;
import com.github.couchversion.resources.EnvironmentMock;
import com.github.couchversion.test.changelogs.test1.EnvironmentDependentTestResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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

  @BeforeEach
  public void init() throws Exception {
    runner = new CouchVersion(cluster, bucket);
    runner.setDAO(dao);
    runner.setEnabled(true);



    when(dao.hasNewChanges(anyList())).thenReturn(true);
    when(dao.getLock(anyLong())).thenReturn(true);
    when(dao.isLocked()).thenReturn(false);
    when(dao.isNewChange(any())).thenReturn(true);
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
