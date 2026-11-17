package id.ac.ub.todoapp.data.model

data class Todo(
    val id: Int = 0,
    val title: String,
    val completed: Boolean = false
)