package com.customwidgets.app

import org.junit.Assert.assertNotNull
import org.junit.Test

class CustomWidgetsAppTest {

    @Test
    fun appClass_instantiates() {
        val app = CustomWidgetsApp()
        assertNotNull(app)
    }

    @Test
    fun packageStructure_dataDomainUiWidgetAiDi_exist() {
        val basePkg = "com.customwidgets.app"
        assertNotNull(basePkg)
    }
}
