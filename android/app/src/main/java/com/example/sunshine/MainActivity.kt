package com.example.sunshine

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max

private const val ML_PER_OUNCE = 29.5735295625
private const val ML_PER_GALLON = 3785.411784

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SunshineApp()
            }
        }
    }
}

enum class UnitOption(val label: String) {
    ML("mL"),
    L("L"),
    OZ("oz"),
    GAL("gal"),
}

data class Ratio(
    val waterParts: Double,
    val totalParts: Double,
    val label: String,
    val valid: Boolean,
)

data class Inputs(
    val productName: String = "Shampoo A",
    val bottleSize: String = "473",
    val bottleUnit: UnitOption = UnitOption.ML,
    val bottleCost: String = "29.99",
    val dilution: String = "1:16",
    val mixSize: String = "500",
    val mixUnit: UnitOption = UnitOption.ML,
    val currency: String = "\$",
    val pumpMl: String = "2",
    val condBottleSize: String = "473",
    val condBottleUnit: UnitOption = UnitOption.ML,
    val condBottleCost: String = "19.99",
    val condUsePerBathMl: String = "10",
)

data class Calc(
    val dilutionLabel: String,
    val dilutionValid: Boolean,
    val yieldFromBottleMl: Double,
    val costPerMixedL: Double,
    val costPer100ml: Double,
    val costPerMixBottle: Double,
    val mixesPerBottle: Double,
    val concPerMixMl: Double,
    val waterPerMixMl: Double,
    val concFromBottleMl: Double,
    val pumpsPerMix: Double,
    val pumpsPerBottle: Double,
    val costPerPump: Double,
    val conditionerCostPerBath: Double,
    val totalBathCost: Double,
)

data class InputErrors(
    val bottleSize: Boolean = false,
    val bottleCost: Boolean = false,
    val mixSize: Boolean = false,
    val condBottleSize: Boolean = false,
    val condBottleCost: Boolean = false,
    val condUse: Boolean = false,
)

private val quickRatios = listOf("1:4", "1:10", "1:16", "1:25", "1:32", "1:50", "1:64", "1:100")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SunshineApp() {
    var inputs by remember { mutableStateOf(Inputs()) }
    val errors = remember(inputs) { validateInputs(inputs) }
    val calc = remember(inputs) {
        compute(
            bottleSize = inputs.bottleSize.toDoubleOrNull() ?: 0.0,
            bottleUnit = inputs.bottleUnit,
            bottleCost = inputs.bottleCost.toDoubleOrNull() ?: 0.0,
            dilutionInput = inputs.dilution,
            mixSize = inputs.mixSize.toDoubleOrNull() ?: 0.0,
            mixUnit = inputs.mixUnit,
            pumpMl = inputs.pumpMl.toDoubleOrNull() ?: 0.0,
            condBottleSize = inputs.condBottleSize.toDoubleOrNull() ?: 0.0,
            condBottleUnit = inputs.condBottleUnit,
            condBottleCost = inputs.condBottleCost.toDoubleOrNull() ?: 0.0,
            condUsePerBathMl = inputs.condUsePerBathMl.toDoubleOrNull() ?: 0.0,
        )
    }
    var showHelp by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sunshine Calculator") },
                actions = {
                    IconButton(onClick = { showHelp = true }) {
                        Icon(imageVector = Icons.Default.HelpOutline, contentDescription = "Help")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                Text(
                    text = "Soap Dilution Calculator",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Plan concentrate, water, and costs for every grooming session.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            item {
                SectionCard(
                    title = "Shampoo",
                    subtitle = "Enter bottle, dilution, and pump details.",
                ) {
                    LabeledField(
                        label = "Product name",
                        value = inputs.productName,
                        onValueChange = { inputs = inputs.copy(productName = it.take(40)) },
                    )
                    NumberField(
                        label = "Bottle size",
                        value = inputs.bottleSize,
                        error = if (errors.bottleSize) "Enter a value above 0" else null,
                        onValueChange = { inputs = inputs.copy(bottleSize = cleanNumeric(it)) },
                    )
                    UnitPicker(
                        label = "Unit",
                        value = inputs.bottleUnit,
                        onUnitSelected = { inputs = inputs.copy(bottleUnit = it) },
                    )
                    NumberField(
                        label = "Bottle cost (${inputs.currency})",
                        value = inputs.bottleCost,
                        error = if (errors.bottleCost) "Enter a cost ≥ 0" else null,
                        onValueChange = { inputs = inputs.copy(bottleCost = cleanNumeric(it)) },
                    )
                    LabeledField(
                        label = "Dilution",
                        value = inputs.dilution,
                        onValueChange = { inputs = inputs.copy(dilution = it) },
                    )
                    QuickRatioRow(onRatioSelected = { ratio -> inputs = inputs.copy(dilution = ratio) })
                    NumberField(
                        label = "Mix size",
                        value = inputs.mixSize,
                        error = if (errors.mixSize) "Enter a value above 0" else null,
                        onValueChange = { inputs = inputs.copy(mixSize = cleanNumeric(it)) },
                    )
                    UnitPicker(
                        label = "Unit",
                        value = inputs.mixUnit,
                        onUnitSelected = { inputs = inputs.copy(mixUnit = it) },
                    )
                    LabeledField(
                        label = "Currency",
                        value = inputs.currency,
                        onValueChange = { value -> inputs = inputs.copy(currency = value.take(2)) },
                    )
                    NumberField(
                        label = "mL per pump",
                        value = inputs.pumpMl,
                        supportingText = "Used for concentrate",
                        onValueChange = { inputs = inputs.copy(pumpMl = cleanNumeric(it)) },
                    )
                }
            }

            item {
                SectionCard(
                    title = "Conditioner",
                    subtitle = "Simple per-bath costing.",
                ) {
                    NumberField(
                        label = "Bottle size",
                        value = inputs.condBottleSize,
                        error = if (errors.condBottleSize) "Enter a value above 0" else null,
                        onValueChange = { inputs = inputs.copy(condBottleSize = cleanNumeric(it)) },
                    )
                    UnitPicker(
                        label = "Unit",
                        value = inputs.condBottleUnit,
                        onUnitSelected = { inputs = inputs.copy(condBottleUnit = it) },
                    )
                    NumberField(
                        label = "Bottle cost (${inputs.currency})",
                        value = inputs.condBottleCost,
                        error = if (errors.condBottleCost) "Enter a cost ≥ 0" else null,
                        onValueChange = { inputs = inputs.copy(condBottleCost = cleanNumeric(it)) },
                    )
                    NumberField(
                        label = "mL used per bath",
                        value = inputs.condUsePerBathMl,
                        error = if (errors.condUse) "Enter a value ≥ 0" else null,
                        onValueChange = { inputs = inputs.copy(condUsePerBathMl = cleanNumeric(it)) },
                    )
                }
            }

            item {
                SectionCard(
                    title = "Results",
                    subtitle = "Calculations update live.",
                ) {
                    if (!calc.dilutionValid) {
                        Text(
                            text = "Enter a ratio like 1:16, 1/16, or just 16 (interpreted as 1:16).",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    val rows = buildResultRows(calc, inputs.currency)
                    ResultRows(rows)
                }
            }
        }
    }

    if (showHelp) {
        HelpDialog(onDismiss = { showHelp = false })
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            content()
        }
    }
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
    )
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String? = null,
    supportingText: String? = null,
) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(keyboardType = KeyboardType.Decimal),
        supportingText = {
            when {
                error != null -> Text(error, color = MaterialTheme.colorScheme.error)
                supportingText != null -> Text(supportingText)
            }
        },
        singleLine = true,
    )
}

@Composable
private fun UnitPicker(
    label: String,
    value: UnitOption,
    onUnitSelected: (UnitOption) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        TextButton(onClick = { expanded = true }) { Text(value.label) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            UnitOption.values().forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onUnitSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickRatioRow(onRatioSelected: (String) -> Unit) {
    Column {
        Text("Quick ratios", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            quickRatios.forEach { ratio ->
                Button(onClick = { onRatioSelected(ratio) }) {
                    Text(ratio)
                }
            }
        }
    }
}

@Composable
private fun ResultRows(rows: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { (label, value) ->
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(label, style = MaterialTheme.typography.bodyMedium)
                Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Got it") } },
        title = { Text("How to use this tool") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text("Inputs", fontWeight = FontWeight.SemiBold)
                    Text("• Enter bottle size, unit, and cost for shampoo and conditioner.")
                    Text("• Provide dilution ratio such as 1:16 or 16 (meaning 1:16).")
                    Text("• Mix size, currency, and optional pump mL refine calculations.")
                }
                Column {
                    Text("Calculations", fontWeight = FontWeight.SemiBold)
                    Text("• Yield = bottle mL × (1 + water parts).")
                    Text("• Mixed cost = (bottle cost ÷ bottle mL) ÷ (1 + water parts).")
                    Text("• Pumps are concentrate per mix ÷ mL per pump.")
                    Text("• Conditioner cost per bath = (cond cost ÷ cond mL) × mL used.")
                }
            }
        },
    )
}

private fun buildResultRows(calc: Calc, currency: String): List<Pair<String, String>> {
    val rows = mutableListOf(
        "Dilution" to calc.dilutionLabel,
        "Yield from bottle" to "${formatNumber(calc.yieldFromBottleMl / 1000.0, 2)} L (${formatNumber(calc.yieldFromBottleMl, 0)} mL)",
        "Cost per mixed L" to formatCurrency(calc.costPerMixedL, currency),
        "Cost per 100 mL" to formatCurrency(calc.costPer100ml, currency),
        "Cost per mix" to formatCurrency(calc.costPerMixBottle, currency),
        "Mix concentrate / water" to "${formatNumber(calc.concPerMixMl, 0)} mL / ${formatNumber(calc.waterPerMixMl, 0)} mL",
        "Mixes per bottle" to formatNumber(calc.mixesPerBottle, 2),
        "Soap cost per bath" to formatCurrency(calc.costPerMixBottle, currency),
        "Conditioner cost per bath" to formatCurrency(calc.conditionerCostPerBath, currency),
        "Total bath cost" to formatCurrency(calc.totalBathCost, currency),
    )

    if (calc.pumpsPerMix > 0.0) {
        rows.add(6, "Pumps per mix" to formatNumber(calc.pumpsPerMix, 1))
        rows.add(7, "Total pumps per bottle" to formatNumber(calc.pumpsPerBottle, 1))
        rows.add(8, "Cost per pump" to formatCurrency(calc.costPerPump, currency))
    }

    return rows
}

private fun compute(
    bottleSize: Double,
    bottleUnit: UnitOption,
    bottleCost: Double,
    dilutionInput: String,
    mixSize: Double,
    mixUnit: UnitOption,
    pumpMl: Double,
    condBottleSize: Double,
    condBottleUnit: UnitOption,
    condBottleCost: Double,
    condUsePerBathMl: Double,
): Calc {
    val bottleMl = toMl(bottleSize, bottleUnit)
    val mixMl = toMl(mixSize, mixUnit)
    val ratio = parseDilution(dilutionInput)

    val mixedFromBottleMl = if (ratio.valid) bottleMl * ratio.totalParts else 0.0
    val costPerMlConcentrate = if (bottleMl > 0.0) bottleCost / bottleMl else 0.0
    val costPerMlMixed = if (ratio.valid && ratio.totalParts > 0.0) costPerMlConcentrate / ratio.totalParts else 0.0

    val costPerMixedL = costPerMlMixed * 1000.0
    val costPer100ml = costPerMlMixed * 100.0
    val costPerMixBottle = costPerMlMixed * mixMl

    val mixesPerBottle = if (ratio.valid && mixMl > 0.0) mixedFromBottleMl / mixMl else 0.0
    val concPerMixMl = if (ratio.valid && ratio.totalParts > 0.0) mixMl / ratio.totalParts else 0.0
    val waterPerMixMl = if (ratio.valid) mixMl - concPerMixMl else 0.0

    val pumpsPerMix = if (pumpMl > 0.0) concPerMixMl / pumpMl else 0.0
    val pumpsPerBottle = if (pumpMl > 0.0) bottleMl / pumpMl else 0.0
    val costPerPump = if (pumpsPerMix > 0.0) costPerMixBottle / pumpsPerMix else 0.0

    val condBottleMl = toMl(condBottleSize, condBottleUnit)
    val condCostPerMl = if (condBottleMl > 0.0) condBottleCost / condBottleMl else 0.0
    val conditionerCostPerBath = max(condUsePerBathMl, 0.0) * condCostPerMl
    val totalBathCost = costPerMixBottle + conditionerCostPerBath

    return Calc(
        dilutionLabel = ratio.label,
        dilutionValid = ratio.valid,
        yieldFromBottleMl = mixedFromBottleMl,
        costPerMixedL = costPerMixedL,
        costPer100ml = costPer100ml,
        costPerMixBottle = costPerMixBottle,
        mixesPerBottle = mixesPerBottle,
        concPerMixMl = concPerMixMl,
        waterPerMixMl = waterPerMixMl,
        concFromBottleMl = bottleMl,
        pumpsPerMix = pumpsPerMix,
        pumpsPerBottle = pumpsPerBottle,
        costPerPump = costPerPump,
        conditionerCostPerBath = conditionerCostPerBath,
        totalBathCost = totalBathCost,
    )
}

private fun validateInputs(state: Inputs): InputErrors {
    val bottleSizeErr = (state.bottleSize.toDoubleOrNull() ?: 0.0) <= 0.0
    val bottleCostErr = (state.bottleCost.toDoubleOrNull() ?: -1.0) < 0.0
    val mixSizeErr = (state.mixSize.toDoubleOrNull() ?: 0.0) <= 0.0
    val condBottleSizeErr = (state.condBottleSize.toDoubleOrNull() ?: 0.0) <= 0.0
    val condBottleCostErr = (state.condBottleCost.toDoubleOrNull() ?: -1.0) < 0.0
    val condUseErr = (state.condUsePerBathMl.toDoubleOrNull() ?: -1.0) < 0.0

    return InputErrors(
        bottleSize = bottleSizeErr,
        bottleCost = bottleCostErr,
        mixSize = mixSizeErr,
        condBottleSize = condBottleSizeErr,
        condBottleCost = condBottleCostErr,
        condUse = condUseErr,
    )
}

private fun toMl(value: Double, unit: UnitOption): Double = when (unit) {
    UnitOption.ML -> value
    UnitOption.L -> value * 1000.0
    UnitOption.OZ -> value * ML_PER_OUNCE
    UnitOption.GAL -> value * ML_PER_GALLON
}

private fun parseDilution(input: String): Ratio {
    val raw = input.trim().lowercase()
    if (raw.isEmpty()) {
        return Ratio(0.0, 1.0, "1:0", valid = false)
    }

    val normalized = raw
        .replace(Regex("\\s*(to|in|x)\\s*"), ":")
        .replace(Regex("\\s*[-/\\s*]\\s*"), ":")
        .replace(Regex("\\s+"), "")

    Regex("^(\\d+(?:\\.\\d+)?):(\\d+(?:\\.\\d+)?)$").matchEntire(normalized)?.let { match ->
        val a = match.groupValues[1].toDoubleOrNull()
        val b = match.groupValues[2].toDoubleOrNull()
        if (a == null || b == null || a <= 0.0 || b < 0.0) {
            return Ratio(0.0, 1.0, "1:0", valid = false)
        }
        val effectiveWater = if (a == 1.0) b else b / a
        if (!effectiveWater.isFinite() || effectiveWater < 0.0) {
            return Ratio(0.0, 1.0, "1:0", valid = false)
        }
        return Ratio(
            waterParts = effectiveWater,
            totalParts = 1.0 + effectiveWater,
            label = "1:${trimTrailingZeros(effectiveWater)}",
            valid = true,
        )
    }

    normalized.toDoubleOrNull()?.takeIf { it >= 0.0 }?.let { numeric ->
        return Ratio(
            waterParts = numeric,
            totalParts = 1.0 + numeric,
            label = "1:${trimTrailingZeros(numeric)}",
            valid = true,
        )
    }

    return Ratio(0.0, 1.0, "1:0", valid = false)
}

private fun trimTrailingZeros(value: Double): String {
    val text = "%.6f".format(value)
    return text.replace(Regex("\\.0+$"), "").replace(Regex("0+$"), "").ifEmpty { "0" }
}

private fun cleanNumeric(value: String): String {
    val normalized = value.replace(',', '.')
    val builder = StringBuilder()
    var dotSeen = false
    normalized.forEach { ch ->
        when {
            ch.isDigit() -> builder.append(ch)
            ch == '.' && !dotSeen -> {
                builder.append('.')
                dotSeen = true
            }
        }
    }
    return builder.toString()
}

private fun formatCurrency(value: Double, currency: String): String {
    val safeValue = if (value.isFinite()) value else 0.0
    return "$currency${"%.4f".format(safeValue)}"
}

private fun formatNumber(value: Double, decimals: Int): String {
    if (!value.isFinite()) return "-"
    return NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = decimals
        maximumFractionDigits = decimals
    }.format(value)
}
