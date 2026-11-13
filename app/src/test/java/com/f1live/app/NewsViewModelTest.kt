package com.f1live.app

import androidx.test.core.app.ApplicationProvider
import com.f1live.app.ui.F1ViewModel
import com.f1live.app.ui.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = F1LiveApp::class)
class NewsViewModelTest {

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun loadNewsReturnsNonEmptyLists() = kotlinx.coroutines.runBlocking {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = F1ViewModel(app)
        vm.loadNews()
        kotlinx.coroutines.withTimeout(45_000) {
            while (vm.news.value is UiState.Loading) kotlinx.coroutines.delay(500)
        }
        val state = vm.news.value
        println("NEWS_STATE=${state::class.simpleName}")
        if (state is UiState.Success) {
            println("LAST=${state.data.lastRace?.raceName} lastNews=${state.data.lastNews.size} | NEXT=${state.data.nextRace?.raceName} nextNews=${state.data.nextNews.size}")
            state.data.lastNews.take(3).forEach { println("  LAST-NEWS: ${it.title}") }
            state.data.nextNews.take(3).forEach { println("  NEXT-NEWS: ${it.title}") }
        } else if (state is UiState.Error) {
            println("NEWS_ERROR=${state.message}")
        }
        assertTrue("news state should be Success", state is UiState.Success)
        val s = state as UiState.Success
        assertTrue("last news empty!", s.data.lastNews.isNotEmpty())
        assertTrue("next news empty!", s.data.nextNews.isNotEmpty())
    }
}
