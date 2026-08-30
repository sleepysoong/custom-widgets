package com.customwidgets.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.customwidgets.app.data.local.dao.WidgetDao
import com.customwidgets.app.data.local.entity.AppWidgetIdEntity
import com.customwidgets.app.data.local.entity.WidgetEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WidgetDaoTest {

    private lateinit var database: WidgetDatabase
    private lateinit var dao: WidgetDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, WidgetDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.widgetDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetById() = runTest {
        val widget = WidgetEntity(
            name = "Clock Widget",
            description = "A simple clock",
            widthCells = 2,
            heightCells = 2,
            definitionJson = "{\"type\":\"column\"}"
        )
        val id = dao.insertWidget(widget)
        val retrieved = dao.getWidgetById(id)

        assertNotNull(retrieved)
        assertEquals("Clock Widget", retrieved?.name)
        assertEquals(2, retrieved?.widthCells)
    }

    @Test
    fun deleteWidget_cascadesToAppWidgetId() = runTest {
        val widgetId = dao.insertWidget(
            WidgetEntity(
                name = "Test",
                description = "desc",
                widthCells = 2,
                heightCells = 1,
                definitionJson = "{}"
            )
        )

        dao.insertAppWidgetId(AppWidgetIdEntity(widgetEntityId = widgetId, appWidgetId = 101))
        dao.insertAppWidgetId(AppWidgetIdEntity(widgetEntityId = widgetId, appWidgetId = 102))

        val idsBefore = dao.getAppWidgetIdsForWidget(widgetId)
        assertEquals(2, idsBefore.size)

        dao.deleteWidgetById(widgetId)

        val idsAfter = dao.getAppWidgetIdsForWidget(widgetId)
        assertEquals(0, idsAfter.size)
    }

    @Test
    fun getWidgetByAppWidgetId_joinsCorrectly() = runTest {
        val widgetId = dao.insertWidget(
            WidgetEntity(
                name = "Target Widget",
                description = "desc",
                widthCells = 4,
                heightCells = 2,
                definitionJson = "{}"
            )
        )

        dao.insertAppWidgetId(AppWidgetIdEntity(widgetEntityId = widgetId, appWidgetId = 200))

        val found = dao.getWidgetByAppWidgetId(200)
        assertNotNull(found)
        assertEquals("Target Widget", found?.name)

        val notFound = dao.getWidgetByAppWidgetId(999)
        assertNull(notFound)
    }

    @Test
    fun getAllWidgets_emitsFlow() = runTest {
        dao.insertWidget(WidgetEntity(name = "W1", description = "d1", widthCells = 1, heightCells = 1, definitionJson = "{}"))
        dao.insertWidget(WidgetEntity(name = "W2", description = "d2", widthCells = 2, heightCells = 2, definitionJson = "{}"))

        val all = dao.getAllWidgets().first()
        assertEquals(2, all.size)
    }
}
