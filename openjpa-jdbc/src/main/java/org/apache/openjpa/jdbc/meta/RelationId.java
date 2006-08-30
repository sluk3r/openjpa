/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.jdbc.meta;

import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.kernel.OpenJPAStateManager;

/**
 * Callback to store a relation after the object id has been assigned.
 *
 * @author Abe White
 * @since 0.4.0
 */
public interface RelationId {

    /**
     * Return the serialized value for the given related object, now that
     * its id has been assigned.
     */
    public Object toRelationDataStoreValue(OpenJPAStateManager sm, Column col);
}

