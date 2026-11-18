#!/usr/bin/env node
'use strict';

/**
 * Contract smoke test for the Todo REST API.
 *
 * Verifies the exact status codes and body expectations used by the Android
 * client in Bagian 2-10. Run it while `node server.js` is running:
 *
 *   npm run smoke
 *   BASE_URL=http://localhost:3000/api npm run smoke
 *
 * Exits with code 1 when any assertion fails, so it can be used in CI.
 */

const BASE_URL = (process.env.BASE_URL || 'http://localhost:3000/api').replace(/\/+$/, '');

const results = [];

/**
 * Record one assertion result.
 * @param {string} name Human-readable request label.
 * @param {number} expected Expected HTTP status.
 * @param {number} actual Actual HTTP status.
 * @param {string} body Raw response body.
 * @param {boolean} bodyOk Whether the body expectation was met.
 * @param {string} detail Optional failure detail.
 */
function record(name, expected, actual, body, bodyOk, detail = '') {
  const ok = expected === actual && bodyOk;
  results.push({ name, expected, actual, hasBody: body.length > 0, bodyOk, ok, detail });
}

/**
 * Perform one HTTP request and return the status plus raw body.
 * @param {string} method HTTP method.
 * @param {string} path Path below BASE_URL, starting with "/".
 * @param {object} [body] Optional JSON body.
 * @returns {Promise<{status: number, text: string, json: any}>}
 */
async function request(method, path, body) {
  const headers = body === undefined ? undefined : { 'Content-Type': 'application/json' };
  const response = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await response.text();
  let json = null;
  if (text.length > 0) {
    try {
      json = JSON.parse(text);
    } catch {
      json = null;
    }
  }
  return { status: response.status, text, json };
}

/**
 * Check that a response body is a todo object with the expected shape.
 * @param {any} json Parsed response body.
 * @param {string} expectedTitle Expected title.
 * @param {boolean} expectedCompleted Expected completed flag.
 * @returns {boolean}
 */
function isTodo(json, expectedTitle, expectedCompleted) {
  return Boolean(
    json &&
      Number.isInteger(json.id) &&
      json.id > 0 &&
      json.title === expectedTitle &&
      json.completed === expectedCompleted
  );
}

async function main() {
  const stamp = new Date().toISOString();
  const createdTitle = `Smoke Test ${stamp}`;
  const updatedTitle = `Smoke Test Updated ${stamp}`;
  let createdId = null;

  // 1. GET /todos
  {
    const response = await request('GET', '/todos');
    const bodyOk = response.status === 200 && Array.isArray(response.json);
    record('GET /todos', 200, response.status, response.text, bodyOk);
  }

  // 2. POST /todos (valid)
  {
    const response = await request('POST', '/todos', { title: createdTitle, completed: false });
    const bodyOk = isTodo(response.json, createdTitle, false);
    if (bodyOk) createdId = response.json.id;
    record('POST /todos (valid)', 201, response.status, response.text, bodyOk);
  }

  if (createdId === null) {
    printResults();
    throw new Error('POST did not return a usable id; skipping dependent checks.');
  }

  // 3. PUT /todos/:id (valid)
  {
    const response = await request('PUT', `/todos/${createdId}`, {
      title: updatedTitle,
      completed: true,
    });
    const bodyOk = response.text.length === 0;
    record(`PUT /todos/${createdId}`, 204, response.status, response.text, bodyOk);
  }

  // 4. GET /todos confirms the update
  {
    const response = await request('GET', '/todos');
    const found = Array.isArray(response.json)
      ? response.json.find((todo) => todo.id === createdId)
      : null;
    const bodyOk = Boolean(found && found.title === updatedTitle && found.completed === true);
    record('GET /todos (after PUT)', 200, response.status, response.text, bodyOk);
  }

  // 5. DELETE /todos/:id (valid)
  {
    const response = await request('DELETE', `/todos/${createdId}`);
    const bodyOk = response.text.length === 0;
    record(`DELETE /todos/${createdId}`, 204, response.status, response.text, bodyOk);
  }

  // 6. DELETE the same id again -> 404 with a JSON message
  {
    const response = await request('DELETE', `/todos/${createdId}`);
    const bodyOk = Boolean(response.json && response.json.message);
    record(`DELETE /todos/${createdId} (again)`, 404, response.status, response.text, bodyOk);
  }

  // 7. POST with a blank title -> 400
  {
    const response = await request('POST', '/todos', { title: '   ', completed: false });
    const bodyOk = Boolean(response.json && response.json.message);
    record('POST /todos (blank title)', 400, response.status, response.text, bodyOk);
  }

  // 8. DELETE a missing id -> 404
  {
    const response = await request('DELETE', '/todos/9999');
    const bodyOk = Boolean(response.json && response.json.message);
    record('DELETE /todos/9999', 404, response.status, response.text, bodyOk);
  }

  printResults();
  if (results.some((entry) => !entry.ok)) {
    process.exitCode = 1;
  }
}

/** Print the collected results as a compact table. */
function printResults() {
  const header = ['Request', 'Expected', 'Actual', 'Body', 'Result'];
  const rows = results.map((entry) => [
    entry.name,
    String(entry.expected),
    String(entry.actual),
    entry.hasBody ? 'yes' : 'no',
    entry.ok ? 'PASS' : `FAIL${entry.detail ? ` (${entry.detail})` : ''}`,
  ]);
  const widths = header.map((value, index) =>
    Math.max(value.length, ...rows.map((row) => row[index].length))
  );
  const format = (row) =>
    row.map((value, index) => value.padEnd(widths[index])).join('  ');

  console.log(`\nTodo API smoke test -> ${BASE_URL}\n`);
  console.log(format(header));
  console.log(widths.map((width) => '-'.repeat(width)).join('  '));
  for (const row of rows) console.log(format(row));
  console.log();
}

main().catch((error) => {
  console.error(`Smoke test could not complete: ${error.message}`);
  console.error(`Is the API running at ${BASE_URL}?`);
  process.exitCode = 1;
});
