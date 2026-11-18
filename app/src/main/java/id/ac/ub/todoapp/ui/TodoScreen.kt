package id.ac.ub.todoapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import id.ac.ub.todoapp.data.model.Todo

@Composable
fun TodoScreen(modifier: Modifier = Modifier, vm: TodoViewModel = viewModel()) {

    // View hanya mengamati state dari ViewModel
    val todos by vm.todos.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()

    // State lokal UI: isi kolom input. Tidak perlu ke ViewModel.
    var todoTitle by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {

        // ---- Input todo baru ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = todoTitle,
                onValueChange = { todoTitle = it },
                label = { Text("Todo Title") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                if (todoTitle.isNotBlank()) {
                    vm.addTodo(Todo(title = todoTitle.trim()))
                    todoTitle = ""
                }
            }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ---- Status: loading dan error ----
        if (isLoading) {
            CircularProgressIndicator()
        }
        error?.let { message ->
            Text(text = message, color = MaterialTheme.colorScheme.error)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.fetchTodos() }) { Text("Coba lagi") }
                Button(onClick = { vm.clearError() }) { Text("Tutup") }
            }
        }

        // ---- Daftar todo ----
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(todos, key = { it.id }) { todo ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = todo.completed,
                        onCheckedChange = { checked ->
                            vm.updateTodo(todo.copy(completed = checked))
                        }
                    )
                    Text(
                        text = todo.title,
                        modifier = Modifier.weight(1f),
                        textDecoration = if (todo.completed) TextDecoration.LineThrough else null
                    )
                    IconButton(onClick = { vm.deleteTodo(todo.id) }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
        }
    }
}
