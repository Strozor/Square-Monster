package game;

import engine.GameContainer;
import engine.Renderer;
import engine.gfx.Font;

public class Notification {

    private String message;
    private float duration, elapsed = 0;
    private int color, distance;

    public Notification(String message, float duration, int distance, int color) {
        this.message = message;
        this.duration = duration;
        this.distance = distance;
        this.color = color;
    }

    public void update(float dt) {
        elapsed += dt;
        if (elapsed >= duration) elapsed = duration;
    }

    public void render(GameContainer gc, Renderer r) {
        float percent = elapsed / duration;
        float offset = gc.getHeight() - percent * distance;

        int b = ((color >> 24) & 255) - (int) (percent * ((color >> 24) & 255));
        int newColor = b << 24 | ((color >> 16) & 255) << 16 | ((color >> 8) & 255) << 8 | (color & 255);

        r.drawText(message, 1, (int) offset, 1, -1, newColor, Font.STANDARD);
    }

    public boolean isEnded() {
        return elapsed == duration;
    }
}
