package com.github.waterpeak.jsonfit

import okhttp3.MediaType
import okhttp3.RequestBody
import java.io.File

class MultipartFile(val filename: String, val body: RequestBody) {
    constructor(updateName: String, type: String, file: File) : this(updateName, RequestBody.create(MediaType.parse(type), file))

    constructor(type: String, file: File) : this(file.name, type, file)
}