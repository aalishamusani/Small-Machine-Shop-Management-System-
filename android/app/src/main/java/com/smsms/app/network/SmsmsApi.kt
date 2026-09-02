package com.smsms.app.network

import com.smsms.app.model.Batch
import com.smsms.app.model.CreateBatchRequest
import com.smsms.app.model.CreateOrderRequest
import com.smsms.app.model.Order
import com.smsms.app.model.UpdateBatchStatusRequest
import com.smsms.app.model.UpdateOrderRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// One function per backend endpoint. Retrofit generates the actual HTTP
// call implementation from these annotations at runtime — this interface
// is never implemented by hand.
interface SmsmsApi {

    @POST("api/orders")
    suspend fun createOrder(@Body body: CreateOrderRequest): Response<Order>

    @GET("api/orders")
    suspend fun listOrders(
        @Query("status") status: String? = null,
        @Query("customer_name") customerName: String? = null
    ): Response<List<Order>>

    @GET("api/orders/{orderId}")
    suspend fun getOrder(@Path("orderId") orderId: Int): Response<Order>

    @PATCH("api/orders/{orderId}")
    suspend fun updateOrder(@Path("orderId") orderId: Int, @Body body: UpdateOrderRequest): Response<Order>

    @PATCH("api/orders/{orderId}/close")
    suspend fun closeOrder(@Path("orderId") orderId: Int): Response<Order>

    @POST("api/orders/{orderId}/batches")
    suspend fun createBatch(@Path("orderId") orderId: Int, @Body body: CreateBatchRequest): Response<Batch>

    @GET("api/orders/{orderId}/batches")
    suspend fun listBatches(@Path("orderId") orderId: Int): Response<List<Batch>>

    @PATCH("api/batches/{batchId}/status")
    suspend fun updateBatchStatus(@Path("batchId") batchId: Int, @Body body: UpdateBatchStatusRequest): Response<Batch>
}
