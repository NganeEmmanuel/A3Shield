package com.a3shield.policymgmt.migration;

/**
 * What to do when a migration file conflicts with what was already applied
 * (ADR-0003) — for M4 that means: the version is already recorded with a
 * different checksum (e.g. an applied file was edited in place). With the
 * M5 runtime-admin layer this same setting governs file-vs-edited-policy
 * conflicts.
 */
public enum OnConflict {
    /** Abort startup; a human resolves the drift. Default. */
    FAIL,

    /** The migration file wins: re-apply and overwrite the recorded state. */
    OVERWRITE,

    /** The previously applied state wins; the file change is skipped (drift recorded in logs). */
    SKIP
}
