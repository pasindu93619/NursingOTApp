package com.pasindu.nursingotapp.data.local.seed

import com.pasindu.nursingotapp.data.local.entity.SalaryStep2027Entity

/** Exact 2026 paid/basic -> 2027 paid/basic rows supplied by the user. */
object SalaryTableSeed {
    val rows: List<SalaryStep2027Entity> = listOf(
        // Grade III, steps 1-19
        SalaryStep2027Entity(grade="Grade III", salaryStep=1, currentBasicSalary2026=51457.0, basicSalary2027=54920.0),
        SalaryStep2027Entity(grade="Grade III", salaryStep=2, currentBasicSalary2026=52133.0, basicSalary2027=55720.0),
        SalaryStep2027Entity(grade="Grade III", salaryStep=3, currentBasicSalary2026=52809.0, basicSalary2027=56520.0),
        SalaryStep2027Entity(grade="Grade III", salaryStep=4, currentBasicSalary2026=53484.0, basicSalary2027=57320.0),
        SalaryStep2027Entity(grade="Grade III", salaryStep=5, currentBasicSalary2026=54160.0, basicSalary2027=58120.0),
        SalaryStep2027Entity(grade="Grade III", salaryStep=6, currentBasicSalary2026=54836.0, basicSalary2027=58920.0),
        SalaryStep2027Entity(grade="Grade III", salaryStep=7, currentBasicSalary2026=55512.0, basicSalary2027=59720.0),
        SalaryStep2027Entity(grade="Grade III", salaryStep=8, currentBasicSalary2026=56187.0, basicSalary2027=60520.0),
        SalaryStep2027Entity(grade="Grade III", salaryStep=9, currentBasicSalary2026=56863.0, basicSalary2027=61320.0),
        SalaryStep2027Entity(grade="Grade III", salaryStep=10, currentBasicSalary2026=57539.0, basicSalary2027=62120.0),
        SalaryStep2027Entity(grade="Grade III", salaryStep=11, currentBasicSalary2026=58215.0, basicSalary2027=62920.0),
        SalaryStep2027Entity(grade="Grade III", salaryStep=12, currentBasicSalary2026=58890.0, basicSalary2027=63720.0),
        SalaryStep2027Entity(grade="Grade III", salaryStep=13, currentBasicSalary2026=59566.0, basicSalary2027=64520.0),
        SalaryStep2027Entity(grade="Grade III", salaryStep=14, currentBasicSalary2026=60242.0, basicSalary2027=65320.0),
        SalaryStep2027Entity(grade="Grade III", salaryStep=15, currentBasicSalary2026=60918.0, basicSalary2027=66120.0),
        SalaryStep2027Entity(grade="Grade III", salaryStep=16, currentBasicSalary2026=61593.0, basicSalary2027=66920.0),
        SalaryStep2027Entity(grade="Grade III", salaryStep=17, currentBasicSalary2026=62269.0, basicSalary2027=67720.0),
        SalaryStep2027Entity(grade="Grade III", salaryStep=18, currentBasicSalary2026=62945.0, basicSalary2027=68520.0),
        SalaryStep2027Entity(grade="Grade III", salaryStep=19, currentBasicSalary2026=63621.0, basicSalary2027=69320.0),

        // Grade II, steps 12-30
        SalaryStep2027Entity(grade="Grade II", salaryStep=12, currentBasicSalary2026=59219.0, basicSalary2027=64110.0),
        SalaryStep2027Entity(grade="Grade II", salaryStep=13, currentBasicSalary2026=60224.0, basicSalary2027=65300.0),
        SalaryStep2027Entity(grade="Grade II", salaryStep=14, currentBasicSalary2026=61228.0, basicSalary2027=66490.0),
        SalaryStep2027Entity(grade="Grade II", salaryStep=15, currentBasicSalary2026=62233.0, basicSalary2027=67680.0),
        SalaryStep2027Entity(grade="Grade II", salaryStep=16, currentBasicSalary2026=63237.0, basicSalary2027=68870.0),
        SalaryStep2027Entity(grade="Grade II", salaryStep=17, currentBasicSalary2026=64242.0, basicSalary2027=70060.0),
        SalaryStep2027Entity(grade="Grade II", salaryStep=18, currentBasicSalary2026=65246.0, basicSalary2027=71250.0),
        SalaryStep2027Entity(grade="Grade II", salaryStep=19, currentBasicSalary2026=66251.0, basicSalary2027=72440.0),
        SalaryStep2027Entity(grade="Grade II", salaryStep=20, currentBasicSalary2026=67255.0, basicSalary2027=73630.0),
        SalaryStep2027Entity(grade="Grade II", salaryStep=21, currentBasicSalary2026=68260.0, basicSalary2027=74820.0),
        SalaryStep2027Entity(grade="Grade II", salaryStep=22, currentBasicSalary2026=69264.0, basicSalary2027=76010.0),
        SalaryStep2027Entity(grade="Grade II", salaryStep=23, currentBasicSalary2026=70269.0, basicSalary2027=77200.0),
        SalaryStep2027Entity(grade="Grade II", salaryStep=24, currentBasicSalary2026=71273.0, basicSalary2027=78390.0),
        SalaryStep2027Entity(grade="Grade II", salaryStep=25, currentBasicSalary2026=72278.0, basicSalary2027=79580.0),
        SalaryStep2027Entity(grade="Grade II", salaryStep=26, currentBasicSalary2026=73282.0, basicSalary2027=80770.0),
        SalaryStep2027Entity(grade="Grade II", salaryStep=27, currentBasicSalary2026=74287.0, basicSalary2027=81960.0),
        SalaryStep2027Entity(grade="Grade II", salaryStep=28, currentBasicSalary2026=75291.0, basicSalary2027=83150.0),
        SalaryStep2027Entity(grade="Grade II", salaryStep=29, currentBasicSalary2026=76296.0, basicSalary2027=84340.0),
        SalaryStep2027Entity(grade="Grade II", salaryStep=30, currentBasicSalary2026=77300.0, basicSalary2027=85530.0),

        // Grade I, steps 23-42
        SalaryStep2027Entity(grade="Grade I", salaryStep=23, currentBasicSalary2026=70378.0, basicSalary2027=77330.0),
        SalaryStep2027Entity(grade="Grade I", salaryStep=24, currentBasicSalary2026=71491.0, basicSalary2027=78650.0),
        SalaryStep2027Entity(grade="Grade I", salaryStep=25, currentBasicSalary2026=72605.0, basicSalary2027=79970.0),
        SalaryStep2027Entity(grade="Grade I", salaryStep=26, currentBasicSalary2026=73718.0, basicSalary2027=81290.0),
        SalaryStep2027Entity(grade="Grade I", salaryStep=27, currentBasicSalary2026=74832.0, basicSalary2027=82610.0),
        SalaryStep2027Entity(grade="Grade I", salaryStep=28, currentBasicSalary2026=75945.0, basicSalary2027=83930.0),
        SalaryStep2027Entity(grade="Grade I", salaryStep=29, currentBasicSalary2026=77059.0, basicSalary2027=85250.0),
        SalaryStep2027Entity(grade="Grade I", salaryStep=30, currentBasicSalary2026=78172.0, basicSalary2027=86570.0),
        SalaryStep2027Entity(grade="Grade I", salaryStep=31, currentBasicSalary2026=79286.0, basicSalary2027=87890.0),
        SalaryStep2027Entity(grade="Grade I", salaryStep=32, currentBasicSalary2026=80399.0, basicSalary2027=89210.0),
        SalaryStep2027Entity(grade="Grade I", salaryStep=33, currentBasicSalary2026=81539.0, basicSalary2027=90560.0),
        SalaryStep2027Entity(grade="Grade I", salaryStep=34, currentBasicSalary2026=82679.0, basicSalary2027=91910.0),
        SalaryStep2027Entity(grade="Grade I", salaryStep=35, currentBasicSalary2026=83819.0, basicSalary2027=93260.0),
        SalaryStep2027Entity(grade="Grade I", salaryStep=36, currentBasicSalary2026=84959.0, basicSalary2027=94610.0),
        SalaryStep2027Entity(grade="Grade I", salaryStep=37, currentBasicSalary2026=86099.0, basicSalary2027=95960.0),
        SalaryStep2027Entity(grade="Grade I", salaryStep=38, currentBasicSalary2026=87239.0, basicSalary2027=97310.0),
        SalaryStep2027Entity(grade="Grade I", salaryStep=39, currentBasicSalary2026=88379.0, basicSalary2027=98660.0),
        SalaryStep2027Entity(grade="Grade I", salaryStep=40, currentBasicSalary2026=89519.0, basicSalary2027=100010.0),
        SalaryStep2027Entity(grade="Grade I", salaryStep=41, currentBasicSalary2026=90659.0, basicSalary2027=101360.0),
        SalaryStep2027Entity(grade="Grade I", salaryStep=42, currentBasicSalary2026=91799.0, basicSalary2027=102710.0)
    )
}
