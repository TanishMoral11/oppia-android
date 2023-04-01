package org.oppia.android.scripts.license

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.oppia.android.testing.assertThrows
import java.io.ByteArrayOutputStream
import java.io.PrintStream

/** Tests for [LicenseTextsCheck]. */
class LicenseTextsCheckTest {
  private val WARNING_COMMENT =
    "<!-- Do not edit this file. It is generated by running RetrieveLicenseTexts.kt. -->"
  private val SCRIPT_PASSED_MESSAGE = "License texts Check Passed"
  private val LICENSE_TEXTS_CHECKED_IN_FAILURE = "License texts potentially checked into VCS"
  private val TOO_FEW_ARGS_PASSED_FAILURE = "Too few arguments passed"

  private val outContent: ByteArrayOutputStream = ByteArrayOutputStream()
  private val originalOut: PrintStream = System.out

  @field:[Rule JvmField] val tempFolder = TemporaryFolder()

  @Before
  fun setUp() {
    tempFolder.newFolder("values")
    System.setOut(PrintStream(outContent))
  }

  @After
  fun restoreStreams() {
    System.setOut(originalOut)
  }

  @Test
  fun testLicenseTexsCheck_noArgsPassed_failWithException() {
    val thirdPartyDepsXmlFile = tempFolder.newFile("values/third_party_dependencies.xml")
    thirdPartyDepsXmlFile.writeText(WARNING_COMMENT)

    val exception = assertThrows(Exception::class) {
      main(arrayOf())
    }

    assertThat(exception).hasMessageThat().contains(TOO_FEW_ARGS_PASSED_FAILURE)
  }

  @Test
  fun testLicenseTexsCheck_emptyXmlFile_checkFailsWithException() {
    val thirdPartyDepsXmlFile = tempFolder.newFile("values/third_party_dependencies.xml")
    thirdPartyDepsXmlFile.writeText("")

    val exception = assertThrows(Exception::class) {
      main(arrayOf("${tempFolder.root}/values/third_party_dependencies.xml"))
    }

    assertThat(exception).hasMessageThat().contains(LICENSE_TEXTS_CHECKED_IN_FAILURE)
  }

  @Test
  fun testLicenseTexsCheck_warningCommentNotPresent_checkFailsWithException() {
    val thirdPartyDepsXmlFile = tempFolder.newFile("values/third_party_dependencies.xml")
    thirdPartyDepsXmlFile.writeText(
      """
      <?xml version="1.0" encoding="utf-8"?>
      <resources>
        <string name="third_party_dependency_name_0">Glide</string>
      </resources>
      """.trimIndent()
    )

    val exception = assertThrows(Exception::class) {
      main(arrayOf("${tempFolder.root}/values/third_party_dependencies.xml"))
    }

    assertThat(exception).hasMessageThat().contains(LICENSE_TEXTS_CHECKED_IN_FAILURE)
  }

  @Test
  fun testLicenseTexsCheck_onlyWarningCommentPresent_checkPasses() {
    val thirdPartyDepsXmlFile = tempFolder.newFile("values/third_party_dependencies.xml")
    thirdPartyDepsXmlFile.writeText(WARNING_COMMENT)

    main(arrayOf("${tempFolder.root}/values/third_party_dependencies.xml"))

    assertThat(outContent.toString()).contains(SCRIPT_PASSED_MESSAGE)
  }

  @Test
  fun testLicenseTexsCheck_warningCommentPresentWithSomeXmlCode_checkPasses() {
    val thirdPartyDepsXmlFile = tempFolder.newFile("values/third_party_dependencies.xml")
    thirdPartyDepsXmlFile.writeText(
      """
      <?xml version="1.0" encoding="utf-8"?>
      $WARNING_COMMENT
      <resources>
        <string name="third_party_dependency_name_0">Glide</string>
      </resources>
      """.trimIndent()
    )

    main(arrayOf("${tempFolder.root}/values/third_party_dependencies.xml"))

    assertThat(outContent.toString()).contains(SCRIPT_PASSED_MESSAGE)
  }

  @Test
  fun testLicenseTexsCheck_xmlFileNotPresent_checkFailsWithFileNotFoundException() {
    val pathToThirdPartyDepsXml = "${tempFolder.root}/values/third_party_dependencies.xml"
    val exception = assertThrows(Exception::class) {
      main(arrayOf("${tempFolder.root}/values/third_party_dependencies.xml"))
    }

    assertThat(exception).hasMessageThat().contains(
      "File does not exist: $pathToThirdPartyDepsXml"
    )
  }
}
