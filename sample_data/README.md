# sample_data — anonymised stand-ins for the real shop data

The real Excel exports (customer names, GST numbers, vehicle numbers, phone
numbers, real rates and quantities) are **not in this repository** and must
never be committed. These files have the same sheets, columns and formulas
as the real ones so the EDA notebook and the code can be developed and shown
without exposing the family business's data.

| File | Stands in for |
|---|---|
| `SCHEDULE_VS_ACUTAL_sample.zip` | `SCHEDULE VS ACUTAL.zip` (8 monthly workbooks) |
| `reschedulevsactualdispatchdata_sample.zip` | `reschedulevsactualdispatchdata.zip` (4 workbooks plus a nested zip) |

To run the notebook on the samples, put copies of the two zips next to it and
rename them to the names the notebook expects (`SCHEDULE_VS_ACUTAL.zip` and
`reschedulevsactualdispatchdata.zip`).

## How the data was anonymised

- **GST numbers, phone numbers, vehicle numbers, e-mails** were replaced with
  obviously fake values (fake GST numbers start with the state code 99, which
  does not exist). The same real value always maps to the same fake one, so repeated
  customers stay repeated.
- **Customer names, part names, addresses, notes and letterhead text** — every
  piece of text that is not a generic label such as RATE, QTY, TOTAL or a month
  name — was replaced with `Text-0001`, `Text-0002`, and so on.
- **Rates, quantities and amounts:** every number got its own random
  multiplier between 0.7 and 1.3 (rounded; whole numbers stay whole). The
  multipliers were never stored, so the real figures cannot be worked back.
  Running 1, 2, 3 serial-number columns and very small numbers were left alone.
- **Totals** are still Excel formulas and were recalculated, so rows still add up.
- **Removed:** links to other Excel files (and the data Excel cached from
  them), saved filter selections, comments, headers/footers, author and
  company properties, and file paths.
- **File names** keep only the month and year.

## Things to know

- The numbers are **made-up**: monthly totals computed from these samples will
  NOT match the real `SMSMS_monthly_sales_history.csv`, and forecasts built
  from them are for development only.
- One workbook whose original name did not follow the month pattern is called
  `SAMPLE 6`; rename it to the right month if the notebook needs that.
- Excel is needed only to *open* the files; their formula results are already
  stored, so tools such as pandas can read them directly.
