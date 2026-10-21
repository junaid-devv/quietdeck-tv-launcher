package com.still.launcher;

import org.junit.Test;
import static org.junit.Assert.*;

public class AspectFitTest {
    @Test public void squareLogoStaysSquareInTvBanner() {
        assertArrayEquals(new int[]{70, 0, 250, 180}, AspectFit.inside(512, 512, 320, 180));
    }
    @Test public void panoramicLogoIsLetterboxedRatherThanStretched() {
        assertArrayEquals(new int[]{0, 50, 320, 130}, AspectFit.inside(1200, 300, 320, 180));
    }
    @Test public void tallLogoFitsWithoutClipping() {
        assertArrayEquals(new int[]{138, 0, 183, 180}, AspectFit.inside(100, 400, 320, 180));
    }
    @Test public void manySourceRatiosFitAndStayProportionalWithinRounding() {
        for (int width = 1; width < 2000; width += 31) for (int height = 1; height < 2000; height += 37) {
            int[] rect = AspectFit.inside(width, height, 240, 135);
            assertTrue(rect[0] >= 0 && rect[1] >= 0 && rect[2] <= 240 && rect[3] <= 135);
            double scale = Math.min(240d / width, 135d / height);
            assertTrue(Math.abs((rect[2] - rect[0]) - width * scale) <= 1);
            assertTrue(Math.abs((rect[3] - rect[1]) - height * scale) <= 1);
        }
    }
    @Test(expected = IllegalArgumentException.class) public void invalidSourceIsRejected() { AspectFit.inside(0, 30, 320, 180); }
}
