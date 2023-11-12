/*
 * University of Warsaw
 * Concurrent Programming Course 2023/2024
 * Java Assignment
 *
 * Author: Konrad Iwanicki (iwanicki@mimuw.edu.pl)
 */
package cp2023.solution;

import java.util.Map;

import cp2023.base.ComponentId;
import cp2023.base.DeviceId;
import cp2023.base.StorageSystem;


public final class StorageSystemFactory {

    public static StorageSystem newSystem(
            Map<DeviceId, Integer> deviceTotalSlots,
            Map<ComponentId, DeviceId> componentPlacement) {
        // FIXME: implement
        // TODO: process errors of wrong ids etc.
        // 1. Validation:
        // - everything not null
        // - totalSlots > 0  ???? jednak nie >= 0
        // - componentPlacement deviceID -> *exists* in deviceTotalSlots
        // - each componentId in componentPlacement is *unique*
        // - deviceID capacity not exceeded

        // 2. Creation of StorageSystemImpl
        //
        throw new RuntimeException("not implemented");
    }

}
