package com.openai.keycloak.captcha;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.imageio.ImageIO;

final class CaptchaGenerator {
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private CaptchaGenerator() {
    }

    static String randomText(int length) {
        int safeLength = Math.max(4, Math.min(length, 10));
        StringBuilder builder = new StringBuilder(safeLength);
        for (int i = 0; i < safeLength; i++) {
            builder.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return builder.toString();
    }

    static String renderAsDataUrl(String text, int width, int height, int noiseLines) {
        int safeWidth = Math.max(160, width);
        int safeHeight = Math.max(60, height);
        int safeNoiseLines = Math.max(0, Math.min(noiseLines, 30));

        BufferedImage image = new BufferedImage(safeWidth, safeHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g.setColor(new Color(250, 250, 250));
            g.fillRect(0, 0, safeWidth, safeHeight);

            for (int i = 0; i < safeWidth * safeHeight / 24; i++) {
                g.setColor(randomPastelColor(170, 245));
                int x = RANDOM.nextInt(safeWidth);
                int y = RANDOM.nextInt(safeHeight);
                g.drawLine(x, y, x, y);
            }

            for (int i = 0; i < safeNoiseLines; i++) {
                g.setColor(randomPastelColor(120, 210));
                g.setStroke(new BasicStroke(1.4f + RANDOM.nextFloat() * 1.8f));
                int x1 = RANDOM.nextInt(safeWidth);
                int y1 = RANDOM.nextInt(safeHeight);
                int x2 = RANDOM.nextInt(safeWidth);
                int y2 = RANDOM.nextInt(safeHeight);
                g.drawLine(x1, y1, x2, y2);
            }

            drawVectorCaptchaText(g, text, safeWidth, safeHeight);

            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                ImageIO.write(image, "png", output);
                String base64 = Base64.getEncoder().encodeToString(output.toByteArray());
                return "data:image/png;base64," + base64;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to render CAPTCHA image", e);
        } finally {
            g.dispose();
        }
    }

    private static void drawVectorCaptchaText(Graphics2D g, String text, int width, int height) {
        int charCount = Math.max(1, text.length());
        int cellWidth = width / charCount;
        int glyphWidth = Math.max(18, Math.min(34, cellWidth - 14));
        int glyphHeight = Math.max(32, height - 22);
        int top = Math.max(8, (height - glyphHeight) / 2);

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);

            int left = i * cellWidth + Math.max(8, (cellWidth - glyphWidth) / 2);
            left += RANDOM.nextInt(7) - 3;

            AffineTransform originalTransform = g.getTransform();

            double angle = Math.toRadians((RANDOM.nextDouble() * 28.0) - 14.0);
            int cx = left + glyphWidth / 2;
            int cy = top + glyphHeight / 2;

            g.rotate(angle, cx, cy);
            g.setColor(randomDarkColor());
            g.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            drawGlyph(g, ch, left, top, glyphWidth, glyphHeight);

            g.setTransform(originalTransform);
        }
    }

    /**
     * Font-free 7-segment-like glyph renderer.
     * This intentionally avoids java.awt.Font and Graphics2D.drawString().
     */
    private static void drawGlyph(Graphics2D g, char ch, int x, int y, int w, int h) {
        boolean[] s = segmentsFor(ch);

        int x1 = x;
        int x2 = x + w / 2;
        int x3 = x + w;
        int y1 = y;
        int y2 = y + h / 2;
        int y3 = y + h;

        // 0: top
        if (s[0]) g.drawLine(x1 + 4, y1, x3 - 4, y1);

        // 1: upper-left
        if (s[1]) g.drawLine(x1, y1 + 4, x1, y2 - 4);

        // 2: upper-right
        if (s[2]) g.drawLine(x3, y1 + 4, x3, y2 - 4);

        // 3: middle
        if (s[3]) g.drawLine(x1 + 4, y2, x3 - 4, y2);

        // 4: lower-left
        if (s[4]) g.drawLine(x1, y2 + 4, x1, y3 - 4);

        // 5: lower-right
        if (s[5]) g.drawLine(x3, y2 + 4, x3, y3 - 4);

        // 6: bottom
        if (s[6]) g.drawLine(x1 + 4, y3, x3 - 4, y3);

        // Optional diagonals for letters
        if (s.length > 7 && s[7]) g.drawLine(x1 + 3, y3 - 3, x3 - 3, y1 + 3);
        if (s.length > 8 && s[8]) g.drawLine(x1 + 3, y1 + 3, x3 - 3, y3 - 3);
        if (s.length > 9 && s[9]) g.drawLine(x2, y1 + 3, x2, y3 - 3);
    }

    private static boolean[] segmentsFor(char ch) {
        switch (Character.toUpperCase(ch)) {
            case '0': return seg(1, 1, 1, 0, 1, 1, 1);
            case '1': return seg(0, 0, 1, 0, 0, 1, 0);
            case '2': return seg(1, 0, 1, 1, 1, 0, 1);
            case '3': return seg(1, 0, 1, 1, 0, 1, 1);
            case '4': return seg(0, 1, 1, 1, 0, 1, 0);
            case '5': return seg(1, 1, 0, 1, 0, 1, 1);
            case '6': return seg(1, 1, 0, 1, 1, 1, 1);
            case '7': return seg(1, 0, 1, 0, 0, 1, 0);
            case '8': return seg(1, 1, 1, 1, 1, 1, 1);
            case '9': return seg(1, 1, 1, 1, 0, 1, 1);

            case 'A': return seg(1, 1, 1, 1, 1, 1, 0);
            case 'B': return seg(0, 1, 0, 1, 1, 1, 1, 0, 0, 1);
            case 'C': return seg(1, 1, 0, 0, 1, 0, 1);
            case 'D': return seg(0, 0, 1, 1, 1, 1, 1);
            case 'E': return seg(1, 1, 0, 1, 1, 0, 1);
            case 'F': return seg(1, 1, 0, 1, 1, 0, 0);
            case 'G': return seg(1, 1, 0, 1, 1, 1, 1);
            case 'H': return seg(0, 1, 1, 1, 1, 1, 0);
            case 'J': return seg(0, 0, 1, 0, 1, 1, 1);
            case 'K': return seg(0, 1, 0, 1, 1, 0, 0, 1, 1);
            case 'L': return seg(0, 1, 0, 0, 1, 0, 1);
            case 'M': return seg(1, 1, 1, 0, 1, 1, 0, 0, 0, 1);
            case 'N': return seg(0, 1, 1, 0, 1, 1, 0, 0, 1);
            case 'P': return seg(1, 1, 1, 1, 1, 0, 0);
            case 'Q': return seg(1, 1, 1, 0, 1, 1, 1, 0, 1);
            case 'R': return seg(1, 1, 1, 1, 1, 0, 0, 0, 1);
            case 'S': return seg(1, 1, 0, 1, 0, 1, 1);
            case 'T': return seg(1, 0, 0, 0, 0, 0, 0, 0, 0, 1);
            case 'U': return seg(0, 1, 1, 0, 1, 1, 1);
            case 'V': return seg(0, 1, 1, 0, 1, 1, 0, 1);
            case 'W': return seg(0, 1, 1, 0, 1, 1, 1, 0, 0, 1);
            case 'X': return seg(0, 0, 0, 0, 0, 0, 0, 1, 1);
            case 'Y': return seg(0, 1, 1, 1, 0, 1, 1);
            case 'Z': return seg(1, 0, 0, 1, 0, 0, 1, 1);

            default: return seg(1, 1, 1, 1, 1, 1, 1);
        }
    }

    private static boolean[] seg(int... values) {
        boolean[] result = new boolean[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = values[i] == 1;
        }
        return result;
    }

    private static Color randomPastelColor(int min, int max) {
        return new Color(randomChannel(min, max), randomChannel(min, max), randomChannel(min, max));
    }

    private static Color randomDarkColor() {
        return new Color(randomChannel(20, 120), randomChannel(20, 120), randomChannel(20, 120));
    }

    private static int randomChannel(int min, int max) {
        return min + RANDOM.nextInt(Math.max(1, max - min + 1));
    }
}
