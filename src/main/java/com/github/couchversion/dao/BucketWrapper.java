package com.github.couchversion.dao;

import com.couchbase.client.java.Bucket;
import com.github.couchversion.changeset.ChangeEntry;

/**
 * This class is a wrapper for couchbase bucket, its main purpose is to make tests easier
 */
public class BucketWrapper {

  private Bucket bucket;

  public BucketWrapper(Bucket bucket) {
    this.bucket = bucket;
  }

  public void insert(ChangeEntry changeEntry){
    bucket.defaultCollection().insert(changeEntry._getId(), changeEntry);
  }

}
