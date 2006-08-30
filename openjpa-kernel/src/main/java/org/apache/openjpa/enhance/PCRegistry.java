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
package org.apache.openjpa.enhance;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.apache.openjpa.lib.util.ReferenceMap;
import org.apache.openjpa.lib.util.concurrent.ConcurrentReferenceHashMap;

/**
 * Tracks registered persistence-capable classes.
 *
 * @since 0.4.0
 * @author Abe White
 */
public class PCRegistry {
    // DO NOT ADD ADDITIONAL DEPENDENCIES TO THIS CLASS

    // intentionally left unlocalized to minimize dependencies
    private static final String COPY_NO_ID = "Cannot copy identity for "
        + "abstract class ";
    private static final String NO_META = "No metadata found for class ";

    // map of pc classes to meta structs; weak so the VM can GC classes
    private static final Map _metas = new ConcurrentReferenceHashMap
        (ReferenceMap.WEAK, ReferenceMap.HARD);
    // register class listeners
    private static final Collection _listeners = new LinkedList();

    /**
     * Register a {@link RegisterClassListener}.
     */
    public static void addRegisterClassListener(RegisterClassListener rcl) {
        if (rcl == null)
            return;

        // we have to be positive that every listener gets notified for
        // every class, so lots of locking
        synchronized (_listeners) {
            _listeners.add(rcl);
        }
        synchronized (_metas) {
            for (Iterator itr = _metas.keySet().iterator(); itr.hasNext();)
                rcl.register((Class) itr.next());
        }
    }

    /**
     * Removes a {@link RegisterClassListener}.
     */
    public static void removeRegisterClassListener(RegisterClassListener rcl) {
        synchronized (_listeners) {
            _listeners.remove(rcl);
        }
    }

    /**
     * Get the field names for a <code>PersistenceCapable</code> class.
     */
    public static String[] getFieldNames(Class pcClass) {
        Meta meta = getMeta(pcClass);
        return meta.fieldNames;
    }

    /**
     * Get the field types for a <code>PersistenceCapable</code> class.
     */
    public static Class[] getFieldTypes(Class pcClass) {
        Meta meta = getMeta(pcClass);
        return meta.fieldTypes;
    }

    /**
     * Return the persistent superclass for a <code>PersistenceCapable</code>
     * class, or null if none. The superclass may or may not implement
     * {@link PersistenceCapable}, depending on the access type of the class.
     */
    public static Class getPersistentSuperclass(Class pcClass) {
        Meta meta = getMeta(pcClass);
        return meta.pcSuper;
    }

    /**
     * Create a new instance of the class and assign its state manager.
     * The new instance has its flags set to <code>LOAD_REQUIRED</code>.
     */
    public static PersistenceCapable newInstance(Class pcClass,
        StateManager sm, boolean clear) {
        Meta meta = getMeta(pcClass);
        return (meta.pc == null) ? null : meta.pc.pcNewInstance(sm, clear);
    }

    /**
     * Create a new instance of the class and assign its state manager and oid.
     * The new instance has its flags set to <code>LOAD_REQUIRED</code>.
     */
    public static PersistenceCapable newInstance(Class pcClass,
        StateManager sm, Object oid, boolean clear) {
        Meta meta = getMeta(pcClass);
        return (meta.pc == null) ? null : meta.pc.pcNewInstance(sm, oid, clear);
    }

    /**
     * Create a new identity object for the given
     * <code>PersistenceCapable</code> class.
     */
    public static Object newObjectId(Class pcClass) {
        Meta meta = getMeta(pcClass);
        return (meta.pc == null) ? null : meta.pc.pcNewObjectIdInstance();
    }

    /**
     * Create a new identity object for the given
     * <code>PersistenceCapable</code> class, using the <code>String</code>
     * form of the constructor.
     */
    public static Object newObjectId(Class pcClass, String str) {
        Meta meta = getMeta(pcClass);
        return (meta.pc == null) ? null : meta.pc.pcNewObjectIdInstance(str);
    }

    /**
     * Return the alias for the given type.
     */
    public static String getTypeAlias(Class pcClass) {
        return getMeta(pcClass).alias;
    }

    /**
     * Copy fields from an outside source to the key fields in the identity
     * object.
     */
    public static void copyKeyFieldsToObjectId(Class pcClass, FieldSupplier fm,
        Object oid) {
        Meta meta = getMeta(pcClass);
        if (meta.pc == null)
            throw new IllegalStateException(COPY_NO_ID + pcClass.getName());

        meta.pc.pcCopyKeyFieldsToObjectId(fm, oid);
    }

    /**
     * Copy fields to an outside source from the key fields in the identity
     * object.
     */
    public static void copyKeyFieldsFromObjectId(Class pcClass,
        FieldConsumer fm, Object oid) {
        Meta meta = getMeta(pcClass);
        if (meta.pc == null)
            throw new IllegalStateException(COPY_NO_ID + pcClass.getName());

        meta.pc.pcCopyKeyFieldsFromObjectId(fm, oid);
    }

    /**
     * Register metadata by class.
     *
     * @param fieldTypes managed field types
     * @param fieldFlags managed field flags
     * @param sup the most immediate persistent superclass
     * @param pcClass the <code>PersistenceCapable</code> class
     * @param fieldNames managed field names
     * @param alias the class alias
     * @param pc an instance of the class, if not abstract
     */
    public static void register(Class pcClass, String[] fieldNames,
        Class[] fieldTypes, byte[] fieldFlags, Class sup, String alias,
        PersistenceCapable pc) {
        if (pcClass == null)
            throw new NullPointerException();

        // we have to be positive that every listener gets notified for
        // every class, so lots of locking
        Meta meta = new Meta(pc, fieldNames, fieldTypes, sup, alias);
        synchronized (_metas) {
            _metas.put(pcClass, meta);
        }
        synchronized (_listeners) {
            for (Iterator i = _listeners.iterator(); i.hasNext();)
                ((RegisterClassListener) i.next()).register(pcClass);
        }
    }

    /**
     * Returns a collection of class objects of the registered
     * persistence-capable classes.
     */
    public static Collection getRegisteredTypes() {
        return Collections.unmodifiableCollection(_metas.keySet());
    }

    /**
     * Returns <code>true</code> if <code>cls</code> is already registered.
     */
    public static boolean isRegistered(Class cls) {
        return _metas.containsKey(cls);
    }

    /**
     * Look up the metadata for a <code>PersistenceCapable</code> class.
     */
    private static Meta getMeta(Class pcClass) {
        Meta ret = (Meta) _metas.get(pcClass);
        if (ret == null)
            throw new IllegalStateException(NO_META + pcClass.getName());
        return ret;
    }

    /**
     * Listener for persistent class registration events.
     */
    public static interface RegisterClassListener {

        public void register(Class cls);
    }

    /**
     * This is a helper class to manage metadata per persistence-capable class.
     */
    private static class Meta {

        public final PersistenceCapable pc;
        public final String[] fieldNames;
        public final Class[] fieldTypes;
        public final Class pcSuper;
        public final String alias;

        public Meta(PersistenceCapable pc, String[] fieldNames,
            Class[] fieldTypes, Class pcSuper, String alias) {
            this.pc = pc;
            this.fieldNames = fieldNames;
            this.fieldTypes = fieldTypes;
            this.pcSuper = pcSuper;
			this.alias = alias;
		}
	}
}
