package com.example.maiwang

import com.example.maiwang.data.MetadataFetcher
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MetadataFetcherTest {

  @Test
  fun testFetchCover_success() = runTest {
    // Test fetch cover for a valid ISBN
    val coverUrl = MetadataFetcher.fetchCover("9787101133974")
    println("Cover URL: $coverUrl")
    assertNotNull(coverUrl)
    // It should fetch a real cover from isbndb instead of the local placeholder
    assertTrue(coverUrl.contains("isbndb.com") || coverUrl.contains("isbnsearch.org"))
  }

  @Test
  fun testFetchCustomSocket_success() = runTest {
    // Query China Z39.50 for Cefu Yuangui
    val record = MetadataFetcher.fetchCustomSocket("9787101133974")
    assertNotNull(record)
    println("Custom Socket Title: ${record?.title}")
    println("Custom Socket Author: ${record?.author}")
    println("Custom Socket URL: ${record?.source_url}")
    assertTrue(record?.title?.contains("册府元龟") == true)
    assertTrue(record?.author?.contains("张远山") == true)
  }

  @Test
  fun testFetchGlobalMarc_success() = runTest {
    // Query UK Z39.50 for The C Programming Language
    val record = MetadataFetcher.fetchGlobalMarc("9780131103627")
    assertNotNull(record)
    println("GlobalMARC Title: ${record?.title}")
    println("GlobalMARC Author: ${record?.author}")
    assertTrue(record?.title?.contains("C programming language") == true)
    assertTrue(record?.author?.contains("Kernighan") == true)
  }

  @Test
  fun testFetchSru_success() = runTest {
    // Query SRU for The C Programming Language
    val record = MetadataFetcher.fetchSru("9780131103627")
    assertNotNull(record)
    println("SRU Title: ${record?.title}")
    println("SRU Author: ${record?.author}")
    assertTrue(record?.title?.contains("C programming language") == true)
    assertTrue(record?.author?.contains("Kernighan") == true)
  }
}
