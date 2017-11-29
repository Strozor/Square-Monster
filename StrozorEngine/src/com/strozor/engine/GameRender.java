package com.strozor.engine;

import com.strozor.engine.gfx.*;
import com.strozor.game.GameManager;
import com.strozor.game.Crea;
import com.strozor.game.GameObject;

import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class GameRender {

    private Font font = Font.STANDARD;
    private ArrayList<ImageRequest> imageRequest = new ArrayList<>();

    private int pW, pH;
    private int[] p, zb, lm;
    private int camX, camY;
    private int zDepth = 0;
    private boolean processing = false;

    public GameRender(GameContainer gc) {
        pW = gc.getWidth();
        pH = gc.getHeight();
        p = ((DataBufferInt)gc.getWindow().getImage().getRaster().getDataBuffer()).getData();
        zb = new int[p.length];
        lm = new int[p.length];
    }

    public void clear() {
        for(int i = 0; i < p.length; i++) {
            p[i] = 0;
            zb[i] = 0;
            lm[i] = 0xff424242;
        }
    }

    public void process() {
        processing = true;

        Collections.sort(imageRequest, new Comparator<ImageRequest>(){
            @Override
            public int compare(ImageRequest i0, ImageRequest i1) {
                return Integer.compare(i0.zDepth, i1.zDepth);
            }
        });

        for(ImageRequest ir : imageRequest) {
            setzDepth(ir.zDepth);
            drawImage(ir.image, ir.offX, ir.offY);
        }

        for(int i = 0; i < p.length; i++) {

            int pR = (p[i] >> 16) & 255;
            int pG = (p[i] >> 8) & 255;
            int pB = p[i] & 255;

            int lR = (lm[i] >> 16) & 255;
            int lG = (lm[i] >> 8) & 255;
            int lB = lm[i] & 255;

            p[i] = (int)(pR * lR / 255f) << 16 | (int)(pG * lG / 255f) << 8 | (int)(pB * lB / 255f);
        }

        imageRequest.clear();
        processing = false;
    }

    private void setPixel(int x, int y, int value) {
        int alpha = (value >> 24) & 255;

        if((x < 0 || x >= pW || y < 0 || y >= pH) || alpha == 0) return;

        int index = x + y * pW;

        if(zb[index] > zDepth) return;

        zb[index] = zDepth;

        if(alpha == 255) {
            p[index] = value;
        } else {
            int pR = (p[index] >> 16) & 255;
            int pG = (p[index] >> 8) & 255;
            int pB = p[index] & 255;

            int vR = (value >> 16) & 255;
            int vG = (value >> 8) & 255;
            int vB = value & 255;

            int newR = pR - (int)((pR - vR) * alpha / 255f);
            int newG = pG - (int)((pG - vG) * alpha / 255f);
            int newB = pB - (int)((pB - vB) * alpha / 255f);

            p[index] = newR << 16 | newG << 8 | newB;
        }
    }

    private void setLightMap(int x, int y, int value) {
        if(x < 0 || x >= pW || y < 0 || y >= pH) return;

        int baseColor = lm[x + y * pW];

        int maxR = Math.max((baseColor >> 16) & 255, (value >> 16) & 255);
        int maxG = Math.max((baseColor >> 8) & 255, (value >> 8) & 255);
        int maxB = Math.max(baseColor & 255, value & 255);

        lm[x + y * pW] = maxR << 16 | maxG << 8 | maxB;
    }

    public void drawText(String text, int offX, int offY, int alignX, int alignY, int color) {

        if(alignX != 1) {
            int textW = 0;
            for(int i = 0; i < text.length(); i++) textW += font.getWidths()[text.codePointAt(i) - 32];
            textW++;
            if(alignX == 0) offX -= textW / 2;
            else if(alignX == -1) offX -= textW;
        }

        if(alignY != 1) {
            if(alignY == 0) offY -= 9 / 2;
            else if(alignY == -1) offY -= 9;
        }

        int offset = 0;
        for(int i = 0; i < text.length(); i++) {
            int unicode = text.codePointAt(i) - 32;

            for(int y = 0; y < font.getFontImage().getH(); y++) {
                for(int x = 0; x < font.getWidths()[unicode]; x++) {
                    if(font.getFontImage().getP()[(x + font.getOffsets()[unicode]) + y * font.getFontImage().getW()] == -1) {
                        setPixel(x + offX + offset, y + offY, color);
                    }
                }
            }
            offset += font.getWidths()[unicode];
        }
    }

    public void drawImage(Image image, int offX, int offY) {

        offX -= camX;
        offY -= camY;

        if(image.isAlpha() && !processing) {
            imageRequest.add(new ImageRequest(image, zDepth, offX, offY));
            return;
        }

        if(offX < -image.getW()) return;
        if(offY < -image.getH()) return;
        if(offX >= pW) return;
        if(offY >= pH) return;

        int newX = 0;
        int newY = 0;
        int newWidth = image.getW();
        int newHeight = image.getH();

        if(offX < 0) newX -= offX;
        if(offY < 0) newY -= offY;
        if(offX + newWidth >= pW) newWidth -= newWidth + offX - pW;
        if(offY + newHeight >= pH) newHeight -= newHeight + offY - pH;

        for(int y = newY; y < newHeight; y++) {
            for(int x = newX; x < newWidth; x++) {
                setPixel(x + offX, y + offY, image.getP()[x + y * image.getW()]);
            }
        }
    }

    public void drawImageTile(ImageTile image, int offX, int offY, int tileX, int tileY) {

        offX -= camX;
        offY -= camY;

        if(image.isAlpha() && !processing) {
            imageRequest.add(new ImageRequest(image.getTileImage(tileX, tileY), zDepth, offX, offY));
            return;
        }

        if(offX < -image.getTileW()) return;
        if(offY < -image.getTileH()) return;
        if(offX >= pW) return;
        if(offY >= pH) return;

        int newX = 0;
        int newY = 0;
        int newWidth = image.getTileW();
        int newHeight = image.getTileH();

        if(offX < 0) newX -= offX;
        if(offY < 0) newY -= offY;
        if(offX + newWidth >= pW) newWidth -= newWidth + offX - pW;
        if(offY + newHeight >= pH) newHeight -= newHeight + offY - pH;

        for(int y = newY; y < newHeight; y++) {
            for(int x = newX; x < newWidth; x++) {
                setPixel(x + offX, y + offY, image.getP()[(x + tileX * image.getTileW()) + (y + tileY * image.getTileH()) * image.getW()]);
            }
        }
    }

    public void drawRect(int offX, int offY, int width, int height, int color) {

        offX -= camX;
        offY -= camY;

        for(int y = 0; y <= height; y++) {
            setPixel(offX, y + offY, color);
            setPixel(offX + width, y + offY, color);
        }

        for(int x = 0; x <= width; x++) {
            setPixel(x + offX, offY, color);
            setPixel(x + offX, offY + height, color);
        }
    }

    public void fillRect(int offX, int offY, int width, int height, int color) {

        offX -= camX;
        offY -= camY;

        if(offX < -width) return;
        if(offY < -height) return;
        if(offX >= pW) return;
        if(offY >= pH) return;

        int newX = 0;
        int newY = 0;
        int newWidth = width;
        int newHeight = height;

        if(offX < 0) newX -= offX;
        if(offY < 0) newY -= offY;
        if(offX + newWidth >= pW) newWidth -= newWidth + offX - pW;
        if(offY + newHeight >= pH) newHeight -= newHeight + offY - pH;

        for(int y = newY; y < newHeight; y++) {
            for(int x = newX; x < newWidth; x++) {
                setPixel(x + offX, y + offY, color);
            }
        }
    }

    public void drawButton(Button b, int color) {
        drawRect(b.getOffX(), b.getOffY(), b.getWidth(), b.getHeight(), color);
        fillRect(b.getOffX()+1, b.getOffY()+1, b.getWidth()-1, b.getHeight()-1, b.getBgColor());
        drawText(b.getText(), b.getOffX() + b.getWidth() / 2, b.getOffY() + b.getHeight() / 2, 0, 0, color);
    }

    public void drawLight(Light l, int offX, int offY) {

        offX -= camX;
        offY -= camY;

        for(int i = 0; i < l.getDiameter(); i++) {
            drawLightLine(l, l.getRadius(), l.getRadius(), i, 0, offX, offY);
            drawLightLine(l, l.getRadius(), l.getRadius(), i, l.getDiameter(), offX, offY);
            drawLightLine(l, l.getRadius(), l.getRadius(), 0, i, offX, offY);
            drawLightLine(l, l.getRadius(), l.getRadius(), l.getDiameter(), i, offX, offY);
        }
    }

    private void drawLightLine(Light l, int x0, int y0, int x1, int y1, int offX, int offY) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);

        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;

        int err = dx - dy;
        int e2;

        while(true) {

            int screenX = x0 - l.getRadius() + offX;
            int screenY = y0 - l.getRadius() + offY;

            int lightColor = l.getLightValue(x0, y0);
            if(lightColor == 0) return;

            setLightMap(screenX, screenY, lightColor);

            if(x0 == x1 && y0 == y1) break;

            e2 = 2 * err;

            if(e2 > -dy) {
                err -= dy;
                x0 += sx;
            }

            if(e2 < dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    public void drawGameStates(GameManager gm) {
        GameObject obj = gm.getObject("player");

        drawImageTile(gm.getObjectsImage(), camX, camY, 3, 2);
        drawText("x"+obj.getLives(), gm.TS, gm.TS, 1, -1,-1);

        drawImageTile(gm.getObjectsImage(), camX, camY + 16, 5, 0);
        drawText("x"+obj.getCoins(), gm.TS, gm.TS * 2, 1, -1,-1);

        drawImageTile(gm.getObjectsImage(), camX, camY + 32, 3, 1);
        drawText("x"+obj.getKeys(), gm.TS, gm.TS * 3, 1, -1,-1);
    }

    public void drawDock(int offX, int offY, ImageTile objectsImage, int[] elems, int selected) {

        int tileX = 0, tileY = 0;

        drawRect(offX, offY, elems.length * Crea.TS + 1, Crea.TS + 1, 0xff999999);

        for(int i = 0; i < elems.length; i++) {
            switch(elems[i]) {
                case 0: tileX = 2; tileY = 0; break;
                case 1: tileX = 0; tileY = 0; break;
                case 2: tileX = 3; tileY = 2; break;
                case 3: tileX = 1; tileY = 1; break;
                case 4: tileX = 2; tileY = 1; break;
                case 5: tileX = 3; tileY = 1; break;
                case 6: tileX = 3; tileY = 0; break;
                case 7: tileX = 5; tileY = 0; break;
                case 11: tileX = 4; tileY = 0; break;
                case 12: tileX = 0; tileY = 2; break;
                case 13: tileX = 4; tileY = 4; break;
            }
            drawImageTile(objectsImage, offX + 1 + Crea.TS * i, offY + 1, tileX, tileY);
        }
        drawRect(offX + Crea.TS * selected, offY, Crea.TS + 1, Crea.TS + 1, 0xff00ff00);

        String sltText = "";
        switch(elems[selected]) {
            case 0: sltText = "spawn"; break;
            case 1: sltText = "walls"; break;
            case 2: sltText = "heart"; break;
            case 3: sltText = "skewer top"; break;
            case 4: sltText = "skewer down"; break;
            case 5: sltText = "level key"; break;
            case 6: sltText = "check point"; break;
            case 7: sltText = "coin"; break;
            case 11: sltText = "torch"; break;
            case 12: sltText = "bouncing"; break;
            case 13: sltText = "exit door"; break;
        }
        drawText(sltText, offX, offY, 1, -1, -1);
    }

    public void setCamX(int camX) {
        this.camX = camX;
    }

    public void setCamY(int camY) {
        this.camY = camY;
    }

    private void setzDepth(int zDepth) {
        this.zDepth = zDepth;
    }
}
