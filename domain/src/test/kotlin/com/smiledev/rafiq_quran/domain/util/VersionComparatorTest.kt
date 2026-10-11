package com.smiledev.rafiq_quran.domain.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionComparatorTest {

    @Test
    fun isNewer_newerPatchVersion_returnsTrue() {
        assertTrue(VersionComparator.isNewer("1.0.73", "1.0.74"))
        assertTrue(VersionComparator.isNewer("v1.0.73", "v1.0.74"))
        assertTrue(VersionComparator.isNewer("1.0.73", "v1.0.74"))
    }

    @Test
    fun isNewer_baseVersionAgainstExtended_returnsTrue() {
        assertTrue(VersionComparator.isNewer("1.0", "1.0.74"))
        assertTrue(VersionComparator.isNewer("1.0", "v1.0.1"))
    }

    @Test
    fun isNewer_newerMinorOrMajorVersion_returnsTrue() {
        assertTrue(VersionComparator.isNewer("1.0.74", "1.1.0"))
        assertTrue(VersionComparator.isNewer("1.9.9", "2.0.0"))
    }

    @Test
    fun isNewer_equalVersions_returnsFalse() {
        assertFalse(VersionComparator.isNewer("1.0.74", "1.0.74"))
        assertFalse(VersionComparator.isNewer("v1.0.74", "1.0.74"))
        assertFalse(VersionComparator.isNewer("1.0.74", "v1.0.74"))
        assertFalse(VersionComparator.isNewer("1.0", "1.0"))
    }

    @Test
    fun isNewer_olderVersion_returnsFalse() {
        assertFalse(VersionComparator.isNewer("1.0.75", "1.0.74"))
        assertFalse(VersionComparator.isNewer("2.0.0", "1.0.74"))
        assertFalse(VersionComparator.isNewer("1.1.0", "1.0.99"))
    }

    @Test
    fun isNewer_handlesSuffixes() {
        assertFalse(VersionComparator.isNewer("1.0.74-debug", "1.0.74"))
        assertTrue(VersionComparator.isNewer("1.0-staging", "1.0.1"))
    }

    @Test
    fun isNewer_invalidOrEmpty_returnsSafeDefaults() {
        assertFalse(VersionComparator.isNewer("1.0.0", ""))
        assertTrue(VersionComparator.isNewer("", "1.0.0"))
    }
}
