package com.ledgeros.app.service

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class CsvBankStatementParserTest {
    @Test
    fun parsesDebitCreditCsvRows() {
        val csv = """
            Date,Description,Debit,Credit,Category,GST
            14/07/2025,Software subscription,88.00,,Software,8.00
            20/02/2026,Client payment,,2420.00,Sales,220.00
        """.trimIndent()

        val transactions = CsvBankStatementParser().parse(
            statementName = "sample.csv",
            content = csv,
            businessId = "business-demo",
        )

        assertEquals(2, transactions.size)
        assertEquals(-88.00, transactions[0].amount, 0.001)
        assertEquals(2420.00, transactions[1].amount, 0.001)
        assertEquals(220.00, transactions[1].gstEstimate, 0.001)
    }

    @Test
    fun infersCategoryAndGstWhenMissing() {
        val csv = """
            date,description,amount
            2025-10-09,Fuel and travel,-132.00
        """.trimIndent()

        val transaction = CsvBankStatementParser().parse(
            statementName = "sample.csv",
            content = csv,
            businessId = "business-demo",
        ).single()

        assertEquals("Motor vehicle", transaction.category)
        assertEquals(12.00, transaction.gstEstimate, 0.001)
    }

    @Test
    fun parsesTabDelimitedExcelExports() {
        val exportedSheet = """
            Date	Description	Amount	Category	GST
            2026-01-10	Office supplies	-55.00	Office supplies	5.00
        """.trimIndent()

        val transaction = AutoBankStatementParser().parse(
            statementName = "statement.xls",
            content = exportedSheet.toByteArray(),
            businessId = "business-demo",
        ).single()

        assertEquals(-55.00, transaction.amount, 0.001)
        assertEquals("Office supplies", transaction.category)
    }

    @Test
    fun parsesSimpleXlsxRows() {
        val xlsx = buildSimpleXlsx()

        val transactions = AutoBankStatementParser().parse(
            statementName = "statement.xlsx",
            content = xlsx,
            businessId = "business-demo",
        )

        assertEquals(2, transactions.size)
        assertEquals(-88.00, transactions[0].amount, 0.001)
        assertEquals(2420.00, transactions[1].amount, 0.001)
        assertEquals("Sales", transactions[1].category)
    }

    private fun buildSimpleXlsx(): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putXml(
                "xl/sharedStrings.xml",
                """
                    <sst>
                        <si><t>Date</t></si>
                        <si><t>Description</t></si>
                        <si><t>Amount</t></si>
                        <si><t>Category</t></si>
                        <si><t>GST</t></si>
                        <si><t>Software subscription</t></si>
                        <si><t>Software</t></si>
                        <si><t>Client payment</t></si>
                        <si><t>Sales</t></si>
                    </sst>
                """.trimIndent(),
            )
            zip.putXml(
                "xl/worksheets/sheet1.xml",
                """
                    <worksheet>
                        <sheetData>
                            <row r="1">
                                <c r="A1" t="s"><v>0</v></c>
                                <c r="B1" t="s"><v>1</v></c>
                                <c r="C1" t="s"><v>2</v></c>
                                <c r="D1" t="s"><v>3</v></c>
                                <c r="E1" t="s"><v>4</v></c>
                            </row>
                            <row r="2">
                                <c r="A2"><v>2025-07-14</v></c>
                                <c r="B2" t="s"><v>5</v></c>
                                <c r="C2"><v>-88.00</v></c>
                                <c r="D2" t="s"><v>6</v></c>
                                <c r="E2"><v>8.00</v></c>
                            </row>
                            <row r="3">
                                <c r="A3"><v>2026-02-20</v></c>
                                <c r="B3" t="s"><v>7</v></c>
                                <c r="C3"><v>2420.00</v></c>
                                <c r="D3" t="s"><v>8</v></c>
                                <c r="E3"><v>220.00</v></c>
                            </row>
                        </sheetData>
                    </worksheet>
                """.trimIndent(),
            )
        }
        return output.toByteArray()
    }

    private fun ZipOutputStream.putXml(name: String, xml: String) {
        putNextEntry(ZipEntry(name))
        write(xml.toByteArray())
        closeEntry()
    }
}
