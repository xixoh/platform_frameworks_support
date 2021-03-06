/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.arch.persistence.room.processor

import android.arch.persistence.room.Embedded
import android.arch.persistence.room.testing.TestInvocation
import android.arch.persistence.room.testing.TestProcessor
import android.arch.persistence.room.vo.Entity
import android.support.annotation.NonNull
import com.google.auto.common.MoreElements
import com.google.common.truth.Truth
import com.google.testing.compile.CompileTester
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourceSubjectFactory
import com.google.testing.compile.JavaSourcesSubjectFactory
import javax.tools.JavaFileObject

abstract class BaseEntityParserTest {
    companion object {
        const val ENTITY_PREFIX = """
            package foo.bar;
            import android.arch.persistence.room.*;
            import android.support.annotation.NonNull;
            @Entity%s
            public class MyEntity %s {
            """
        const val ENTITY_SUFFIX = "}"
    }

    fun singleEntity(input: String, attributes: Map<String, String> = mapOf(),
                     baseClass : String = "",
                     jfos : List<JavaFileObject> = emptyList(),
                     handler: (Entity, TestInvocation) -> Unit): CompileTester {
        val attributesReplacement : String
        if (attributes.isEmpty()) {
            attributesReplacement = ""
        } else {
            attributesReplacement = "(" +
                    attributes.entries.map { "${it.key} = ${it.value}" }.joinToString(",") +
                    ")".trimIndent()
        }
        val baseClassReplacement : String
        if (baseClass == "") {
            baseClassReplacement = ""
        } else {
            baseClassReplacement = " extends $baseClass"
        }
        return Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
                .that(jfos + JavaFileObjects.forSourceString("foo.bar.MyEntity",
                        ENTITY_PREFIX.format(attributesReplacement, baseClassReplacement)
                                + input + ENTITY_SUFFIX
                ))
                .processedWith(TestProcessor.builder()
                        .forAnnotations(android.arch.persistence.room.Entity::class,
                                android.arch.persistence.room.PrimaryKey::class,
                                android.arch.persistence.room.Ignore::class,
                                Embedded::class,
                                android.arch.persistence.room.ColumnInfo::class,
                                NonNull::class)
                        .nextRunHandler { invocation ->
                            val entity = invocation.roundEnv
                                    .getElementsAnnotatedWith(
                                            android.arch.persistence.room.Entity::class.java)
                                    .first { it.toString() == "foo.bar.MyEntity" }
                            val parser = EntityProcessor(invocation.context,
                                    MoreElements.asType(entity))
                            val parsedQuery = parser.process()
                            handler(parsedQuery, invocation)
                            true
                        }
                        .build())
    }
}
