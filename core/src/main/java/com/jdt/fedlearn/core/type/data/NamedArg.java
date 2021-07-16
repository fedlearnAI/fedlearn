/* Copyright 2020 The FedLearn Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.jdt.fedlearn.core.type.data;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;


/**
 * Annotation that provides information about argument's name.
 *
 * @since JavaFX 8.0
 */
@Retention(RUNTIME)
@Target(PARAMETER)
public @interface NamedArg {
    /**
     * The name of the annotated argument.
     *
     * @return the name of the annotated argument
     */
    public String value();

    /**
     * The default value of the annotated argument.
     *
     * @return the default value of the annotated argument
     */
    public String defaultValue() default "";
}
