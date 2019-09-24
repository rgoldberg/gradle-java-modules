/*
 * Copyright 2017 - 2020 the original author or authors.
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
package org.gradle.java.extension


interface SeparableValueOptionInternal<T, I>: SeparableValueCascading, ValueOptionInternal<T, I> {

    val flagValueSeparator: String
}


interface SeparableValueScalarOption<T>: ScalarOption<T>, SeparateValueSettableCascading

interface SeparableValueScalarOptionInternal<T>: SeparableValueOptionInternal<T, T>, SeparableValueScalarOption<T>, ScalarOptionInternal<T>

open class DefaultSeparableValueScalarOptionInternal<T>(
                 flag:                String,
    override val flagValueSeparator:  String,
                 value:               T,
                 separateValueParent: SeparateValueGettable
):
SeparableValueScalarOptionInternal<T>,
DefaultScalarOptionInternal<T>(flag, value),
SeparableValueCascading by DefaultSeparableValueCascading(separateValueParent)


interface SeparableValueVarargOption<T>: VarargOption<T>, SeparateValueSettableCascading

interface SeparableValueVarargOptionInternal<T>:
SeparableValueVarargOption<T>,
SeparableValueOptionInternal<Collection<T>, Collection<T>>,
VarargOptionInternal<T>

interface SeparableValueSetOptionInternal<T>: SeparableValueVarargOptionInternal<T>, SetOptionInternal<T>

open class DefaultSeparableValueSetOptionInternal<T>(
                 flag:                String,
    override val flagValueSeparator:  String,
                 delimiter:           String,
                 separateValueParent: SeparateValueGettable
):
SeparableValueSetOptionInternal<T>,
DefaultSetOptionInternal<T>(flag, delimiter),
SeparableValueCascading by DefaultSeparableValueCascading(separateValueParent)


interface SeparableValueKeyedVarargOption<K, V>: KeyedVarargOption<K, V>, SeparateValueSettableCascading

interface SeparableValueKeyedVarargOptionInternal<K, V, C: Collection<V>>:
SeparableValueKeyedVarargOption<K, V>,
SeparableValueOptionInternal<Map<K, C>, Map.Entry<K, C>>,
KeyedVarargOptionInternal<K, V, C>

interface SeparableValueLinkedHashMultimapOptionInternal<K, V>:
SeparableValueKeyedVarargOptionInternal<K, V, Set<V>>,
LinkedHashMultimapOptionInternal<K, V>

open class DefaultSeparableValueLinkedHashMultimapOptionInternal<K, V>(
                 flag:                String,
    override val flagValueSeparator:  String,
                 keyValueSeparator:   String,
                 valueDelimiter:      String,
                 separateValueParent: SeparateValueGettable
):
SeparableValueLinkedHashMultimapOptionInternal<K, V>,
DefaultLinkedHashMultimapOptionInternal<K, V>(flag, keyValueSeparator, valueDelimiter),
SeparableValueCascading by DefaultSeparableValueCascading(separateValueParent)
