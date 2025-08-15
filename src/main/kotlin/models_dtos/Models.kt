package com.newmotion.models_dtos



import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

// DTOs for API
@Serializable
data class CategoryDto(
    val id: Long? = null,
    val name: String,
    val description: String? = null,
    val createdAt: Instant? = null
)

@Serializable
data class PartDto(
    val id: Long? = null,
    val name: String,
    val description: String? = null,
    val partNumber: String,
    val categoryId: Long,
    val categoryName: String? = null,
    val unitPrice: Double,
    val currentStock: Int,
    val minimumStock: Int,
    val maxStock: Int? = null,
    val location: String? = null,
    val supplier: String? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null
)

@Serializable
data class TransactionDto(
    val id: Long? = null,
    val partId: Long,
    val partName: String? = null,
    val type: TransactionType,
    val quantity: Int,
    val unitPrice: Double? = null,
    val totalAmount: Double? = null,
    val recipientName: String? = null,
    val reason: String? = null,
    val isPaid: Boolean = false,
    val amountPaid: Double = 0.0,
    val transactionDate: Instant? = null,
    val notes: String? = null
)

@Serializable
enum class TransactionType {
    IN, OUT, ADJUSTMENT
}

@Serializable
data class InventoryStatsDto(
    val totalCategories: Int,
    val totalParts: Int,
    val totalValue: Double,
    val lowStockParts: List<PartDto>,
    val fastMovingParts: List<FastMovingPartDto>,
    val topCategories: List<CategoryStatsDto>
)

@Serializable
data class FastMovingPartDto(
    val partId: Long,
    val partName: String,
    val totalOutQuantity: Int,
    val transactionCount: Int,
    val averagePerMonth: Double
)

@Serializable
data class CategoryStatsDto(
    val categoryId: Long,
    val categoryName: String,
    val partCount: Int,
    val totalValue: Double,
    val lowStockCount: Int
)

@Serializable
data class SearchPartsRequest(
    val query: String? = null,
    val categoryId: Long? = null,
    val lowStock: Boolean? = null,
    val page: Int = 1,
    val limit: Int = 20
)

@Serializable
data class AddPartRequest(
    val name: String,
    val description: String? = null,
    val partNumber: String,
    val categoryId: Long,
    val unitPrice: Double,
    val initialStock: Int,
    val minimumStock: Int,
    val maxStock: Int? = null,
    val location: String? = null,
    val supplier: String? = null
)

@Serializable
data class UpdatePartRequest(
    val name: String? = null,
    val description: String? = null,
    val unitPrice: Double? = null,
    val minimumStock: Int? = null,
    val maxStock: Int? = null,
    val location: String? = null,
    val supplier: String? = null
)

@Serializable
data class CreateTransactionRequest(
    val partId: Long,
    val type: TransactionType,
    val quantity: Int,
    val unitPrice: Double? = null,
    val recipientName: String? = null,
    val reason: String? = null,
    val isPaid: Boolean = false,
    val amountPaid: Double = 0.0,
    val notes: String? = null
)

@Serializable
data class PaymentUpdateRequest(
    val amountPaid: Double,
    val isPaid: Boolean = false
)

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val error: String? = null
)

@Serializable
data class PaginatedResponse<T>(
    val data: List<T>,
    val page: Int,
    val limit: Int,
    val total: Int,
    val totalPages: Int
)