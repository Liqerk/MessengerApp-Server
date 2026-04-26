package com.messenger.presentation.route

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File
import java.util.*

fun Route.fileUploadRoute() {
    
    val allowedExtensions = listOf("jpg", "jpeg", "png", "webp", "m4a", "mp3", "mp4", "gif")

    post("/upload") {
        try {
            var fileName = ""
            var savedFile: File? = null
            var hasError = false
            var errorMessage = ""

            val multipart = call.receiveMultipart()
            
            multipart.forEachPart { part ->
                if (hasError) {
                    part.dispose()
                    return@forEachPart
                }

                if (part is PartData.FileItem) {
                    val originalExt = part.originalFileName
                        ?.substringAfterLast('.', "")?.lowercase() ?: ""

                    if (originalExt !in allowedExtensions) {
                        hasError = true
                        errorMessage = "Недопустимый тип файла: $originalExt"
                        part.dispose()
                        return@forEachPart
                    }

                    fileName = "upload_${UUID.randomUUID()}.$originalExt"
                    val uploadDir = File("/app/uploads")
                    if (!uploadDir.exists()) uploadDir.mkdirs()

                    savedFile = File(uploadDir, fileName)
                    
                    part.streamProvider().use { input ->
                        savedFile!!.outputStream().buffered().use { output ->
                            input.copyTo(output)
                        }
                    }
                    println("✅ Uploaded: $fileName (${savedFile!!.length()} bytes)")
                }
                part.dispose()
            }

            if (hasError) {
                savedFile?.delete()
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to errorMessage))
                return@post
            }

            if (savedFile != null && savedFile!!.exists() && savedFile!!.length() > 0) {
                val url = "/files/$fileName"
                call.respond(HttpStatusCode.OK, mapOf("url" to url))
            } else {
                savedFile?.delete()
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Не удалось сохранить файл"))
            }
        } catch (e: Exception) {
            println("❌ Upload error: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
        }
    }
    
    staticFiles("/files", File("/app/uploads"))
}