package com.github.couchversion;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.github.couchversion.changeset.ChangeEntry;
import com.github.couchversion.dao.CouchVersionDAO;
import com.github.couchversion.exception.CouchVersionChangeSetVersionException;
import com.github.couchversion.exception.CouchVersionConfigurationVersionException;
import com.github.couchversion.exception.CouchVersionCounterException;
import com.github.couchversion.exception.CouchVersionException;
import com.github.couchversion.utils.ChangeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.springframework.util.StringUtils.hasText;

/**
 * CouchVersion runner
 *
 * @author deniswsrosa
 */
public class CouchVersion implements InitializingBean {
  private static final Logger logger = LoggerFactory.getLogger(CouchVersion.class);

  private CouchVersionDAO dao;

  private boolean enabled = true;
  private String changeLogsScanPackage;
  private Bucket bucket;
  private Cluster cluster;
  private ApplicationContext context;
  private Environment springEnvironment;


  public CouchVersion(ApplicationContext context) {
    this.context = context;
    this.springEnvironment = context.getEnvironment();

    this.cluster = Cluster.connect(springEnvironment.getProperty("spring.couchbase.connection"),
            springEnvironment.getProperty("spring.couchbase.user"),
            springEnvironment.getProperty("spring.couchbase.password"));

    this.bucket = this.cluster.bucket(springEnvironment.getProperty("spring.couchbase.bucket"));
    this.dao = new CouchVersionDAO(cluster, bucket);
  }


  public CouchVersion(Cluster cluster, Bucket bucket) {
    this.cluster = cluster;
    this.bucket = bucket;
    this.dao = new CouchVersionDAO(cluster, bucket);
  }

  public CouchVersion(String connectionString, String bucketName, String user, String password) {
    this.cluster = Cluster.connect(connectionString, user, password);
    this.bucket = this.cluster.bucket(bucketName);
    this.dao = new CouchVersionDAO(cluster, bucket);
  }

  public CouchVersion(ApplicationContext context, String connectionString, String bucketName, String user, String password) {
    this.context = context;
    this.cluster = Cluster.connect(connectionString, user, password);
    this.bucket = this.cluster.bucket(bucketName);
    this.dao = new CouchVersionDAO(cluster, bucket);
  }

  public void setApplicationContext(ApplicationContext context) {
    this.context = context;
  }

  /**
   * For Spring users: executing CouchVersion after bean is created in the Spring context
   *
   * @throws Exception exception
   */
  @Override
  public void afterPropertiesSet() throws Exception {
      execute();
  }

  /**
   * Executing migration
   *
   * @throws CouchVersionException exception
   */
  public void execute() throws CouchVersionException, InterruptedException {
    if (!isEnabled()) {
      logger.info("CouchVersion is disabled. Exiting.");
      return;
    }

    validateConfig();

    if(hasNewChanges()) {
      logger.info("CouchVersion is starting the data migration sequence..");
      executeMigrationWithLock();
    } else {
      logger.info("CouchVersion - running Changesets with 'always'");
      executeRunAlways();
    }
    logger.info("CouchVersion has finished its job.");
  }

  private void executeMigrationWithLock() throws CouchVersionException, InterruptedException  {
    //up to ~5 minutes waiting for the lock
    int timeout = 120000;
    int waitingTime = 10000;
    int tries = 0;

    while (waitingTime < timeout) {

      if(hasNewChanges()) {

        long timestamp = new Date().getTime();
        if(!dao.isLocked() && dao.getLock(timestamp)) {
          logger.info("CouchVersion - Lock acquired.");

          try{
            executeMigration();
            break;

          } finally {
            dao.releaseLock(timestamp);
            logger.info("CouchVersion - Lock released.");
          }

        } else {
          logger.info("CouchVersion - Waiting "+waitingTime+"ms to get CouchVersion Lock.'");
          Thread.sleep(waitingTime);
          waitingTime =  (int) (1.5 * waitingTime) ;
        }

      } else {
        logger.info("CouchVersion - Looks like the migration ran in another instance.'");
        logger.info("CouchVersion - running Changesets with 'always'");
        executeRunAlways();
        break;
      }
    }

    if(waitingTime > timeout) {
      throw new CouchVersionException("Couldn't get the lock. Are there any other migrations running? " +
              "If not, delete document with id 'couchversion_lock' ");
    }

  }


  private boolean hasNewChanges() throws CouchVersionException {
    logger.info("CouchVersion - checking for new schema changes");
    if( dao.hasNewChanges(getChangeEntryIds()) ) {
      logger.info("CouchVersion - new schema changes found");
      return true;
    } else {
      logger.info("CouchVersion - no new changes found");
      return false;
    }
  }

  private List<String> changeEntryIdsCache;

  private List<String> getChangeEntryIds() throws CouchVersionException {


    if(changeEntryIdsCache == null ) {

      changeEntryIdsCache = new ArrayList<>();
      ChangeService service = new ChangeService(changeLogsScanPackage, springEnvironment);

      for (Class<?> changelogClass : service.fetchChangeLogs()) {

        Object changelogInstance = null;
        try {
          changelogInstance = changelogClass.getConstructor().newInstance();

          if (context != null) {
            context.getAutowireCapableBeanFactory().autowireBean(changelogInstance);
          }
          List<Method> changesetMethods = service.fetchChangeSets(changelogInstance.getClass());
          for (Method changesetMethod : changesetMethods) {
            ChangeEntry changeEntry = service.createChangeEntry(changesetMethod);
            if (!service.isRunAlwaysChangeSet(changesetMethod)) {
              changeEntryIdsCache.add(changeEntry._getId());
            }
          }

        } catch (NoSuchMethodException e) {
          throw new CouchVersionException(e.getMessage(), e);
        } catch (IllegalAccessException e) {
          throw new CouchVersionException(e.getMessage(), e);
        } catch (InvocationTargetException e) {
          Throwable targetException = e.getTargetException();
          throw new CouchVersionException(targetException.getMessage(), e);
        } catch (InstantiationException e) {
          throw new CouchVersionException(e.getMessage(), e);
        }

      }
    }

    return changeEntryIdsCache;
  }



  private void executeRunAlways() throws CouchVersionException {

    ChangeService service = new ChangeService(changeLogsScanPackage, springEnvironment);

    for (Class<?> changelogClass : service.fetchChangeLogs()) {

      Object changelogInstance = null;
      try {
        changelogInstance = changelogClass.getConstructor().newInstance();

        if (context != null) {
          context.getAutowireCapableBeanFactory().autowireBean(changelogInstance);
        }

        List<Method> changesetMethods = service.fetchChangeSets(changelogInstance.getClass());

        for (Method changesetMethod : changesetMethods) {
          ChangeEntry changeEntry = service.createChangeEntry(changesetMethod);

          try {
            if (service.isRunAlwaysChangeSet(changesetMethod)) {
              executeMethod(changesetMethod, changelogInstance, changeEntry);
              logger.info(changeEntry + " reapplied");

            }
          } catch (CouchVersionChangeSetVersionException e) {
            logger.error(e.getMessage());
          }
        }
      } catch (NoSuchMethodException e) {
        throw new CouchVersionException(e.getMessage(), e);
      } catch (IllegalAccessException e) {
        throw new CouchVersionException(e.getMessage(), e);
      } catch (InvocationTargetException e) {
        Throwable targetException = e.getTargetException();
        throw new CouchVersionException(targetException.getMessage(), e);
      } catch (InstantiationException e) {
        throw new CouchVersionException(e.getMessage(), e);
      }
    }
  }


  private void executeMigration() throws CouchVersionException {

    ChangeService service = new ChangeService(changeLogsScanPackage, springEnvironment);

    for (Class<?> changelogClass : service.fetchChangeLogs()) {

      Object changelogInstance = null;
      try {
        changelogInstance = changelogClass.getConstructor().newInstance();

        if (context != null) {
          context.getAutowireCapableBeanFactory().autowireBean(changelogInstance);
        }

        List<Method> changesetMethods = service.fetchChangeSets(changelogInstance.getClass());

        for (Method changesetMethod : changesetMethods) {
          ChangeEntry changeEntry = service.createChangeEntry(changesetMethod);

          try {
            if (dao.isNewChange(changeEntry)) {
              executeMethod(changesetMethod, changelogInstance, changeEntry);
              dao.save(changeEntry);
              logger.info(changeEntry + " applied");

            } else if (service.isRunAlwaysChangeSet(changesetMethod)) {
              executeMethod(changesetMethod, changelogInstance, changeEntry);
              logger.info(changeEntry + " reapplied");

            } else {
              logger.info(changeEntry + " passed over");
            }
          } catch (CouchVersionChangeSetVersionException e) {
            logger.error(e.getMessage());
          }
        }
      } catch (NoSuchMethodException e) {
        throw new CouchVersionException(e.getMessage(), e);
      } catch (IllegalAccessException e) {
        throw new CouchVersionException(e.getMessage(), e);
      } catch (InvocationTargetException e) {
        Throwable targetException = e.getTargetException();
        throw new CouchVersionException(targetException.getMessage(), e);
      } catch (InstantiationException e) {
        throw new CouchVersionException(e.getMessage(), e);
      }
    }
  }

  private void executeMethod(Method changesetMethod, Object changelogInstance, ChangeEntry entry) throws CouchVersionException {
    for (int i = 0; i < (entry.getRetries()+1); i++) {
      try {

        executeChangeSetMethod(changesetMethod, changelogInstance);

      } catch(CouchVersionCounterException e) {
        //if is the last retry throw the exception
        if (i == entry.getRetries()) {
          throw new CouchVersionException("All retries have failed for changeSet " + entry.getChangeId(), e);
        } else {
          logger.warn("All recounts have failed, retrying to execute changeSet " + entry.getChangeId());
        }

      } catch (IllegalAccessException e) {
        throw new CouchVersionException(e.getMessage(), e);

      } catch (InvocationTargetException e) {
        Throwable targetException = e.getTargetException();
        throw new CouchVersionException(targetException.getMessage(), e);
      }
    }
  }

  private Object executeChangeSetMethod(Method changeSetMethod, Object changeLogInstance)
      throws IllegalAccessException, InvocationTargetException, CouchVersionChangeSetVersionException {
    if (changeSetMethod.getParameterTypes().length == 1 ){
      logger.debug("method with single argument");
      if(changeSetMethod.getParameterTypes()[0].equals(Bucket.class)) {
        return changeSetMethod.invoke(changeLogInstance, bucket);

      } else {
        return changeSetMethod.invoke(changeLogInstance, cluster);
      }
    } else if (changeSetMethod.getParameterTypes().length == 2) {
      logger.debug("method with 2 params");
      if(changeSetMethod.getParameterTypes()[0].equals(Bucket.class)) {
        return changeSetMethod.invoke(changeLogInstance, bucket, cluster);
      } else {
        return changeSetMethod.invoke(changeLogInstance, cluster, bucket);
      }

    } else if (changeSetMethod.getParameterTypes().length == 0) {
      logger.debug("method with no params");
      return changeSetMethod.invoke(changeLogInstance);
    } else {
      throw new CouchVersionChangeSetVersionException("ChangeSet method " + changeSetMethod.getName() +
          " has wrong arguments list. Please see docs for more info!");
    }

  }

  private void validateConfig() throws CouchVersionConfigurationVersionException {
    if (!hasText(changeLogsScanPackage)) {
      throw new CouchVersionConfigurationVersionException("Scan package for changelogs is not set: use appropriate setter");
    }
  }


  /**
   * Package name where @ChangeLog-annotated classes are kept.
   *
   * @param changeLogsScanPackage package where your changelogs are
   * @return CouchVersion object for fluent interface
   */
  public CouchVersion setChangeLogsScanPackage(String changeLogsScanPackage) {
    this.changeLogsScanPackage = changeLogsScanPackage;
    return this;
  }

  /**
   * @return true if CouchVersion runner is enabled and able to run, otherwise false
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Feature which enables/disables CouchVersion runner execution
   *
   * @param enabled CouchVersion will run only if this option is set to true
   * @return CouchVersion object for fluent interface
   */
  public CouchVersion setEnabled(boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * Set Environment object for Spring Profiles (@Profile) integration
   *
   * @param environment org.springframework.core.env.Environment object to inject
   * @return CouchVersion object for fluent interface
   */
  public CouchVersion setSpringEnvironment(Environment environment) {
    this.springEnvironment = environment;
    return this;
  }

  /**
   * Should only be used for testing purposes
   */
  public void setDAO(CouchVersionDAO couchVersionDAO){
    this.dao = couchVersionDAO;
  }

}
