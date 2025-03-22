package com.as.gateway.states.rpg.gui.menu;

import cms.mortalchen.orange.MainGame;
import cms.mortalchen.orange.OrangeEngine;
import cms.mortalchen.orange.render.Image;
import cms.mortalchen.orange.render.font.FontManager;
import cms.mortalchen.orange.resource.ResourceLocation;
import cms.mortalchen.orange.utils.render.RenderUtils;
import cms.mortalchen.orange.utils.render.anim.AnimationUtil;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class StatusWindow extends MenuWindow{
    public Image playerCharacterImage = new Image(OrangeEngine.resourceManager, new ResourceLocation("/textures/image/character/player_character.png"));
    public float introX2 = 0f;

    /**
     * 主体渲染
     *
     * @param vg
     */
    @Override
    public void render(long vg) {
        float imgWidth = (1920 / 1.3f) * (MainGame.WINDOW_WIDTH / 1920f);
        float imgHeight = (1080 / 1.3f) * (MainGame.WINDOW_WIDTH / 1920f);

        float width = MainGame.WINDOW_WIDTH;
        float height = MainGame.WINDOW_HEIGHT;

        RenderUtils.drawRect(vg,width / 7f + (MainGame.WINDOW_WIDTH - introX2) ,height / 14f,width - (width / 14f) + (MainGame.WINDOW_WIDTH - introX),height - (height / 14f),new Color(255,255,255,180));

        playerCharacterImage.render(MainGame.WINDOW_WIDTH - (imgWidth / 1.5f) + (MainGame.WINDOW_WIDTH - introX), MainGame.WINDOW_HEIGHT - imgHeight, imgWidth, imgHeight, Color.WHITE);
        FontManager.sourceSerif_CHS.drawText("当前状态", width / 7f + (MainGame.WINDOW_WIDTH - introX2) + 40f, (height / 14f) + 40,45f, Color.BLACK);
        RenderUtils.drawRect(vg,width / 7f + (MainGame.WINDOW_WIDTH - introX2) + 40f + 45f,(height / 14f) + 40f + FontManager.sourceSerif_CHS.getStringHeight(45f) + 2f,width / 7f + (MainGame.WINDOW_WIDTH - introX2) + 40f + FontManager.sourceSerif_CHS.getStringWidth("当前状态",45f),(height / 14f) + 40 + FontManager.sourceSerif_CHS.getStringHeight(45f) + 4f,Color.BLACK);

        List<String> statusList = Arrays.asList(
                "名称：凯梅尔",
                "状态：正常",
                ""
                //"PJGW_OBJ_ID：00",
                //"实体交流系统：异常",
                //"Gateway状态：稳定，正常"
        );

        for (int i = 0; i < statusList.size(); i++) {
            FontManager.sourceSerif_CHS.drawText(statusList.get(i), width / 7f + (MainGame.WINDOW_WIDTH - introX2) + 40f, (height / 14f) + 40f + 80f + i * 40f,25f, new Color(26,26,26));
        }

        if (!exiting) {
            introX = AnimationUtil.moveUD(introX, MainGame.WINDOW_WIDTH, 16f, 12f);
            introX2 = AnimationUtil.moveUD(introX2, MainGame.WINDOW_WIDTH, 20f, 16f);
        } else {
            introX = AnimationUtil.moveUD(introX, 0, 10f, 5f);
            introX2 = AnimationUtil.moveUD(introX2, 0, 7f, 2f);

            if (introX <= 1) {
                exited = true;
            }
        }
    }

    @Override
    public void onKeyPressed(int key, int action) {

    }
}
