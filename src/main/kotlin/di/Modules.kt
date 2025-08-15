package com.newmotion.di



import com.newmotion.database.*
import com.newmotion.repository.*
import com.newmotion.services.*
import io.ktor.server.config.*
import org.koin.dsl.module



val appModule = module {

    // Repositories - no database dependency injection needed since we use DatabaseFactory
    single<CategoryRepository> { CategoryRepositoryImpl() }
    single<PartRepository> { PartRepositoryImpl() }
    single<TransactionRepository> { TransactionRepositoryImpl() }
    single<StatsRepository> { StatsRepositoryImpl() }

    // Services
    single<CategoryService> { CategoryServiceImpl(get()) }
    single<PartService> { PartServiceImpl(get()) }
    single<TransactionService> { TransactionServiceImpl(get(), get()) }
    single<StatsService> { StatsServiceImpl(get()) }
}