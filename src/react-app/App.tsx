import { useMemo, useState } from "react";
import type { ChangeEvent, ReactElement, ReactNode } from "react";
import "./App.css";

type UnitOption = "ML" | "L" | "OZ" | "GAL";

type Ratio = {
  waterParts: number;
  totalParts: number;
  label: string;
  valid: boolean;
};

type Calculation = {
  dilutionLabel: string;
  dilutionValid: boolean;
  yieldFromBottleMl: number;
  costPerMixedL: number;
  costPer100ml: number;
  costPerMixBottle: number;
  mixesPerBottle: number;
  concPerMixMl: number;
  waterPerMixMl: number;
  concFromBottleMl: number;
  pumpsPerMix: number;
  pumpsPerBottle: number;
  costPerPump: number;
  conditionerCostPerBath: number;
  totalBathCost: number;
};

type NumericInputField =
  | "bottleSize"
  | "bottleCost"
  | "mixSize"
  | "pumpMl"
  | "condBottleSize"
  | "condBottleCost"
  | "condUsePerBathMl";

type InputErrors = Record<NumericInputField, boolean>;

type Inputs = {
  productName: string;
  bottleSize: string;
  bottleUnit: UnitOption;
  bottleCost: string;
  dilution: string;
  mixSize: string;
  mixUnit: UnitOption;
  currency: string;
  pumpMl: string;
  condBottleSize: string;
  condBottleUnit: UnitOption;
  condBottleCost: string;
  condUsePerBathMl: string;
};

const ML_PER_OUNCE = 29.5735295625;
const ML_PER_GALLON = 3785.411784;

const UNIT_OPTIONS: ReadonlyArray<{ value: UnitOption; label: string }> = [
  { value: "ML", label: "mL" },
  { value: "L", label: "L" },
  { value: "OZ", label: "oz" },
  { value: "GAL", label: "gal" },
];

const QUICK_RATIOS = ["1:4", "1:10", "1:16", "1:25", "1:32", "1:50", "1:64", "1:100"];

const DEFAULT_INPUTS: Inputs = {
  productName: "Shampoo A",
  bottleSize: "473",
  bottleUnit: "ML",
  bottleCost: "29.99",
  dilution: "1:16",
  mixSize: "500",
  mixUnit: "ML",
  currency: "$",
  pumpMl: "2",
  condBottleSize: "473",
  condBottleUnit: "ML",
  condBottleCost: "19.99",
  condUsePerBathMl: "10",
};

function toMl(value: number, unit: UnitOption): number {
  switch (unit) {
    case "ML":
      return value;
    case "L":
      return value * 1000;
    case "OZ":
      return value * ML_PER_OUNCE;
    case "GAL":
      return value * ML_PER_GALLON;
    default:
      return value;
  }
}

function trimTrailingZeros(value: number): string {
  const fixed = value.toFixed(6);
  return fixed.replace(/\.0+$|0+$/u, "");
}

function parseDilution(input: string): Ratio {
  const raw = input.trim().toLowerCase();

  if (!raw) {
    return { waterParts: 0, totalParts: 1, label: "1:0", valid: false };
  }

  const normalized = raw
    .replace(/\s*(to|in|x)\s*/gu, ":")
    .replace(/\s*[-/\s*]\s*/gu, ":")
    .replace(/\s+/gu, "");

  const ratioMatch = normalized.match(/^(\d+(?:\.\d+)?):(\d+(?:\.\d+)?)$/u);

  if (ratioMatch) {
    const [, partA, partB] = ratioMatch;
    const concentrate = Number.parseFloat(partA);
    const water = Number.parseFloat(partB);

    if (!Number.isFinite(concentrate) || !Number.isFinite(water) || concentrate <= 0 || water < 0) {
      return { waterParts: 0, totalParts: 1, label: "1:0", valid: false };
    }

    const effectiveWater = concentrate === 1 ? water : water / concentrate;

    if (!Number.isFinite(effectiveWater) || effectiveWater < 0) {
      return { waterParts: 0, totalParts: 1, label: "1:0", valid: false };
    }

    return {
      waterParts: effectiveWater,
      totalParts: 1 + effectiveWater,
      label: `1:${trimTrailingZeros(effectiveWater)}`,
      valid: true,
    };
  }

  const numeric = Number.parseFloat(normalized);

  if (Number.isFinite(numeric) && numeric >= 0) {
    return {
      waterParts: numeric,
      totalParts: 1 + numeric,
      label: `1:${trimTrailingZeros(numeric)}`,
      valid: true,
    };
  }

  return { waterParts: 0, totalParts: 1, label: "1:0", valid: false };
}

function cleanNumeric(value: string): string {
  const normalized = value.replace(",", ".");
  let dotSeen = false;
  const builder: string[] = [];

  for (const char of normalized) {
    if (/\d/u.test(char)) {
      builder.push(char);
      continue;
    }

    if (char === "." && !dotSeen) {
      builder.push(char);
      dotSeen = true;
    }
  }

  return builder.join("");
}

function toFloat(value: string): number {
  return Number.parseFloat(value) || 0;
}

function computeCalculation(inputs: Inputs): Calculation {
  const bottleSizeMl = toMl(toFloat(inputs.bottleSize), inputs.bottleUnit);
  const mixSizeMl = toMl(toFloat(inputs.mixSize), inputs.mixUnit);
  const ratio = parseDilution(inputs.dilution);

  const pumpMl = toFloat(inputs.pumpMl);
  const bottleCost = Math.max(toFloat(inputs.bottleCost), 0);
  const condBottleSizeMl = toMl(toFloat(inputs.condBottleSize), inputs.condBottleUnit);
  const condBottleCost = Math.max(toFloat(inputs.condBottleCost), 0);
  const condUsePerBathMl = Math.max(toFloat(inputs.condUsePerBathMl), 0);

  const mixedFromBottleMl = ratio.valid ? bottleSizeMl * ratio.totalParts : 0;
  const costPerMlConcentrate = bottleSizeMl > 0 ? bottleCost / bottleSizeMl : 0;
  const costPerMlMixed = ratio.valid && ratio.totalParts > 0 ? costPerMlConcentrate / ratio.totalParts : 0;

  const costPerMixedL = costPerMlMixed * 1000;
  const costPer100ml = costPerMlMixed * 100;
  const costPerMixBottle = costPerMlMixed * mixSizeMl;

  const mixesPerBottle = ratio.valid && mixSizeMl > 0 ? mixedFromBottleMl / mixSizeMl : 0;
  const concentratePerMix = ratio.valid && ratio.totalParts > 0 ? mixSizeMl / ratio.totalParts : 0;
  const waterPerMix = ratio.valid ? mixSizeMl - concentratePerMix : 0;

  const pumpsPerMix = pumpMl > 0 ? concentratePerMix / pumpMl : 0;
  const pumpsPerBottle = pumpMl > 0 ? bottleSizeMl / pumpMl : 0;
  const costPerPump = pumpsPerMix > 0 ? costPerMixBottle / pumpsPerMix : 0;

  const condCostPerMl = condBottleSizeMl > 0 ? condBottleCost / condBottleSizeMl : 0;
  const conditionerCostPerBath = condCostPerMl * condUsePerBathMl;
  const totalBathCost = costPerMixBottle + conditionerCostPerBath;

  return {
    dilutionLabel: ratio.label,
    dilutionValid: ratio.valid,
    yieldFromBottleMl: mixedFromBottleMl,
    costPerMixedL,
    costPer100ml,
    costPerMixBottle,
    mixesPerBottle,
    concPerMixMl: concentratePerMix,
    waterPerMixMl: waterPerMix,
    concFromBottleMl: bottleSizeMl,
    pumpsPerMix,
    pumpsPerBottle,
    costPerPump,
    conditionerCostPerBath,
    totalBathCost,
  };
}

function formatCurrency(value: number, currencySymbol: string): string {
  const safeValue = Number.isFinite(value) ? value : 0;
  return `${currencySymbol}${safeValue.toFixed(4)}`;
}

function formatNumber(value: number, fractionDigits = 2): string {
  if (!Number.isFinite(value)) {
    return "-";
  }

  return value.toLocaleString("en-US", {
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
  });
}

function validateNumericInputs(inputs: Inputs): InputErrors {
  return {
    bottleSize: !(toFloat(inputs.bottleSize) > 0),
    bottleCost: !(toFloat(inputs.bottleCost) >= 0),
    mixSize: !(toFloat(inputs.mixSize) > 0),
    pumpMl: !(toFloat(inputs.pumpMl) >= 0),
    condBottleSize: !(toFloat(inputs.condBottleSize) > 0),
    condBottleCost: !(toFloat(inputs.condBottleCost) >= 0),
    condUsePerBathMl: !(toFloat(inputs.condUsePerBathMl) >= 0),
  };
}

function App(): ReactElement {
  const [inputs, setInputs] = useState<Inputs>(() => ({ ...DEFAULT_INPUTS }));
  const [showHelp, setShowHelp] = useState(false);

  const errors = useMemo(() => validateNumericInputs(inputs), [inputs]);
  const calculation = useMemo(() => computeCalculation(inputs), [inputs]);

  const updateField = (field: keyof Inputs) => (value: string | UnitOption) => {
    setInputs((prev) => ({ ...prev, [field]: value }));
  };

  const handleNumericChange = (field: NumericInputField) => (event: ChangeEvent<HTMLInputElement>) => {
    updateField(field)(cleanNumeric(event.target.value));
  };

  const resetAll = () => setInputs({ ...DEFAULT_INPUTS });

  return (
    <div className="app-shell">
      <header className="hero">
        <div className="hero__text">
          <h1>Sunshine — Soap Dilution Calculator</h1>
          <p>Quickly plan concentrate, water, and costs for every grooming session.</p>
        </div>
        <button className="hero__help" type="button" onClick={() => setShowHelp(true)}>
          Help
        </button>
      </header>

      <main className="content">
        <section className="card">
          <header className="card__header">
            <div>
              <h2 className="card__title">Shampoo</h2>
              <p className="card__subtitle">Enter bottle, dilution, and pump details to see batch costings.</p>
            </div>
            <button type="button" className="link-button" onClick={resetAll}>
              Reset values
            </button>
          </header>

          <div className="field-grid">
            <LabeledField label="Product name">
              <input
                value={inputs.productName}
                onChange={(event) => updateField("productName")(event.target.value)}
                maxLength={40}
              />
            </LabeledField>

            <LabeledField label="Shampoo bottle size" error={errors.bottleSize ? "Enter a value above 0" : undefined}>
              <input value={inputs.bottleSize} onChange={handleNumericChange("bottleSize")} inputMode="decimal" />
            </LabeledField>

            <LabeledField label="Unit">
              <select value={inputs.bottleUnit} onChange={(event) => updateField("bottleUnit")(event.target.value as UnitOption)}>
                {UNIT_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </LabeledField>

            <LabeledField label="Shampoo bottle cost ($)" error={errors.bottleCost ? "Enter a cost above or equal to 0" : undefined}>
              <input value={inputs.bottleCost} onChange={handleNumericChange("bottleCost")} inputMode="decimal" />
            </LabeledField>

            <LabeledField label="Dilution">
              <div className="dilution-group">
                <input value={inputs.dilution} onChange={(event) => updateField("dilution")(event.target.value)} placeholder="1:16" />
                <select
                  value=""
                  onChange={(event) => {
                    if (event.target.value) {
                      updateField("dilution")(event.target.value);
                      event.target.value = "";
                    }
                  }}
                >
                  <option value="">Quick ratio…</option>
                  {QUICK_RATIOS.map((ratio) => (
                    <option key={ratio} value={ratio}>
                      {ratio}
                    </option>
                  ))}
                </select>
              </div>
            </LabeledField>

            <LabeledField label="Mixing bottle size" error={errors.mixSize ? "Enter a value above 0" : undefined}>
              <input value={inputs.mixSize} onChange={handleNumericChange("mixSize")} inputMode="decimal" />
            </LabeledField>

            <LabeledField label="Unit">
              <select value={inputs.mixUnit} onChange={(event) => updateField("mixUnit")(event.target.value as UnitOption)}>
                {UNIT_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </LabeledField>

            <LabeledField label="Currency">
              <input value={inputs.currency} onChange={(event) => updateField("currency")(event.target.value.slice(0, 2))} maxLength={2} />
            </LabeledField>

            <LabeledField label="mL per pump" hint="Used for concentrate" error={errors.pumpMl ? "Enter a value above or equal to 0" : undefined}>
              <input value={inputs.pumpMl} onChange={handleNumericChange("pumpMl")} inputMode="decimal" />
            </LabeledField>
          </div>
        </section>

        <section className="card">
          <header className="card__header">
            <div>
              <h2 className="card__title">Conditioner</h2>
              <p className="card__subtitle">Simple per-bath costing.</p>
            </div>
          </header>

          <div className="field-grid">
            <LabeledField label="Bottle size" error={errors.condBottleSize ? "Enter a value above 0" : undefined}>
              <input value={inputs.condBottleSize} onChange={handleNumericChange("condBottleSize")} inputMode="decimal" />
            </LabeledField>

            <LabeledField label="Unit">
              <select value={inputs.condBottleUnit} onChange={(event) => updateField("condBottleUnit")(event.target.value as UnitOption)}>
                {UNIT_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </LabeledField>

            <LabeledField label="Bottle cost ($)" error={errors.condBottleCost ? "Enter a cost above or equal to 0" : undefined}>
              <input value={inputs.condBottleCost} onChange={handleNumericChange("condBottleCost")} inputMode="decimal" />
            </LabeledField>

            <LabeledField label="mL used per bath" error={errors.condUsePerBathMl ? "Enter a value above or equal to 0" : undefined}>
              <input value={inputs.condUsePerBathMl} onChange={handleNumericChange("condUsePerBathMl")} inputMode="decimal" />
            </LabeledField>
          </div>
        </section>

        <section className="card">
          <header className="card__header">
            <div>
              <h2 className="card__title">Results</h2>
              <p className="card__subtitle">Live calculations update as you adjust your inputs.</p>
            </div>
          </header>

          {!calculation.dilutionValid && (
            <p className="error-text">Enter a ratio like 1:16, 1/16, or just 16 (interpreted as 1:16).</p>
          )}

          <dl className="data-grid">
            <DataRow label="Dilution" value={calculation.dilutionLabel} />
            <DataRow
              label="Yield from shampoo bottle"
              value={`${formatNumber(calculation.yieldFromBottleMl / 1000, 2)} L (${formatNumber(
                calculation.yieldFromBottleMl,
                0,
              )} mL)`}
            />
            <DataRow label="Cost per mixed L (shampoo)" value={formatCurrency(calculation.costPerMixedL, inputs.currency)} />
            <DataRow label="Cost per 100 mL (shampoo)" value={formatCurrency(calculation.costPer100ml, inputs.currency)} />
            <DataRow
              label="Cost per mixing bottle (soap)"
              value={formatCurrency(calculation.costPerMixBottle, inputs.currency)}
            />
            <DataRow
              label="Mixing bottle (conc / water)"
              value={`${formatNumber(calculation.concPerMixMl, 0)} mL / ${formatNumber(calculation.waterPerMixMl, 0)} mL`}
            />
            <DataRow label="Mixing bottles per shampoo bottle" value={formatNumber(calculation.mixesPerBottle, 2)} />

            {calculation.pumpsPerMix > 0 && (
              <>
                <DataRow label="Pumps (conc) per mixing bottle" value={formatNumber(calculation.pumpsPerMix, 1)} />
                <DataRow label="Total pumps of conc per shampoo bottle" value={formatNumber(calculation.pumpsPerBottle, 1)} />
                <DataRow label="Cost per pump (shampoo)" value={formatCurrency(calculation.costPerPump, inputs.currency)} />
              </>
            )}

            <DataRow label="Soap cost per bath (1 full mix)" value={formatCurrency(calculation.costPerMixBottle, inputs.currency)} />
            <DataRow label="Conditioner cost per bath" value={formatCurrency(calculation.conditionerCostPerBath, inputs.currency)} />
            <DataRow label="Total bath cost (soap + conditioner)" value={formatCurrency(calculation.totalBathCost, inputs.currency)} />
          </dl>

          <p className="footnote">
            Yield = shampoo bottle mL × (1 + water parts); cost/mL mixed = (shampoo cost ÷ shampoo mL) ÷ (1 + water parts).
            Conditioner cost per bath = (cond cost ÷ cond mL) × mL used.
          </p>
        </section>
      </main>

      {showHelp && (
        <dialog className="modal" open>
          <div className="modal__content">
            <header className="modal__header">
              <h2>How to use this tool</h2>
              <button type="button" onClick={() => setShowHelp(false)} aria-label="Close help">
                ×
              </button>
            </header>

            <div className="modal__body">
              <section>
                <h3>Inputs</h3>
                <ul>
                  <li>Shampoo bottle size, unit, and cost.</li>
                  <li>Dilution: 1:16, 1/16, “1 to 16”, or 16 (meaning 1:16).</li>
                  <li>Mixing bottle size &amp; unit plus per-pump volume.</li>
                  <li>Conditioner bottle size, cost, and mL used per bath.</li>
                </ul>
              </section>

              <section>
                <h3>Calculations</h3>
                <ul>
                  <li>We parse dilution to find water parts (W). Total parts = 1 + W.</li>
                  <li>Shampoo yield = shampoo mL × (1 + W).</li>
                  <li>Cost per mL mixed (shampoo) = (shampoo cost ÷ shampoo mL) ÷ (1 + W).</li>
                  <li>Pumps are derived from concentrate per mix ÷ mL per pump.</li>
                  <li>Conditioner cost per bath = (cond cost ÷ cond mL) × mL used.</li>
                </ul>
              </section>
            </div>

            <footer className="modal__footer">
              <button type="button" className="primary" onClick={() => setShowHelp(false)}>
                Got it
              </button>
            </footer>
          </div>
        </dialog>
      )}
    </div>
  );
}

function LabeledField({
  label,
  children,
  error,
  hint,
}: {
  label: string;
  children: ReactNode;
  error?: string;
  hint?: string;
}): ReactElement {
  return (
    <label className={`field${error ? " field--error" : ""}`}>
      <span className="field__label">{label}</span>
      <div className="field__control">{children}</div>
      {hint && !error && <span className="field__hint">{hint}</span>}
      {error && <span className="field__error">{error}</span>}
    </label>
  );
}

function DataRow({ label, value }: { label: string; value: string }): ReactElement {
  return (
    <div className="data-row">
      <dt>{label}</dt>
      <dd>{value}</dd>
    </div>
  );
}

export default App;
