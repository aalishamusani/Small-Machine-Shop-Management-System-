// One-time setup for a FRESH cloud database (e.g. Neon): creates the tables,
// the 3 login accounts, the demo orders, and runs the forecast + anomaly
// scripts so those screens have results straight away.
//
// Usage (from the backend/ folder, with PGHOST/PGDATABASE/PGUSER/PGPASSWORD
// and PGSSLMODE=require set in the shell to point at the CLOUD database):
//     node scripts/setupDeployedDatabase.js --yes
//
// Safety: it refuses to run if the database already has the `orders` table,
// so pointing it at your existing local database by mistake changes nothing.
//
// The generated passwords are written to a private file in your home folder
// (SMSMS_private_data\deployed-credentials.txt, outside OneDrive and git) -
// they are never printed to the screen or put in the code.

const fs = require('fs');
const path = require('path');
const os = require('os');
const crypto = require('crypto');
const bcrypt = require('bcryptjs');
const db = require('../src/config/db');
const { runMlScript } = require('../src/utils/mlRunner');
const DEMO_ORDERS = require('./demoData');

const DB_DIR = path.join(__dirname, '..', '..', 'database');
// Saved OUTSIDE the project folder: the project lives in OneDrive, which would
// upload the passwords to the cloud. Override with the CREDENTIALS_FILE env var.
const CREDENTIALS_FILE =
  process.env.CREDENTIALS_FILE || path.join(os.homedir(), 'SMSMS_private_data', 'deployed-credentials.txt');
const BCRYPT_ROUNDS = 10;

const ACCOUNTS = [
  { username: 'plant_head', role: 'plant_head' },
  { username: 'production_head', role: 'production_head' },
  { username: 'quality_head', role: 'quality_head' },
];

// Easy to read out and type on a phone in front of an audience, but still
// 3 random words + 2 digits (about 24 million combinations) rather than
// something guessable like "Password123".
const WORDS = [
  'amber', 'anchor', 'arrow', 'birch', 'bridge', 'cedar', 'cobalt', 'comet', 'coral', 'crown', 'dawn', 'delta',
  'eagle', 'ember', 'falcon', 'forest', 'garnet', 'glacier', 'harbor', 'hazel', 'indigo', 'island', 'jade', 'jasper',
  'kestrel', 'lantern', 'lotus', 'maple', 'meadow', 'mirror', 'nectar', 'oasis', 'onyx', 'orchid', 'panda', 'pebble',
  'quartz', 'raven', 'river', 'saffron', 'sierra', 'silver', 'summit', 'tiger', 'topaz', 'tundra', 'velvet', 'violet',
  'walnut', 'willow', 'zephyr', 'copper', 'breeze', 'canyon', 'dune', 'fjord', 'grove', 'horizon', 'ivory', 'marble',
  'noble', 'prairie', 'rocket', 'sunset',
];
const pick = () => WORDS[crypto.randomInt(WORDS.length)];
const makePassword = () => `${pick()}-${pick()}-${pick()}-${crypto.randomInt(10, 100)}`;

async function main() {
  if (!process.argv.includes('--yes')) {
    console.log(`Target database: ${process.env.PGDATABASE} on ${process.env.PGHOST}`);
    console.log('Re-run with --yes to set it up. Nothing has been changed.');
    process.exit(0);
  }
  console.log(`Target database: ${process.env.PGDATABASE} on ${process.env.PGHOST}`);

  const existing = await db.query("SELECT to_regclass('orders') AS t");
  if (existing.rows[0].t !== null) {
    console.log('REFUSING: this database already has an "orders" table (is it your local one?). Nothing changed.');
    process.exit(1);
  }

  // 1) Tables
  await db.query(fs.readFileSync(path.join(DB_DIR, 'schema_phase1.sql'), 'utf8'));
  await db.query(fs.readFileSync(path.join(DB_DIR, 'migration_monthly_actuals.sql'), 'utf8'));
  console.log('tables created');

  // 2) Login accounts with fresh random passwords
  const lines = ['SMSMS deployed-app logins (keep private - do NOT commit this file)', ''];
  const ids = {};
  for (const a of ACCOUNTS) {
    const password = makePassword();
    const hash = await bcrypt.hash(password, BCRYPT_ROUNDS);
    const r = await db.query(
      'INSERT INTO users (username, password_hash, role) VALUES ($1,$2,$3) RETURNING user_id',
      [a.username, hash, a.role]
    );
    ids[a.username] = r.rows[0].user_id;
    lines.push(`${a.username.padEnd(16)} ${password}`);
  }
  fs.mkdirSync(path.dirname(CREDENTIALS_FILE), { recursive: true });
  fs.writeFileSync(CREDENTIALS_FILE, lines.join('\n') + '\n');
  console.log('accounts created; passwords saved to', CREDENTIALS_FILE);

  // 3) Demo orders / batches / quality logs / payments, all or nothing
  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');
    for (const o of DEMO_ORDERS) {
      const ins = await client.query(
        `INSERT INTO orders (customer_name, job_type, order_date, due_date, total_qty, total_amount, status)
         VALUES ($1,$2,$3,$4,$5,$6,$7) RETURNING order_id`,
        [o.customer, o.job, o.date, o.due, o.qty, o.qty * o.rate, o.status]
      );
      const orderId = ins.rows[0].order_id;
      for (const b of o.batches) {
        const bi = await client.query(
          'INSERT INTO batches (order_id, quantity, status) VALUES ($1,$2,$3) RETURNING batch_id',
          [orderId, b.qty, b.status]
        );
        for (const [d, qc, df] of b.logs) {
          await client.query(
            `INSERT INTO quality_logs (batch_id, logged_by, check_date, qty_checked, defect_count, pass_fail)
             VALUES ($1,$2,$3,$4,$5,$6)`,
            [bi.rows[0].batch_id, ids.quality_head, d, qc, df, df === 0 ? 'pass' : 'fail']
          );
        }
      }
      for (const [amount, status, d] of o.payments) {
        await client.query(
          'INSERT INTO payments (order_id, amount, status, payment_date) VALUES ($1,$2,$3,$4)',
          [orderId, amount, status, d]
        );
      }
    }
    await client.query('COMMIT');
  } catch (e) {
    await client.query('ROLLBACK');
    throw e;
  } finally {
    client.release();
  }
  console.log(`${DEMO_ORDERS.length} demo orders loaded`);

  // 4) Fill the forecast + anomaly tables (the child processes inherit the PG* env vars)
  console.log(await runMlScript('train_forecast.py'));
  console.log(await runMlScript('anomaly_detection.py'));

  console.log('\nDone.');
  await db.pool.end();
}

main().catch((err) => {
  console.error('FAILED:', err.message);
  process.exit(1);
});
