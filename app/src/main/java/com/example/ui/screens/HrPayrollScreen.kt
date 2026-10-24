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
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.Translations
import com.example.designsystem.*
import com.example.domain.model.*
import java.util.*

private enum class HrTab(val titleEn: String, val titleAr: String) {
    ATTENDANCE_SHIFTS("Attendance & Shift Schedules", "الحضور والانصراف والورديات"),
    LEAVE_REQUESTS("Leave Requests & Balances", "طلبات وأرصدة الإجازات"),
    PAYROLL_FOUNDATION("Payroll & Salary Sheets", "مسيرات الرواتب والمستحقات"),
    EMPLOYEES_ESS("Employee Directory & Self-Service", "دليل الموظفين والخدمة الذاتية")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HrPayrollScreen(
    currentUser: User,
    employees: List<Employee>,
    shiftTemplates: List<ShiftTemplate>,
    attendanceRecords: List<AttendanceRecord>,
    leaveRequests: List<LeaveRequest>,
    payrollRecords: List<PayrollRecord>,
    language: AppLanguage,
    onRecordAttendanceCheckIn: (
        employeeId: String,
        employeeName: String,
        department: String,
        source: AttendanceVerificationSource,
        note: String
    ) -> Unit,
    onSubmitOrUpdateLeave: (
        leaveId: String?,
        employeeId: String,
        employeeName: String,
        leaveType: LeaveType,
        daysCount: Int,
        reason: String,
        newStatus: String
    ) -> Unit,
    onUpdatePayrollStatus: (payrollId: String, newStatus: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAr = language == AppLanguage.ARABIC
    var selectedTab by remember { mutableStateOf(HrTab.ATTENDANCE_SHIFTS) }
    val isHrPrivileged = currentUser.roleId in setOf("ROLE_ADMIN", "ROLE_MANAGER")

    var selectedSource by remember { mutableStateOf(AttendanceVerificationSource.MANUAL_SUPERVISOR_OVERRIDE) }
    var selectedLeaveType by remember { mutableStateOf(LeaveType.ANNUAL) }
    var leaveReason by remember { mutableStateOf("Family travel") }
    var leaveDays by remember { mutableStateOf("3") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("hr_payroll_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = Translations.get("nav_hr_payroll", language),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Employee Identities • Configurable Attendance Adapters • Shifts, Leave & RBAC Payroll (Phase 11)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RestoBadge(
                    text = if (isHrPrivileged) "HR ADMIN ACCESS" else "EMPLOYEE SELF-SERVICE",
                    variant = if (isHrPrivileged) BadgeVariant.SUCCESS else BadgeVariant.INFO
                )
                RestoBadge(text = "${employees.size} Staff", variant = BadgeVariant.NEUTRAL)
            }
        }

        // Scrollable Tab Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HrTab.entries.forEach { tab ->
                FilterChip(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    label = { Text(if (isAr) tab.titleAr else tab.titleEn) },
                    modifier = Modifier.testTag("hr_tab_${tab.name.lowercase()}")
                )
            }
        }

        when (selectedTab) {
            HrTab.ATTENDANCE_SHIFTS -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Honest Attendance Verification Architecture Card
                    item {
                        RestoCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Configurable Attendance Verification Architecture",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Hardware note: External biometric fingerprint reader / camera device adapters are modeled via clean service boundaries. In this cloud environment, select an adapter source to log verified attendance.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AttendanceVerificationSource.entries.forEach { src ->
                                        FilterChip(
                                            selected = selectedSource == src,
                                            onClick = { selectedSource = src },
                                            label = { Text(if (isAr) src.titleAr else src.titleEn) }
                                        )
                                    }
                                }
                                Button(
                                    onClick = {
                                        val firstEmp = employees.firstOrNull()
                                        onRecordAttendanceCheckIn(
                                            firstEmp?.id ?: "emp_01",
                                            firstEmp?.fullName ?: currentUser.fullName,
                                            firstEmp?.department ?: "Operations",
                                            selectedSource,
                                            "Verified via ${selectedSource.code}"
                                        )
                                    },
                                    modifier = Modifier.testTag("record_attendance_btn")
                                ) {
                                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Clock In Active Staff (${selectedSource.titleEn})")
                                }
                            }
                        }
                    }

                    // Shift Templates
                    item {
                        RestoCard {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Configured Shift Templates", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                shiftTemplates.forEach { shf ->
                                    Text(
                                        text = "${shf.code} • ${if (isAr) shf.nameAr else shf.nameEn}: ${shf.startTime} - ${shf.endTime} (Break: ${shf.breakDurationMinutes}m • Dept: ${shf.department})",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }

                    items(attendanceRecords, key = { it.id }) { att ->
                        RestoCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${att.employeeNumber} • ${att.employeeName} (${att.department})", fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "Date: ${att.date} • In: ${att.checkInTime} • Out: ${att.checkOutTime ?: "Active"} • Source: ${att.source.titleEn}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    RestoBadge(
                                        text = att.status,
                                        variant = if (att.status == "PRESENT") BadgeVariant.SUCCESS else BadgeVariant.WARNING
                                    )
                                    if (att.overtimeMinutes > 0) {
                                        Text("+${att.overtimeMinutes}m OT", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HrTab.LEAVE_REQUESTS -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        RestoCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Submit Employee Leave Request", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    LeaveType.entries.forEach { lt ->
                                        FilterChip(
                                            selected = selectedLeaveType == lt,
                                            onClick = { selectedLeaveType = lt },
                                            label = { Text(lt.titleEn) }
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = leaveDays,
                                        onValueChange = { leaveDays = it },
                                        label = { Text("Days") },
                                        singleLine = true,
                                        modifier = Modifier.width(90.dp)
                                    )
                                    OutlinedTextField(
                                        value = leaveReason,
                                        onValueChange = { leaveReason = it },
                                        label = { Text("Leave Reason") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(
                                        onClick = {
                                            onSubmitOrUpdateLeave(
                                                null,
                                                "emp_04",
                                                currentUser.fullName,
                                                selectedLeaveType,
                                                leaveDays.toIntOrNull() ?: 2,
                                                leaveReason,
                                                "PENDING"
                                            )
                                        },
                                        modifier = Modifier.testTag("submit_leave_btn")
                                    ) {
                                        Text("Request Leave")
                                    }
                                }
                            }
                        }
                    }

                    items(leaveRequests, key = { it.id }) { lv ->
                        RestoCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${lv.employeeName} • ${lv.leaveType.titleEn} (${lv.daysCount} days)", fontWeight = FontWeight.Bold)
                                    Text("${lv.startDate} to ${lv.endDate} • Reason: ${lv.reason} • Remaining Balance: ${lv.remainingBalanceDays}d", style = MaterialTheme.typography.bodySmall)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    RestoBadge(
                                        text = lv.status,
                                        variant = if (lv.status == "APPROVED") BadgeVariant.SUCCESS else BadgeVariant.WARNING
                                    )
                                    if (isHrPrivileged && lv.status == "PENDING") {
                                        Button(
                                            onClick = {
                                                onSubmitOrUpdateLeave(lv.id, lv.employeeId, lv.employeeName, lv.leaveType, lv.daysCount, lv.reason, "APPROVED")
                                            },
                                            modifier = Modifier.testTag("approve_leave_${lv.id}")
                                        ) {
                                            Text("Approve")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HrTab.PAYROLL_FOUNDATION -> {
                if (!isHrPrivileged) {
                    RestoCard {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Restricted Sensitive Payroll Data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            Text("Only HR Administrators and Branch Managers have permission to view company-wide salary components and approve payroll sheets.")
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(payrollRecords, key = { it.id }) { pay ->
                            RestoCard {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                        Text("${pay.employeeNumber} • ${pay.employeeName} (${pay.jobTitle})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        RestoBadge(
                                            text = pay.status,
                                            variant = if (pay.status == "PAID") BadgeVariant.SUCCESS else BadgeVariant.INFO
                                        )
                                    }
                                    Text(
                                        text = "Period: ${pay.payrollPeriod} • Base: $${pay.baseSalary} • Housing: $${pay.housingAllowance} • Transport: $${pay.transportAllowance} • OT: $${pay.overtimePay} • Deductions: -$${pay.deductions + pay.salaryAdvanceDeduction}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                        Text(
                                            text = "Net Payable: $${String.format(Locale.US, "%.2f", pay.netSalary)}",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (pay.status == "DRAFT") {
                                                Button(onClick = { onUpdatePayrollStatus(pay.id, "APPROVED") }) {
                                                    Text("Approve Payroll")
                                                }
                                            }
                                            if (pay.status != "PAID") {
                                                OutlinedButton(
                                                    onClick = { onUpdatePayrollStatus(pay.id, "PAID") },
                                                    modifier = Modifier.testTag("pay_salary_${pay.id}")
                                                ) {
                                                    Text("Mark Disbursed (PAID)")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HrTab.EMPLOYEES_ESS -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(employees, key = { it.id }) { emp ->
                        RestoCard {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${emp.employeeNumber} • ${emp.fullName}", fontWeight = FontWeight.Bold)
                                    Text("Department: ${emp.department} • Job Title: ${emp.jobTitle} • Phone: ${emp.phone}", style = MaterialTheme.typography.bodySmall)
                                    Text("Integrated across: POS • Waiter • Kitchen • Inventory • Audit Trail", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    RestoBadge(text = emp.status, variant = BadgeVariant.SUCCESS)
                                    if (isHrPrivileged) {
                                        Text("Rate: $${emp.hourlyRate}/hr", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
