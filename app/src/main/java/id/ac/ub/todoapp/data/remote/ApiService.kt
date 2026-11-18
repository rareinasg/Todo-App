package id.ac.ub.todoapp.data.remote

import id.ac.ub.todoapp.data.model.Todo
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {
    @GET("todos")
    suspend fun getTodos(): Response<List<Todo>>

    @POST("todos")
    suspend fun addTodo(@Body todo: Todo): Response<Todo>

    @PUT("todos/{id}")
    suspend fun updateTodo(@Path("id") id: Int, @Body todo: Todo): Response<Unit>

    @DELETE("todos/{id}")
    suspend fun deleteTodo(@Path("id") id: Int): Response<Unit>
}