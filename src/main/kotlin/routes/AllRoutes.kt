package com.newmotion.routes


import com.newmotion.models_dtos.*
import com.newmotion.services.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    routing {
        categoryRoutes()
        partRoutes()
        transactionRoutes()
        statsRoutes()
    }
}

fun Route.categoryRoutes() {
    val categoryService by inject<CategoryService>()

    route("/api/categories") {
        get {
            try {
                val categories = categoryService.getAllCategories()
                call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = categories))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError,
                    ApiResponse<List<CategoryDto>>(success = false, error = e.message))
            }
        }

        get("/{id}") {
            try {
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest,
                        ApiResponse<CategoryDto>(success = false, error = "Invalid category ID"))

                val category = categoryService.getCategoryById(id)
                if (category != null) {
                    call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = category))
                } else {
                    call.respond(HttpStatusCode.NotFound,
                        ApiResponse<CategoryDto>(success = false, error = "Category not found"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError,
                    ApiResponse<CategoryDto>(success = false, error = e.message))
            }
        }

        post {
            try {
                val categoryDto = call.receive<CategoryDto>()
                val createdCategory = categoryService.createCategory(categoryDto)
                call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = createdCategory))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest,
                    ApiResponse<CategoryDto>(success = false, error = e.message))
            }
        }

        put("/{id}") {
            try {
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest,
                        ApiResponse<CategoryDto>(success = false, error = "Invalid category ID"))

                val categoryDto = call.receive<CategoryDto>()
                val updatedCategory = categoryService.updateCategory(id, categoryDto)

                if (updatedCategory != null) {
                    call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = updatedCategory))
                } else {
                    call.respond(HttpStatusCode.NotFound,
                        ApiResponse<CategoryDto>(success = false, error = "Category not found"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest,
                    ApiResponse<CategoryDto>(success = false, error = e.message))
            }
        }

        delete("/{id}") {
            try {
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest,
                        ApiResponse<Boolean>(success = false, error = "Invalid category ID"))

                val deleted = categoryService.deleteCategory(id)
                if (deleted) {
                    call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = true))
                } else {
                    call.respond(HttpStatusCode.NotFound,
                        ApiResponse<Boolean>(success = false, error = "Category not found"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError,
                    ApiResponse<Boolean>(success = false, error = e.message))
            }
        }
    }
}

fun Route.partRoutes() {
    val partService by inject<PartService>()

    route("/api/parts") {
        get {
            try {
                val parts = partService.getAllParts()
                call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = parts))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError,
                    ApiResponse<List<PartDto>>(success = false, error = e.message))
            }
        }

        get("/{id}") {
            try {
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest,
                        ApiResponse<PartDto>(success = false, error = "Invalid part ID"))

                val part = partService.getPartById(id)
                if (part != null) {
                    call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = part))
                } else {
                    call.respond(HttpStatusCode.NotFound,
                        ApiResponse<PartDto>(success = false, error = "Part not found"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError,
                    ApiResponse<PartDto>(success = false, error = e.message))
            }
        }

        post("/search") {
            try {
                val searchRequest = call.receive<SearchPartsRequest>()
                val result = partService.searchParts(searchRequest)
                call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = result))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest,
                    ApiResponse<PaginatedResponse<PartDto>>(success = false, error = e.message))
            }
        }

        get("/low-stock") {
            try {
                val lowStockParts = partService.getLowStockParts()
                call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = lowStockParts))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError,
                    ApiResponse<List<PartDto>>(success = false, error = e.message))
            }
        }

        post {
            try {
                val addPartRequest = call.receive<AddPartRequest>()
                val createdPart = partService.createPart(addPartRequest)
                call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = createdPart))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest,
                    ApiResponse<PartDto>(success = false, error = e.message))
            }
        }

        put("/{id}") {
            try {
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest,
                        ApiResponse<PartDto>(success = false, error = "Invalid part ID"))

                val updateRequest = call.receive<UpdatePartRequest>()
                val updatedPart = partService.updatePart(id, updateRequest)

                if (updatedPart != null) {
                    call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = updatedPart))
                } else {
                    call.respond(HttpStatusCode.NotFound,
                        ApiResponse<PartDto>(success = false, error = "Part not found"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest,
                    ApiResponse<PartDto>(success = false, error = e.message))
            }
        }

        delete("/{id}") {
            try {
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest,
                        ApiResponse<Boolean>(success = false, error = "Invalid part ID"))

                val deleted = partService.deletePart(id)
                if (deleted) {
                    call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = true))
                } else {
                    call.respond(HttpStatusCode.NotFound,
                        ApiResponse<Boolean>(success = false, error = "Part not found"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError,
                    ApiResponse<Boolean>(success = false, error = e.message))
            }
        }
    }
}

fun Route.transactionRoutes() {
    val transactionService by inject<TransactionService>()

    route("/api/transactions") {
        get {
            try {
                val transactions = transactionService.getAllTransactions()
                call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = transactions))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError,
                    ApiResponse<List<TransactionDto>>(success = false, error = e.message))
            }
        }

        get("/{id}") {
            try {
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest,
                        ApiResponse<TransactionDto>(success = false, error = "Invalid transaction ID"))

                val transaction = transactionService.getTransactionById(id)
                if (transaction != null) {
                    call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = transaction))
                } else {
                    call.respond(HttpStatusCode.NotFound,
                        ApiResponse<TransactionDto>(success = false, error = "Transaction not found"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError,
                    ApiResponse<TransactionDto>(success = false, error = e.message))
            }
        }

        get("/part/{partId}") {
            try {
                val partId = call.parameters["partId"]?.toLongOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest,
                        ApiResponse<List<TransactionDto>>(success = false, error = "Invalid part ID"))

                val transactions = transactionService.getTransactionsByPartId(partId)
                call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = transactions))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError,
                    ApiResponse<List<TransactionDto>>(success = false, error = e.message))
            }
        }

        get("/fast-moving") {
            try {
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
                val fastMovingParts = transactionService.getFastMovingParts(limit)
                call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = fastMovingParts))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError,
                    ApiResponse<List<FastMovingPartDto>>(success = false, error = e.message))
            }
        }

        post {
            try {
                val createRequest = call.receive<CreateTransactionRequest>()
                val createdTransaction = transactionService.createTransaction(createRequest)
                call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = createdTransaction))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest,
                    ApiResponse<TransactionDto>(success = false, error = e.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError,
                    ApiResponse<TransactionDto>(success = false, error = e.message))
            }
        }

        put("/{id}/payment") {
            try {
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest,
                        ApiResponse<TransactionDto>(success = false, error = "Invalid transaction ID"))

                val paymentRequest = call.receive<PaymentUpdateRequest>()
                val updatedTransaction = transactionService.updatePayment(id, paymentRequest)

                if (updatedTransaction != null) {
                    call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = updatedTransaction))
                } else {
                    call.respond(HttpStatusCode.NotFound,
                        ApiResponse<TransactionDto>(success = false, error = "Transaction not found"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest,
                    ApiResponse<TransactionDto>(success = false, error = e.message))
            }
        }

        delete("/{id}") {
            try {
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest,
                        ApiResponse<Boolean>(success = false, error = "Invalid transaction ID"))

                val deleted = transactionService.deleteTransaction(id)
                if (deleted) {
                    call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = true))
                } else {
                    call.respond(HttpStatusCode.NotFound,
                        ApiResponse<Boolean>(success = false, error = "Transaction not found"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError,
                    ApiResponse<Boolean>(success = false, error = e.message))
            }
        }
    }
}

fun Route.statsRoutes() {
    val statsService by inject<StatsService>()

    route("/api/stats") {
        get("/inventory") {
            try {
                val stats = statsService.getInventoryStats()
                call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = stats))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError,
                    ApiResponse<InventoryStatsDto>(success = false, error = e.message))
            }
        }

        get("/categories") {
            try {
                val categoryStats = statsService.getCategoryStats()
                call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = categoryStats))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError,
                    ApiResponse<List<CategoryStatsDto>>(success = false, error = e.message))
            }
        }
    }
}