package com.strozor.view;

import com.strozor.engine.GameContainer;
import com.strozor.engine.GameRender;
import com.strozor.engine.Settings;
import com.strozor.engine.View;
import com.strozor.engine.audio.SoundClip;
import com.strozor.engine.gfx.Bloc;
import com.strozor.engine.gfx.Button;
import com.strozor.engine.gfx.Font;

import java.awt.event.KeyEvent;

public class Stats extends View {

    private Settings s;
    private SoundClip select;
    private Button back;

    public Stats(Settings settings) {
        s = settings;
        select = new SoundClip("/audio/select.wav");
        buttons.add(back = new Button(60, 20, "Back", 0));
    }

    @Override
    public void update(GameContainer gc, float dt) {

        if(gc.getInput().isKeyDown(KeyEvent.VK_ESCAPE)) gc.setState(gc.getLastState());

        //Button selection
        for(Button btn : buttons) {
            if (isSelected(gc, btn)) {
                select.play();
                gc.setState(btn.getGoState());
                gc.setState(gc.getLastState());
            }
        }
    }

    @Override
    public void render(GameContainer gc, GameRender r) {

        if(gc.getLastState() == 0) r.drawBackground(gc, new Bloc(0));
        else r.fillRect(0, 0, gc.getWidth(), gc.getHeight(), 0x99000000);
        r.drawMenuTitle(gc, s.translate("Stats").toUpperCase(), "");

        for(int i = 0; i < gc.getData().getStates().length; i++) {
            r.drawText(gc.getData().getStates()[i], gc.getWidth()/2, gc.getHeight()/4+i*15, -1, 1, -1, Font.STANDARD);
            r.drawText(" = " + gc.getData().getValues()[i], gc.getWidth()/2, gc.getHeight()/4+i*15, 1, 1, -1, Font.STANDARD);
        }

        back.setOffX(5);
        back.setOffY(5);

        for(Button btn : buttons) r.drawButton(btn, s.translate(btn.getText()));
    }
}
