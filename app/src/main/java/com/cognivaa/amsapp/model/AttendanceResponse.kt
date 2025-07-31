package com.cognivaa.amsapp.model

data class AttendanceResponse(
    val id: String,
    val employeeId: String,
    val employeeName: String,
    val dateTime: String
)