package com.focuslite.core.database

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusLiteDatabaseTest {

    @Test
    fun testFocusLiteDatabase_ImplExistsAndExtendsDatabase() {
        // Verifies that Room annotation processing (KSP) generated FocusLiteDatabase_Impl
        val clazz = Class.forName("com.focuslite.core.database.FocusLiteDatabase_Impl")
        assertNotNull("FocusLiteDatabase_Impl class must exist in the classpath", clazz)
        assertTrue(
            "FocusLiteDatabase_Impl must inherit from FocusLiteDatabase",
            FocusLiteDatabase::class.java.isAssignableFrom(clazz)
        )
    }
}
