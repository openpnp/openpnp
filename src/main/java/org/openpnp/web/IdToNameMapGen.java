package org.openpnp.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openpnp.model.Identifiable;
import org.openpnp.model.Named;

public class IdToNameMapGen<E extends Identifiable & Named> {
    public Map<String, String> getMap(List<E> objects) {
        Map<String, String> res = new HashMap<>();
        for (E o : objects) {
            res.put(o.getId(), o.getName());
        }
        return res;
    }
}