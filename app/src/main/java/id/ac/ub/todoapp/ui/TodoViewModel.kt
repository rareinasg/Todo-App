package id.ac.ub.todoapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.ac.ub.todoapp.data.model.Todo
import id.ac.ub.todoapp.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.IOException

class TodoViewModel : ViewModel() {

    private val api = RetrofitClient.apiService

    private val _todos = MutableStateFlow<List<Todo>>(emptyList())
    val todos: StateFlow<List<Todo>> = _todos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        fetchTodos()
    }

    // READ
    fun fetchTodos() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = api.getTodos()
                if (response.isSuccessful) {
                    _todos.value = response.body() ?: emptyList()
                    _error.value = null
                } else {
                    _error.value = "Server error: HTTP ${response.code()}"
                }
            } catch (e: IOException) {
                _error.value = "Tidak dapat terhubung ke server: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // CREATE
    fun addTodo(todo: Todo) = mutate("Tambah todo") {
        api.addTodo(todo)
    }

    // UPDATE
    fun updateTodo(todo: Todo) = mutate("Update todo") {
        api.updateTodo(todo.id, todo)
    }

    // DELETE
    fun deleteTodo(id: Int) = mutate("Delete todo") {
        api.deleteTodo(id)
    }

    fun clearError() {
        _error.value = null
    }

    // Satu fungsi untuk semua operasi tulis: panggil API, cek hasil, muat ulang.
    private fun <T> mutate(action: String, call: suspend() -> Response<T>) {
        viewModelScope.launch {
            try {
                val response = call()
                if (response.isSuccessful) {
                    fetchTodos()
                } else {
                    _error.value = "$action gagal: HTTP ${response.code()}"
                }
            } catch (e: IOException) {
                _error.value = "$action gagal: ${e.message}"
            }
        }
    }
}