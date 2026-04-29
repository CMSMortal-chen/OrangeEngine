package cms.mortalchen.orange.utils;

import lombok.experimental.UtilityClass;
import org.joml.Vector4f;

@UtilityClass
public class InputUtils {
    public static boolean isHovering(Vector4f pos,float mouseX, float mouseY){
        return pos.x <= mouseX && pos.z >= mouseX && pos.y <= mouseY && pos.w >= mouseY;
    }
}
