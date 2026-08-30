package com.customwidgets.app.i18n

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.customwidgets.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class StringResourcesTest {

    @Test
    @Config(qualifiers = "en")
    fun englishStrings_loadedCorrectly() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Custom Widgets", context.getString(R.string.app_name))
        assertEquals("My Widgets", context.getString(R.string.title_gallery))
        assertEquals("Create Widget", context.getString(R.string.title_create))
        assertEquals("AI Settings", context.getString(R.string.title_settings))
    }

    @Test
    @Config(qualifiers = "ko")
    fun koreanStrings_loadedCorrectly() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("커스텀 위젯", context.getString(R.string.app_name))
        assertEquals("내 위젯", context.getString(R.string.title_gallery))
        assertEquals("위젯 만들기", context.getString(R.string.title_create))
        assertEquals("AI API 설정", context.getString(R.string.title_settings))
        assertEquals("크기 선택", context.getString(R.string.step_select_size))
        assertEquals("위젯 설명", context.getString(R.string.step_describe))
    }
}
