package cz.marstaj.metrocatcher.util;

import android.os.Binder;

import java.lang.ref.WeakReference;

/**
 * Generic class representing a Binder which is used for connecting activities to a service.
 *
 * @param <T> Object type
 * @author Martin Stajner (marstaj@seznam.cz)
 */

public class LocalBinder<T> extends Binder {

    /**
     * Weak reference to a object
     */
    private WeakReference<T> newService;

    /**
     * Method creates weak reference to a object.
     *
     * @param service Object from which to make weak reference
     */
    public LocalBinder(T service) {
        newService = new WeakReference<T>(service);
    }

    /**
     * Method returns weak reference to a object.
     *
     * @return Weak reference
     */
    public T getService() {
        return newService.get();
    }
}