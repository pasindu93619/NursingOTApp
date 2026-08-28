package com.pasindu.nursingotapp.data.local

import com.pasindu.nursingotapp.data.local.entity.SalaryStep2027Entity

/** Exact 2026-paid -> 2027-paid mappings transcribed from the supplied salary table PDF. */
object SalaryTable2026_2027Seed {
    val rows: List<SalaryStep2027Entity> = listOf(
        // Grade III
        row("III", 1, 51457, 54920), row("III", 2, 52133, 55720), row("III", 3, 52809, 56520),
        row("III", 4, 53484, 57320), row("III", 5, 54160, 58120), row("III", 6, 54836, 58920),
        row("III", 7, 55512, 59720), row("III", 8, 56187, 60520), row("III", 9, 56863, 61320),
        row("III", 10, 57539, 62120), row("III", 11, 58215, 62920), row("III", 12, 58890, 63720),
        row("III", 13, 59566, 64520), row("III", 14, 60242, 65320), row("III", 15, 60918, 66120),
        row("III", 16, 61593, 66920), row("III", 17, 62269, 67720), row("III", 18, 62945, 68520),
        row("III", 19, 63621, 69320),
        // Grade II
        row("II", 12, 59219, 64110), row("II", 13, 60224, 65300), row("II", 14, 61228, 66490),
        row("II", 15, 62233, 67680), row("II", 16, 63237, 68870), row("II", 17, 64242, 70060),
        row("II", 18, 65246, 71250), row("II", 19, 66251, 72440), row("II", 20, 67255, 73630),
        row("II", 21, 68260, 74820), row("II", 22, 69264, 76010), row("II", 23, 70269, 77200),
        row("II", 24, 71273, 78390), row("II", 25, 72278, 79580), row("II", 26, 73282, 80770),
        row("II", 27, 74287, 81960), row("II", 28, 75291, 83150), row("II", 29, 76296, 84340),
        row("II", 30, 77300, 85530),
        // Grade I
        row("I", 23, 70378, 77330), row("I", 24, 71491, 78650), row("I", 25, 72605, 79970),
        row("I", 26, 73718, 81290), row("I", 27, 74832, 82610), row("I", 28, 75945, 83930),
        row("I", 29, 77059, 85250), row("I", 30, 78172, 86570), row("I", 31, 79286, 87890),
        row("I", 32, 80399, 89210), row("I", 33, 81539, 90560), row("I", 34, 82679, 91910),
        row("I", 35, 83819, 93260), row("I", 36, 84959, 94610), row("I", 37, 86099, 95960),
        row("I", 38, 87239, 97310), row("I", 39, 88379, 98660), row("I", 40, 89519, 100010),
        row("I", 41, 90659, 101360), row("I", 42, 91799, 102710)
    )

    private fun row(grade: String, step: Int, current2026: Int, basic2027: Int) = SalaryStep2027Entity(
        grade = grade,
        salaryStep = step,
        currentBasicSalary2026 = current2026.toDouble(),
        basicSalary2027 = basic2027.toDouble()
    )
}
