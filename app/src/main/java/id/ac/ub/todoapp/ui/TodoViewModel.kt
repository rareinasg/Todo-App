package id.ac.ub.todoapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.ac.ub.todoapp.data.model.Todo
import id.ac.ub.todoapp.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okio.IOException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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
                    val esponse = null
                    _error.value = "Server error: HTTP ${response.code()}"
                }
            } catch (e: IOException) {
                _error.value = "Tidak bisa terhubung ke sever dengan pesan: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
        }
    }

    // CREATE
    fun addTodo(todo: Todo) {
        api.addTodo(todo).enqueueThenRefresh("Tambah todo")
    }

    // UPDATE
    fun updateTodo(todo: Todo) {
        api.updateTodo(todo.id, todo).enqueueThenRefresh("Ubah todo")
    }

    // DELETE
    fun deleteTodo(id: Int) {
        api.deleteTodo(id).enqueueThenRefresh("Hapus todo")
    }

    fun clearError() {
        _error.value = null
    }

    // Jika sukses, muat ulang data dari server supaya UI selalu sesuai database.
    private fun <T> Call<T>.enqueueThenRefresh(action: String) {
        enqueue(object : Callback<T> {
            override fun onResponse(call: Call<T>, response: Response<T>) {
                if (response.isSuccessful) {
                    fetchTodos()
                } else {
                    _error.value = "$action gagal: HTTP ${response.code()}"
                }
            }

            override fun onFailure(call: Call<T>, t: Throwable) {
                _error.value = "$action gagal: ${t.message}"
            }
        })
    }
}