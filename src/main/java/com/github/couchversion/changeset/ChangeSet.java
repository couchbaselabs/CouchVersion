package com.github.couchversion.changeset;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Set of changes to be added to the DB. Many changesets are included in one changelog.
 *
 * @author deniswsrosa
 * @see ChangeLog
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ChangeSet {

    /**
     * Author of the changeset.
     * Obligatory
     *
     * @return author
     */
    String author();  // must be set

    /**
     * Unique ID of the changeset.
     * Obligatory
     *
     * @return unique id
     */
    String id();      // must be set

    /**
     * Sequence that provide correct order for changesets. Sorted alphabetically, ascending.
     * Obligatory.
     *
     * @return ordering
     */
    String order();   // must be set

    /**
     * Executes the change set on every couchversion's execution, even if it has been run before.
     * Optional (default is false)
     *
     * @return should run always?
     */
    boolean runAlways() default false;

    int retries() default 0;

    /**
     * Executes the change set after an interruption, such as an application shutdown, or server a crash.
     * Optional (default is true)
     *
     * @return should restart after interruption?
     */
    boolean restartInterrupted() default true;

}
