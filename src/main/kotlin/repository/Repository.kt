package com.newmotion.repository

import com.newmotion.database.*
import com.newmotion.models_dtos.*
import org.jetbrains.exposed.sql.*
import kotlinx.datetime.Clock
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.jetbrains.exposed.dao.id.EntityID
import java.math.BigDecimal

// Repository Interfaces
interface CategoryRepository {
    suspend fun findAll(): List<Category>
    suspend fun findById(id: Long): Category?
    suspend fun create(categoryDto: CategoryDto): Category
    suspend fun update(id: Long, categoryDto: CategoryDto): Category?
    suspend fun delete(id: Long): Boolean
}

interface PartRepository {
    suspend fun findAll(): List<Part>
    suspend fun findById(id: Long): Part?
    suspend fun search(request: SearchPartsRequest): Pair<List<Part>, Int>
    suspend fun create(request: AddPartRequest): Part
    suspend fun update(id: Long, request: UpdatePartRequest): Part?
    suspend fun delete(id: Long): Boolean
    suspend fun updateStock(partId: Long, newStock: Int): Boolean
    suspend fun findLowStockParts(): List<Part>
}

interface TransactionRepository {
    suspend fun findAll(): List<TransactionTable>
    suspend fun findById(id: Long): TransactionTable?
    suspend fun findByPartId(partId: Long): List<TransactionTable>
    suspend fun create(request: CreateTransactionRequest): TransactionTable
    suspend fun updatePayment(id: Long, request: PaymentUpdateRequest): TransactionTable?
    suspend fun delete(id: Long): Boolean
    suspend fun getFastMovingParts(limit: Int = 10): List<FastMovingPartDto>
}

interface StatsRepository {
    suspend fun getInventoryStats(): InventoryStatsDto
    suspend fun getCategoryStats(): List<CategoryStatsDto>
}

// Implementations
class CategoryRepositoryImpl : CategoryRepository {

    override suspend fun findAll(): List<Category> = dbQuery {
        Category.all().toList()
    }

    override suspend fun findById(id: Long): Category? = dbQuery {
        Category.findById(id)
    }

    override suspend fun create(categoryDto: CategoryDto): Category = dbQuery {
        Category.new {
            name = categoryDto.name
            description = categoryDto.description
        }
    }

    override suspend fun update(id: Long, categoryDto: CategoryDto): Category? = dbQuery {
        Category.findById(id)?.apply {
            name = categoryDto.name
            description = categoryDto.description
        }
    }

    override suspend fun delete(id: Long): Boolean = dbQuery {
        Category.findById(id)?.delete() != null
    }
}

class PartRepositoryImpl : PartRepository {

    override suspend fun findAll(): List<Part> = dbQuery {
        Part.all().toList()
    }

    override suspend fun findById(id: Long): Part? = dbQuery {
        Part.findById(id)
    }

    override suspend fun search(request: SearchPartsRequest): Pair<List<Part>, Int> = dbQuery {
        var query = Parts.selectAll()

        request.query?.let { searchQuery ->
            query = query.where {
                (Parts.name like "%$searchQuery%") or
                        (Parts.partNumber like "%$searchQuery%") or
                        (Parts.description like "%$searchQuery%")
            }
        }

        request.categoryId?.let { categoryId ->
            query = query.andWhere { Parts.categoryId eq categoryId }
        }

        request.lowStock?.let { lowStock ->
            if (lowStock) {
                query = query.andWhere { Parts.currentStock lessEq Parts.minimumStock }
            }
        }

        val total = query.count().toInt()
        val offset = ((request.page - 1) * request.limit).toLong()

        val parts = Part.wrapRows(
            query.limit(request.limit, offset)
        ).toList()

        Pair(parts, total)
    }

    override suspend fun create(request: AddPartRequest): Part = dbQuery {
        val part = Part.new {
            name = request.name
            description = request.description
            partNumber = request.partNumber
            categoryId = EntityID(request.categoryId, Categories)
            unitPrice = request.unitPrice.toBigDecimal()
            currentStock = request.initialStock
            minimumStock = request.minimumStock
            maxStock = request.maxStock
            location = request.location
            supplier = request.supplier
        }

        // Create initial stock transaction
        if (request.initialStock > 0) {
            TransactionTable.new {
                partId = part.id
                type = TransactionType.IN
                quantity = request.initialStock
                unitPrice = request.unitPrice.toBigDecimal()
                totalAmount = (request.unitPrice * request.initialStock).toBigDecimal()
                reason = "Initial stock"
                isPaid = true
                amountPaid = (request.unitPrice * request.initialStock).toBigDecimal()
            }
        }

        part
    }

    override suspend fun update(id: Long, request: UpdatePartRequest): Part? = dbQuery {
        Part.findById(id)?.apply {
            request.name?.let { name = it }
            request.description?.let { description = it }
            request.unitPrice?.let { unitPrice = it.toBigDecimal() }
            request.minimumStock?.let { minimumStock = it }
            request.maxStock?.let { maxStock = it }
            request.location?.let { location = it }
            request.supplier?.let { supplier = it }
            updatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        }
    }

    override suspend fun delete(id: Long): Boolean = dbQuery {
        Part.findById(id)?.delete() != null
    }

    override suspend fun updateStock(partId: Long, newStock: Int): Boolean = dbQuery {
        Part.findById(partId)?.let { part ->
            part.currentStock = newStock
            part.updatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            true
        } ?: false
    }

    override suspend fun findLowStockParts(): List<Part> = dbQuery {
        Part.find { Parts.currentStock lessEq Parts.minimumStock }.toList()
    }
}

class TransactionRepositoryImpl : TransactionRepository {

    override suspend fun findAll(): List<TransactionTable> = dbQuery {
        TransactionTable.all().orderBy(Transactions.createdAt to SortOrder.DESC).toList()
    }

    override suspend fun findById(id: Long): TransactionTable? = dbQuery {
        TransactionTable.findById(id)
    }

    override suspend fun findByPartId(partId: Long): List<TransactionTable> = dbQuery {
        TransactionTable.find { Transactions.partId eq partId }
            .orderBy(Transactions.createdAt to SortOrder.DESC)
            .toList()
    }

    override suspend fun create(request: CreateTransactionRequest): TransactionTable = dbQuery {
        val part = Part.findById(request.partId) ?: throw IllegalArgumentException("Part not found")

        val unitPrice = request.unitPrice?.toBigDecimal() ?: part.unitPrice
        val totalAmount = (unitPrice.toDouble() * request.quantity).toBigDecimal()

        val transactionTable = TransactionTable.new {
            partId = EntityID(request.partId, Parts)
            type = request.type
            quantity = request.quantity
            this.unitPrice = unitPrice
            this.totalAmount = totalAmount
            recipientName = request.recipientName
            reason = request.reason
            isPaid = request.isPaid
            amountPaid = request.amountPaid.toBigDecimal()
        }

        // Update part stock
        when (request.type) {
            TransactionType.IN -> {
                part.currentStock += request.quantity
            }
            TransactionType.OUT -> {
                part.currentStock -= request.quantity
                if (part.currentStock < 0) part.currentStock = 0
            }
            TransactionType.ADJUSTMENT -> {
                part.currentStock = request.quantity
            }
        }
        part.updatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        transactionTable
    }

    override suspend fun updatePayment(id: Long, request: PaymentUpdateRequest): TransactionTable? = dbQuery {
        TransactionTable.findById(id)?.apply {
            amountPaid = request.amountPaid.toBigDecimal()
            isPaid = request.isPaid
        }
    }

    override suspend fun delete(id: Long): Boolean = dbQuery {
        TransactionTable.findById(id)?.delete() != null
    }

    override suspend fun getFastMovingParts(limit: Int): List<FastMovingPartDto> = dbQuery {
        val result = (Transactions innerJoin Parts)
            .select(
                Parts.id, Parts.name,
                Transactions.quantity.sum(),
                Transactions.id.count()
            )
            .where { Transactions.type eq TransactionType.OUT }
            .groupBy(Parts.id, Parts.name)
            .orderBy(Transactions.quantity.sum(), SortOrder.DESC)
            .limit(limit)

        result.map {
            FastMovingPartDto(
                partId = it[Parts.id].value,
                partName = it[Parts.name],
                totalOutQuantity = it[Transactions.quantity.sum()] ?: 0,
                transactionCount = it[Transactions.id.count()].toInt(),
                averagePerMonth = ((it[Transactions.quantity.sum()] ?: 0) / 12.0) // Simplified calculation
            )
        }
    }
}

class StatsRepositoryImpl : StatsRepository {

    override suspend fun getInventoryStats(): InventoryStatsDto = dbQuery {
        val totalCategories = Categories.selectAll().count().toInt()
        val totalParts = Parts.selectAll().count().toInt()

        val totalValue = Parts.selectAll().map { row ->
            row[Parts.currentStock] * row[Parts.unitPrice].toDouble()
        }.sum()

        val lowStockParts = Part.find { Parts.currentStock lessEq Parts.minimumStock }.toList()

        // Get fast moving parts inline
        val fastMovingResult = (Transactions innerJoin Parts)
            .select(
                Parts.id, Parts.name,
                Transactions.quantity.sum(),
                Transactions.id.count()
            )
            .where { Transactions.type eq TransactionType.OUT }
            .groupBy(Parts.id, Parts.name)
            .orderBy(Transactions.quantity.sum(), SortOrder.DESC)
            .limit(5)

        val fastMovingParts = fastMovingResult.map {
            FastMovingPartDto(
                partId = it[Parts.id].value,
                partName = it[Parts.name],
                totalOutQuantity = it[Transactions.quantity.sum()] ?: 0,
                transactionCount = it[Transactions.id.count()].toInt(),
                averagePerMonth = ((it[Transactions.quantity.sum()] ?: 0) / 12.0)
            )
        }

        // Get category stats inline
        val categoryStats = (Categories leftJoin Parts)
            .select(
                Categories.id, Categories.name,
                Parts.id.count(),
                Parts.currentStock.sum(),
                Parts.unitPrice.sum()
            )
            .groupBy(Categories.id, Categories.name)
            .orderBy(Parts.id.count(), SortOrder.DESC)

        val topCategories = categoryStats.take(5).map { row ->
            val categoryId = row[Categories.id].value
            val lowStockCount = Part.find {
                (Parts.categoryId eq categoryId) and (Parts.currentStock lessEq Parts.minimumStock)
            }.count().toInt()

            CategoryStatsDto(
                categoryId = categoryId,
                categoryName = row[Categories.name],
                partCount = row[Parts.id.count()].toInt(),
                totalValue = (row[Parts.currentStock.sum()] ?: 0) * (row[Parts.unitPrice.sum()]?.toDouble() ?: 0.0),
                lowStockCount = lowStockCount
            )
        }

        InventoryStatsDto(
            totalCategories = totalCategories,
            totalParts = totalParts,
            totalValue = totalValue,
            lowStockParts = lowStockParts.map { it.toDto() },
            fastMovingParts = fastMovingParts,
            topCategories = topCategories
        )
    }

    override suspend fun getCategoryStats(): List<CategoryStatsDto> = dbQuery {
        val stats = (Categories leftJoin Parts)
            .select(
                Categories.id, Categories.name,
                Parts.id.count(),
                Parts.currentStock.sum(),
                Parts.unitPrice.sum()
            )
            .groupBy(Categories.id, Categories.name)
            .orderBy(Parts.id.count(), SortOrder.DESC)

        stats.map { row ->
            val categoryId = row[Categories.id].value
            val lowStockCount = Part.find {
                (Parts.categoryId eq categoryId) and (Parts.currentStock lessEq Parts.minimumStock)
            }.count().toInt()

            CategoryStatsDto(
                categoryId = categoryId,
                categoryName = row[Categories.name],
                partCount = row[Parts.id.count()].toInt(),
                totalValue = (row[Parts.currentStock.sum()] ?: 0) * (row[Parts.unitPrice.sum()]?.toDouble() ?: 0.0),
                lowStockCount = lowStockCount
            )
        }
    }
}

// Extension functions to convert entities to DTOs
fun Category.toDto(): CategoryDto {
    return CategoryDto(
        id = this.id.value,
        name = this.name,
        description = this.description,
        createdAt = this.createdAt.toInstant(TimeZone.UTC)
    )
}
fun Part.toDto(): PartDto {
    return PartDto(
        id = this.id.value,
        name = this.name,
        description = this.description,
        partNumber = this.partNumber,
        categoryId = this.categoryId.value,
        categoryName = try {
            // Safely access category name within transaction
            this.category.name
        } catch (e: Exception) {
            null
        },
        unitPrice = this.unitPrice.toDouble(),
        currentStock = this.currentStock,
        minimumStock = this.minimumStock,
        maxStock = this.maxStock,
        location = this.location,
        supplier = this.supplier,
        createdAt = this.createdAt.toInstant(TimeZone.UTC),
        updatedAt = this.updatedAt.toInstant(TimeZone.UTC)
    )
}

fun TransactionTable.toDto(): TransactionDto {
    return TransactionDto(
        id = this.id.value,
        partId = this.partId.value,
        partName = try {
            // Safely access part name within transaction
            this.part.name
        } catch (e: Exception) {
            null
        },
        type = this.type,
        quantity = this.quantity,
        unitPrice = this.unitPrice?.toDouble(),
        totalAmount = this.totalAmount?.toDouble(),
        recipientName = this.recipientName,
        reason = this.reason,
        isPaid = this.isPaid,
        amountPaid = this.amountPaid.toDouble(),
        transactionDate = this.transactionDate.toInstant(TimeZone.UTC),
        notes = this.notes
    )
}

// Helper function to safely convert BigDecimal to Double
private fun BigDecimal.toDouble(): Double = this.toDouble()