package com.unisight.gropos.core.di

import com.unisight.gropos.features.lottery.data.FakeLotteryRepository
import com.unisight.gropos.features.lottery.domain.repository.LotteryRepository
import org.koin.dsl.module

/**
 * Koin dependency injection module for the Lottery feature.
 * 
 * **Per Phase 5 (Lottery Module):**
 * - Provides LotteryRepository (Fake for now, Remote later)
 * - Provides LotteryViewModel (when created)
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
    // ViewModels (to be added when UI is implemented)
    // ========================================================================
    
    // TODO: Add LotteryViewModel when LotteryScreen is created
    // factory { LotteryViewModel(get()) }
    
    // TODO: Add LotterySaleViewModel
    // factory { LotterySaleViewModel(get()) }
    
    // TODO: Add LotteryPayoutViewModel
    // factory { LotteryPayoutViewModel(get()) }
}

