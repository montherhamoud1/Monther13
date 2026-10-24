package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.engine.AccountingEngine
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.Translations
import com.example.designsystem.*
import com.example.domain.model.*
import java.util.*

private enum class AccountingTab(val titleEn: String, val titleAr: String) {
    STATEMENTS("Trial Balance, P&L & Balance Sheet", "ميزان المراجعة والقوائم المالية"),
    JOURNAL_LEDGER("Double-Entry Journal Ledger", "دفتر القيود اليومية المزدوجة"),
    CHART_OF_ACCOUNTS("Chart of Accounts & Periods", "دليل الحسابات والفترات المالية"),
    EXPENSES_BANK("Expenses & Bank Accounts", "المصروفات والحسابات البنكية"),
    AR_AP_AGING("Receivables (AR) & Payables (AP)", "الذمم المدينة والدائنة وأعمار الديون")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountingScreen(
    accounts: List<ChartOfAccount>,
    costCenters: List<CostCenter>,
    financialPeriods: List<FinancialPeriod>,
    journalEntries: List<JournalEntry>,
    expenses: List<Expense>,
    bankAccounts: List<BankAccount>,
    subledgerInvoices: List<SubledgerInvoice>,
    language: AppLanguage,
    onRecordExpense: (
        category: String,
        description: String,
        amount: Double,
        costCenterCode: String,
        paymentMethod: PaymentMethod
    ) -> Unit,
    onSettleInvoice: (invoiceId: String, paymentAmount: Double) -> Unit,
    onUpdatePeriodStatus: (periodId: String, status: FinancialPeriodStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAr = language == AppLanguage.ARABIC
    var selectedTab by remember { mutableStateOf(AccountingTab.STATEMENTS) }

    val fin = remember(accounts, journalEntries) {
        AccountingEngine.computeFinancialOverview(accounts, journalEntries)
    }

    // Expense form states
    var expCategory by remember { mutableStateOf("UTILITIES") }
    var expDesc by remember { mutableStateOf("") }
    var expAmount by remember { mutableStateOf("250.00") }
    var expCostCenter by remember { mutableStateOf("CC-BR01") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("accounting_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header & Financial Health Badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = Translations.get("nav_accounting", language),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Automated Double-Entry Accounting • Source-Document Traceability • Trial Balance, P&L & AR/AP (Phase 10)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RestoBadge(
                    text = "Net Profit: $${String.format(Locale.US, "%.2f", fin.netProfit)}",
                    variant = if (fin.netProfit >= 0) BadgeVariant.SUCCESS else BadgeVariant.DANGER
                )
                RestoBadge(
                    text = "${journalEntries.size} Posted Journals",
                    variant = BadgeVariant.INFO
                )
            }
        }

        // Scrollable Tab Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AccountingTab.entries.forEach { tab ->
                FilterChip(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    label = { Text(if (isAr) tab.titleAr else tab.titleEn) },
                    modifier = Modifier.testTag("accounting_tab_${tab.name.lowercase()}")
                )
            }
        }

        when (selectedTab) {
            AccountingTab.STATEMENTS -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Profit & Loss Statement Card
                    item {
                        val grossMarginPct = if (fin.grossRevenue > 0.0) (fin.grossProfit / fin.grossRevenue) * 100.0 else 0.0
                        val netMarginPct = if (fin.grossRevenue > 0.0) (fin.netProfit / fin.grossRevenue) * 100.0 else 0.0
                        RestoCard {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Profit & Loss (Income Statement)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                HorizontalDivider()
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                    Text("Gross Sales Revenue (4100-4900):")
                                    Text("$${String.format(Locale.US, "%.2f", fin.grossRevenue)}", fontWeight = FontWeight.SemiBold)
                                }
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                    Text("Less: Cost of Goods Sold / Recipe COGS (5100-5900):")
                                    Text("-$${String.format(Locale.US, "%.2f", fin.totalCogs)}")
                                }
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                    Text("Gross Profit (${String.format(Locale.US, "%.1f", grossMarginPct)}%):", fontWeight = FontWeight.Bold)
                                    Text("$${String.format(Locale.US, "%.2f", fin.grossProfit)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                    Text("Less: Operating Expenses (6100-6900):")
                                    Text("-$${String.format(Locale.US, "%.2f", fin.totalOperatingExpenses)}")
                                }
                                HorizontalDivider()
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                    Text("Net Profit (${String.format(Locale.US, "%.1f", netMarginPct)}%):", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("$${String.format(Locale.US, "%.2f", fin.netProfit)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    // Balance Sheet Summary Card
                    item {
                        RestoCard {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Text("Balance Sheet & Trial Balance Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    RestoBadge(
                                        text = if (fin.isTrialBalanceInBalance) "DEBITS = CREDITS ($${String.format(Locale.US, "%.2f", fin.totalDebits)})" else "VARIANCE",
                                        variant = if (fin.isTrialBalanceInBalance) BadgeVariant.SUCCESS else BadgeVariant.WARNING
                                    )
                                }
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                    Text("Total Assets: $${String.format(Locale.US, "%.2f", fin.totalAssets)}", fontWeight = FontWeight.Bold)
                                    Text("Total Liabilities: $${String.format(Locale.US, "%.2f", fin.totalLiabilities)}", fontWeight = FontWeight.Bold)
                                    Text("Equity + Net Profit: $${String.format(Locale.US, "%.2f", fin.totalEquity + fin.netProfit)}", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Trial Balance Table
                    item {
                        Text("General Ledger Trial Balance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(accounts, key = { it.id }) { acc ->
                        RestoCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${acc.accountCode} • ${if (isAr) acc.nameAr else acc.nameEn}", fontWeight = FontWeight.Bold)
                                    Text("Type: ${acc.accountType.titleEn} (${acc.accountType.normalBalance})", style = MaterialTheme.typography.bodySmall)
                                }
                                Text("Balance: $${String.format(Locale.US, "%.2f", acc.balance)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            AccountingTab.JOURNAL_LEDGER -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(journalEntries, key = { it.id }) { je ->
                        RestoCard {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Text("${je.entryNumber} • ${if (isAr) je.descriptionAr else je.descriptionEn}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    RestoBadge(
                                        text = if (je.isBalanced) "BALANCED ($${String.format(Locale.US, "%.2f", je.totalDebit)})" else "UNBALANCED",
                                        variant = if (je.isBalanced) BadgeVariant.SUCCESS else BadgeVariant.DANGER
                                    )
                                }
                                Text(
                                    text = "Source Document: ${je.sourceDocumentType} #${je.referenceNumber} (${je.sourceDocumentId}) • Cost Center: ${je.costCenterCode}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                HorizontalDivider()
                                je.lines.forEach { ln ->
                                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                        Text("${ln.accountCode} ${if (isAr) ln.accountNameAr else ln.accountNameEn} (${ln.memo})", style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            text = if (ln.debit > 0) "DR $${String.format(Locale.US, "%.2f", ln.debit)}" else "CR $${String.format(Locale.US, "%.2f", ln.credit)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AccountingTab.CHART_OF_ACCOUNTS -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        RestoCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Financial Periods & Cost Centers", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                financialPeriods.forEach { fp ->
                                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                        Text("${fp.periodCode} • ${if (isAr) fp.nameAr else fp.nameEn} (${fp.startDate} to ${fp.endDate})", fontWeight = FontWeight.Medium)
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            FinancialPeriodStatus.entries.forEach { st ->
                                                FilterChip(
                                                    selected = fp.status == st,
                                                    onClick = { onUpdatePeriodStatus(fp.id, st) },
                                                    label = { Text(st.titleEn) }
                                                )
                                            }
                                        }
                                    }
                                }
                                Text(
                                    text = "Cost Centers: " + costCenters.joinToString(" • ") { "${it.code} (${it.nameEn})" },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    items(accounts, key = { it.id }) { acc ->
                        RestoCard {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Column {
                                    Text("${acc.accountCode} • ${if (isAr) acc.nameAr else acc.nameEn}", fontWeight = FontWeight.Bold)
                                    Text("${acc.accountType.titleEn} • Normal Balance: ${acc.accountType.normalBalance}", style = MaterialTheme.typography.bodySmall)
                                }
                                Text("Opening/Base: $${String.format(Locale.US, "%.2f", acc.balance)}", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            AccountingTab.EXPENSES_BANK -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        RestoCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Post Operating Expense (Auto-Generates Double-Entry Journal)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("UTILITIES", "RENT", "MARKETING", "MAINTENANCE").forEach { cat ->
                                        FilterChip(
                                            selected = expCategory == cat,
                                            onClick = { expCategory = cat },
                                            label = { Text(cat) }
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = expDesc,
                                        onValueChange = { expDesc = it },
                                        label = { Text("Expense Description") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = expAmount,
                                        onValueChange = { expAmount = it },
                                        label = { Text("Amount ($)") },
                                        singleLine = true,
                                        modifier = Modifier.width(120.dp)
                                    )
                                    Button(
                                        onClick = {
                                            val amt = expAmount.toDoubleOrNull() ?: 0.0
                                            if (amt > 0.0) {
                                                onRecordExpense(
                                                    expCategory,
                                                    expDesc.ifBlank { "$expCategory voucher" },
                                                    amt,
                                                    expCostCenter,
                                                    PaymentMethod.CREDIT_CARD
                                                )
                                                expDesc = ""
                                            }
                                        },
                                        modifier = Modifier.testTag("post_expense_btn")
                                    ) {
                                        Text("Post Expense")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text("Corporate Bank Accounts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(bankAccounts, key = { it.id }) { bank ->
                        RestoCard {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Column {
                                    Text("${if (isAr) bank.accountNameAr else bank.accountNameEn} (${bank.bankName})", fontWeight = FontWeight.Bold)
                                    Text("IBAN: ${bank.ibanNumber} • GL Account: ${bank.glAccountCode}", style = MaterialTheme.typography.bodySmall)
                                }
                                Text("$${String.format(Locale.US, "%.2f", bank.currentBalance)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    item {
                        Text("Posted Operating Expenses (${expenses.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(expenses, key = { it.id }) { exp ->
                        RestoCard {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Column {
                                    Text("${exp.documentNumber} • ${exp.category}: ${exp.description}", fontWeight = FontWeight.Bold)
                                    Text("Cost Center: ${exp.costCenterCode} • Approved by: ${exp.approvedBy} • Journal: ${exp.journalEntryId ?: "Linked"}", style = MaterialTheme.typography.bodySmall)
                                }
                                Text("$${String.format(Locale.US, "%.2f", exp.amount)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            AccountingTab.AR_AP_AGING -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(subledgerInvoices, key = { it.id }) { inv ->
                        RestoCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        RestoBadge(
                                            text = inv.ledgerType,
                                            variant = if (inv.ledgerType == "RECEIVABLE") BadgeVariant.INFO else BadgeVariant.WARNING
                                        )
                                        Text("${inv.invoiceNumber} • ${inv.partyName}", fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        text = "Source: ${inv.sourceDocumentId} • Aging: ${inv.agingBucket} (${inv.daysPastDue}d past due) • Total: $${inv.totalAmount} • Balance: $${inv.remainingBalance}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                if (inv.remainingBalance > 0.0) {
                                    Button(
                                        onClick = { onSettleInvoice(inv.id, inv.remainingBalance) },
                                        modifier = Modifier.testTag("settle_inv_${inv.id}")
                                    ) {
                                        Text("Settle ($${String.format(Locale.US, "%.2f", inv.remainingBalance)})")
                                    }
                                } else {
                                    RestoBadge(text = "SETTLED", variant = BadgeVariant.SUCCESS)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
