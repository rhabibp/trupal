package com.newmotion.services

import com.newmotion.database.dbQuery
import com.newmotion.models_dtos.*
import com.newmotion.repository.*


interface CategoryService {
    suspend fun getAllCategories(): List<CategoryDto>
    suspend fun getCategoryById(id: Long): CategoryDto?
    suspend fun createCategory(categoryDto: CategoryDto): CategoryDto
    suspend fun updateCategory(id: Long, categoryDto: CategoryDto): CategoryDto?
    suspend fun deleteCategory(id: Long): Boolean
}

interface PartService {
    suspend fun getAllParts(): List<PartDto>
    suspend fun getPartById(id: Long): PartDto?
    suspend fun searchParts(request: SearchPartsRequest): PaginatedResponse<PartDto>
    suspend fun createPart(request: AddPartRequest): PartDto
    suspend fun updatePart(id: Long, request: UpdatePartRequest): PartDto?
    suspend fun deletePart(id: Long): Boolean
    suspend fun getLowStockParts(): List<PartDto>
}

interface TransactionService {
    suspend fun getAllTransactions(): List<TransactionDto>
    suspend fun getTransactionById(id: Long): TransactionDto?
    suspend fun getTransactionsByPartId(partId: Long): List<TransactionDto>
    suspend fun createTransaction(request: CreateTransactionRequest): TransactionDto
    suspend fun updatePayment(id: Long, request: PaymentUpdateRequest): TransactionDto?
    suspend fun deleteTransaction(id: Long): Boolean
    suspend fun getFastMovingParts(limit: Int = 10): List<FastMovingPartDto>
}

interface StatsService {
    suspend fun getInventoryStats(): InventoryStatsDto
    suspend fun getCategoryStats(): List<CategoryStatsDto>
}


class PartServiceImpl(
    private val partRepository: PartRepository
) : PartService {

    override suspend fun getAllParts(): List<PartDto> = dbQuery {
        partRepository.findAll().map { it.toDto() }
    }

    override suspend fun getPartById(id: Long): PartDto? = dbQuery {
        partRepository.findById(id)?.toDto()
    }

    override suspend fun searchParts(request: SearchPartsRequest): PaginatedResponse<PartDto> = dbQuery {
        val (parts, total) = partRepository.search(request)
        val totalPages = (total + request.limit - 1) / request.limit

        PaginatedResponse(
            data = parts.map { it.toDto() },
            page = request.page,
            limit = request.limit,
            total = total,
            totalPages = totalPages
        )
    }

    override suspend fun createPart(request: AddPartRequest): PartDto = dbQuery {
        partRepository.create(request).toDto()
    }

    override suspend fun updatePart(id: Long, request: UpdatePartRequest): PartDto? = dbQuery {
        partRepository.update(id, request)?.toDto()
    }

    override suspend fun deletePart(id: Long): Boolean {
        return partRepository.delete(id)
    }

    override suspend fun getLowStockParts(): List<PartDto> = dbQuery {
        partRepository.findLowStockParts().map { it.toDto() }
    }
}
class TransactionServiceImpl(
    private val transactionRepository: TransactionRepository,
    private val partRepository: PartRepository
) : TransactionService {

    override suspend fun getAllTransactions(): List<TransactionDto> = dbQuery {
        transactionRepository.findAll().map { it.toDto() }
    }

    override suspend fun getTransactionById(id: Long): TransactionDto? = dbQuery {
        transactionRepository.findById(id)?.toDto()
    }

    override suspend fun getTransactionsByPartId(partId: Long): List<TransactionDto> = dbQuery {
        transactionRepository.findByPartId(partId).map { it.toDto() }
    }

    override suspend fun createTransaction(request: CreateTransactionRequest): TransactionDto = dbQuery {
        // Validate part exists
        partRepository.findById(request.partId)
            ?: throw IllegalArgumentException("Part with id ${request.partId} not found")

        // Validate stock for OUT transactions
        if (request.type == TransactionType.OUT) {
            val part = partRepository.findById(request.partId)!!
            if (part.currentStock < request.quantity) {
                throw IllegalArgumentException("Insufficient stock. Available: ${part.currentStock}, Requested: ${request.quantity}")
            }
        }

        transactionRepository.create(request).toDto()
    }

    override suspend fun updatePayment(id: Long, request: PaymentUpdateRequest): TransactionDto? = dbQuery {
        transactionRepository.updatePayment(id, request)?.toDto()
    }

    override suspend fun deleteTransaction(id: Long): Boolean {
        return transactionRepository.delete(id)
    }

    override suspend fun getFastMovingParts(limit: Int): List<FastMovingPartDto> {
        return transactionRepository.getFastMovingParts(limit)
    }
}

class CategoryServiceImpl(
    private val categoryRepository: CategoryRepository
) : CategoryService {

    override suspend fun getAllCategories(): List<CategoryDto> = dbQuery {
        categoryRepository.findAll().map { it.toDto() }
    }

    override suspend fun getCategoryById(id: Long): CategoryDto? = dbQuery {
        categoryRepository.findById(id)?.toDto()
    }

    override suspend fun createCategory(categoryDto: CategoryDto): CategoryDto = dbQuery {
        categoryRepository.create(categoryDto).toDto()
    }

    override suspend fun updateCategory(id: Long, categoryDto: CategoryDto): CategoryDto? = dbQuery {
        categoryRepository.update(id, categoryDto)?.toDto()
    }

    override suspend fun deleteCategory(id: Long): Boolean {
        return categoryRepository.delete(id)
    }
}

class StatsServiceImpl(
    private val statsRepository: StatsRepository
) : StatsService {

    override suspend fun getInventoryStats(): InventoryStatsDto = dbQuery {
        statsRepository.getInventoryStats()
    }

    override suspend fun getCategoryStats(): List<CategoryStatsDto> = dbQuery {
        statsRepository.getCategoryStats()
    }
}