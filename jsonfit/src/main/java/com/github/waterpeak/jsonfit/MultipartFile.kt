package com.github.waterpeak.jsonfit

import okhttp3.RequestBody

class MultipartFile(val filename: String, val body: RequestBody)