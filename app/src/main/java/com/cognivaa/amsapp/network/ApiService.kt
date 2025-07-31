package com.cognivaa.amsapp.network

import com.cognivaa.amsapp.model.AttendanceRequest
import com.cognivaa.amsapp.model.AttendanceResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface ApiService {
    @Headers("Content-Type: application/json")
    @POST("api/attendances/by-employee-code")
    fun getAttendance(@Body request: AttendanceRequest): Call<AttendanceResponse>
}