package com.pasindu.nursingotapp.data.paysheet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * P-01 verification tests for Pay Sheet Vault backup exclusion across AndroidManifest.xml,
 * backup_rules.xml (Android 11 and below), data_extraction_rules.xml (Android 12+ cloud-backup
 * and device-transfer), and PaySheetVaultManager storage root alignment.
 */
class PaySheetBackupExclusionRulesTest {

    private data class BackupRule(
        val tag: String,
        val domain: String,
        val path: String
    )

    private fun resolveFile(relativePath: String): File {
        val candidates = listOf(
            File(relativePath),
            File("app", relativePath),
            File("..", relativePath)
        )
        return candidates.firstOrNull { it.exists() && it.isFile }
            ?: error("Could not locate file: $relativePath")
    }

    private fun parseRules(parent: Element): List<BackupRule> {
        val children: NodeList = parent.childNodes
        val rules = mutableListOf<BackupRule>()
        for (i in 0 until children.length) {
            val node = children.item(i)
            if (node is Element && (node.tagName == "include" || node.tagName == "exclude")) {
                rules += BackupRule(
                    tag = node.tagName,
                    domain = node.getAttribute("domain"),
                    path = node.getAttribute("path")
                )
            }
        }
        return rules
    }

    @Test
    fun androidManifest_referencesBothBackupRulesAndDataExtractionRules() {
        val manifestFile = resolveFile("src/main/AndroidManifest.xml")
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(manifestFile)
        val appNodes = doc.getElementsByTagName("application")
        assertEquals(1, appNodes.length)
        val appElement = appNodes.item(0) as Element

        assertEquals("true", appElement.getAttribute("android:allowBackup"))
        assertEquals("@xml/backup_rules", appElement.getAttribute("android:fullBackupContent"))
        assertEquals("@xml/data_extraction_rules", appElement.getAttribute("android:dataExtractionRules"))
    }

    @Test
    fun backupRulesXml_excludesPaySheetVaultAndPreservesDatabaseAndSharedPrefExclusions() {
        val file = resolveFile("src/main/res/xml/backup_rules.xml")
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val root = doc.documentElement
        assertEquals("full-backup-content", root.tagName)

        val rules = parseRules(root)
        assertTrue(rules.contains(BackupRule("exclude", "database", ".")))
        assertTrue(rules.contains(BackupRule("exclude", "sharedpref", ".")))
        assertTrue(rules.contains(BackupRule("exclude", "file", "paysheet_vault")))
        assertFalse(rules.any { it.tag == "include" && it.domain == "file" })
    }

    @Test
    fun dataExtractionRulesXml_excludesPaySheetVaultFromCloudBackupAndDeviceTransfer() {
        val file = resolveFile("src/main/res/xml/data_extraction_rules.xml")
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val root = doc.documentElement
        assertEquals("data-extraction-rules", root.tagName)

        val cloudBackupNodes = root.getElementsByTagName("cloud-backup")
        assertEquals(1, cloudBackupNodes.length)
        val cloudRules = parseRules(cloudBackupNodes.item(0) as Element)
        assertTrue(cloudRules.contains(BackupRule("exclude", "database", ".")))
        assertTrue(cloudRules.contains(BackupRule("exclude", "sharedpref", ".")))
        assertTrue(cloudRules.contains(BackupRule("exclude", "file", "paysheet_vault")))
        assertFalse(cloudRules.any { it.tag == "include" && it.domain == "file" })

        val deviceTransferNodes = root.getElementsByTagName("device-transfer")
        assertEquals(1, deviceTransferNodes.length)
        val transferRules = parseRules(deviceTransferNodes.item(0) as Element)
        assertTrue(transferRules.contains(BackupRule("include", "database", ".")))
        assertTrue(transferRules.contains(BackupRule("include", "sharedpref", ".")))
        assertTrue(transferRules.contains(BackupRule("include", "file", ".")))
        assertTrue(transferRules.contains(BackupRule("exclude", "file", "paysheet_vault")))
        assertFalse(transferRules.any { it.tag == "include" && it.domain == "file" && it.path == "paysheet_vault" })
    }

    @Test
    fun paySheetVaultManager_rootDirectoryMatchesExcludedFileDomainPath() {
        val managerSource = resolveFile("src/main/java/com/pasindu/nursingotapp/data/paysheet/PaySheetVaultManager.kt")
            .readText()
        assertTrue(
            "Expected PaySheetVaultManager to store vault files in File(context.filesDir, \"paysheet_vault\")",
            managerSource.contains("File(context.filesDir, \"paysheet_vault\")")
        )
    }
}
