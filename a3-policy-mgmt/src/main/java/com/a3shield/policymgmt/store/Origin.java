package com.a3shield.policymgmt.store;

/** Where an effective policy came from — migration files or runtime admin edits. */
public enum Origin {
    MIGRATION,

    /** Created/modified at runtime through the admin API; files never silently overwrite these. */
    ADMIN
}
