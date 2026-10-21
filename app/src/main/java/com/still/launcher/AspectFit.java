package com.still.launcher;

/** Geometry shared by artwork decoding and tests. Never changes the source aspect ratio. */
public final class AspectFit {
    private AspectFit() { }
    public static int[] inside(int width, int height, int boxWidth, int boxHeight) {
        if (width <= 0 || height <= 0 || boxWidth <= 0 || boxHeight <= 0)
            throw new IllegalArgumentException("Dimensions must be positive");
        double scale = Math.min((double) boxWidth / width, (double) boxHeight / height);
        int w = Math.min(boxWidth, Math.max(1, (int) Math.round(width * scale)));
        int h = Math.min(boxHeight, Math.max(1, (int) Math.round(height * scale)));
        int x = (boxWidth - w) / 2, y = (boxHeight - h) / 2;
        return new int[]{x, y, x + w, y + h};
    }
}
