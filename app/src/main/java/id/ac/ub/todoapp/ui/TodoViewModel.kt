package id.ac.ub.todoapp.ui
import androidx.lifecycle.ViewModel
import id.ac.ub.todoapp.data.model.Todo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import id.ac.ub.todoapp.data.remote.RetrofitClient
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

    fun fetchTodos() {
        _isLoading.value = true

        api.getTodos().enqueue(object : Callback<List<Todo>> {

            override fun onResponse(
                call: Call<List<Todo>>,
                response: Response<List<Todo>>
            ) {
                _isLoading.value = false

                if (response.isSuccessful) {
                    _todos.value = response.body() ?: emptyList()
                    _error.value = null
                } else {
                    _error.value = "Server error: HTTP ${response.code()}"
                }
            }

            override fun onFailure(
                call: Call<List<Todo>>,
                t: Throwable
            ) {
                _isLoading.value = false
                _error.value = "Tidak dapat terhubung ke server: ${t.message}"
            }

            fun addTodo(todo: Todo) {
                api.addTodo(todo).enqueueThenRefresh("Tambah todo")
            }

            fun updateTodo(todo: Todo) {
                api.updateTodo(todo.id, todo).enqueueThenRefresh("Ubah todo")
            }

            fun deleteTodo(id: Int) {
                api.deleteTodo(id).enqueueThenRefresh("Hapus todo")
            }

            fun clearError() {
                _error.value = null
            }

            private fun <T> Call<T>.enqueueThenRefresh(action: String) {
                enqueue(object : Callback<T> {

                    override fun onResponse(
                        call: Call<T>,
                        response: Response<T>
                    ) {
                        if (response.isSuccessful) {
                            fetchTodos()
                        } else {
                            _error.value = "$action gagal: HTTP ${response.code()}"
                        }
                    }

                    override fun onFailure(
                        call: Call<T>,
                        t: Throwable
                    ) {
                        _error.value = "$action gagal: ${t.message}"
                    }
                })
            }
        })
    }

}

