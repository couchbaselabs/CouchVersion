package com.github.couchversion;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.github.couchversion.changeset.ChangeEntry;
import com.github.couchversion.dao.BucketWrapper;
import com.github.couchversion.dao.CouchVersionDAO;
import com.github.couchversion.resources.EnvironmentMock;
import com.github.couchversion.test.changelogs.CouchVersionChange2TestResource;
import com.github.couchversion.test.profiles.def.UnProfiledChangeLog;
import com.github.couchversion.test.profiles.dev.ProfiledDevChangeLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.Mockito.*;

/**
 * Tests for Spring profiles integration
 *
 * @author deniswsrosa
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CouchVersionProfileTest {

  @Mock
  private Bucket bucket;

  @Mock
  private Cluster cluster;

  @Mock
  private BucketWrapper bucketWrapper;

  private CouchVersion runner;

  @Mock
  private Collection collection;

  @Mock
  private CouchVersionDAO dao;


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
  public void shouldRunDevProfileAndNonAnnotated() throws Exception {
    // given
    runner.setSpringEnvironment(new EnvironmentMock("dev", "test"));
    runner.setChangeLogsScanPackage(ProfiledDevChangeLog.class.getPackage().getName());
    when(bucket.defaultCollection().get(any(String.class))).thenReturn(null);

    // when
    runner.execute();

    // then
    verify(dao, times(1)).save(new ChangeEntry("Pdev1"));
    verify(dao, times(1)).save(new ChangeEntry("Pdev4"));
    verify(dao, times(0)).save(new ChangeEntry("Pdev3"));

  }

  @Test
  public void shouldRunUnprofiledChangeLog() throws Exception {
    // given
    runner.setSpringEnvironment(new EnvironmentMock("test"));
    runner.setChangeLogsScanPackage(UnProfiledChangeLog.class.getPackage().getName());
    when(bucket.defaultCollection().get(any(String.class))).thenReturn(null);

    // when
    runner.execute();

    // then
    verify(dao, times(1)).save(new ChangeEntry("Pdev1"));
    verify(dao, times(1)).save(new ChangeEntry("Pdev2"));
    verify(dao, times(1)).save(new ChangeEntry("Pdev3"));
    verify(dao, times(0)).save(new ChangeEntry("Pdev4"));
    verify(dao, times(1)).save(new ChangeEntry("Pdev5"));
  }

  @Test
  public void shouldNotRunAnyChangeSet() throws Exception {
    // given
    runner.setSpringEnvironment(new EnvironmentMock("foobar"));
    runner.setChangeLogsScanPackage(ProfiledDevChangeLog.class.getPackage().getName());
    when(bucket.defaultCollection().get(any(String.class))).thenReturn(null);

    // when
    runner.execute();

    // then
    verify(dao, times(0)).save(new ChangeEntry("Pdev1"));
    verify(dao, times(0)).save(new ChangeEntry("Pdev2"));
    verify(dao, times(0)).save(new ChangeEntry("Pdev3"));
    verify(dao, times(0)).save(new ChangeEntry("Pdev4"));
  }



  @Test
  public void shouldRunChangeSetsWhenNoEnv() throws Exception {
    // given
    runner.setSpringEnvironment(null);
    runner.setChangeLogsScanPackage(CouchVersionChange2TestResource.class.getPackage().getName());
    when(bucket.defaultCollection().get(any(String.class))).thenReturn(null);

    // when
    runner.execute();

    // then
    verify(dao, times(1)).save(new ChangeEntry("Btest1"));
    verify(dao, times(1)).save(new ChangeEntry("Btest2"));
    verify(dao, times(1)).save(new ChangeEntry("Btest3"));
  }


  @Test
  public void shouldRunChangeSetsWhenEmptyEnv() throws Exception {
    // given
    runner.setSpringEnvironment(new EnvironmentMock());
    runner.setChangeLogsScanPackage(CouchVersionChange2TestResource.class.getPackage().getName());
    when(bucket.defaultCollection().get(any(String.class))).thenReturn(null);

    // when
    runner.execute();

    // then
    verify(dao, times(1)).save(new ChangeEntry("Btest1"));
    verify(dao, times(1)).save(new ChangeEntry("Btest2"));
    verify(dao, times(1)).save(new ChangeEntry("Btest3"));
  }



  @Test
  public void shouldRunAllChangeSets() throws Exception {
    // given
    runner.setSpringEnvironment(new EnvironmentMock("dev"));
    runner.setChangeLogsScanPackage(CouchVersionChange2TestResource.class.getPackage().getName());
    when(bucket.defaultCollection().get(any(String.class))).thenReturn(null);

    // when
    runner.execute();

    // then
    verify(dao, times(1)).save(new ChangeEntry("Btest1"));
    verify(dao, times(1)).save(new ChangeEntry("Btest2"));
    verify(dao, times(1)).save(new ChangeEntry("Btest3"));
  }

}
