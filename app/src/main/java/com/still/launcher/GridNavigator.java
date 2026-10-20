package com.still.launcher;

/** Pure grid movement shared by the D-pad UI and JVM boundary tests. */
final class GridNavigator {
    enum Direction { UP, DOWN, LEFT, RIGHT }

    private GridNavigator() { }

    static int move(int index, int count, int columns, Direction direction) {
        if (columns < 1) throw new IllegalArgumentException("columns must be positive");
        if (count <= 0) return 0;
        index = Math.max(0, Math.min(index, count - 1));
        int column = index % columns;
        switch (direction) {
            case LEFT: return column > 0 ? index - 1 : index;
            case RIGHT: return column < columns - 1 && index + 1 < count ? index + 1 : index;
            case UP: return index >= columns ? index - columns : index;
            case DOWN: return (index / columns + 1) * columns < count
                    ? Math.min(count - 1, index + columns) : index;
            default: return index;
        }
    }
}
