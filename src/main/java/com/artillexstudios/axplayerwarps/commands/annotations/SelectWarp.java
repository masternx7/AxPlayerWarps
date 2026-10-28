package com.artillexstudios.axplayerwarps.commands.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a warp parameter that should fall back to a warp selection dialog
 * when the provided warp name could not be found.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface SelectWarp {
}
