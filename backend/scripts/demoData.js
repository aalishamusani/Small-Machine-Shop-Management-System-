// Demo/test orders used to populate a freshly deployed database so the live
// app looks "in use" during the demo. THIS IS MADE-UP TEST DATA (names,
// amounts, defect counts) - it is not the shop's real order history.
//
// Shape: order -> batches -> quality logs ([date, qty_checked, defects]),
// plus payments ([amount, status, date]).
//
// Quality-log pattern: normal inspections have ~2-9 defects per 100 checked.
// The FIRST inspection of a new job is the "trying out machine settings" one,
// with 15-17 defects - those are the intended anomaly-detection outliers.
module.exports = [
  { customer: 'Shree Engineering Works', job: 'Shaft turning (CNC)', date: '2026-08-04', due: '2026-08-25', qty: 600, rate: 185, status: 'closed',
    batches: [
      { qty: 200, status: 'completed', logs: [['2026-08-07', 100, 16], ['2026-08-08', 100, 7]] },
      { qty: 200, status: 'completed', logs: [['2026-08-12', 100, 6], ['2026-08-13', 100, 5]] },
      { qty: 200, status: 'completed', logs: [['2026-08-18', 100, 8], ['2026-08-19', 100, 4]] },
    ],
    payments: [[60000, 'paid', '2026-08-20'], [51000, 'paid', '2026-09-05']] },
  { customer: 'Patel Hydraulics', job: 'Flange machining (VMC)', date: '2026-08-12', due: '2026-09-02', qty: 400, rate: 320, status: 'closed',
    batches: [
      { qty: 200, status: 'completed', logs: [['2026-08-17', 100, 9], ['2026-08-18', 100, 6]] },
      { qty: 200, status: 'completed', logs: [['2026-08-24', 100, 5], ['2026-08-25', 100, 7]] },
    ],
    payments: [[128000, 'paid', '2026-09-10']] },
  { customer: 'Deccan Pumps Pvt Ltd', job: 'Pump casing (HMC)', date: '2026-08-20', due: '2026-09-15', qty: 300, rate: 410, status: 'closed',
    batches: [
      { qty: 150, status: 'completed', logs: [['2026-08-24', 100, 15], ['2026-08-25', 50, 3]] },
      { qty: 150, status: 'completed', logs: [['2026-09-02', 100, 6], ['2026-09-03', 50, 2]] },
    ],
    payments: [[80000, 'paid', '2026-09-01'], [43000, 'pending', '2026-09-20']] },
  { customer: 'Sai Precision Tools', job: 'Bracket milling (VMC)', date: '2026-09-03', due: '2026-09-24', qty: 500, rate: 150, status: 'closed',
    batches: [
      { qty: 250, status: 'completed', logs: [['2026-09-07', 100, 8], ['2026-09-08', 100, 6], ['2026-09-09', 50, 3]] },
      { qty: 250, status: 'completed', logs: [['2026-09-14', 100, 7], ['2026-09-15', 100, 9], ['2026-09-16', 50, 2]] },
    ],
    payments: [[40000, 'paid', '2026-09-12'], [35000, 'paid', '2026-09-30']] },
  { customer: 'Mahalaxmi Gears', job: 'Gear cutting (HMC)', date: '2026-09-10', due: '2026-10-12', qty: 250, rate: 480, status: 'active',
    batches: [
      { qty: 150, status: 'completed', logs: [['2026-09-13', 100, 17], ['2026-09-14', 50, 3]] },
      { qty: 100, status: 'in_progress', logs: [['2026-09-30', 50, 2]] },
    ],
    payments: [[60000, 'paid', '2026-09-12']] },
  { customer: 'Kiran Auto Ancillaries', job: 'Axle sleeve turning (CNC)', date: '2026-09-18', due: '2026-10-10', qty: 800, rate: 95, status: 'active',
    batches: [
      { qty: 300, status: 'completed', logs: [['2026-09-22', 100, 6], ['2026-09-23', 100, 8], ['2026-09-24', 100, 5]] },
      { qty: 300, status: 'in_progress', logs: [['2026-10-03', 100, 7]] },
      { qty: 200, status: 'pending', logs: [] },
    ],
    payments: [[38000, 'paid', '2026-09-20']] },
  { customer: 'Venkatesh Pneumatics', job: 'Cylinder end cap (VMC)', date: '2026-09-28', due: '2026-10-20', qty: 350, rate: 275, status: 'active',
    batches: [
      { qty: 200, status: 'in_progress', logs: [['2026-10-01', 100, 16], ['2026-10-02', 50, 4]] },
      { qty: 150, status: 'pending', logs: [] },
    ],
    payments: [] },
  { customer: 'Om Sai Engineering', job: 'Hydraulic block (HMC)', date: '2026-10-02', due: '2026-10-30', qty: 120, rate: 760, status: 'active',
    batches: [{ qty: 120, status: 'pending', logs: [] }],
    payments: [[25000, 'paid', '2026-10-03']] },
  { customer: 'Bharat Auto Components', job: 'Brake drum machining (HMC)', date: '2026-10-05', due: '2026-11-02', qty: 450, rate: 225, status: 'active',
    batches: [
      { qty: 150, status: 'in_progress', logs: [['2026-10-06', 50, 3]] },
      { qty: 150, status: 'pending', logs: [] },
      { qty: 150, status: 'pending', logs: [] },
    ],
    payments: [] },
];
