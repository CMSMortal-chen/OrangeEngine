package cms.mortalchen.orange.utils.render.anim;


import cms.mortalchen.orange.OrangeEngine;
import cms.mortalchen.orange.render.font.FontManager;

import java.awt.*;

public class AnimationUtil {
   public static float deltaTime = 0;
   public static float deltaTimeUD = 0;

   public static double animate(double target, double current, double speed) {
      deltaTime = OrangeEngine.deltaTimeMills;
      speed = (speed / (1f / deltaTime * 1000f));
      boolean larger = target > current;
      if (speed < 0.0) {
         speed = 0.0;
      } else if (speed > 1.0) {
         speed = 1.0;
      }

      double dif = Math.max(target, current) - Math.min(target, current);
      if (dif < 0.1) {
         return target;
      }
      double factor = dif * speed;
      if (factor < 0.1) {
         factor = 0.1;
      }
      if (larger) {
         if (current + factor > target) {
            current = target;
         } else {
            current += factor;
         }
      } else {
         if (current - factor < target) {
            current = target;
         } else {
            current -= factor;
         }
      }
      return current;
   }
   public static float animate(float target, float current, float speed) {
      deltaTime = OrangeEngine.deltaTimeMills;
      speed = (speed / (1f / deltaTime * 1000f));
      boolean larger = target > current;
      if (speed < 0.0f) {
         speed = 0.0f;
      } else if (speed > 1.0f) {
         speed = 1.0f;
      }

      double dif = Math.max(target, current) - Math.min(target, current);
      if (dif < 0.1) {
         return target;
      }
      double factor = dif * speed;
      if (factor < 0.1) {
         factor = 0.1;
      }
      if (larger) {
         if (current + factor > target) {
            current = target;
         } else {
            current += factor;
         }
      } else {
         if (current - factor < target) {
            current = target;
         } else {
            current -= factor;
         }
      }
      return current;
   }

   public static double mvoeUD(double current, double end, float minSpeed) {
      float defaultSpeed = 0.125f;
      return moveUD(current, end, defaultSpeed, minSpeed);
   }

   public static float moveUD(float current, float end, float smoothSpeed, float minSpeed) {
      deltaTimeUD = OrangeEngine.deltaTimeMills;
      smoothSpeed = (smoothSpeed / (1f / deltaTimeUD * 1000f));
      minSpeed = (minSpeed / (1f / deltaTimeUD * 1000f));

      boolean larger = end > current;
      if (smoothSpeed < 0.0f) {
         smoothSpeed = 0.0f;
      } else if (smoothSpeed > 1.0f) {
         smoothSpeed = 1.0f;
      }
      if (minSpeed < 0.0f) {
         minSpeed = 0.0f;
      } else if (minSpeed > 1.0f) {
         minSpeed = 1.0f;
      }
      float movement = (end - current) * smoothSpeed;
      if (movement > 0) {
         movement = Math.max(minSpeed, movement);
         movement = Math.min(end - current, movement);
      } else if (movement < 0) {
         movement = Math.min(-minSpeed, movement);
         movement = Math.max(end - current, movement);
      }
      if (larger) {
         if (end <= current + movement) {
            return end;
         }
      } else {
         if (end >= current + movement) {
            return end;
         }
      }
      return current + movement;
   }

   public static double moveUD(double current, double end, float smoothSpeed, float minSpeed) {
      deltaTimeUD = OrangeEngine.deltaTimeMills;
      smoothSpeed = (smoothSpeed / (1f / deltaTimeUD * 1000f));//0.12 8 0.035 2.5
      minSpeed = (minSpeed / (1f / deltaTimeUD * 1000f));
      boolean larger = end > current;
      if (smoothSpeed < 0.0f) {
         smoothSpeed = 0.0f;
      } else if (smoothSpeed > 1.0f) {
         smoothSpeed = 1.0f;
      }
      if (minSpeed < 0.0f) {
         minSpeed = 0.0f;
      } else if (minSpeed > 1.0f) {
         minSpeed = 1.0f;
      }
      double movement = (end - current) * smoothSpeed;
      if (movement > 0) {
         movement = Math.max(minSpeed, movement);
         movement = Math.min(end - current, movement);
      } else if (movement < 0) {
         movement = Math.min(-minSpeed, movement);
         movement = Math.max(end - current, movement);
      }
      if (larger) {
         if (end <= current + movement) {
            return end;
         }
      } else {
         if (end >= current + movement) {
            return end;
         }
      }
      return current + movement;
   }

   public static float getAnimationState(float animation, float finalState, float speed) {
      deltaTimeUD = OrangeEngine.deltaTimeMills;
      float add = (speed / (1f / deltaTimeUD * 1000f));
      if (animation < finalState) {
         if (animation + add < finalState) {
            animation += add;
         } else {
            animation = finalState;
         }
      } else if (animation - add > finalState) {
         animation -= add;
      } else {
         animation = finalState;
      }
      return animation;
   }

   public static double getAnimationState(double animation, double finalState, double speed) {
      float add = (float) ((0.1 * speed) / (1f / deltaTimeUD * 1000f));
      if (animation < finalState) {
         if (animation + add < finalState) {
            animation += add;
         } else {
            animation = finalState;
         }
      } else if (animation - add > finalState) {
         animation -= add;
      } else {
         animation = finalState;
      }
      return animation;
   }

   public static float calculateCompensation(float target, float current, long delta, int speed) {
      float diff = current - target;
      if (delta < 1L) {
         delta = 1L;
      }

      double xD;
      if (diff > (float) speed) {
         xD = (double) ((long) speed * delta / 16L) < 0.25D ? 0.5D : (double) ((long) speed * delta / 16L);
         current = (float) ((double) current - xD);
         if (current < target) {
            current = target;
         }
      } else if (diff < (float) (-speed)) {
         xD = (double) ((long) speed * delta / 16L) < 0.25D ? 0.5D : (double) ((long) speed * delta / 16L);
         current = (float) ((double) current + xD);
         if (current > target) {
            current = target;
         }
      } else {
         current = target;
      }

      return current;
   }
}
