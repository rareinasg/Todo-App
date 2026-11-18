const express = require('express');
const cors = require('cors');
const mysql = require('mysql2/promise');

const app = express();
app.use(cors());
app.use(express.json());

// Log setiap request yang masuk, berguna untuk melihat apa yang dikirim Retrofit
app.use((req, res, next) => {
  console.log(`${new Date().toISOString()} ${req.method} ${req.originalUrl}`, req.body ?? '');
  next();
});

const pool = mysql.createPool({
  host: process.env.DB_HOST || '127.0.0.1',
  user: process.env.DB_USER || 'root',
  password: process.env.DB_PASSWORD || '',
  database: process.env.DB_NAME || 'tododb',
  waitForConnections: true,
  connectionLimit: 10,
});

// MySQL menyimpan BOOLEAN sebagai TINYINT(1), jadi nilainya 0/1.
// Gson di Android membutuhkan true/false, maka kita konversi di sini.
const toTodo = (row) => ({
  id: row.id,
  title: row.title,
  completed: Boolean(row.completed),
});

// GET /api/todos
app.get('/api/todos', async (req, res) => {
  try {
    const [rows] = await pool.query('SELECT id, title, completed FROM todos ORDER BY id');
    res.json(rows.map(toTodo));
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: 'Database error' });
  }
});

// POST /api/todos
app.post('/api/todos', async (req, res) => {
  const { title, completed = false } = req.body;
  if (typeof title !== 'string' || title.trim() === '') {
    return res.status(400).json({ message: 'title is required' });
  }
  try {
    const [result] = await pool.query(
      'INSERT INTO todos (title, completed) VALUES (?, ?)',
      [title.trim(), Boolean(completed)]
    );
    res.status(201).json({ id: result.insertId, title: title.trim(), completed: Boolean(completed) });
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: 'Database error' });
  }
});

// PUT /api/todos/:id
app.put('/api/todos/:id', async (req, res) => {
  const id = Number(req.params.id);
  const { title, completed } = req.body;
  if (!Number.isInteger(id)) return res.status(400).json({ message: 'invalid id' });
  if (typeof title !== 'string' || title.trim() === '' || typeof completed !== 'boolean') {
    return res.status(400).json({ message: 'title (string) and completed (boolean) are required' });
  }
  try {
    const [found] = await pool.query('SELECT id FROM todos WHERE id = ?', [id]);
    if (found.length === 0) return res.status(404).json({ message: 'todo not found' });
    await pool.query('UPDATE todos SET title = ?, completed = ? WHERE id = ?', [title.trim(), completed, id]);
    res.sendStatus(204); // tanpa body, cocok dengan Call<Void> di Retrofit
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: 'Database error' });
  }
});

// DELETE /api/todos/:id
app.delete('/api/todos/:id', async (req, res) => {
  const id = Number(req.params.id);
  if (!Number.isInteger(id)) return res.status(400).json({ message: 'invalid id' });
  try {
    const [result] = await pool.query('DELETE FROM todos WHERE id = ?', [id]);
    if (result.affectedRows === 0) return res.status(404).json({ message: 'todo not found' });
    res.sendStatus(204);
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: 'Database error' });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, '0.0.0.0', () => console.log(`Todo API running on http://0.0.0.0:${PORT}/api/`));
