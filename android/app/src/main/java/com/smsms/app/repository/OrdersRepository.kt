package com.smsms.app.repository

import com.google.gson.Gson
import com.smsms.app.model.ApiErrorResponse
import com.smsms.app.model.Batch
import com.smsms.app.model.CreateBatchRequest
import com.smsms.app.model.CreateOrderRequest
import com.smsms.app.model.CreateQualityLogRequest
import com.smsms.app.model.Order
import com.smsms.app.model.QualityLog
import com.smsms.app.model.UpdateBatchStatusRequest
import com.smsms.app.network.ApiClient
import com.smsms.app.util.ApiResult
import retrofit2.Response
import java.io.IOException

// Sits between the ViewModels and Retrofit. Its one job: turn a Retrofit
// Response — which might be a 2xx with a body, OR a 4xx/5xx carrying an
// `{ "error": "..." }` JSON body, OR a thrown exception if the device
// can't reach the server at all — into a single ApiResult every screen
// can handle the same way.
object OrdersRepository {

    private val api = ApiClient.api
    private val gson = Gson()

    private fun <T> toResult(response: Response<T>): ApiResult<T> {
        if (response.isSuccessful) {
            val body = response.body()
            return if (body != null) ApiResult.Success(body) else ApiResult.Failure("Empty response from server.")
        }
        val message = try {
            val errorJson = response.errorBody()?.string()
            gson.fromJson(errorJson, ApiErrorResponse::class.java)?.error
        } catch (e: Exception) {
            null
        } ?: "Request failed (HTTP ${response.code()})."
        return ApiResult.Failure(message)
    }

    private suspend fun <T> safeCall(call: suspend () -> Response<T>): ApiResult<T> =
        try {
            toResult(call())
        } catch (e: IOException) {
            // Thrown by OkHttp when there's no network / the server can't
            // be reached at all (as opposed to reaching it and getting an
            // error status, which is handled above).
            ApiResult.Failure("Could not reach the server. Is the backend running and reachable? (${e.message})")
        }

    suspend fun createOrder(request: CreateOrderRequest): ApiResult<Order> = safeCall { api.createOrder(request) }

    suspend fun listOrders(): ApiResult<List<Order>> = safeCall { api.listOrders() }

    suspend fun getOrder(orderId: Int): ApiResult<Order> = safeCall { api.getOrder(orderId) }

    suspend fun closeOrder(orderId: Int): ApiResult<Order> = safeCall { api.closeOrder(orderId) }

    suspend fun createBatch(orderId: Int, quantity: Int): ApiResult<Batch> =
        safeCall { api.createBatch(orderId, CreateBatchRequest(quantity)) }

    suspend fun updateBatchStatus(batchId: Int, status: String): ApiResult<Batch> =
        safeCall { api.updateBatchStatus(batchId, UpdateBatchStatusRequest(status)) }

    suspend fun createQualityLog(batchId: Int, loggedBy: Int, qtyChecked: Int, defectCount: Int): ApiResult<QualityLog> =
        safeCall { api.createQualityLog(batchId, CreateQualityLogRequest(loggedBy, qtyChecked, defectCount)) }

    suspend fun listQualityLogs(batchId: Int): ApiResult<List<QualityLog>> =
        safeCall { api.listQualityLogs(batchId) }
}
