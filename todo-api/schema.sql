-- Database setup for the Todo List hands-on (Bagian 1).
--
-- Safe to re-run:
--   * creates tododb and the todos table only when missing
--   * inserts the three seed rows only when the table is empty
--   * never deletes existing todos
--
-- Usage:
--   mysql -u root -p < todo-api/schema.sql

CREATE DATABASE IF NOT EXISTS tododb
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE tododb;

CREATE TABLE IF NOT EXISTS todos (
  id INT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  completed BOOLEAN DEFAULT FALSE
);

INSERT INTO todos (title, completed)
SELECT seed.title, seed.completed
FROM (
  SELECT 'Eat' AS title, FALSE AS completed
  UNION ALL SELECT 'Sleep', TRUE
  UNION ALL SELECT 'Work', FALSE
) AS seed
WHERE NOT EXISTS (SELECT 1 FROM todos);

SELECT * FROM todos;
DESCRIBE todos;
