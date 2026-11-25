package com.example.cartproject.data.local

import com.example.cartproject.domain.AppError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertSame
import org.junit.Test

/** Проверяет, что локальное хранилище сохраняет отмену корутины. */
class LocalOutcomeTest {
    /** Явная отмена не превращается в сбой записи. */
    @Test
    fun propagatesCancellation() = runTest {
        val cancelled = CancellationException("storage operation cancelled")
        try {
            sqliteOutcome<Unit>(AppError.LocalWrite) { throw cancelled }
            error("Expected cancellation")
        } catch (failure: CancellationException) {
            assertSame(cancelled, failure)
        }
    }
}
