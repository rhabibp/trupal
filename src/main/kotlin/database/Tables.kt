package com.newmotion.database


import com.newmotion.models_dtos.TransactionType
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// Tables
object Categories : LongIdTable("categories") {
    val name = varchar("name", 100).uniqueIndex()
    val description = text("description").nullable()
    val createdAt = datetime("created_at").default(Clock.System.now().toLocalDateTime(TimeZone.UTC))
}

object Parts : LongIdTable("parts") {
    val name = varchar("name", 200)
    val description = text("description").nullable()
    val partNumber = varchar("part_number", 100).uniqueIndex()
    val categoryId = reference("category_id", Categories)
    val unitPrice = decimal("unit_price", 10, 2)
    val currentStock = integer("current_stock").default(0)
    val minimumStock = integer("minimum_stock").default(0)
    val maxStock = integer("max_stock").nullable()
    val location = varchar("location", 100).nullable()
    val supplier = varchar("supplier", 200).nullable()
    val createdAt = datetime("created_at").default(Clock.System.now().toLocalDateTime(TimeZone.UTC))
    val updatedAt = datetime("updated_at").default(Clock.System.now().toLocalDateTime(TimeZone.UTC))
}

object Transactions : LongIdTable("transactions") {
    val partId = reference("part_id", Parts)
    val type = enumerationByName("type", 20, TransactionType::class)
    val quantity = integer("quantity")
    val unitPrice = decimal("unit_price", 10, 2).nullable()
    val totalAmount = decimal("total_amount", 10, 2).nullable()
    val recipientName = varchar("recipient_name", 200).nullable()
    val reason = text("reason").nullable()
    val isPaid = bool("is_paid").default(false)
    val amountPaid = decimal("amount_paid", 10, 2).default(0.toBigDecimal())
    val transactionDate = datetime("transaction_date").default(Clock.System.now().toLocalDateTime(TimeZone.UTC))
    val notes = text("notes").nullable()
    val createdAt = datetime("created_at").default(Clock.System.now().toLocalDateTime(TimeZone.UTC))
}

// Entities
class Category(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Category>(Categories)

    var name by Categories.name
    var description by Categories.description
    var createdAt by Categories.createdAt

    val parts by Part referrersOn Parts.categoryId
}

class Part(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Part>(Parts)

    var name by Parts.name
    var description by Parts.description
    var partNumber by Parts.partNumber
    var categoryId by Parts.categoryId
    var unitPrice by Parts.unitPrice
    var currentStock by Parts.currentStock
    var minimumStock by Parts.minimumStock
    var maxStock by Parts.maxStock
    var location by Parts.location
    var supplier by Parts.supplier
    var createdAt by Parts.createdAt
    var updatedAt by Parts.updatedAt

    val category by Category referencedOn Parts.categoryId
    val transactions by TransactionTable referrersOn Transactions.partId
}

class TransactionTable(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<TransactionTable>(Transactions)

    var partId by Transactions.partId
    var type by Transactions.type
    var quantity by Transactions.quantity
    var unitPrice by Transactions.unitPrice
    var totalAmount by Transactions.totalAmount
    var recipientName by Transactions.recipientName
    var reason by Transactions.reason
    var isPaid by Transactions.isPaid
    var amountPaid by Transactions.amountPaid
    var transactionDate by Transactions.transactionDate
    var notes by Transactions.notes
    var createdAt by Transactions.createdAt

    val part by Part referencedOn Transactions.partId
}
