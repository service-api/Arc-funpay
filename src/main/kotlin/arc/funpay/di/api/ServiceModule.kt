package arc.funpay.di.api

import org.koin.core.module.Module

interface ServiceModule {
    fun createModule(): Module
}