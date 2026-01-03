package com.unisight.gropos.core.di

import com.unisight.gropos.features.lottery.data.FakeLotteryRepository
import com.unisight.gropos.features.lottery.domain.repository.LotteryRepository
import com.unisight.gropos.features.lottery.presentation.LotteryPayoutViewModel
import com.unisight.gropos.features.lottery.presentation.LotteryReportViewModel
import com.unisight.gropos.features.lottery.presentation.LotterySaleViewModel
import org.koin.dsl.module

/**
 * Koin dependency injection module for the Lottery feature.
 * 
 * **Per Phase 5 (Lottery Module):**
 * - Provides LotteryRepository (Fake for now, Remote later)
 * - Provides LotterySaleViewModel for sales screen
 * - Provides LotteryPayoutViewModel for payout screen
 * 
 * **Current Implementation:**
 * Uses FakeLotteryRepository with seeded games.
 * 
 * **Future:**
 * Will switch to RemoteLotteryRepository when API is ready.
 * 
 * Per project-structure.mdc: DI modules are in core/di.
 */
val lotteryModule = module {
    
    // ========================================================================
    // Repository
    // ========================================================================
    
    /**
     * Provides LotteryRepository.
     * 
     * Currently: FakeLotteryRepository (for development)
     * Future: RemoteLotteryRepository (for production)
     */
    single<LotteryRepository> {
        FakeLotteryRepository()
    }
    
    // ========================================================================
    // ViewModels
    // ========================================================================
    
    /**
     * LotterySaleViewModel - handles lottery ticket sales.
     * 
     * Factory scope: New instance per screen.
     * 
     * Note: staffId should come from AuthRepository/SessionManager in production.
     * Using hardcoded 100 for development.
     */
    factory {
        LotterySaleViewModel(
            repository = get(),
            staffId = 100 // TODO: Get from session manager
        )
    }
    
    /**
     * LotteryPayoutViewModel - handles lottery payout processing.
     * 
     * Factory scope: New instance per screen.
     * 
     * Note: staffId should come from AuthRepository/SessionManager in production.
     * Using hardcoded 100 for development.
     */
    factory {
        LotteryPayoutViewModel(
            repository = get(),
            staffId = 100 // TODO: Get from session manager
        )
    }
    
    /**
     * LotteryReportViewModel - displays lottery sales/payouts summary.
     * 
     * Factory scope: New instance per screen.
     */
    factory {
        LotteryReportViewModel(repository = get())
    }
}

