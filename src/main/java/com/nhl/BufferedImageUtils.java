package com.nhl;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.LookupOp;
import java.awt.image.LookupTable;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class BufferedImageUtils {
    public static Optional<BufferedImage> readPath(final String path) {

        try {
            return Optional.of(ImageIO.read(new File(path)));
        } catch (IOException e) {
            System.out.println("Could not read \"" + path + "\" as image");
            return Optional.empty();
        }
    }

    public static BufferedImage invert(BufferedImage image) {
        if (image.getType() != BufferedImage.TYPE_INT_ARGB) {
            image = convertToARGB(image);
        }
        var lookup = new LookupTable(0, 4) {
            @Override
            public int[] lookupPixel(int[] src, int[] dest) {
                dest[0] = (int)(255-src[0]);
                dest[1] = (int)(255-src[1]);
                dest[2] = (int)(255-src[2]);
                return dest;
            }
        };
        var op = new LookupOp(lookup, new RenderingHints(null));
        return op.filter(image, null);
    }

    private static BufferedImage convertToARGB(BufferedImage image) {
        final var newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        final var g = newImage.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return newImage;
    }
}
