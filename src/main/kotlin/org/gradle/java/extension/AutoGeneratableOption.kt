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


interface AutoGeneratableOption: Option, AutoGenerateSettableCascading

interface AutoGeneratableOptionInternal: AutoGeneratableOption, OptionInternal, AutoGeneratableCascading

abstract class AbstractAutoGeneratableOptionInternal(
    parent:    AutoGenerateGettable,
    isEnabled: Boolean? = null
):
AutoGeneratableOptionInternal,
AutoGeneratableCascading by DefaultAutoGeneratableCascading(parent, isEnabled)

interface AutoGeneratableScalarOption<T>: AutoGeneratableOption, ScalarOption<T>

interface AutoGeneratableScalarOptionInternal<T>: AutoGeneratableScalarOption<T>, AutoGeneratableOptionInternal, ScalarOptionInternal<T>

abstract class AbstractAutoGeneratableScalarOptionInternal<T>(
    option: ScalarOptionInternal<T>
):
AutoGeneratableScalarOptionInternal<T>,
ScalarOptionInternal<T> by option


interface AutoGeneratableVarargOption<T>: AutoGeneratableOption, VarargOption<T>

interface AutoGeneratableVarargOptionInternal<T>: AutoGeneratableVarargOption<T>, AutoGeneratableOptionInternal, VarargOptionInternal<T>


interface AutoGeneratableKeyedVarargOption<K, V>: AutoGeneratableOption, KeyedVarargOption<K, V>

interface AutoGeneratableKeyedVarargOptionInternal<K, V, C: Collection<V>>:
AutoGeneratableKeyedVarargOption<K, V>,
AutoGeneratableOptionInternal,
KeyedVarargOptionInternal<K, V, C>

abstract class AbstractAutoGeneratableKeyedVarargOptionInternal<K, V, C: Collection<V>>(
    option: KeyedVarargOptionInternal<K, V, C>
):
AutoGeneratableKeyedVarargOptionInternal<K, V, C>,
KeyedVarargOptionInternal<K, V, C> by option
