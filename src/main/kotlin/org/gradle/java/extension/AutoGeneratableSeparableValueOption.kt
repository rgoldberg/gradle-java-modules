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


interface AutoGeneratableSeparableValueOptionInternal: AutoGeneratableOption, AutoGeneratableCascading, SeparableValueCascading, OptionInternal

abstract class AbstractAutoGeneratableSeparableValueOptionInternal(
    autoGenerate:  AutoGenerateGettable,
    separateValue: SeparateValueGettable,
    isEnabled:     Boolean? = null
):
AutoGeneratableSeparableValueOptionInternal,
AutoGeneratableCascading by DefaultAutoGeneratableCascading(autoGenerate,  isEnabled),
 SeparableValueCascading by  DefaultSeparableValueCascading(separateValue, isEnabled)


interface AutoGeneratableSeparableValueScalarOption<T>: AutoGeneratableScalarOption<T>, SeparableValueScalarOption<T>

interface AutoGeneratableSeparableValueScalarOptionInternal<T>:
AutoGeneratableSeparableValueScalarOption<T>,
AutoGeneratableSeparableValueOptionInternal,
AutoGeneratableScalarOptionInternal<T>,
SeparableValueScalarOptionInternal<T>

abstract class AbstractAutoGeneratableSeparableValueScalarOptionInternal<T>(
    option: ScalarOptionInternal<T>
):
AutoGeneratableSeparableValueScalarOptionInternal<T>,
ScalarOptionInternal<T> by option


interface AutoGeneratableSeparableValueVarargOption<T>: AutoGeneratableVarargOption<T>, SeparableValueVarargOption<T>

interface AutoGeneratableSeparableValueVarargOptionInternal<T>:
AutoGeneratableSeparableValueVarargOption<T>,
AutoGeneratableSeparableValueOptionInternal,
AutoGeneratableVarargOptionInternal<T>,
SeparableValueVarargOptionInternal<T>


interface AutoGeneratableSeparableValueSetOptionInternal<T>:
AutoGeneratableSeparableValueVarargOptionInternal<T>,
SeparableValueSetOptionInternal<T>


interface AutoGeneratableSeparableValueKeyedVarargOption<K, V>: AutoGeneratableKeyedVarargOption<K, V>, SeparableValueKeyedVarargOption<K, V>

interface AutoGeneratableSeparableValueKeyedVarargOptionInternal<K, V, C: Collection<V>>:
AutoGeneratableSeparableValueKeyedVarargOption<K, V>,
AutoGeneratableSeparableValueOptionInternal,
AutoGeneratableKeyedVarargOptionInternal<K, V, C>,
SeparableValueKeyedVarargOptionInternal<K, V, C>

abstract class AbstractAutoGeneratableSeparableValueKeyedVarargOptionInternal<K, V, C: Collection<V>>(
    option: KeyedVarargOptionInternal<K, V, C>
):
AutoGeneratableSeparableValueKeyedVarargOptionInternal<K, V, C>,
KeyedVarargOptionInternal<K, V, C> by option

interface AutoGeneratableSeparableValueLinkedHashMultimapOptionInternal<K, V>:
AutoGeneratableSeparableValueKeyedVarargOptionInternal<K, V, Set<V>>,
SeparableValueLinkedHashMultimapOptionInternal<K, V>
