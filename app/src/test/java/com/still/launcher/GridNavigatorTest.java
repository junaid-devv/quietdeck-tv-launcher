package com.still.launcher;

import org.junit.Test;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

public class GridNavigatorTest {
    @Test public void emptyAndSingleAppStayInBounds() {
        for (GridNavigator.Direction direction : GridNavigator.Direction.values()) {
            assertEquals(0, GridNavigator.move(0, 0, 6, direction));
            assertEquals(0, GridNavigator.move(0, 1, 6, direction));
        }
    }

    @Test public void horizontalMovementDoesNotWrapRows() {
        assertEquals(5, GridNavigator.move(5, 12, 6, GridNavigator.Direction.RIGHT));
        assertEquals(6, GridNavigator.move(6, 12, 6, GridNavigator.Direction.LEFT));
    }

    @Test public void partialLastRowRemainsReachable() {
        assertEquals(7, GridNavigator.move(5, 8, 6, GridNavigator.Direction.DOWN));
        assertEquals(7, GridNavigator.move(7, 8, 6, GridNavigator.Direction.RIGHT));
        assertEquals(1, GridNavigator.move(7, 8, 6, GridNavigator.Direction.UP));
    }

    @Test public void shrinkingAppListClampsOldSelection() {
        assertEquals(2, GridNavigator.move(35, 3, 6, GridNavigator.Direction.RIGHT));
        assertEquals(0, GridNavigator.move(-2, 3, 6, GridNavigator.Direction.LEFT));
    }

    @Test public void everyAppIsReachableForSparseAndMultiPageGrids() {
        for (int count = 1; count <= 80; count++) {
            Set<Integer> visited = new HashSet<>();
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            visited.add(0); queue.add(0);
            while (!queue.isEmpty()) {
                int index = queue.remove();
                for (GridNavigator.Direction direction : GridNavigator.Direction.values()) {
                    int next = GridNavigator.move(index, count, 6, direction);
                    assertTrue(next >= 0 && next < count);
                    if (visited.add(next)) queue.add(next);
                }
            }
            assertEquals("Reachability for " + count + " apps", count, visited.size());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidColumnCountFailsClearly() {
        GridNavigator.move(0, 5, 0, GridNavigator.Direction.DOWN);
    }
}
