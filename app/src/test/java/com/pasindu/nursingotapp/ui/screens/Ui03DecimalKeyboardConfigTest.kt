package com.pasindu.nursingotapp.ui.screens

import androidx.compose.ui.text.input.KeyboardType
import com.pasindu.nursingotapp.ui.components.ELECTROLYTE_INPUT_KEYBOARD_TYPE
import com.pasindu.nursingotapp.ui.components.INSULIN_INPUT_KEYBOARD_TYPE
import com.pasindu.nursingotapp.ui.components.SEDATION_INPUT_KEYBOARD_TYPE
import com.pasindu.nursingotapp.ui.parseStrictMoneyOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class Ui03DecimalKeyboardConfigTest {

    @Test
    fun decimalInputFields_useKeyboardTypeDecimal() {
        assertEquals(KeyboardType.Decimal, VASOACTIVE_INPUT_KEYBOARD_TYPE)
        assertEquals(KeyboardType.Decimal, ICU_CALCULATOR_INPUT_KEYBOARD_TYPE)
        assertEquals(KeyboardType.Decimal, PROFILE_MONEY_KEYBOARD_TYPE)
        assertEquals(KeyboardType.Decimal, DAILY_ENTRY_CUSTOM_BRUSH_KEYBOARD_TYPE)
        assertEquals(KeyboardType.Decimal, ELECTROLYTE_INPUT_KEYBOARD_TYPE)
        assertEquals(KeyboardType.Decimal, INSULIN_INPUT_KEYBOARD_TYPE)
        assertEquals(KeyboardType.Decimal, SEDATION_INPUT_KEYBOARD_TYPE)
    }

    @Test
    fun integerAndIdentifierFields_preserveKeyboardTypeNumber() {
        assertEquals(KeyboardType.Number, PROFILE_IDENTIFIER_KEYBOARD_TYPE)
        assertNotEquals(KeyboardType.Decimal, PROFILE_IDENTIFIER_KEYBOARD_TYPE)

        // Integer-only parsers (CPD points, Loan years, Gravity IV hours/minutes) must reject decimals
        assertEquals(12, "12".toIntOrNull())
        assertNull("12.5".toIntOrNull())
    }

    @Test
    fun profileAndFinanceDecimalMoneyParsing_preservesDecimalsAndValidationRules() {
        assertEquals(52809.0, parsedMoney("52809"), 0.0001)
        assertEquals(52809.75, parsedMoney("52,809.75"), 0.0001)
        assertEquals(1250.5, parsedMoney(" 1250.50 "), 0.0001)
        assertEquals(0.0, parsedMoney(""), 0.0001)
        assertEquals(0.0, parsedMoney("invalid"), 0.0001)
        assertEquals(0.0, parsedMoney("-250.50"), 0.0001)

        assertEquals(1250.75, parseStrictMoneyOrNull("1,250.75") ?: -1.0, 0.0001)
        assertEquals(0.05, parseStrictMoneyOrNull("0.05") ?: -1.0, 0.0001)
        assertNull(parseStrictMoneyOrNull(""))
        assertNull(parseStrictMoneyOrNull("-0.05"))
        assertNull(parseStrictMoneyOrNull("12.3.4"))
    }

    @Test
    fun icuAndVasoactiveCalculators_acceptDecimalInputsAndPreserveFormulas() {
        // Vasoactive: 4 mg in 50 mL = 80 mcg/mL; 0.05 mcg/kg/min for 62.5 kg = 2.34375 mL/hr
        val conc = IcuCalculatorMath.vasoactiveConcentrationMcgPerMl("4.0", "50.0")
        assertEquals(80.0, conc, 0.0001)
        val rate = IcuCalculatorMath.vasoactiveRateMlPerHr("0.05", "62.5", conc.toString())
        assertEquals(2.34375, rate, 0.0001)

        // Sedation: 0.5 mg/kg/hr for 65.5 kg at 10 mg/mL = 3.275 mL/hr
        val sedationRate = IcuCalculatorMath.sedationRateMlPerHr("0.5", "65.5", "10.0")
        assertEquals(3.275, sedationRate, 0.0001)

        // Electrolyte: 2.5 g over 1.5 hr in 100 mL = 66.6667 mL/hr, 1.6667 g/hr
        assertEquals(66.6667, IcuCalculatorMath.electrolyteRateMlPerHr("100.0", "1.5"), 0.001)
        assertEquals(1.6667, IcuCalculatorMath.electrolyteDosePerHr("2.5", "1.5"), 0.001)

        // Glycemic: 1.5 Units/hr with 50 Units in 50 mL = 1.5 mL/hr
        assertEquals(1.5, IcuCalculatorMath.insulinRateMlPerHr("1.5", "50.0", "50.0"), 0.0001)

        // Parkland: 62.5 kg * 18.5% TBSA * 4 = 4625.0 mL
        assertEquals(4625.0, IcuCalculatorMath.parklandTotalMl("62.5", "18.5"), 0.0001)

        // Cockcroft-Gault: 65 yr, 62.5 kg, serum creatinine 1.25 mg/dL (male) = 52.0833 mL/min
        assertEquals(52.0833, IcuCalculatorMath.cockcroftGaultCrCl("65", "62.5", "1.25", isFemale = false), 0.001)

        // Hemodynamics: MAP 93.3333 mmHg, cardiac output 4.5 L/min -> SVR 1481.4815
        val map = IcuCalculatorMath.map("120", "80")
        assertEquals(1481.4815, IcuCalculatorMath.svr(map, "4.5"), 0.001)

        // Negative or non-positive inputs remain safely rejected (0.0)
        assertEquals(0.0, IcuCalculatorMath.vasoactiveRateMlPerHr("-0.05", "62.5", "80.0"), 0.0001)
        assertEquals(0.0, IcuCalculatorMath.cockcroftGaultCrCl("65", "62.5", "0", isFemale = true), 0.0001)
    }
}
