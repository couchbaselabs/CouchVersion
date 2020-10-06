package com.github.couchversion.dao;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.ExistsResult;
import com.couchbase.client.java.query.QueryOptions;
import com.couchbase.client.java.query.QueryScanConsistency;
import com.github.couchversion.changeset.ChangeEntry;
import com.github.couchversion.utils.Partition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author deniswsrosa
 */
public class CouchVersionDAO {
  private static final Logger logger = LoggerFactory.getLogger("CouchVersion DAO");

  private BucketWrapper bucketWrapper;
  private Bucket bucket;
  private Cluster cluster;

  private final String lockId = "couchversion_lock";

  public CouchVersionDAO(Cluster cluster, Bucket bucket) {
    this.cluster = cluster;
    this.bucket = bucket;
    this.bucketWrapper = new BucketWrapper(bucket);
  }

  public boolean isNewChange(ChangeEntry changeEntry) {
    ExistsResult entry = bucket.defaultCollection().exists(changeEntry._getId());
    return !entry.exists();
  }

  public boolean hasNewChanges(List<String> changeEntries) {
    List<List<String>> partitions = Partition.ofSize(changeEntries, 50);

    for(List<String> partition: partitions) {
      String query = "select count(*) as val from "+bucket.name()+" USE KEYS ["+
              partition.stream()
                      .map(e-> "'"+e+"'")
                      .collect(Collectors.joining(","))
              +"]";

      Long val = cluster.query(query, QueryOptions.queryOptions()
              .scanConsistency(QueryScanConsistency.REQUEST_PLUS) )
              .rowsAs(SingleResultLongVO.class).get(0).getVal();

      //has new changes, there are ids in the code that are not in the database
      if(val < partition.size()) {
        return true;
      }
    }
    return false;
  }

  public void save(ChangeEntry changeEntry) {
    bucketWrapper.insert(changeEntry);
  }

  public boolean isLocked() {
    return bucket.defaultCollection().exists(lockId).exists();
  }

  public boolean getLock(Long timestamp) {
    bucket.defaultCollection().insert(lockId, new CouchVersionLock(timestamp));

    //sanity check to be sure that we got the lock
    CouchVersionLock lock = bucket.defaultCollection().get(lockId).contentAs(CouchVersionLock.class);
    return lock.getLockTimestamp().longValue() == timestamp.longValue();
  }


  public void releaseLock(Long timestamp) {
    CouchVersionLock lock = bucket.defaultCollection().get(lockId).contentAs(CouchVersionLock.class);

    if( lock.getLockTimestamp() == timestamp) {
      throw new IllegalStateException("Lock is invalid, expectedTimestamp = "+timestamp+" lockTimestamp="+lock.getLockTimestamp());
    }

    bucket.defaultCollection().remove(lockId);
  }
}
