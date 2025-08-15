package com.newmotion.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    private lateinit var database: Database
    private lateinit var dataSource: HikariDataSource

    fun init(config: ApplicationConfig) {
        val dbConfig = config.config("database")

        val hikariConfig = HikariConfig().apply {
            jdbcUrl = dbConfig.property("url").getString()
            driverClassName = dbConfig.property("driver").getString()
            username = dbConfig.property("user").getString()
            password = dbConfig.property("password").getString()
            maximumPoolSize = dbConfig.property("maxPoolSize").getString().toInt()
            minimumIdle = dbConfig.property("minIdleConnections").getString().toInt()

            // Connection pool settings
            connectionTimeout = 30000
            idleTimeout = 600000
            maxLifetime = 1800000
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_READ_COMMITTED"

            // Performance tuning
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
            addDataSourceProperty("useServerPrepStmts", "true")
            addDataSourceProperty("useLocalSessionState", "true")
            addDataSourceProperty("rewriteBatchedStatements", "true")
        }

        dataSource = HikariDataSource(hikariConfig)
        database = Database.connect(dataSource)

        // Create tables
        transaction(database) {
            SchemaUtils.create(Categories, Parts, Transactions)
            println("Database tables created/verified successfully")
        }

        println("Database connected successfully!")
    }

    fun getDatabase(): Database {
        return if (::database.isInitialized) {
            database
        } else {
            throw IllegalStateException("Database not initialized. Call init() first.")
        }
    }

    fun close() {
        if (::dataSource.isInitialized) {
            dataSource.close()
            println("Database connection closed")
        }
    }
}

// Simplified transaction function that doesn't use suspend
fun <T> dbQuery(block: () -> T): T {
    return transaction { block() }
}