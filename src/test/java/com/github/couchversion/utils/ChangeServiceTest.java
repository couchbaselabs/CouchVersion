package com.github.couchversion.utils;

import com.github.couchversion.changeset.ChangeEntry;
import com.github.couchversion.exception.CouchVersionChangeSetVersionException;
import com.github.couchversion.test.changelogs.*;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author deniswsrosa
 */
public class ChangeServiceTest {

  @Test
  public void shouldFindChangeLogClasses(){
    // given
    String scanPackage = CouchVersionTestResource.class.getPackage().getName();
    ChangeService service = new ChangeService(scanPackage);
    // when
    List<Class<?>> foundClasses = service.fetchChangeLogs();
    // then
    assertTrue(foundClasses != null && foundClasses.size() > 0);
  }

  @Test
  public void shouldFindChangeSetMethods() throws CouchVersionChangeSetVersionException {
    // given
    String scanPackage = CouchVersionTestResource.class.getPackage().getName();
    ChangeService service = new ChangeService(scanPackage);

    // when
    List<Method> foundMethods = service.fetchChangeSets(CouchVersionTestResource.class);

    // then
    assertTrue(foundMethods != null && foundMethods.size() == 3);
  }

  @Test
  public void shouldFindAnotherChangeSetMethods() throws CouchVersionChangeSetVersionException {
    // given
    String scanPackage = CouchVersionTestResource.class.getPackage().getName();
    ChangeService service = new ChangeService(scanPackage);

    // when
    List<Method> foundMethods = service.fetchChangeSets(CouchVersionChange2TestResource.class);

    // then
    assertTrue(foundMethods != null && foundMethods.size() == 3);
  }


  @Test
  public void shouldFindIsRunAlwaysMethod() throws CouchVersionChangeSetVersionException {
    // given
    String scanPackage = CouchVersionTestResource.class.getPackage().getName();
    ChangeService service = new ChangeService(scanPackage);

    // when
    List<Method> foundMethods = service.fetchChangeSets(CouchVersionChange2TestResource.class);
    // then
    for (Method foundMethod : foundMethods) {
      if (foundMethod.getName().equals("testChangeSetWithAlways")){
        assertTrue(service.isRunAlwaysChangeSet(foundMethod));
      } else {
        assertFalse(service.isRunAlwaysChangeSet(foundMethod));
      }
    }
  }

  @Test
  public void shouldCreateEntry() throws CouchVersionChangeSetVersionException {

    // given
    String scanPackage = CouchVersionTestResource.class.getPackage().getName();
    ChangeService service = new ChangeService(scanPackage);
    List<Method> foundMethods = service.fetchChangeSets(CouchVersionTestResource.class);

    for (Method foundMethod : foundMethods) {

      // when
      ChangeEntry entry = service.createChangeEntry(foundMethod);

      // then
      assertEquals("testuser", entry.getAuthor());
      assertEquals(CouchVersionTestResource.class.getName(), entry.getChangeLogClass());
      assertNotNull(entry.getTimestamp());
      assertNotNull(entry.getChangeId());
      assertNotNull(entry.getChangeSetMethodName());
    }
  }

  @Test(expected = CouchVersionChangeSetVersionException.class)
  public void shouldFailOnDuplicatedChangeSets() throws CouchVersionChangeSetVersionException {
    String scanPackage = ChangeLogWithDuplicate.class.getPackage().getName();
    ChangeService service = new ChangeService(scanPackage);
    service.fetchChangeSets(ChangeLogWithDuplicate.class);
  }

}
