package com.posthog.android.internal

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.posthog.PostHog
import com.posthog.android.FakeLifecycle
import com.posthog.android.PostHogAndroidConfig
import com.posthog.android.apiKey
import com.posthog.android.createPostHogFake
import com.posthog.android.mockPackageInfo
import com.posthog.android.replay.PostHogReplayIntegration
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
internal class PostHogLifecycleObserverIntegrationTest {
    private val context = mock<Context>()
    private val fakeLifecycle = FakeLifecycle(Lifecycle.State.CREATED)

    private fun getSut(): PostHogLifecycleObserverIntegration {
        val config = PostHogAndroidConfig(apiKey)
        val mainHandler = MainHandler()
        val replay = PostHogReplayIntegration(context, config, mainHandler)
        return PostHogLifecycleObserverIntegration(context, config, replay, mainHandler, lifecycle = fakeLifecycle)
    }

    @BeforeTest
    fun `set up`() {
        PostHog.resetSharedInstance()
    }

    @Test
    fun `install adds the observer`() {
        val sut = getSut()

        sut.install()

        assertEquals(1, fakeLifecycle.observers)
    }

    @Test
    fun `uninstall removes the observer`() {
        val sut = getSut()

        sut.install()
        sut.uninstall()

        assertEquals(0, fakeLifecycle.observers)
    }

    @Test
    fun `onStart captures app opened - cold state`() {
        val sut = getSut()

        val fake = createPostHogFake()
        context.mockPackageInfo("1.0.0", 1)

        sut.onStart(ProcessLifecycleOwner.get())

        assertEquals("Application Opened", fake.event)
        assertEquals("1.0.0", fake.properties?.get("version"))
        assertEquals(1L, fake.properties?.get("build"))
        assertEquals(false, fake.properties?.get("from_background"))
    }

    @Test
    fun `onStart captures app opened - warm state`() {
        val sut = getSut()

        val fake = createPostHogFake()
        context.mockPackageInfo("1.0.0", 1)

        sut.onStart(ProcessLifecycleOwner.get())
        sut.onStart(ProcessLifecycleOwner.get())

        assertEquals("Application Opened", fake.event)
        assertEquals(true, fake.properties?.get("from_background"))
    }

    @Test
    fun `onStart captures app backgrounded`() {
        val sut = getSut()

        val fake = createPostHogFake()

        sut.onStart(ProcessLifecycleOwner.get())
        sut.onStop(ProcessLifecycleOwner.get())

        assertEquals("Application Backgrounded", fake.event)
    }
}
