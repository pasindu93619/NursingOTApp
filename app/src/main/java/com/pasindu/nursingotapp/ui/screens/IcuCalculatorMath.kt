package com.pasindu.nursingotapp.ui.screens

import kotlin.math.round

/** Pure deterministic ICU calculation helpers. UI should only collect/format inputs and results. */
object IcuCalculatorMath {
    fun vasoactiveConcentrationMcgPerMl(mg: String, volumeMl: String): Double {
        val mass = mg.toDoubleOrNull() ?: 0.0
        val volume = volumeMl.toDoubleOrNull() ?: 0.0
        return if (mass > 0.0 && volume > 0.0) (mass * 1000.0) / volume else 0.0
    }

    fun vasoactiveRateMlPerHr(doseMcgKgMin: String, weightKg: String, concentrationMcgMl: String): Double {
        val dose = doseMcgKgMin.toDoubleOrNull() ?: 0.0
        val weight = weightKg.toDoubleOrNull() ?: 0.0
        val concentration = concentrationMcgMl.toDoubleOrNull() ?: 0.0
        return if (dose > 0.0 && weight > 0.0 && concentration > 0.0) {
            (dose * weight * 60.0) / concentration
        } else 0.0
    }

    fun vasoactiveDoseMcgKgMin(rateMlHr: String, weightKg: String, concentrationMcgMl: String): Double {
        val rate = rateMlHr.toDoubleOrNull() ?: 0.0
        val weight = weightKg.toDoubleOrNull() ?: 0.0
        val concentration = concentrationMcgMl.toDoubleOrNull() ?: 0.0
        return if (rate > 0.0 && weight > 0.0 && concentration > 0.0) {
            (rate * concentration) / (weight * 60.0)
        } else 0.0
    }

    fun sedationRateMlPerHr(doseMgKgHr: String, weightKg: String, concentrationMgMl: String): Double {
        val dose = doseMgKgHr.toDoubleOrNull() ?: 0.0
        val weight = weightKg.toDoubleOrNull() ?: 0.0
        val concentration = concentrationMgMl.toDoubleOrNull() ?: 0.0
        return if (dose > 0.0 && weight > 0.0 && concentration > 0.0) {
            (dose * weight) / concentration
        } else 0.0
    }

    fun electrolyteRateMlPerHr(volumeMl: String, timeHr: String): Double {
        val volume = volumeMl.toDoubleOrNull() ?: 0.0
        val time = timeHr.toDoubleOrNull() ?: 0.0
        return if (volume > 0.0 && time > 0.0) volume / time else 0.0
    }

    fun electrolyteDosePerHr(dose: String, timeHr: String): Double {
        val amount = dose.toDoubleOrNull() ?: 0.0
        val time = timeHr.toDoubleOrNull() ?: 0.0
        return if (amount > 0.0 && time > 0.0) amount / time else 0.0
    }

    fun insulinRateMlPerHr(targetUnitsHr: String, insulinUnits: String, diluentMl: String): Double {
        val target = targetUnitsHr.toDoubleOrNull() ?: 0.0
        val units = insulinUnits.toDoubleOrNull() ?: 0.0
        val volume = diluentMl.toDoubleOrNull() ?: 0.0
        return if (target > 0.0 && units > 0.0 && volume > 0.0) {
            target / (units / volume)
        } else 0.0
    }

    fun parklandTotalMl(weightKg: String, tbsaPercent: String): Double {
        val weight = weightKg.toDoubleOrNull() ?: 0.0
        val tbsa = tbsaPercent.toDoubleOrNull() ?: 0.0
        return if (weight > 0.0 && tbsa > 0.0) 4.0 * weight * tbsa else 0.0
    }

    fun parklandFirstEightHoursRateMlPerHr(totalMl: Double): Double = if (totalMl > 0.0) (totalMl / 2.0) / 8.0 else 0.0

    fun parklandNextSixteenHoursRateMlPerHr(totalMl: Double): Double = if (totalMl > 0.0) (totalMl / 2.0) / 16.0 else 0.0

    fun cockcroftGaultCrCl(ageYears: String, weightKg: String, creatinineMgDl: String, isFemale: Boolean): Double {
        val age = ageYears.toDoubleOrNull() ?: 0.0
        val weight = weightKg.toDoubleOrNull() ?: 0.0
        val creatinine = creatinineMgDl.toDoubleOrNull() ?: 0.0
        if (age <= 0.0 || weight <= 0.0 || creatinine <= 0.0) return 0.0
        val base = ((140.0 - age) * weight) / (72.0 * creatinine)
        return if (isFemale) base * 0.85 else base
    }

    fun map(systolic: String, diastolic: String): Double {
        val sbp = systolic.toDoubleOrNull() ?: 0.0
        val dbp = diastolic.toDoubleOrNull() ?: 0.0
        return if (sbp > 0.0 && dbp > 0.0) (sbp + (2.0 * dbp)) / 3.0 else 0.0
    }

    fun svr(map: Double, cardiacOutputLMin: String): Double {
        val co = cardiacOutputLMin.toDoubleOrNull() ?: 0.0
        return if (map > 0.0 && co > 0.0) ((map - 10.0) * 80.0) / co else 0.0
    }

    fun round1(value: Double): Double = round(value * 10.0) / 10.0
    fun round0(value: Double): Double = round(value)
}
